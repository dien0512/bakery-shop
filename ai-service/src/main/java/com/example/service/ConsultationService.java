package com.example.service;

import com.example.client.ProductServiceClient;
import com.example.dto.ConsultationFilterRequest;
import com.example.dto.ConsultationProfile;
import com.example.dto.ConsultationRequest;
import com.example.dto.ConsultationResponse;
import com.example.dto.ProductResponse;
import com.example.dto.QuestionDefinition;
import com.example.dto.Recommendation;
import com.example.dto.RecommendationItem;
import com.example.exception.BadRequestException;
import com.example.exception.RateLimitException;
import com.example.exception.ServiceUnavailableException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ConsultationService {
    private final QuestionnaireService questionnaireService;
    private final ConsultationNormalizer normalizer;
    private final CandidateBuilder candidateBuilder;
    private final OpenAiRankingClient rankingClient;
    private final AiRateLimiter rateLimiter;
    private final RecommendationValidator recommendationValidator;
    private final AiMetrics metrics;
    private final ProductServiceClient productServiceClient;
    private final JsonWebToken jwt;

    @Inject
    public ConsultationService(QuestionnaireService questionnaireService,
                               ConsultationNormalizer normalizer,
                               CandidateBuilder candidateBuilder,
                               OpenAiRankingClient rankingClient,
                               AiRateLimiter rateLimiter,
                               RecommendationValidator recommendationValidator,
                               AiMetrics metrics,
                               @RestClient ProductServiceClient productServiceClient,
                               JsonWebToken jwt) {
        this.questionnaireService = questionnaireService;
        this.normalizer = normalizer;
        this.candidateBuilder = candidateBuilder;
        this.rankingClient = rankingClient;
        this.rateLimiter = rateLimiter;
        this.recommendationValidator = recommendationValidator;
        this.metrics = metrics;
        this.productServiceClient = productServiceClient;
        this.jwt = jwt;
    }

    public ConsultationResponse consult(ConsultationRequest request) {
        long started = System.nanoTime();
        boolean error = false;
        try {
            return consultInternal(request);
        } catch (RuntimeException exception) {
            error = true;
            throw exception;
        } finally {
            metrics.recordConsultation(System.nanoTime() - started, error);
        }
    }

    private ConsultationResponse consultInternal(ConsultationRequest request) {
        String userId = jwt == null || jwt.getSubject() == null ? "anonymous" : jwt.getSubject();
        if (!rateLimiter.tryAcquire(userId)) throw new RateLimitException();

        ConsultationNormalizer.NormalizationResult normalized = normalizer.normalize(request);
        if (normalized.error != null) {
            if ("QUESTIONNAIRE_VERSION_EXPIRED".equals(normalized.error)) {
                ConsultationResponse expired = new ConsultationResponse();
                expired.status = "QUESTIONNAIRE_VERSION_EXPIRED";
                expired.code = "QUESTIONNAIRE_VERSION_EXPIRED";
                expired.assistantMessage = "Phiên bản bảng câu hỏi đã cũ. Vui lòng tải lại bảng câu hỏi mới.";
                expired.profile = new ConsultationProfile();
                expired.questions = questionnaireService.getQuestionnaire().questions;
                return expired;
            }
            throw new BadRequestException("INVALID_ANSWER", normalized.error);
        }
        if (!normalized.missing.isEmpty()) return needsInput(normalized.missing);

        ConsultationProfile profile = normalized.profile;
        List<ProductResponse> products;
        try {
            products = productServiceClient.findCandidates(toFilter(profile));
        } catch (Exception e) {
            throw new ServiceUnavailableException("CATALOG_TEMPORARILY_UNAVAILABLE",
                    "Product catalog is temporarily unavailable");
        }

        List<CandidateBuilder.ComboCandidate> candidates = candidateBuilder.build(profile, products);
        if (candidates.isEmpty()) return noMatch(profile);

        OpenAiRankingClient.RankingResult ranking = rankingClient.rank(profile, candidates);
        if (ranking.outcome == OpenAiRankingClient.Outcome.UNAVAILABLE) {
            ConsultationResponse unavailable = new ConsultationResponse();
            unavailable.status = "AI_TEMPORARILY_UNAVAILABLE";
            unavailable.code = "AI_TEMPORARILY_UNAVAILABLE";
            unavailable.assistantMessage = "AI đang tạm thời bận. Bạn có thể xem catalog và chọn sản phẩm trực tiếp.";
            unavailable.profile = profile;
            return unavailable;
        }

        List<Recommendation> recommendations = ranking.ranking == null
                ? fallback(candidates, profile)
                : mapRanking(ranking.ranking, candidates, profile);
        if (recommendations.isEmpty()) recommendations = fallback(candidates, profile);

        ConsultationResponse response = new ConsultationResponse();
        response.status = "COMPLETED";
        response.assistantMessage = Optional.ofNullable(ranking.ranking)
                .map(result -> result.assistantMessage)
                .filter(message -> !message.isBlank())
                .orElse("Mình đã chọn một số combo phù hợp với yêu cầu của bạn.");
        response.profile = profile;
        response.recommendations = recommendations;
        if (ranking.ranking != null && ranking.ranking.followUpQuestion != null
                && !ranking.ranking.followUpQuestion.isBlank()) {
            response.questions.add(new QuestionDefinition(
                    "ai_follow_up", limit(ranking.ranking.followUpQuestion, 500,
                    "Bạn muốn ưu tiên combo nào hơn?"), "SINGLE", false, 0, 1,
                    true, List.of()));
        }
        if ("NOT_SURE".equals(profile.allergyCertainty)) {
            response.assistantMessage += " Bạn vẫn nên kiểm tra lại thành phần với cửa hàng vì thông tin dị ứng chưa chắc chắn.";
        }
        return response;
    }

    private ConsultationResponse needsInput(Set<String> missing) {
        ConsultationResponse response = new ConsultationResponse();
        response.status = "NEEDS_INPUT";
        response.code = "MISSING_REQUIRED_INPUT";
        response.assistantMessage = "Bạn vui lòng bổ sung các thông tin cần thiết để mình tư vấn chính xác hơn.";
        response.profile = new ConsultationProfile();
        response.questions = questionnaireService.forMissing(missing);
        return response;
    }

    private ConsultationResponse noMatch(ConsultationProfile profile) {
        ConsultationResponse response = new ConsultationResponse();
        response.status = "NO_MATCH";
        response.code = "NO_PRODUCTS_MATCHED";
        response.assistantMessage = "Hiện chưa có combo nào đồng thời đáp ứng các lựa chọn này. Bạn có thể tăng ngân sách hoặc bỏ bớt một điều kiện để thử lại.";
        response.profile = profile;
        return response;
    }

    private ConsultationFilterRequest toFilter(ConsultationProfile profile) {
        ConsultationFilterRequest filter = new ConsultationFilterRequest();
        filter.peopleMin = profile.peopleMin;
        filter.peopleMax = profile.peopleMax;
        filter.budgetMax = profile.budgetMax;
        filter.flavors = profile.flavors;
        filter.dietaryRequirements = profile.dietaryRequirements;
        filter.excludedAllergens = profile.excludedAllergens;
        filter.allergyCertainty = profile.allergyCertainty;
        return filter;
    }

    private List<Recommendation> mapRanking(OpenAiRankingClient.AiRanking ranking,
                                            List<CandidateBuilder.ComboCandidate> candidates,
                                            ConsultationProfile profile) {
        Map<String, CandidateBuilder.ComboCandidate> byId = new HashMap<>();
        candidates.forEach(candidate -> byId.put(candidate.candidateId, candidate));
        List<Recommendation> result = new ArrayList<>();
        for (OpenAiRankingClient.AiRecommendation selected : recommendationValidator.validRecommendations(ranking, candidates)) {
            CandidateBuilder.ComboCandidate candidate = byId.get(selected.candidateId);
            Recommendation recommendation = new Recommendation();
            recommendation.recommendationId = UUID.randomUUID().toString();
            recommendation.title = limit(selected.title, 120, "Gợi ý combo");
            recommendation.rationale = limit(selected.rationale, 500, "Phù hợp với các lựa chọn của bạn");
            recommendation.items = copyItems(candidate.items);
            recommendation.totalPrice = candidate.totalPrice;
            if (selected.warnings != null) recommendation.warnings = new ArrayList<>(selected.warnings);
            if ("NOT_SURE".equals(profile.allergyCertainty)) {
                recommendation.warnings.add("Chưa thể khẳng định an toàn dị ứng vì bạn chưa chắc chắn về thành phần cần tránh.");
            }
            result.add(recommendation);
        }
        return result;
    }

    private List<Recommendation> fallback(List<CandidateBuilder.ComboCandidate> candidates,
                                          ConsultationProfile profile) {
        List<Recommendation> result = new ArrayList<>();
        for (CandidateBuilder.ComboCandidate candidate : candidates.stream().limit(3).toList()) {
            Recommendation recommendation = new Recommendation();
            recommendation.recommendationId = UUID.randomUUID().toString();
            recommendation.title = "Combo phù hợp ngân sách";
            recommendation.rationale = "Đủ số lượng tối thiểu cho nhóm của bạn và còn hàng tại thời điểm tư vấn.";
            recommendation.items = copyItems(candidate.items);
            recommendation.totalPrice = candidate.totalPrice;
            if ("NOT_SURE".equals(profile.allergyCertainty)) {
                recommendation.warnings.add("Hãy kiểm tra lại thành phần dị ứng với cửa hàng trước khi đặt.");
            }
            result.add(recommendation);
        }
        return result;
    }

    private List<RecommendationItem> copyItems(List<RecommendationItem> items) {
        return items.stream().map(item -> {
            RecommendationItem copy = new RecommendationItem();
            copy.productId = item.productId;
            copy.name = item.name;
            copy.quantity = item.quantity;
            copy.unitPrice = item.unitPrice;
            copy.subtotal = item.subtotal;
            return copy;
        }).toList();
    }

    private String limit(String value, int max, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
