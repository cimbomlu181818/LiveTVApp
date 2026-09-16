package com.example.livetvapp.database;
import com.example.livetvapp.Adapter.Dizi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class DiziCache {
    private static DiziCache instance;
    private final Map<String, List<Dizi>> cache      = new HashMap<>();
    private final Map<String, Long>       timestamps = new HashMap<>();
    private static final long TTL_MS = 10 * 60 * 1000L;
    private DiziCache() {}
    public static synchronized DiziCache getInstance() {
        if (instance == null) instance = new DiziCache();
        return instance;
    }
    public synchronized void put(String kategori, List<Dizi> diziler) {
        if (kategori == null || diziler == null) return;
        cache.put(kategori, new ArrayList<>(diziler));
        timestamps.put(kategori, System.currentTimeMillis());
        System.out.println("✅ DiziCache PUT [" + kategori + "]: " + diziler.size() + " dizi");
    }
    public synchronized List<Dizi> get(String kategori) {
        if (kategori == null) return null;
        Long ts = timestamps.get(kategori);
        if (ts == null) return null;
        if (System.currentTimeMillis() - ts > TTL_MS) {
            cache.remove(kategori);
            timestamps.remove(kategori);
            System.out.println("⏰ DiziCache EXPIRED [" + kategori + "]");
            return null;
        }
        List<Dizi> result = cache.get(kategori);
        if (result != null) {
            System.out.println("⚡ DiziCache HIT [" + kategori + "]: " + result.size() + " dizi");
            return new ArrayList<>(result);
        }
        return null;
    }
    public synchronized boolean has(String kategori) {
        return get(kategori) != null;
    }
    public synchronized void invalidate(String kategori) {
        cache.remove(kategori);
        timestamps.remove(kategori);
        System.out.println("🗑️ DiziCache INVALIDATE [" + kategori + "]");
    }
    public synchronized void invalidateAll() {
        cache.clear();
        timestamps.clear();
        System.out.println("🗑️ DiziCache INVALIDATE ALL");
    }
}