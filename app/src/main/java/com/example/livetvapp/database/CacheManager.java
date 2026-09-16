package com.example.livetvapp.database;
import android.content.Context;
import com.example.livetvapp.Channel;
import java.util.List;
public class CacheManager {
    private final MemoryCache memoryCache;
    private final Context context;
    public CacheManager(Context context) {
        this.context = context;
        this.memoryCache = new MemoryCache();
    }
    public List<Channel> getChannelsFromMemory(String m3uName, int page) {
        String key = generateKey(m3uName, page);
        return memoryCache.get(key);
    }
    public void cacheChannelsToMemory(String m3uName, int page, List<Channel> channels) {
        String key = generateKey(m3uName, page);
        memoryCache.put(key, channels);
    }
    public void clearMemoryCache() {
        memoryCache.clear();
    }
    public void clearMemoryCacheForM3U(String m3uName) {
        memoryCache.clearByPrefix(m3uName);
    }
    private String generateKey(String m3uName, int page) {
        return m3uName + "_page_" + page;
    }
}