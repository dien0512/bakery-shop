package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsultationRequest {
    @NotBlank(message = "Questionnaire version is required")
    public String questionnaireVersion;

    public Map<String, List<String>> answers = new HashMap<>();

    /** Values for options such as a custom budget. */
    public Map<String, String> customValues = new HashMap<>();

    @Size(max = 500, message = "Additional note must not exceed 500 characters")
    public String additionalNote;

    public Map<String, List<String>> safeAnswers() {
        return answers == null ? new HashMap<>() : answers;
    }

    public List<String> answer(String id) {
        List<String> values = safeAnswers().get(id);
        return values == null ? new ArrayList<>() : values;
    }
}
