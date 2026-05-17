package com.example.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderItemResponse {
    public String id;
    public String productId;
    public String productName;
    public BigDecimal price;
    public Integer quantity;
    public BigDecimal subtotal;
    public LocalDateTime createdAt;
}
