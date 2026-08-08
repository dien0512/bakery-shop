package com.example.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AiRateLimiter {
    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_REQUESTS = 10;
    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    public boolean tryAcquire(String userId) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requests.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= WINDOW_MILLIS) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) return false;
            timestamps.addLast(now);
            return true;
        }
    }
}
