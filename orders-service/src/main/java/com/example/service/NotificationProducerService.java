package com.example.service;

import com.example.dto.event.NotificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationProducerService {

    private static final Logger LOG = Logger.getLogger(NotificationProducerService.class);

    @Inject
    ObjectMapper objectMapper;

    @Channel("order-created")
    Emitter<String> orderCreatedEmitter;

    @Channel("order-paid")
    Emitter<String> orderPaidEmitter;

    @Channel("order-status-updated")
    Emitter<String> orderStatusUpdatedEmitter;

    @Channel("order-cancelled")
    Emitter<String> orderCancelledEmitter;

    public void sendOrderCreated(NotificationEvent event) {
        send(orderCreatedEmitter, event, "order.created");
    }

    public void sendOrderPaid(NotificationEvent event) {
        send(orderPaidEmitter, event, "order.paid");
    }

    public void sendOrderStatusUpdated(NotificationEvent event) {
        send(orderStatusUpdatedEmitter, event, "order.status.updated");
    }

    public void sendOrderCancelled(NotificationEvent event) {
        send(orderCancelledEmitter, event, "order.cancelled");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void send(Emitter<String> emitter, NotificationEvent event, String topic) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            emitter.send(payload);
            LOG.infof("Kafka event sent to [%s]: userId=%s type=%s",
                    topic, event.getUserId(), event.getType());
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to serialize NotificationEvent for topic [%s]", topic);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send Kafka event to topic [%s]", topic);
        }
    }
}