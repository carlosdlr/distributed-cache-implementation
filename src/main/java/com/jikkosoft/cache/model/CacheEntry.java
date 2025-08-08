package com.jikkosoft.cache.model;

import com.jikkosoft.cache.impl.*;

public record CacheEntry(String value, long timestamp, VectorClock vectorClock) {
    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - timestamp > ttlMillis;
    }
}
