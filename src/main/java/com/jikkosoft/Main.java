package com.jikkosoft;

import com.jikkosoft.cache.impl.*;
import org.slf4j.*;

import java.io.*;
import java.util.Arrays;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    /**
     * Main entry point for the distributed cache application.
     * Reads configuration from environment variables and starts the cache.
     */
    public static void main(String[] args) {
        try{
        var port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        var peersEnv = System.getenv().getOrDefault("PEER_NODES", "");
        var peers = Arrays.stream(peersEnv.split(","))
                .filter(s -> !s.isEmpty())
                .toList();
        logger.info("Starting distributed cache application");
        var cache = new DistributedCache(port, peers, 60000);
        logger.info("Cache initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to start cache application", e);
            System.exit(1);
        }
    }
}