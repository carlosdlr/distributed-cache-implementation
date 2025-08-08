package com.jikkosoft.cache.model;

import com.fasterxml.jackson.annotation.*;
import com.jikkosoft.cache.impl.*;

public record CacheUpdate(String key, String value, @JsonIgnore VectorClock vectorClock) {
    // Record automatically provides constructor, getters, equals, hashCode, and toString
}