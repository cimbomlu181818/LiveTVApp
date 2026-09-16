package com.example.livetvapp.stalker;

import java.util.concurrent.ConcurrentHashMap;

public class StalkerTokenCache {
    private static final StalkerTokenCache instance = new StalkerTokenCache();
    private final ConcurrentHashMap<Long, String> tokenMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> expiryMap = new ConcurrentHashMap<>();

    private StalkerTokenCache() {}

    public static StalkerTokenCache getInstance() { return instance; }

    public void put(long portalId, String token, long expiryMs) {
        tokenMap.put(portalId, token);
        expiryMap.put(portalId, expiryMs);
    }

    public String getToken(long portalId) {
        Long expiry = expiryMap.get(portalId);
        if (expiry == null || System.currentTimeMillis() > expiry) {
            tokenMap.remove(portalId);
            expiryMap.remove(portalId);
            return null;
        }
        return tokenMap.get(portalId);
    }

    public void clear(long portalId) {
        tokenMap.remove(portalId);
        expiryMap.remove(portalId);
    }
}