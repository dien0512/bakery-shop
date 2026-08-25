package com.example.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ConsultationProfile {
    public String occasion;
    public Integer peopleMin;
    public Integer peopleMax;
    public BigDecimal budgetMin;
    public BigDecimal budgetMax;
    public List<String> flavors = new ArrayList<>();
    public List<String> dietaryRequirements = new ArrayList<>();
    public List<String> excludedAllergens = new ArrayList<>();
    public String allergyCertainty;
    public List<String> priorities = new ArrayList<>();
    public String additionalNote;
}
