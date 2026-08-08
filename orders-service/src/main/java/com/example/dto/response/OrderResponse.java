package com.example.dto.response;

import com.example.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {
    public String id;
    public String userId;
    public BigDecimal totalPrice;
    public Order.OrderStatus status;
    public Order.PaymentStatus paymentStatus;
    public Order.PaymentMethod paymentMethod;
    public Order.OrderSource source;
    public String aiRecommendationId;
    public String shippingAddress;
    public String receiverName;
    public String receiverPhone;
    public String note;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public List<OrderItemResponse> items;
}
