package com.example.livetvapp.database;
import android.content.Context;
import com.example.livetvapp.Channel;
import java.util.ArrayList;
import java.util.List;
public class ChannelRepository {
    private final DatabaseHelper databaseHelper;
    private final CacheManager cacheManager;
    private final AppDatabase database;
    private static final int PAGE_SIZE = 50;
    private static final int ARAMA_SAYFA_BOYUTU = 20;
    private static final int KATEGORI_SAYFA_BOYUTU = 50;
    public ChannelRepository(Context context) {
        this.databaseHelper = new DatabaseHelper(context);
        this.cacheManager   = new CacheManager(context);
        this.database       = AppDatabase.getInstance(context);
    }
    public void getChannelsPaged(String m3uName, int page, OnChannelsLoadedListener listener) {
        int offset = page * PAGE_SIZE;
        databaseHelper.getChannelsByM3U(m3uName, PAGE_SIZE, offset, new DatabaseHelper.OnChannelsLoadedListener() {
            @Override public void onChannelsLoaded(List<Channel> channels) { listener.onChannelsLoaded(channels, false); }
            @Override public void onError(Exception e)                      { listener.onError(e); }
        });
    }
    public void loadChannels(String m3uName, int page, OnChannelsLoadedListener listener) {
        List<Channel> cached = cacheManager.getChannelsFromMemory(m3uName, page);
        if (cached != null && !cached.isEmpty()) {
            listener.onChannelsLoaded(cached, true);
            return;
        }
        int offset = page * PAGE_SIZE;
        databaseHelper.getChannelsByM3U(m3uName, PAGE_SIZE, offset, new DatabaseHelper.OnChannelsLoadedListener() {
            @Override
            public void onChannelsLoaded(List<Channel> channels) {
                cacheManager.cacheChannelsToMemory(m3uName, page, channels);
                listener.onChannelsLoaded(channels, false);
            }
            @Override public void onError(Exception e) { listener.onError(e); }
        });
    }
    public void loadAllChannels(String m3uName, OnChannelsLoadedListener listener) {
        databaseHelper.getAllChannelsByM3U(m3uName, new DatabaseHelper.OnChannelsLoadedListener() {
            @Override public void onChannelsLoaded(List<Channel> channels) { listener.onChannelsLoaded(channels, false); }
            @Override public void onError(Exception e)                      { listener.onError(e); }
        });
    }
    public void loadAllChannelsByType(String m3uName, String contentType, OnChannelsLoadedListener listener) {
        databaseHelper.getAllChannelsByType(m3uName, contentType, new DatabaseHelper.OnChannelsLoadedListener() {
            @Override public void onChannelsLoaded(List<Channel> channels) { listener.onChannelsLoaded(channels, false); }
            @Override public void onError(Exception e)                      { listener.onError(e); }
        });
    }
    public void loadChannelsByTypeAndCategory(String m3uName, String contentType, String category,
                                              OnChannelsLoadedListener listener) {
        databaseHelper.getChannelsByTypeAndCategory(m3uName, contentType, category,
                new DatabaseHelper.OnChannelsLoadedListener() {
                    @Override public void onChannelsLoaded(List<Channel> channels) { listener.onChannelsLoaded(channels, false); }
                    @Override public void onError(Exception e)                      { listener.onError(e); }
                });
    }
    public void loadChannelsByTypeAndCategoryPaged(String m3uName, String contentType,
                                                   String category, int offset,
                                                   OnChannelsLoadedListener listener) {
        databaseHelper.execute(() -> {
            try {
                List<Channel> sayfa;
                if (m3uName == null || m3uName.isEmpty()) {
                    sayfa = database.channelDao()
                            .getChannelsByTypeAndCategoryAllM3UPaged(contentType, category, PAGE_SIZE, offset);
                } else {
                    sayfa = database.channelDao()
                            .getChannelsByTypeAndCategoryPaged(m3uName, contentType, category, PAGE_SIZE, offset);
                }
                if (listener != null) listener.onChannelsLoaded(sayfa, false);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void loadChannelsByCategory(String category, OnChannelsLoadedListener listener) {
        databaseHelper.execute(() -> {
            try {
                List<Channel> live   = database.channelDao().getChannelsByTypeAndCategoryAllM3U("LIVE",   category);
                List<Channel> movie  = database.channelDao().getChannelsByTypeAndCategoryAllM3U("MOVIE",  category);
                List<Channel> series = database.channelDao().getChannelsByTypeAndCategoryAllM3U("SERIES", category);
                List<Channel> tumKanallar = new java.util.ArrayList<>();
                tumKanallar.addAll(live);
                tumKanallar.addAll(movie);
                tumKanallar.addAll(series);
                java.util.Collections.sort(tumKanallar,
                        (a, b) -> Integer.compare(a.getPosition(), b.getPosition()));
                if (listener != null) listener.onChannelsLoaded(tumKanallar, false);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void loadChannelsByM3UAndCategory(String m3uName, String category,
                                             OnChannelsLoadedListener listener) {
        databaseHelper.execute(() -> {
            try {
                List<Channel> tumKanallar = new java.util.ArrayList<>();
                String[] tipler = {"LIVE", "MOVIE", "SERIES"};
                for (String tip : tipler) {
                    List<Channel> kanallar;
                    if (m3uName == null || m3uName.isEmpty()) {
                        kanallar = database.channelDao()
                                .getChannelsByTypeAndCategoryAllM3U(tip, category);
                    } else {
                        kanallar = database.channelDao()
                                .getChannelsByTypeAndCategory(m3uName, tip, category);
                    }
                    tumKanallar.addAll(kanallar);
                }
                java.util.Collections.sort(tumKanallar,
                        (a, b) -> Integer.compare(a.getPosition(), b.getPosition()));
                if (listener != null) listener.onChannelsLoaded(tumKanallar, false);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void loadCategoriesByType(String m3uName, String contentType, OnCategoriesLoadedListener listener) {
        databaseHelper.getCategoriesByType(m3uName, contentType, new DatabaseHelper.OnCategoriesLoadedListener() {
            @Override public void onCategoriesLoaded(List<String> categories) { listener.onCategoriesLoaded(categories); }
            @Override public void onError(Exception e)                         { listener.onError(e); }
        });
    }
    public void loadCategoriesWithSource(String m3uName, String contentType,
                                         OnKategoriItemsLoadedListener listener) {
        databaseHelper.getCategoriesWithSource(m3uName, contentType,
                new DatabaseHelper.OnKategoriItemsLoadedListener() {
                    @Override
                    public void onKategoriItemsLoaded(List<KategoriItem> items) {
                        listener.onKategoriItemsLoaded(items);
                    }
                    @Override
                    public void onError(Exception e) {
                        listener.onError(e);
                    }
                });
    }
    public void loadCategoriesWithSourcePaged(String m3uName, String contentType,
                                              int offset,
                                              OnKategoriItemsLoadedListener listener) {
        databaseHelper.getCategoriesWithSourcePaged(
                m3uName, contentType, KATEGORI_SAYFA_BOYUTU, offset,
                new DatabaseHelper.OnKategoriItemsLoadedListener() {
                    @Override
                    public void onKategoriItemsLoaded(List<KategoriItem> items) {
                        listener.onKategoriItemsLoaded(items);
                    }
                    @Override
                    public void onError(Exception e) {
                        listener.onError(e);
                    }
                });
    }
    public void loadAllCategoriesWithTypes(OnCategoryMapLoadedListener listener) {
        databaseHelper.execute(() -> {
            try {
                java.util.LinkedHashMap<String, String> katTurMap = new java.util.LinkedHashMap<>();
                String[] tipler = {"LIVE", "MOVIE", "SERIES"};
                for (String tip : tipler) {
                    List<String> kategoriler = database.channelDao().getCategoriesByTypeAllM3U(tip);
                    for (String kat : kategoriler) {
                        if (!katTurMap.containsKey(kat)) {
                            katTurMap.put(kat, tip);
                        }
                    }
                }
                if (listener != null) listener.onCategoryMapLoaded(katTurMap);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void loadCategoriesByM3U(OnM3UCategoryMapLoadedListener listener) {
        databaseHelper.execute(() -> {
            try {
                List<Channel> distinctList = database.channelDao().getDistinctM3UCategoryList();
                java.util.LinkedHashMap<String, java.util.LinkedHashMap<String, String>> m3uKatMap
                        = new java.util.LinkedHashMap<>();
                for (Channel c : distinctList) {
                    String m3u = c.getSourceName();
                    String kat = c.getCategory();
                    String tip = c.getContentType() != null ? c.getContentType() : "LIVE";
                    if (m3u == null || m3u.isEmpty()) m3u = "Varsayılan";
                    if (kat == null || kat.isEmpty()) kat = "Genel";
                    if (!m3uKatMap.containsKey(m3u)) {
                        m3uKatMap.put(m3u, new java.util.LinkedHashMap<>());
                    }
                    if (!m3uKatMap.get(m3u).containsKey(kat)) {
                        m3uKatMap.get(m3u).put(kat, tip);
                    }
                }
                if (listener != null) listener.onM3UCategoryMapLoaded(m3uKatMap);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void loadAllUrls(OnUrlListLoadedListener listener) {
        databaseHelper.execute(() -> {
            try {
                List<String> urls = database.channelDao().getAllUrls();
                if (listener != null) listener.onUrlListLoaded(urls);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void searchChannels(String query, OnChannelsLoadedListener listener) {
        databaseHelper.execute(() -> {
            try {
                List<Channel> results = database.channelDao().searchChannelsAllTypes(query);
                if (listener != null) listener.onChannelsLoaded(results, false);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void searchChannelsByTypePaged(String query, String contentType, int offset,
                                          OnChannelsLoadedListener listener) {
        databaseHelper.execute(() -> {
            try {
                List<Channel> results = database.channelDao()
                        .searchChannelsByTypePaged(query, contentType, ARAMA_SAYFA_BOYUTU, offset);
                if (listener != null) listener.onChannelsLoaded(results, false);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void loadChannelByUrl(String url, OnChannelLoadedListener listener) {
        databaseHelper.getChannelByUrl(url, new DatabaseHelper.OnChannelLoadedListener() {
            @Override public void onChannelLoaded(Channel channel) { listener.onChannelLoaded(channel); }
            @Override public void onError(Exception e)              { listener.onError(e); }
        });
    }
    public void loadChannelsByUrls(List<String> urls, OnChannelsLoadedListener listener) {
        if (urls == null || urls.isEmpty()) {
            if (listener != null) listener.onChannelsLoaded(new ArrayList<>(), false);
            return;
        }
        databaseHelper.execute(() -> {
            try {
                List<Channel> sonuc = database.channelDao().getChannelsByUrls(urls);
                if (listener != null) listener.onChannelsLoaded(sonuc, false);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void insertChannels(List<Channel> channels, OnCompleteListener listener) {
        databaseHelper.insertChannels(channels, new DatabaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess() {
                cacheManager.clearMemoryCache();
                listener.onSuccess();
            }
            @Override public void onError(Exception e) { listener.onError(e); }
        });
    }
    public void deleteChannelsByM3U(String m3uName, OnCompleteListener listener) {
        databaseHelper.deleteChannelsByM3U(m3uName, new DatabaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess() {
                cacheManager.clearMemoryCacheForM3U(m3uName);
                listener.onSuccess();
            }
            @Override public void onError(Exception e) { listener.onError(e); }
        });
    }
    public void getChannelCount(String m3uName, OnCountLoadedListener listener) {
        databaseHelper.getChannelCount(m3uName, new DatabaseHelper.OnCountLoadedListener() {
            @Override public void onCountLoaded(int count) { listener.onCountLoaded(count); }
            @Override public void onError(Exception e)      { listener.onError(e); }
        });
    }
    public interface OnUrlListLoadedListener {
        void onUrlListLoaded(List<String> urls);
        void onError(Exception e);
    }
    public interface OnChannelsLoadedListener {
        void onChannelsLoaded(List<Channel> channels, boolean fromCache);
        void onError(Exception e);
    }
    public interface OnChannelLoadedListener {
        void onChannelLoaded(Channel channel);
        void onError(Exception e);
    }
    public interface OnCategoriesLoadedListener {
        void onCategoriesLoaded(List<String> categories);
        void onError(Exception e);
    }
    public interface OnKategoriItemsLoadedListener {
        void onKategoriItemsLoaded(List<KategoriItem> items);
        void onError(Exception e);
    }
    public interface OnCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }
    public interface OnCountLoadedListener {
        void onCountLoaded(int count);
        void onError(Exception e);
    }
    public interface OnCategoryMapLoadedListener {
        void onCategoryMapLoaded(java.util.LinkedHashMap<String, String> kategoriTurMap);
        void onError(Exception e);
    }
    public interface OnM3UCategoryMapLoadedListener {
        void onM3UCategoryMapLoaded(
                java.util.LinkedHashMap<String, java.util.LinkedHashMap<String, String>> m3uKatMap);
        void onError(Exception e);
    }
    public void loadChannelByName(String name, OnChannelLoadedListener listener) {
        databaseHelper.execute(() -> {
            try {
                Channel channel = database.channelDao().getChannelByName(name);
                if (listener != null) listener.onChannelLoaded(channel);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public java.util.Map<String, Integer> kategoriIciSiralariHesaplaSync(List<Channel> channels) {
        java.util.Map<String, Integer> siraMap = new java.util.HashMap<>();
        if (channels == null) return siraMap;
        for (Channel ch : channels) {
            if (ch.getUrl() == null) continue;
            try {
                int sira = database.channelDao().getKategoriIciSira(
                        ch.getSourceName(), ch.getContentType(), ch.getCategory(), ch.getPosition());
                siraMap.put(ch.getUrl(), sira);
            } catch (Exception e) {
                System.out.println("⚠️ kategoriIciSiralariHesapla hata: " + e.getMessage());
            }
        }
        return siraMap;
    }
}