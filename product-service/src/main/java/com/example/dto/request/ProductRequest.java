package com.example.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    public String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    public BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    public Integer stock = 0;

    public String description;

    @Min(value = 1, message = "Portion count must be at least 1")
    public Integer portionCount;

    public String ingredients;

    public Set<String> flavorTags;

    public Set<String> dietaryTags;

    public Set<String> allergens;

    public Boolean allergenInfoComplete;

    public String categoryId;
}
