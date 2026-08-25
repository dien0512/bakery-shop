package com.example.dto;

import java.util.ArrayList;
import java.util.List;

public class QuestionDefinition {
    public String id;
    public String label;
    public String selectionMode;
    public boolean required;
    public int minSelections;
    public int maxSelections;
    public boolean allowCustomInput;
    public List<QuestionOption> options = new ArrayList<>();

    public QuestionDefinition() {}

    public QuestionDefinition(String id, String label, String selectionMode,
                              boolean required, int minSelections, int maxSelections,
                              boolean allowCustomInput, List<QuestionOption> options) {
        this.id = id;
        this.label = label;
        this.selectionMode = selectionMode;
        this.required = required;
        this.minSelections = minSelections;
        this.maxSelections = maxSelections;
        this.allowCustomInput = allowCustomInput;
        this.options = options;
    }
}
