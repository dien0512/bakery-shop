package com.example.service;

import com.example.dto.event.NotificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
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

    @Channel("user-registered")
    Emitter<String> userRegisteredEmitter;

    public void sendUserRegistered(NotificationEvent event) {
        send(userRegisteredEmitter, event, "user.registered");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void send(Emitter<String> emitter, NotificationEvent event, String topic) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            emitter.send(payload);
            LOG.infof("Kafka event sent to [%s]: userId=%s type=%s",
                    topic, event.getUserId(), event.getType());
        } catch (JsonProcessingException e) {
            // Không throw exception — lỗi Kafka không nên làm hỏng flow chính
            LOG.errorf(e, "Failed to serialize NotificationEvent for topic [%s]", topic);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send Kafka event to topic [%s]", topic);
        }
    }
}