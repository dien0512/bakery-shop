package com.example.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationValidatorTest {
    @Test
    void dropsUnknownAndDuplicateCandidateIds() {
        RecommendationValidator validator = new RecommendationValidator(3);
        CandidateBuilder.ComboCandidate candidate = new CandidateBuilder.ComboCandidate();
        candidate.candidateId = "candidate-1";

        OpenAiRankingClient.AiRanking ranking = new OpenAiRankingClient.AiRanking();
        OpenAiRankingClient.AiRecommendation valid = recommendation("candidate-1");
        OpenAiRankingClient.AiRecommendation unknown = recommendation("candidate-unknown");
        ranking.recommendations = List.of(valid, unknown, recommendation("candidate-1"));

        assertEquals(1, validator.validRecommendations(ranking, List.of(candidate)).size());
    }

    private OpenAiRankingClient.AiRecommendation recommendation(String candidateId) {
        OpenAiRankingClient.AiRecommendation recommendation = new OpenAiRankingClient.AiRecommendation();
        recommendation.candidateId = candidateId;
        recommendation.title = "Combo";
        recommendation.rationale = "Phù hợp";
        return recommendation;
    }
}
