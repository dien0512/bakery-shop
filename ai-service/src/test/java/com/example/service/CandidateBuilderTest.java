package com.example.service;

import com.example.dto.ConsultationProfile;
import com.example.dto.ProductResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateBuilderTest {
    private final CandidateBuilder builder = new CandidateBuilder(20);

    @Test
    void neverCreatesCandidateOverStockOrBudget() {
        ConsultationProfile profile = new ConsultationProfile();
        profile.peopleMin = 6;
        profile.budgetMax = new BigDecimal("500000");

        ProductResponse product = product("cake", "120000", 2, 2);

        var candidates = builder.build(profile, List.of(product));

        assertTrue(candidates.isEmpty());
    }

    @Test
    void calculatesQuantityAndExactTotalForEnoughPortions() {
        ConsultationProfile profile = new ConsultationProfile();
        profile.peopleMin = 6;
        profile.budgetMax = new BigDecimal("500000");

        ProductResponse product = product("cake", "120000", 8, 5);

        var candidates = builder.build(profile, List.of(product));

        assertEquals(1, candidates.size());
        assertEquals(1, candidates.get(0).items.get(0).quantity);
        assertEquals(new BigDecimal("120000"), candidates.get(0).totalPrice);
    }

    private ProductResponse product(String id, String price, int portions, int stock) {
        ProductResponse product = new ProductResponse();
        product.id = id;
        product.name = id;
        product.price = new BigDecimal(price);
        product.portionCount = portions;
        product.stock = stock;
        product.status = "ACTIVE";
        return product;
    }
}
