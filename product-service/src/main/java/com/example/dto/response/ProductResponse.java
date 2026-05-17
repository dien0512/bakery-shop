package com.example.dto.response;

import com.example.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponse {
    public String id;
    public String name;
    public BigDecimal price;
    public Integer stock;
    public String description;
    public String categoryId;
    public String categoryName;
    public Product.Status status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
