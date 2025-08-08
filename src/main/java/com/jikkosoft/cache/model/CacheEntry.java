package com.jikkosoft.cache.model;

public record CacheEntry(String value, long timestamp) {
    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - timestamp > ttlMillis;
    }
}
