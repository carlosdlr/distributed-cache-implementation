package com.jikkosoft.cache.impl;

import java.util.HashMap;
import java.util.Map;

public class VectorClock {
    private Map<String, Integer> timestamps = new HashMap<>();

    public void increment(String nodeId) {
        timestamps.compute(nodeId, (k, v) -> v == null ? 1 : v + 1);
    }

    public boolean isNewerThan(VectorClock other) {
        boolean hasGreater = false;
        for (Map.Entry<String, Integer> entry : timestamps.entrySet()) {
            Integer otherValue = other.timestamps.get(entry.getKey());
            if (otherValue != null && entry.getValue() < otherValue) {
                return false;
            }
            if (otherValue == null || entry.getValue() > otherValue) {
                hasGreater = true;
            }
        }
        return hasGreater;
    }
}