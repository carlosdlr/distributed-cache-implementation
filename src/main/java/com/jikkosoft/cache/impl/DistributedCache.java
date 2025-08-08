package com.jikkosoft.cache.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jikkosoft.cache.model.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DistributedCache {
    private record PeerNotification(String peer, CacheUpdate update) {}

    private final ConcurrentHashMap<String, String> localCache = new ConcurrentHashMap<>();
    private final List<String> peerNodes;
    private final int port;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DistributedCache(int port, List<String> peerNodes) throws IOException {
        this.port = port;
        this.peerNodes = peerNodes;
        startHttpServer();
    }

    private void startHttpServer() throws IOException {
        var server = HttpServer.create(new InetSocketAddress(port), 0);

        // Map endpoints with clear RESTful paths
        server.createContext("/update", this::handleUpdateRequest);
        server.createContext("/get", this::handleGetRequest);

        // Set thread pool for handling requests
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
    }

    private void handleUpdateRequest(com.sun.net.httpserver.HttpExchange exchange) {
        try {
            var requestBody = exchange.getRequestBody().readAllBytes();
            var update = objectMapper.readValue(requestBody, CacheUpdate.class);
            put(update.key(), update.value());
            exchange.sendResponseHeaders(200, 0);
        } catch (IOException e) {
            try {
                exchange.sendResponseHeaders(500, 0);
            } catch (IOException ignored) {}
        } finally {
            exchange.close();
        }
    }

    private void handleGetRequest(com.sun.net.httpserver.HttpExchange exchange) {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                var query = exchange.getRequestURI().getQuery();
                String key = null;
                if (query != null && query.startsWith("key=")) {
                    key = query.substring(4);
                }
                String value = get(key).orElse(null);
                byte[] response = value != null ? value.getBytes() : new byte[0];
                exchange.sendResponseHeaders(value != null ? 200 : 404, response.length);
                if (response.length > 0) {
                    exchange.getResponseBody().write(response);
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
            }
        } catch (IOException e) {
            try {
                exchange.sendResponseHeaders(500, 0);
            } catch (IOException ignored) {}
        } finally {
            exchange.close();
        }
    }

    public void put(String key, String value) {
        localCache.put(key, value);
        notifyOtherNodes(key, value);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(localCache.get(key));
    }

    private void notifyOtherNodes(String key, String value) {
        var update = new CacheUpdate(key, value);
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
                System.err.println("Failed to notify peer: " + notification.peer() +
                        ", response code: " + responseCode);
            }
        } catch (IOException e) {
            System.err.println("Failed to notify peer: " + notification.peer() +
                    ", error: " + e.getMessage());
        }
    }
}
