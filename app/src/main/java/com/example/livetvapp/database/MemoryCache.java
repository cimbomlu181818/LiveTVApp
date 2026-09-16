package com.example.livetvapp.database;
import android.util.LruCache;
import com.example.livetvapp.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class MemoryCache {
    private static final int MAX_CACHE_SIZE = 50; 
    private final LruCache<String, List<Channel>> cache;
    public MemoryCache() {
        this.cache = new LruCache<String, List<Channel>>(MAX_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, List<Channel> value) {
                return 1; 
            }
        };
    }
    public void put(String key, List<Channel> channels) {
        if (key != null && channels != null) {
            cache.put(key, new ArrayList<>(channels));
        }
    }
    public List<Channel> get(String key) {
        if (key == null) return null;
        List<Channel> cached = cache.get(key);
        return cached != null ? new ArrayList<>(cached) : null;
    }
    public void clear() {
        cache.evictAll();
    }
    public void clearByPrefix(String prefix) {
        Map<String, List<Channel>> snapshot = cache.snapshot();
        for (String key : snapshot.keySet()) {
            if (key.startsWith(prefix)) {
                cache.remove(key);
            }
        }
    }
    public void remove(String key) {
        cache.remove(key);
    }
    public int size() {
        return cache.size();
    }
}