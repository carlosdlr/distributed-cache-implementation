package com.jikkosoft;

import com.jikkosoft.cache.impl.*;

import java.io.*;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException {
        var port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        var peersEnv = System.getenv().getOrDefault("PEER_NODES", "");
        var peers = Arrays.stream(peersEnv.split(","))
                .filter(s -> !s.isEmpty())
                .toList();

        var cache = new DistributedCache(port, peers);

        cache.put("key1", "value1");
        cache.get("key1")
                .ifPresentOrElse(
                        value -> System.out.println("Retrieved: " + value),
                        () -> System.out.println("Key not found")
                );
    }
}