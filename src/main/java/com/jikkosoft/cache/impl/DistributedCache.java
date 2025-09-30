package com.jikkosoft.cache.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jikkosoft.cache.model.*;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public class DistributedCache {
    private record PeerNotification(String peer, CacheUpdate update) {}

    private final String nodeId = java.util.UUID.randomUUID().toString();
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private final List<String> peerNodes;
    private final int port;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService cleanupExecutor;
    private final long ttlMillis;
    private final boolean isLeader;
    private final VectorClock vectorClock = new VectorClock();


    private static final Logger logger = LoggerFactory.getLogger(DistributedCache.class);

    public DistributedCache(int port, List<String> peerNodes, long ttlMillis, boolean isLeader) throws IOException {
        logger.info("Initializing DistributedCache on port {} with {} peer nodes", port, peerNodes.size());
        this.port = port;
        this.peerNodes = peerNodes;
        this.ttlMillis = ttlMillis;
        this.isLeader = peerNodes.isEmpty() || isLeader;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        startHttpServer();
        scheduleCleanup();
    }

    private void scheduleCleanup() {
        cleanupExecutor.scheduleAtFixedRate(
                this::evictExpiredEntries,
                ttlMillis / 2,
                ttlMillis / 2,
                TimeUnit.MILLISECONDS
        );
    }


    private void evictExpiredEntries() {
        int sizeBefore = localCache.size();
        localCache.entrySet().removeIf(entry -> entry.getValue().isExpired(ttlMillis));
        int evicted = sizeBefore - localCache.size();
        if (evicted > 0) {
            logger.info("Evicted {} expired entries from cache", evicted);
        }
    }

    private void startHttpServer() throws IOException {
        var server = HttpServer.create(new InetSocketAddress(port), 0);

        // Map endpoints with clear RESTful paths
        server.createContext("/update", this::handleUpdateRequest);
        server.createContext("/get", this::handleGetRequest);
        server.createContext("/notify", this::handleNotifyRequest);
        // Set thread pool for handling requests
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
    }

    private void handleNotifyRequest(com.sun.net.httpserver.HttpExchange exchange) {
        try {
            logger.info("Received notification request from {}", exchange.getRemoteAddress());
            var requestBody = exchange.getRequestBody().readAllBytes();
            var update = objectMapper.readValue(requestBody, CacheUpdate.class);
            handleUpdate(update);
            exchange.sendResponseHeaders(200, 0);
        } catch (IOException e) {
            logger.error("Error handling notification request", e);
            try {
                exchange.sendResponseHeaders(500, 0);
            } catch (IOException ignored) {}
        } finally {
            exchange.close();
        }
    }

    private void handleUpdateRequest(com.sun.net.httpserver.HttpExchange exchange) {
        try {
            logger.info("Received update request from {}", exchange.getRemoteAddress());
            var requestBody = exchange.getRequestBody().readAllBytes();
            var update = objectMapper.readValue(requestBody, CacheUpdate.class);
            put(update);
            exchange.sendResponseHeaders(200, 0);
            logger.info("Cache update processed: key={}", update.key());
        } catch (IOException e) {
            logger.error("Error handling update request", e);
            try {
                exchange.sendResponseHeaders(500, 0);
            } catch (IOException ignored) {}
        } finally {
            exchange.close();
        }
    }

    private void handleGetRequest(com.sun.net.httpserver.HttpExchange exchange) {
        try {
            logger.info("Received GET request from {}", exchange.getRemoteAddress());
            if ("GET".equals(exchange.getRequestMethod())) {
                var query = exchange.getRequestURI().getQuery();
                String key = null;
                if (query != null && query.startsWith("key=")) {
                    key = query.substring(4);
                }
                logger.debug("Requested key: {}", key);
                String value = get(key).orElse(null);
                if (value != null) {
                    logger.info("Cache hit for key: {}", key);
                } else {
                    logger.info("Cache miss for key: {}", key);
                }
                byte[] response = value != null ? value.getBytes() : new byte[0];
                exchange.sendResponseHeaders(value != null ? 200 : 404, response.length);
                if (response.length > 0) {
                    exchange.getResponseBody().write(response);
                }
            } else {
                logger.warn("Unsupported HTTP method: {}", exchange.getRequestMethod());
                exchange.sendResponseHeaders(405, 0);
            }
        } catch (IOException e) {
            logger.error("Unsupported HTTP method: {}", exchange.getRequestMethod());
            try {
                exchange.sendResponseHeaders(500, 0);
            } catch (IOException ignored) {}
        } finally {
            exchange.close();
        }
    }

    public void put(CacheUpdate update) {
        logger.debug("Adding entry to cache: key={}", update.key());
        vectorClock.increment(nodeId);
        handleUpdate(update);
        if(isLeader) {
            notifyOtherNodes(update.key(), update.value());
        }
    }

    private void handleUpdate(CacheUpdate update) {
        CacheEntry existing = localCache.get(update.key());
        if (existing == null || update.vectorClock().isNewerThan(existing.vectorClock())) {
            logger.debug("Updating cache entry: key={}, value={}", update.key(), update.value());
            localCache.put(update.key(), update.value() != null ?
                    new CacheEntry(update.value(), System.currentTimeMillis(), update.vectorClock()) :
                    null);
        }
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(localCache.get(key))
                .filter(entry -> !entry.isExpired(ttlMillis))
                .map(CacheEntry::value);
    }

    private void notifyOtherNodes(String key, String value) {
        var update = new CacheUpdate(key, value, vectorClock);
        logger.info("Notifying peer nodes of update: key={}, value={}", key, value);
        peerNodes.stream()
                .map(peer -> new PeerNotification(peer, update))
                .forEach(this::sendNotification);
    }

    private void sendNotification(PeerNotification notification) {
        try {
            var url = new URL("http://" + notification.peer() + "/update");
            var conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            var jsonUpdate = objectMapper.writeValueAsString(notification.update());
            try (var os = conn.getOutputStream()) {
                os.write(jsonUpdate.getBytes());
            }

            var responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                logger.error("Failed to notify peer: {}, response code: {}",
                        notification.peer(), responseCode);
            }
            conn.disconnect();
        } catch (IOException e) {
            logger.error("Failed to notify peer: {}", notification.peer(), e);
        }
    }

    public void shutdown() {
        logger.info("Shutting down DistributedCache");
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("DistributedCache shutdown completed");
    }
}
