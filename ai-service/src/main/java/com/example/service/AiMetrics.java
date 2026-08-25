package com.example.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight process metrics. It deliberately keeps no consultation text or
 * notes, so it can be replaced by Micrometer/OpenTelemetry without changing
 * the consultation contract.
 */
@ApplicationScoped
public class AiMetrics {
    private final AtomicLong consultations = new AtomicLong();
    private final AtomicLong consultationErrors = new AtomicLong();
    private final AtomicLong modelCalls = new AtomicLong();
    private final AtomicLong modelFailures = new AtomicLong();
    private final AtomicLong totalLatencyMillis = new AtomicLong();
    private final AtomicLong totalInputTokens = new AtomicLong();
    private final AtomicLong totalOutputTokens = new AtomicLong();

    public void recordConsultation(long elapsedNanos, boolean error) {
        consultations.incrementAndGet();
        if (error) consultationErrors.incrementAndGet();
        totalLatencyMillis.addAndGet(Math.max(0, elapsedNanos / 1_000_000));
    }

    public void recordModelCall(long elapsedNanos, boolean failure, int inputTokens, int outputTokens) {
        modelCalls.incrementAndGet();
        if (failure) modelFailures.incrementAndGet();
        totalInputTokens.addAndGet(Math.max(0, inputTokens));
        totalOutputTokens.addAndGet(Math.max(0, outputTokens));
    }

    public Map<String, Long> snapshot() {
        return Map.of(
                "consultations", consultations.get(),
                "consultationErrors", consultationErrors.get(),
                "modelCalls", modelCalls.get(),
                "modelFailures", modelFailures.get(),
                "totalLatencyMillis", totalLatencyMillis.get(),
                "totalInputTokens", totalInputTokens.get(),
                "totalOutputTokens", totalOutputTokens.get());
    }
}
