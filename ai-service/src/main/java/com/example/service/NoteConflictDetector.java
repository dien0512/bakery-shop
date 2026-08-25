package com.example.service;

import com.example.dto.ConsultationProfile;
import jakarta.enterprise.context.ApplicationScoped;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Detects only explicit contradictions between structured choices and the
 * optional note. It intentionally does not try to reinterpret a free-form
 * note; the user must confirm which source is authoritative.
 */
@ApplicationScoped
public class NoteConflictDetector {
    private static final Map<String, String> FLAVOR_TERMS = Map.of(
            "CHOCOLATE", "chocolate",
            "VANILLA", "vanilla",
            "FRUITY", "trai cay",
            "CREAMY", "kem beo",
            "COFFEE", "ca phe",
            "MATCHA", "matcha",
            "SAVORY", "man");

    private static final Map<String, String> DIETARY_TERMS = Map.of(
            "VEGAN", "vegan",
            "VEGETARIAN", "vegetarian",
            "GLUTEN_FREE", "gluten",
            "DAIRY_FREE", "sua",
            "EGG_FREE", "trung",
            "NUT_FREE", "hat");

    private static final Map<String, String> ALLERGEN_TERMS = Map.of(
            "GLUTEN", "gluten",
            "MILK", "sua",
            "EGG", "trung",
            "PEANUT", "dau phong",
            "TREE_NUT", "hat cay",
            "SOY", "dau nanh",
            "SESAME", "me");

    public List<String> detect(ConsultationProfile profile) {
        if (profile == null || profile.additionalNote == null || profile.additionalNote.isBlank()) {
            return List.of();
        }
        String note = normalize(profile.additionalNote);
        List<String> conflicts = new ArrayList<>();

        for (String flavor : profile.flavors) {
            String term = FLAVOR_TERMS.get(flavor);
            if (term != null && explicitlyNegates(note, term)) {
                conflicts.add("flavors:" + flavor);
            }
        }

        for (String dietary : profile.dietaryRequirements) {
            String term = DIETARY_TERMS.get(dietary);
            if (term != null && explicitlyRequests(note, term)
                    || "VEGAN".equals(dietary) && containsAny(note,
                    "co sua", "muon sua", "co trung", "muon trung", "co thit", "muon thit")
                    || "VEGETARIAN".equals(dietary) && containsAny(note,
                    "co thit", "muon thit", "an thit")) {
                conflicts.add("dietary:" + dietary);
            }
        }

        for (String allergen : profile.excludedAllergens) {
            String term = ALLERGEN_TERMS.get(allergen);
            if (term != null && explicitlyRequests(note, term)) {
                conflicts.add("allergens:" + allergen);
            }
        }

        if ("NONE".equals(profile.allergyCertainty)) {
            for (String term : ALLERGEN_TERMS.values()) {
                if (containsAllergyClaim(note, term)) conflicts.add("allergens:NONE");
            }
        }
        return conflicts.stream().distinct().toList();
    }

    private boolean explicitlyNegates(String note, String term) {
        return containsAny(note,
                "khong " + term,
                "khong an " + term,
                "khong muon " + term,
                "tranh " + term,
                "loai " + term,
                "no " + term);
    }

    private boolean explicitlyRequests(String note, String term) {
        return containsAny(note,
                "co " + term,
                "muon " + term,
                "them " + term,
                "an " + term,
                "dung " + term,
                "su dung " + term);
    }

    private boolean containsAllergyClaim(String note, String term) {
        return containsAny(note,
                "di ung " + term,
                "di ung voi " + term,
                "tranh " + term,
                "khong duoc " + term);
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
