package com.example.dto.response;

import com.example.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public class ProductResponse {
    public String id;
    public String name;
    public BigDecimal price;
    public Integer stock;
    public String description;
    public Integer portionCount;
    public String ingredients;
    public Set<String> flavorTags;
    public Set<String> dietaryTags;
    public Set<String> allergens;
    public boolean allergenInfoComplete;
    public String categoryId;
    public String categoryName;
    public Product.Status status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
