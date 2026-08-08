package com.example.dto;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class ProductResponse {
    public String id;
    public String name;
    public BigDecimal price;
    public Integer stock;
    public String description;
    public String categoryId;
    public String categoryName;
    public String status;
    public Integer portionCount = 1;
    public String ingredients;
    public Set<String> flavorTags = new HashSet<>();
    public Set<String> dietaryTags = new HashSet<>();
    public Set<String> allergens = new HashSet<>();
    public boolean allergenInfoComplete;
}
