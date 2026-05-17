//package com.example.messaging;
//
//import com.example.dto.event.NotificationEvent;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.enterprise.context.ApplicationScoped;
//import jakarta.inject.Inject;
//import org.eclipse.microprofile.reactive.messaging.Channel;
//import org.eclipse.microprofile.reactive.messaging.Emitter;
//import org.jboss.logging.Logger;
//
//@ApplicationScoped
//public class NotificationProducer {
//
//    private static final Logger LOG = Logger.getLogger(NotificationProducer.class);
//
//    @Inject
//    @Channel("notification-events")
//    Emitter<String> emitter;
//
//    @Inject
//    ObjectMapper objectMapper;
//
//    public void sendNotification(NotificationEvent event) {
//        try {
//            String json = objectMapper.writeValueAsString(event);
//            emitter.send(json);
//            LOG.infof("Sent notification event [%s] for user %s", event.getEventType(), event.getUserId());
//        } catch (Exception e) {
//            LOG.errorf("Failed to send notification event: %s", e.getMessage());
//        }
//    }
//}