package com.example.livetvapp.stalker;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.livetvapp.Channel;
import com.example.livetvapp.database.AppDatabase;
import com.example.livetvapp.database.ChannelRepository;
import com.example.livetvapp.database.M3UItem;
import com.example.livetvapp.database.M3UManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StalkerManager {
    private static final String TAG = "StalkerManager";
    private final Context context;
    private final StalkerPortalDao portalDao;
    private final ChannelRepository channelRepo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public StalkerManager(Context context) {
        this.context     = context;
        this.portalDao   = AppDatabase.getInstance(context).stalkerPortalDao();
        this.channelRepo = new ChannelRepository(context);
    }

    public interface SyncListener {
        void onSuccess(int channelCount);
        void onError(String error);
    }

    public void addPortalAndSync(String name, String portalUrl, String mac, SyncListener listener) {
        executor.execute(() -> {
            try {
                StalkerPortal portal = new StalkerPortal(name, portalUrl, mac);
                long id = portalDao.insert(portal);
                portal.setId(id);
                syncPortalInternal(portal, listener);
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError("DB hatası: " + e.getMessage()));
            }
        });
    }

    private void syncPortalInternal(StalkerPortal portal, SyncListener listener) {
        try {

            String token = StalkerTokenCache.getInstance().getToken(portal.getId());
            if (token == null) {
                JSONObject handshake = StalkerApiClient.handshakeSync(portal.getPortalUrl(), portal.getMacAddress());
                JSONObject js = handshake.optJSONObject("js");
                if (js == null) {
                    mainHandler.post(() -> listener.onError("Handshake: 'js' alanı yok"));
                    return;
                }
                token = js.optString("token", "");
                if (token.isEmpty()) {
                    mainHandler.post(() -> listener.onError("Handshake: token boş"));
                    return;
                }
                long expiry = System.currentTimeMillis() + 3_600_000L;
                StalkerTokenCache.getInstance().put(portal.getId(), token, expiry);
                portal.setToken(token);
                portal.setTokenExpiry(expiry);
                portalDao.update(portal);
            }

            final String finalToken = token;
            List<Channel> allChannels = new ArrayList<>();
            String sourceName = "Stalker:" + portal.getName();


            allChannels.addAll(fetchLiveChannels(portal, finalToken, sourceName));







            if (allChannels.isEmpty()) {
                mainHandler.post(() -> listener.onError("Hiç içerik bulunamadı"));
                return;
            }

            Log.d(TAG, "Toplam içerik (ilk sayfalar): " + allChannels.size());

            AppDatabase db = AppDatabase.getInstance(context);
            db.channelDao().deleteBySourceName(sourceName);
            channelRepo.insertChannels(allChannels, new ChannelRepository.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    M3UItem m3uItem = new M3UItem(sourceName, "stalker:" + portal.getPortalUrl(),
                            "", allChannels.size(), false);
                    m3uItem.setHidden(false);
                    new M3UManager(context).addM3U(m3uItem);
                    portal.setLastSync(System.currentTimeMillis());
                    portalDao.update(portal);
                    mainHandler.post(() -> listener.onSuccess(allChannels.size()));
                }
                @Override
                public void onError(Exception e) {
                    mainHandler.post(() -> listener.onError("DB kayıt hatası: " + e.getMessage()));
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "syncPortalInternal hata: " + e.getMessage(), e);
            mainHandler.post(() -> listener.onError("Senkronizasyon hatası: " + e.getMessage()));
        }
    }


    private List<Channel> fetchLiveChannels(StalkerPortal portal, String token, String sourceName) {
        List<Channel> list = new ArrayList<>();
        try {
            java.util.Map<String, String> genreMap = new java.util.HashMap<>();
            try {
                JSONObject genresResp = StalkerApiClient.getGenresSync(
                        portal.getPortalUrl(), token, portal.getMacAddress());
                JSONArray genreArr = null;
                Object jsObj = genresResp.opt("js");
                if (jsObj instanceof JSONArray) {
                    genreArr = (JSONArray) jsObj;
                } else if (jsObj instanceof JSONObject) {
                    JSONObject jso = (JSONObject) jsObj;
                    genreArr = jso.optJSONArray("data");
                    if (genreArr == null) genreArr = jso.optJSONArray("genres");
                }
                if (genreArr != null) {
                    for (int i = 0; i < genreArr.length(); i++) {
                        JSONObject g = genreArr.optJSONObject(i);
                        if (g != null) {
                            String id   = g.optString("id", "");
                            String name = g.optString("title", g.optString("name", "Genel"));
                            if (!id.isEmpty()) genreMap.put(id, name);
                        }
                    }
                }
            } catch (Exception e) { Log.w(TAG, "LIVE kategoriler alınamadı: " + e.getMessage()); }

            JSONObject liveData = StalkerApiClient.getAllChannelsSync(
                    portal.getPortalUrl(), token, portal.getMacAddress());
            list = parseChannels(liveData, genreMap, sourceName,
                    StalkerApiClient.normalizeUrl(portal.getPortalUrl()), token, "LIVE");
            Log.d(TAG, "LIVE kanal sayısı: " + list.size());
        } catch (Exception e) {
            Log.e(TAG, "LIVE alınamadı: " + e.getMessage());
        }
        return list;
    }


    private List<Channel> fetchFirstPageOnly(StalkerPortal portal, String token,
                                             String sourceName, String type, String contentType) {
        List<Channel> list = new ArrayList<>();
        try {
            List<String[]> categories = fetchCategories(portal, token, type);
            if (categories.isEmpty()) {
                categories.add(new String[]{"*", "Genel"});
            }

            for (String[] cat : categories) {
                String catId   = cat[0];
                String catName = cat[1];


                JSONObject resp;
                if ("vod".equals(type)) {
                    resp = StalkerApiClient.getVodListSync(
                            portal.getPortalUrl(), token, portal.getMacAddress(), catId, 0);
                } else {
                    resp = StalkerApiClient.getSeriesListSync(
                            portal.getPortalUrl(), token, portal.getMacAddress(), catId, 0);
                }

                JSONObject js = resp.optJSONObject("js");
                JSONArray data = null;
                int totalItems   = 0;
                int maxPageItems = 14;
                if (js != null) {
                    data         = js.optJSONArray("data");
                    totalItems   = js.optInt("total_items", 0);
                    maxPageItems = js.optInt("max_page_items", 14);
                    if (maxPageItems <= 0) maxPageItems = 14;
                }
                if (data == null) data = resp.optJSONArray("data");
                if (data == null || data.length() == 0) continue;

                int totalPages = (int) Math.ceil((double) totalItems / maxPageItems);
                if (totalPages <= 0) totalPages = 1;


                StalkerCategoryProgressDao progressDao =
                        AppDatabase.getInstance(context).stalkerCategoryProgressDao();
                StalkerCategoryProgress progress =
                        progressDao.getByM3uAndCategory(sourceName, catName);
                if (progress == null) {
                    progress = new StalkerCategoryProgress(sourceName, catName);
                }
                progress.setCategoryId(catId);
                progress.setLastFetchedPage(0);
                progress.setTotalPages(totalPages);
                progress.setTotalItems(totalItems);
                progress.setLastSyncTime(System.currentTimeMillis());
                progressDao.insert(progress);


                int basePosition = list.size();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.optJSONObject(i);
                    if (item == null) continue;
                    String name      = item.optString("name", "İsimsiz");
                    String cmd       = item.optString("cmd", "");
                    String id        = item.optString("id", "");
                    String logo      = item.optString("screenshot_path", item.optString("logo", ""));
                    if (cmd.isEmpty() && id.isEmpty()) continue;
                    String streamUrl = StalkerApiClient.extractUrl(cmd);
                    if (streamUrl.isEmpty()) {


                        String itemId = item.optString("id", "");
                        if (!itemId.isEmpty()) {
                            if ("vod".equals(type)) {
                                streamUrl = "vod:" + itemId;
                            } else if ("series".equals(type)) {
                                streamUrl = "series:" + itemId;
                            }
                        }
                    }
                    if (streamUrl.isEmpty()) continue;

                    Channel channel = new Channel(name, streamUrl, catName, logo, sourceName);
                    channel.setContentType(contentType);
                    channel.setPosition(basePosition + i);
                    list.add(channel);
                }
                Log.d(TAG, type + " kategori=" + catName + " catId=" + catId
                        + " → " + data.length() + " içerik (toplam=" + totalItems
                        + ", sayfa=" + totalPages + ")");
            }
        } catch (Exception e) {
            Log.e(TAG, type + " ilk sayfa alınamadı: " + e.getMessage(), e);
        }
        return list;
    }


    private List<String[]> fetchCategories(StalkerPortal portal, String token, String type) {
        List<String[]> result = new ArrayList<>();
        try {
            JSONObject resp;
            if ("vod".equals(type)) {
                resp = StalkerApiClient.getVodCategoriesSync(
                        portal.getPortalUrl(), token, portal.getMacAddress());
            } else {
                resp = StalkerApiClient.getSeriesCategoriesSync(
                        portal.getPortalUrl(), token, portal.getMacAddress());
            }
            JSONArray arr = null;
            Object jsObj = resp.opt("js");
            if (jsObj instanceof JSONArray) {
                arr = (JSONArray) jsObj;
            } else if (jsObj instanceof JSONObject) {
                JSONObject jso = (JSONObject) jsObj;
                arr = jso.optJSONArray("data");
                if (arr == null) arr = jso.optJSONArray("categories");
            }
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject cat = arr.optJSONObject(i);
                    if (cat == null) continue;
                    String id   = cat.optString("id", "");
                    String name = cat.optString("title", cat.optString("name", "Genel"));
                    if (!id.isEmpty() && !"*".equals(id)) {
                        result.add(new String[]{id, name});
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, type + " kategori alınamadı: " + e.getMessage());
        }
        return result;
    }


    private List<Channel> parseChannels(JSONObject channelsData,
                                        java.util.Map<String, String> genreMap,
                                        String sourceName,
                                        String portalBase,
                                        String token,
                                        String contentType) throws Exception {
        List<Channel> list = new ArrayList<>();
        JSONArray channelsJson = null;
        JSONObject js = channelsData.optJSONObject("js");
        if (js != null) {
            channelsJson = js.optJSONArray("data");
            if (channelsJson == null) channelsJson = js.optJSONArray("channels");
        }
        if (channelsJson == null) channelsJson = channelsData.optJSONArray("data");
        if (channelsJson == null || channelsJson.length() == 0) return list;

        for (int i = 0; i < channelsJson.length(); i++) {
            JSONObject ch = channelsJson.optJSONObject(i);
            if (ch == null) continue;
            String name    = ch.optString("name", "İsimsiz");
            String cmd     = ch.optString("cmd", "");
            String id      = ch.optString("id", ch.optString("stream_id", ""));
            String logo    = ch.optString("logo", "");
            String genreId = ch.optString("tv_genre_id", "");
            String category = genreMap.containsKey(genreId) ? genreMap.get(genreId)
                    : ch.optString("tv_genre", "Genel");

            if (cmd.isEmpty() && id.isEmpty()) continue;
            String streamUrl = StalkerApiClient.extractUrl(cmd);
            if (streamUrl.isEmpty() && !id.isEmpty()) {
                try {
                    String builtCmd = "ffmpeg http://localhost/ch/" + id;
                    streamUrl = StalkerApiClient.createLinkSync(portalBase, token, "", builtCmd);
                    streamUrl = StalkerApiClient.extractUrl(streamUrl);
                } catch (Exception e) { Log.w(TAG, "createLink başarısız ID:" + id); }
            }
            if (streamUrl.isEmpty()) continue;
            Channel channel = new Channel(name, streamUrl, category, logo, sourceName);
            channel.setContentType(contentType);
            channel.setPosition(list.size());
            list.add(channel);
        }
        return list;
    }
}