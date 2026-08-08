package com.example.dto;

import java.util.List;

public class QuestionnaireResponse {
    public String questionnaireVersion;
    public List<QuestionDefinition> questions;

    public QuestionnaireResponse() {}

    public QuestionnaireResponse(String questionnaireVersion, List<QuestionDefinition> questions) {
        this.questionnaireVersion = questionnaireVersion;
        this.questions = questions;
    }
}
