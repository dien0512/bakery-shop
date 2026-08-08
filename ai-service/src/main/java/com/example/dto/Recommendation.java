package com.example.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Recommendation {
    public String recommendationId;
    public String title;
    public String rationale;
    public List<RecommendationItem> items = new ArrayList<>();
    public BigDecimal totalPrice;
    public List<String> warnings = new ArrayList<>();
}
