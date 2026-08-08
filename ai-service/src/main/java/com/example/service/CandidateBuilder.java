package com.example.service;

import com.example.dto.ConsultationProfile;
import com.example.dto.ProductResponse;
import com.example.dto.RecommendationItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class CandidateBuilder {
    private final int maxCandidates;

    public CandidateBuilder(@ConfigProperty(name = "ai.max-candidates", defaultValue = "20") int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public List<ComboCandidate> build(ConsultationProfile profile, List<ProductResponse> products) {
        List<ProductResponse> usable = products == null ? List.of() : products.stream()
                .filter(product -> product != null && product.id != null && product.price != null)
                .sorted(Comparator.comparing(product -> product.price))
                .toList();
        List<ComboCandidate> candidates = new ArrayList<>();

        for (ProductResponse product : usable) {
            int portions = Math.max(1, product.portionCount == null ? 1 : product.portionCount);
            int quantity = Math.max(1, (int) Math.ceil((double) profile.peopleMin / portions));
            if (product.stock != null && quantity > product.stock) continue;
            addCandidate(candidates, profile, List.of(item(product, quantity)));
        }

        for (int i = 0; i < usable.size() && candidates.size() < maxCandidates * 2; i++) {
            for (int j = i + 1; j < usable.size() && candidates.size() < maxCandidates * 2; j++) {
                ProductResponse first = usable.get(i);
                ProductResponse second = usable.get(j);
                int firstPortions = Math.max(1, first.portionCount == null ? 1 : first.portionCount);
                int secondPortions = Math.max(1, second.portionCount == null ? 1 : second.portionCount);
                int firstQuantity = Math.max(1, (int) Math.ceil(profile.peopleMin / 2.0 / firstPortions));
                int secondQuantity = Math.max(1, (int) Math.ceil(profile.peopleMin / 2.0 / secondPortions));
                if ((first.stock != null && firstQuantity > first.stock)
                        || (second.stock != null && secondQuantity > second.stock)) continue;
                addCandidate(candidates, profile, List.of(item(first, firstQuantity), item(second, secondQuantity)));
            }
        }

        return candidates.stream()
                .sorted(Comparator.comparing(candidate -> candidate.totalPrice))
                .limit(maxCandidates)
                .toList();
    }

    private RecommendationItem item(ProductResponse product, int quantity) {
        RecommendationItem item = new RecommendationItem();
        item.productId = product.id;
        item.name = product.name;
        item.quantity = quantity;
        item.unitPrice = product.price;
        item.subtotal = product.price.multiply(BigDecimal.valueOf(quantity));
        return item;
    }

    private void addCandidate(List<ComboCandidate> candidates, ConsultationProfile profile,
                              List<RecommendationItem> items) {
        if (items.stream().anyMatch(item -> item.quantity == null || item.quantity < 1)) return;
        BigDecimal total = items.stream().map(item -> item.subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (profile.budgetMax != null && total.compareTo(profile.budgetMax) > 0) return;
        ComboCandidate candidate = new ComboCandidate();
        candidate.candidateId = "candidate-" + UUID.randomUUID();
        candidate.items = new ArrayList<>(items);
        candidate.totalPrice = total;
        candidate.serves = profile.peopleMin;
        candidates.add(candidate);
    }

    public static class ComboCandidate {
        public String candidateId;
        public List<RecommendationItem> items = new ArrayList<>();
        public BigDecimal totalPrice;
        public Integer serves;

        public Map<String, Object> promptView() {
            Map<String, Object> view = new HashMap<>();
            view.put("candidateId", candidateId);
            view.put("items", items);
            view.put("totalPrice", totalPrice);
            view.put("serves", serves);
            return view;
        }
    }
}
