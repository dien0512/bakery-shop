package com.example.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RetryScheduler {

    private static final Logger LOG = Logger.getLogger(RetryScheduler.class);

    @Inject
    NotificationService notificationService;

    /**
     * Chạy mỗi 5 phút: retry các notification bị FAILED chưa vượt maxRetry.
     */
    @Scheduled(every = "5m", identity = "retry-failed-notifications")
    void retryFailed() {
        LOG.debug("Scheduler: retrying failed notifications...");
        notificationService.retryFailed();
    }
}
