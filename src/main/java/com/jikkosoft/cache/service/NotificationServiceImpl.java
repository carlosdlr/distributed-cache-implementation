package com.jikkosoft.cache.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jikkosoft.cache.model.CacheUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationServiceImpl implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final List<String> peerNodes;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public NotificationServiceImpl(List<String> peerNodes, ObjectMapper objectMapper) {
        this.peerNodes = peerNodes;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newFixedThreadPool(5);
    }

    public void notifyPeers(String path, CacheUpdate update) {
        peerNodes.forEach(peer ->
                executorService.submit(() -> sendNotification(peer, path, update))
        );
    }

    private void sendNotification(String peer, String path, CacheUpdate update) {
        try {
            var url = new URL("http://" + peer + path);
            var conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            var jsonUpdate = objectMapper.writeValueAsString(update);
            try (var os = conn.getOutputStream()) {
                os.write(jsonUpdate.getBytes());
            }

            var responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                logger.error("Failed to notify peer: {}, response code: {}", peer, responseCode);
            }
            conn.disconnect();
        } catch (IOException e) {
            logger.error("Failed to notify peer: {}", peer, e);
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}