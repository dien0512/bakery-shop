package com.example.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    // Loại sự kiện — để notification-service biết gửi template nào
    private String type;

    // Thông tin người nhận — lấy từ User entity
    private String userId;
    private String recipientEmail;   // user.getEmail()
    private String recipientPhone;   // user.getPhone() — có thể null

    // Kênh gửi: ["EMAIL"], ["SMS"], hoặc ["EMAIL", "SMS"]
    @Builder.Default
    private List<String> channels = List.of("EMAIL");

    // Dữ liệu nghiệp vụ để build nội dung thông báo
    private Map<String, Object> data;
}