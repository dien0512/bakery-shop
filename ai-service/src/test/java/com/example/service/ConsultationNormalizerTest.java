package com.example.service;

import com.example.dto.ConsultationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultationNormalizerTest {
    private final ConsultationNormalizer normalizer = new ConsultationNormalizer(
            new QuestionnaireService(), new NoteConflictDetector());

    @Test
    void asksForRequiredPeopleAndBudgetBeforeCallingModel() {
        ConsultationRequest request = new ConsultationRequest();
        request.questionnaireVersion = "1.0";

        ConsultationNormalizer.NormalizationResult result = normalizer.normalize(request);

        assertTrue(result.missing.contains("people"));
        assertTrue(result.missing.contains("budget"));
        assertFalse(result.missing.contains("occasion"));
    }

    @Test
    void rejectsNoneTogetherWithOtherMultiSelectOptions() {
        ConsultationRequest request = validRequest();
        request.answers.put("dietary", List.of("NONE", "VEGAN"));

        ConsultationNormalizer.NormalizationResult result = normalizer.normalize(request);

        assertNotNull(result.error);
        assertTrue(result.error.contains("dietary"));
    }

    @Test
    void normalizesConfirmedAllergenAndCustomBudget() {
        ConsultationRequest request = validRequest();
        request.answers.put("budget", List.of("CUSTOM"));
        request.customValues = Map.of("budgetMax", "350000");
        request.answers.put("allergens", List.of("PEANUT"));
        request.answers.put("flavors", List.of("CHOCOLATE", "FRUITY"));

        ConsultationNormalizer.NormalizationResult result = normalizer.normalize(request);

        assertNotNull(result.profile);
        assertEquals("CONFIRMED", result.profile.allergyCertainty);
        assertEquals("350000", result.profile.budgetMax.toPlainString());
        assertEquals(List.of("PEANUT"), result.profile.excludedAllergens);
        assertEquals(List.of("CHOCOLATE", "FRUITY"), result.profile.flavors);
    }

    @Test
    void asksForConfirmationWhenNoteContradictsCheckbox() {
        ConsultationRequest request = validRequest();
        request.answers.put("flavors", List.of("CHOCOLATE"));
        request.additionalNote = "không chocolate";

        ConsultationNormalizer.NormalizationResult result = normalizer.normalize(request);

        assertTrue(result.missing.contains("note_confirmation"));
    }

    @Test
    void keepsCheckboxAuthorityAfterExplicitConfirmation() {
        ConsultationRequest request = validRequest();
        request.answers.put("flavors", List.of("CHOCOLATE"));
        request.answers.put("note_confirmation", List.of("KEEP_CHECKBOX"));
        request.additionalNote = "không chocolate";

        ConsultationNormalizer.NormalizationResult result = normalizer.normalize(request);

        assertNotNull(result.profile);
        assertEquals(List.of("CHOCOLATE"), result.profile.flavors);
    }

    private ConsultationRequest validRequest() {
        ConsultationRequest request = new ConsultationRequest();
        request.questionnaireVersion = "1.0";
        request.answers = new HashMap<>();
        request.answers.put("people", List.of("SIX_TO_TEN"));
        request.answers.put("budget", List.of("TWO_HUNDRED_TO_FIVE_HUNDRED"));
        request.answers.put("dietary", List.of("NONE"));
        request.answers.put("allergens", List.of("NONE"));
        request.answers.put("flavors", List.of("ANY"));
        request.answers.put("priorities", List.of("BUDGET"));
        return request;
    }
}
