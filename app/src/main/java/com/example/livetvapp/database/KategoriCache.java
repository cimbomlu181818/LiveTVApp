package com.example.livetvapp.database;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class KategoriCache {
    private static KategoriCache instance;
    private final Map<String, List<KategoriItem>> cache = new HashMap<>();
    private final Map<String, Long> timestamps = new HashMap<>();
    private static final long TTL_MS = 5 * 60 * 1000L;
    private KategoriCache() {}
    public static synchronized KategoriCache getInstance() {
        if (instance == null) instance = new KategoriCache();
        return instance;
    }
    public synchronized void put(String icerikTipi, List<KategoriItem> kategoriler) {
        if (icerikTipi == null || kategoriler == null) return;
        cache.put(icerikTipi, new ArrayList<>(kategoriler));
        timestamps.put(icerikTipi, System.currentTimeMillis());
        System.out.println("✅ KategoriCache PUT [" + icerikTipi + "]: " + kategoriler.size() + " kategori");
    }
    public synchronized List<KategoriItem> get(String icerikTipi) {
        if (icerikTipi == null) return null;
        Long ts = timestamps.get(icerikTipi);
        if (ts == null) return null;
        if (System.currentTimeMillis() - ts > TTL_MS) {
            cache.remove(icerikTipi);
            timestamps.remove(icerikTipi);
            System.out.println("⏰ KategoriCache EXPIRED [" + icerikTipi + "]");
            return null;
        }
        List<KategoriItem> result = cache.get(icerikTipi);
        if (result != null) {
            System.out.println("⚡ KategoriCache HIT [" + icerikTipi + "]: " + result.size() + " kategori");
            return new ArrayList<>(result);
        }
        return null;
    }
    public synchronized void invalidate(String icerikTipi) {
        cache.remove(icerikTipi);
        timestamps.remove(icerikTipi);
        System.out.println("🗑️ KategoriCache INVALIDATE [" + icerikTipi + "]");
    }
    public synchronized void invalidateAll() {
        cache.clear();
        timestamps.clear();
        System.out.println("🗑️ KategoriCache INVALIDATE ALL");
    }
    public synchronized boolean has(String icerikTipi) {
        return get(icerikTipi) != null;
    }
}