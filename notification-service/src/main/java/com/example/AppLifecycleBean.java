package com.example;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOG = Logger.getLogger(AppLifecycleBean.class);

    void onStart(@Observes StartupEvent ev) {
        LOG.info("========================================");
        LOG.info("  Notification Service started");
        LOG.info("  Port      : 8084");
        LOG.info("  Swagger UI: http://localhost:8084/swagger-ui");
        LOG.info("  Health    : http://localhost:8084/q/health");
        LOG.info("========================================");
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOG.info("Notification Service is shutting down...");
    }
}
