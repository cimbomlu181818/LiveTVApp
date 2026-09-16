package com.example.livetvapp.stalker;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.livetvapp.Channel;
import com.example.livetvapp.database.AppDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class StalkerPageFetcher {

    private static final String TAG = "StalkerPageFetcher";

    
    public interface PageFetchCallback {
        
        void onResult(List<Channel> channels, boolean isLastPage);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    
    private final Set<String> inFlight = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public StalkerPageFetcher(Context context) {
        this.context = context.getApplicationContext();
    }

    
    public void fetchNextPageIfAvailable(String contentType,
                                         String categoryName,
                                         PageFetchCallback callback) {
        if (categoryName == null || categoryName.isEmpty()) {
            mainHandler.post(() -> callback.onResult(new ArrayList<>(), true));
            return;
        }

        String lockKey = contentType + "|" + categoryName;
        if (!inFlight.add(lockKey)) {
            
            Log.d(TAG, "Zaten yükleniyor: " + lockKey);
            mainHandler.post(() -> callback.onResult(new ArrayList<>(), false));
            return;
        }

        executor.execute(() -> {
            try {
                doFetch(contentType, categoryName, lockKey, callback);
            } catch (Exception e) {
                Log.e(TAG, "fetchNextPageIfAvailable hata: " + e.getMessage(), e);
                inFlight.remove(lockKey);
                mainHandler.post(() -> callback.onResult(new ArrayList<>(), true));
            }
        });
    }

    private void doFetch(String contentType, String categoryName,
                         String lockKey, PageFetchCallback callback) throws Exception {

        AppDatabase db = AppDatabase.getInstance(context);
        StalkerCategoryProgressDao progressDao = db.stalkerCategoryProgressDao();

        
        List<StalkerCategoryProgress> progressList = progressDao.getByCategory(categoryName);
        if (progressList == null || progressList.isEmpty()) {
            Log.d(TAG, "İlerleme kaydı yok: " + categoryName);
            inFlight.remove(lockKey);
            mainHandler.post(() -> callback.onResult(new ArrayList<>(), true));
            return;
        }

        List<Channel> allNew = new ArrayList<>();
        boolean anyMore = false;

        for (StalkerCategoryProgress progress : progressList) {
            int lastFetched = progress.getLastFetchedPage();
            int totalPages  = progress.getTotalPages();

            if (lastFetched >= totalPages - 1) {
                Log.d(TAG, "Tüm sayfalar alınmış: " + progress.getM3uName()
                        + "/" + categoryName + " (" + lastFetched + "/" + (totalPages - 1) + ")");
                continue; 
            }

            anyMore = true;
            int nextPage = lastFetched + 1;

            
            String portalName = progress.getM3uName().startsWith("Stalker:")
                    ? progress.getM3uName().substring("Stalker:".length())
                    : progress.getM3uName();

            List<StalkerPortal> portals = db.stalkerPortalDao().getActivePortals();
            StalkerPortal portal = null;
            for (StalkerPortal p : portals) {
                if (portalName.equals(p.getName())) { portal = p; break; }
            }
            if (portal == null) {
                Log.w(TAG, "Portal bulunamadı: " + portalName);
                continue;
            }

            
            String token = StalkerTokenCache.getInstance().getToken(portal.getId());
            if (token == null || token.isEmpty()) {
                Log.d(TAG, "Token yok, handshake yapılıyor: " + portalName);
                try {
                    JSONObject hs = StalkerApiClient.handshakeSync(
                            portal.getPortalUrl(), portal.getMacAddress());
                    JSONObject js = hs.optJSONObject("js");
                    if (js != null) token = js.optString("token", "");
                    if (token != null && !token.isEmpty()) {
                        long expiry = System.currentTimeMillis() + 3_600_000L;
                        StalkerTokenCache.getInstance().put(portal.getId(), token, expiry);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Handshake hatası: " + e.getMessage());
                    continue;
                }
            }
            if (token == null || token.isEmpty()) {
                Log.w(TAG, "Token alınamadı: " + portalName);
                continue;
            }

            
            String catId = progress.getCategoryId();
            if (catId == null || catId.isEmpty()) catId = "*";

            JSONObject resp;
            String apiType;
            if ("MOVIE".equals(contentType)) {
                apiType = "vod";
                resp = StalkerApiClient.getVodListSync(
                        portal.getPortalUrl(), token, portal.getMacAddress(), catId, nextPage);
            } else {
                apiType = "series";
                resp = StalkerApiClient.getSeriesListSync(
                        portal.getPortalUrl(), token, portal.getMacAddress(), catId, nextPage);
            }

            JSONObject jsResp = resp.optJSONObject("js");
            JSONArray data = null;
            int maxPageItems = 14;
            int totalItems   = progress.getTotalItems();
            if (jsResp != null) {
                data         = jsResp.optJSONArray("data");
                maxPageItems = jsResp.optInt("max_page_items", 14);
                if (jsResp.has("total_items")) totalItems = jsResp.optInt("total_items", totalItems);
            }
            if (data == null) data = resp.optJSONArray("data");
            if (data == null || data.length() == 0) {
                Log.d(TAG, "Sayfa boş döndü: " + portalName + "/" + categoryName
                        + " sayfa=" + nextPage);
                
                progress.setLastFetchedPage(progress.getTotalPages() - 1);
                progress.setLastSyncTime(System.currentTimeMillis());
                progressDao.update(progress);
                continue;
            }

            
            String sourceName = progress.getM3uName();
            int insertedCount = 0;
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null) continue;
                String name      = item.optString("name", "İsimsiz");
                String cmd       = item.optString("cmd", "");
                String id        = item.optString("id", "");
                String logo      = item.optString("screenshot_path", item.optString("logo", ""));
                if (cmd.isEmpty() && id.isEmpty()) continue;
                String streamUrl = StalkerApiClient.extractUrl(cmd);
                if (streamUrl.isEmpty()) continue;

                Channel ch = new Channel(name, streamUrl, categoryName, logo, sourceName);
                ch.setContentType(contentType);
                ch.setPosition(i);
                allNew.add(ch);
                insertedCount++;
            }

            
            if (insertedCount > 0) {
                try {
                    db.channelDao().insertAll(allNew.subList(allNew.size() - insertedCount, allNew.size()));
                } catch (Exception e) {
                    Log.e(TAG, "Kanal kayıt hatası: " + e.getMessage());
                }
            }

            
            progress.setLastFetchedPage(nextPage);
            if (maxPageItems > 0 && totalItems > 0) {
                int recalcPages = (int) Math.ceil((double) totalItems / maxPageItems);
                progress.setTotalPages(Math.max(recalcPages, progress.getTotalPages()));
            }
            progress.setTotalItems(totalItems);
            progress.setLastSyncTime(System.currentTimeMillis());
            progressDao.update(progress);

            Log.d(TAG, "✅ Sayfa yüklendi: " + sourceName + "/" + categoryName
                    + " sayfa=" + nextPage + " +" + insertedCount
                    + " öğe (toplam=" + totalItems + ")");
        }

        final List<Channel> result = new ArrayList<>(allNew);
        final boolean lastPage = !anyMore || allNew.isEmpty();

        inFlight.remove(lockKey);
        mainHandler.post(() -> callback.onResult(result, lastPage));
    }

    
    public void shutdown() {
        executor.shutdownNow();
    }
}