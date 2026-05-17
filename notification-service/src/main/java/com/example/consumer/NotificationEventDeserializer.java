package com.example.consumer;

import com.example.dto.request.NotificationEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

/**
 * Kafka deserializer cho NotificationEvent.
 * Dùng Jackson ObjectMapper (tự động cấu hình bởi Quarkus).
 */
public class NotificationEventDeserializer extends ObjectMapperDeserializer<NotificationEvent> {
    public NotificationEventDeserializer() {
        super(NotificationEvent.class);
    }
}