package com.example.dto.request;

import com.example.entity.Notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotBlank(message = "userId không được để trống")
    private String userId;

    @NotNull(message = "type không được để trống")
    private NotificationType type;

    @NotBlank(message = "title không được để trống")
    private String title;

    @NotBlank(message = "content không được để trống")
    private String content;
}
