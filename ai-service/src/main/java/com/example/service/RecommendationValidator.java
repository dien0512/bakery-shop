package com.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Keeps model output constrained to backend-created candidate IDs. */
@ApplicationScoped
public class RecommendationValidator {
    private final int maxRecommendations;

    public RecommendationValidator(
            @ConfigProperty(name = "ai.max-recommendations", defaultValue = "3") int maxRecommendations) {
        this.maxRecommendations = Math.max(1, maxRecommendations);
    }

    public List<OpenAiRankingClient.AiRecommendation> validRecommendations(
            OpenAiRankingClient.AiRanking ranking,
            List<CandidateBuilder.ComboCandidate> candidates) {
        if (ranking == null || ranking.recommendations == null || candidates == null) return List.of();

        Map<String, CandidateBuilder.ComboCandidate> byId = new HashMap<>();
        candidates.forEach(candidate -> byId.put(candidate.candidateId, candidate));
        Set<String> seen = new HashSet<>();
        List<OpenAiRankingClient.AiRecommendation> valid = new ArrayList<>();

        for (OpenAiRankingClient.AiRecommendation recommendation : ranking.recommendations) {
            if (recommendation == null || recommendation.candidateId == null
                    || !byId.containsKey(recommendation.candidateId)
                    || !seen.add(recommendation.candidateId)
                    || blank(recommendation.title) || blank(recommendation.rationale)) {
                continue;
            }
            if (recommendation.warnings == null) recommendation.warnings = new ArrayList<>();
            recommendation.warnings = recommendation.warnings.stream()
                    .filter(warning -> warning != null && !warning.isBlank())
                    .map(warning -> warning.length() > 300 ? warning.substring(0, 300) : warning)
                    .limit(5)
                    .toList();
            valid.add(recommendation);
            if (valid.size() >= maxRecommendations) break;
        }
        return valid;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank() || value.length() > 1000;
    }
}
