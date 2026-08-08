package com.example.service;

import com.example.dto.ConsultationProfile;
import com.example.dto.ConsultationRequest;
import com.example.dto.QuestionDefinition;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ConsultationNormalizer {
    private final QuestionnaireService questionnaireService;
    private final NoteConflictDetector noteConflictDetector;

    public ConsultationNormalizer(QuestionnaireService questionnaireService,
                                  NoteConflictDetector noteConflictDetector) {
        this.questionnaireService = questionnaireService;
        this.noteConflictDetector = noteConflictDetector;
    }

    public NormalizationResult normalize(ConsultationRequest request) {
        if (!QuestionnaireService.VERSION.equals(request.questionnaireVersion)) {
            return NormalizationResult.error("QUESTIONNAIRE_VERSION_EXPIRED");
        }

        Map<String, List<String>> answers = request.safeAnswers();
        List<String> errors = new ArrayList<>();
        Set<String> missing = new LinkedHashSet<>();
        Set<String> knownQuestionIds = questionnaireService.knownQuestionIds();
        if (answers.keySet().stream().anyMatch(id -> !knownQuestionIds.contains(id))) {
            return NormalizationResult.error("Unknown questionnaire answer");
        }
        for (QuestionDefinition question : questionnaireService.getQuestionnaire().questions) {
            List<String> values = values(answers, question.id);
            if (question.required && values.isEmpty()) missing.add(question.id);
            if (values.size() < question.minSelections || values.size() > question.maxSelections) {
                if (!values.isEmpty()) errors.add(question.id + " has an invalid number of selections");
            }
            Set<String> valid = questionnaireService.validOptionIds(question.id);
            if (values.stream().anyMatch(value -> !valid.contains(value))) {
                errors.add("Invalid option for " + question.id);
            }
        }
        if (answers.containsKey("note_confirmation")) {
            List<String> confirmation = values(answers, "note_confirmation");
            if (confirmation.size() != 1
                    || confirmation.stream().anyMatch(value -> !questionnaireService.validOptionIds("note_confirmation")
                    .contains(value))) {
                errors.add("Invalid note confirmation");
            }
        }
        if (!errors.isEmpty()) return NormalizationResult.error(errors.get(0));

        for (String id : List.of("flavors", "dietary", "allergens")) {
            List<String> values = values(answers, id);
            if (values.contains("ANY") || values.contains("NONE") || values.contains("NOT_SURE")) {
                if (values.size() > 1) return NormalizationResult.error(id + " none/unsure option is exclusive");
            }
        }
        if (values(answers, "priorities").size() > 2) {
            return NormalizationResult.error("At most two priorities may be selected");
        }
        if (request.additionalNote != null && request.additionalNote.length() > 500) {
            return NormalizationResult.error("Additional note must not exceed 500 characters");
        }
        if (!missing.isEmpty()) return NormalizationResult.missing(missing);

        ConsultationProfile profile = new ConsultationProfile();
        profile.occasion = first(answers, "occasion");
        int[] people = peopleRange(first(answers, "people"));
        profile.peopleMin = people[0];
        profile.peopleMax = people[1];
        String budget = first(answers, "budget");
        BigDecimal[] budgetRange = budgetRange(budget, request.customValues);
        if (budgetRange == null) return NormalizationResult.error("A valid custom budget is required");
        profile.budgetMin = budgetRange[0];
        profile.budgetMax = budgetRange[1];
        profile.flavors = copy(values(answers, "flavors"));
        if (profile.flavors.contains("ANY")) profile.flavors = new ArrayList<>();
        profile.dietaryRequirements = copy(values(answers, "dietary"));
        if (profile.dietaryRequirements.contains("NONE")) profile.dietaryRequirements.clear();
        List<String> allergens = copy(values(answers, "allergens"));
        if (allergens.contains("NOT_SURE")) {
            profile.allergyCertainty = "NOT_SURE";
            profile.excludedAllergens.clear();
        } else if (allergens.contains("NONE")) {
            profile.allergyCertainty = "NONE";
        } else {
            profile.allergyCertainty = "CONFIRMED";
            profile.excludedAllergens = allergens;
        }
        profile.priorities = copy(values(answers, "priorities"));
        profile.additionalNote = request.additionalNote == null ? null : request.additionalNote.trim();

        List<String> noteConflicts = noteConflictDetector.detect(profile);
        if (!noteConflicts.isEmpty()) {
            List<String> confirmation = values(answers, "note_confirmation");
            if (confirmation.isEmpty()) {
                missing.add("note_confirmation");
            } else if (!List.of("KEEP_CHECKBOX").equals(confirmation)) {
                return NormalizationResult.error(
                        "The note conflicts with selected checkboxes. Update the checkbox answers before continuing.");
            }
        }
        if (!missing.isEmpty()) return NormalizationResult.missing(missing);
        return NormalizationResult.success(profile);
    }

    private String first(Map<String, List<String>> answers, String id) {
        List<String> values = values(answers, id);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private List<String> values(Map<String, List<String>> answers, String id) {
        List<String> values = answers.get(id);
        return values == null ? List.of() : values;
    }

    private int[] peopleRange(String value) {
        return switch (value) {
            case "ONE_TO_TWO" -> new int[]{1, 2};
            case "THREE_TO_FIVE" -> new int[]{3, 5};
            case "SIX_TO_TEN" -> new int[]{6, 10};
            case "ELEVEN_TO_TWENTY" -> new int[]{11, 20};
            case "OVER_TWENTY" -> new int[]{21, 100};
            default -> new int[]{1, 1};
        };
    }

    private BigDecimal[] budgetRange(String value, Map<String, String> customValues) {
        return switch (value) {
            case "UNDER_TWO_HUNDRED" -> new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("199999")};
            case "TWO_HUNDRED_TO_FIVE_HUNDRED" -> new BigDecimal[]{new BigDecimal("200000"), new BigDecimal("500000")};
            case "FIVE_HUNDRED_TO_ONE_MILLION" -> new BigDecimal[]{new BigDecimal("500000"), new BigDecimal("1000000")};
            case "OVER_ONE_MILLION" -> new BigDecimal[]{new BigDecimal("1000000"), new BigDecimal("1000000000")};
            case "CUSTOM" -> {
                try {
                    String custom = customValues == null ? null : customValues.get("budgetMax");
                    BigDecimal amount = new BigDecimal(custom == null ? "" : custom);
                    yield amount.signum() > 0 ? new BigDecimal[]{BigDecimal.ZERO, amount} : null;
                } catch (RuntimeException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private List<String> copy(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(new LinkedHashSet<>(values));
    }

    public static class NormalizationResult {
        public final ConsultationProfile profile;
        public final Set<String> missing;
        public final String error;

        private NormalizationResult(ConsultationProfile profile, Set<String> missing, String error) {
            this.profile = profile;
            this.missing = missing;
            this.error = error;
        }

        public static NormalizationResult success(ConsultationProfile profile) {
            return new NormalizationResult(profile, Set.of(), null);
        }

        public static NormalizationResult missing(Set<String> missing) {
            return new NormalizationResult(null, missing, null);
        }

        public static NormalizationResult error(String error) {
            return new NormalizationResult(null, Set.of(), error);
        }
    }
}
