package com.jikkosoft.cache.model;

public record CacheUpdate(String key, String value) {
    // Record automatically provides constructor, getters, equals, hashCode, and toString
}