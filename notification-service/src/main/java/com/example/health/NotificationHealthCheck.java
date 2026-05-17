package com.example.health;

import com.example.repository.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness health check: xác nhận DB notification_db accessible.
 * Endpoint: GET /q/health/ready
 */
@Readiness
@ApplicationScoped
public class NotificationHealthCheck implements HealthCheck {

    @Inject
    NotificationRepository repo;

    @Override
    public HealthCheckResponse call() {
        try {
            long count = repo.count();
            return HealthCheckResponse.named("notification-db")
                    .up()
                    .withData("total_notifications", count)
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.named("notification-db")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
