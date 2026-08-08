package com.example.dto;

import java.util.ArrayList;
import java.util.List;

public class ConsultationResponse {
    public String status;
    public String code;
    public String assistantMessage;
    public ConsultationProfile profile;
    public List<QuestionDefinition> questions = new ArrayList<>();
    public List<Recommendation> recommendations = new ArrayList<>();
}
