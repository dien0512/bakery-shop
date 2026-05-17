package com.example.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Event nhận từ Kafka — gửi từ user-service và orders-service.
 * Chứa recipientEmail/Phone để notification-service gửi thẳng,
 * không cần lookup lại user-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationEvent {

    /** Loại event: ORDER_CREATED, ORDER_PAID, USER_REGISTERED, ... */
    private String type;

    /** ID của user */
    private String userId;

    /** Email người nhận — lấy từ Kafka message payload */
    private String recipientEmail;

    /** SĐT người nhận — có thể null */
    private String recipientPhone;

    /** Kênh gửi: ["EMAIL"], ["SMS"], ["EMAIL","SMS"] */
    private List<String> channels;

    /** Dữ liệu nghiệp vụ: orderId, totalPrice, paymentMethod, ... */
    private Map<String, Object> data;
}