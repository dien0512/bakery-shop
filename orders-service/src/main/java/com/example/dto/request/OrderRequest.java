package com.example.dto.request;

import com.example.entity.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class OrderRequest {

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    public List<OrderItemRequest> items;

    @NotNull(message = "Payment method is required")
    public Order.PaymentMethod paymentMethod = Order.PaymentMethod.COD;

    public Order.OrderSource source = Order.OrderSource.DIRECT;

    @Size(max = 36, message = "AI recommendation ID must not exceed 36 characters")
    public String aiRecommendationId;

    @NotBlank(message = "Shipping address is required")
    public String shippingAddress;

    @NotBlank(message = "Receiver name is required")
    public String receiverName;

    @NotBlank(message = "Receiver phone is required")
    public String receiverPhone;

    public String note;
}
