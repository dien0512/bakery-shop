package com.example.dto.response;

import com.example.entity.Notification.NotificationStatus;
import com.example.entity.Notification.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String userId;
    private NotificationType type;
    private String title;
    private String content;
    private NotificationStatus status;
    private int retryCount;
    private LocalDateTime createdAt;
}
