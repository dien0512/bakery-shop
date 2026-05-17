package com.example.dto.request;

import com.example.entity.Order;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    public Order.OrderStatus status;
}
