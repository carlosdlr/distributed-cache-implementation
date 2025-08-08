package com.jikkosoft.cache.impl;

import com.jikkosoft.cache.model.CacheUpdate;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DistributedCacheTest {

    static DistributedCache cache;

    @BeforeAll
    static void setUp() throws IOException {
        // Use an unused port and no peers for local tests
        cache = new DistributedCache(9999, List.of(), 60000, false); // 60 seconds TTL
    }

    @Test
    void testPutAndGet() {
        cache.put(new CacheUpdate("my-key", "value", new VectorClock()));
        Optional<String> value = cache.get("my-key");
        assertTrue(value.isPresent());
        assertEquals("value", value.get());
    }

    @Test
    void testGetMissingKey() {
        assertTrue(cache.get("missing").isEmpty());
    }

    @Test
    void testUpdateOverwritesValue() {
        var vectorClock1 = new VectorClock();
        vectorClock1.increment("node1");
        cache.put(new CacheUpdate("key", "v1", vectorClock1));

        var vectorClock2 = new VectorClock();
        vectorClock2.increment("node1");
        vectorClock2.increment("node2"); // Simulate an update from another node
        cache.put(new CacheUpdate("key", "v2", vectorClock2));

        assertEquals("v2", cache.get("key").orElse(null));
    }

    // For HTTP notification
    // @Test
    // void testNotifyOtherNodes() {
    //     // Set up a mock HTTP server as a peer and verify it receives the update
    // }
}