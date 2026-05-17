package com.example.consumer;

import com.example.dto.request.NotificationEvent;
import com.example.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationConsumer {

    private static final Logger LOG = Logger.getLogger(NotificationConsumer.class);

    @Inject NotificationService notificationService;
    @Inject ObjectMapper objectMapper;

    @Incoming("user-registered")
    public void onUserRegistered(String payload) {
        handle(payload);
    }

    @Incoming("order-created")
    public void onOrderCreated(String payload) {
        handle(payload);
    }

    @Incoming("order-paid")
    public void onOrderPaid(String payload) {
        handle(payload);
    }

    @Incoming("order-status-updated")
    public void onOrderStatusUpdated(String payload) {
        handle(payload);
    }

    @Incoming("order-cancelled")
    public void onOrderCancelled(String payload) {
        handle(payload);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void handle(String payload) {
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            LOG.infof("Received Kafka event: type=%s userId=%s email=%s",
                    event.getType(), event.getUserId(), event.getRecipientEmail());
            notificationService.processEvent(event);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process notification payload: %s", payload);
        }
    }
}