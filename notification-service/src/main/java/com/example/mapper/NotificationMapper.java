package com.example.mapper;

import com.example.dto.response.NotificationResponse;
import com.example.entity.Notification;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationMapper {

    public NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .status(n.getStatus())
                .retryCount(n.getRetryCount())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
