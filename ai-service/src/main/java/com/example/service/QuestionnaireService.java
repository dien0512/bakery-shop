package com.example.service;

import com.example.dto.QuestionDefinition;
import com.example.dto.QuestionOption;
import com.example.dto.QuestionnaireResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class QuestionnaireService {
    public static final String VERSION = "1.0";

    public QuestionnaireResponse getQuestionnaire() {
        return new QuestionnaireResponse(VERSION, allQuestions());
    }

    public List<QuestionDefinition> forMissing(Set<String> missing) {
        List<QuestionDefinition> questions = new ArrayList<>(allQuestions().stream()
                .filter(question -> missing.contains(question.id))
                .toList());
        if (missing.contains("note_confirmation")) questions.add(noteConfirmation());
        return questions;
    }

    public Set<String> validOptionIds(String questionId) {
        if ("note_confirmation".equals(questionId)) {
            return Set.of("KEEP_CHECKBOX", "EDIT_CHECKBOX");
        }
        return allQuestions().stream()
                .filter(question -> question.id.equals(questionId))
                .findFirst()
                .map(question -> question.options.stream().map(option -> option.id).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    public Set<String> knownQuestionIds() {
        Set<String> ids = allQuestions().stream().map(question -> question.id)
                .collect(Collectors.toSet());
        ids.add("note_confirmation");
        return ids;
    }

    public QuestionDefinition question(String id) {
        if ("note_confirmation".equals(id)) return noteConfirmation();
        return allQuestions().stream().filter(q -> q.id.equals(id)).findFirst().orElse(null);
    }

    private QuestionDefinition noteConfirmation() {
        return new QuestionDefinition(
                "note_confirmation",
                "Ghi chú mâu thuẫn với lựa chọn checkbox. Bạn muốn làm gì?",
                "SINGLE", true, 1, 1, false,
                List.of(option("KEEP_CHECKBOX", "Giữ lựa chọn checkbox"),
                        option("EDIT_CHECKBOX", "Chỉnh lại lựa chọn")));
    }

    private List<QuestionDefinition> allQuestions() {
        List<QuestionDefinition> questions = new ArrayList<>();
        questions.add(single("occasion", "Dịp sử dụng của bạn là gì?", false, false,
                option("BIRTHDAY", "Sinh nhật"), option("PARTY", "Tiệc/gặp mặt"),
                option("GIFT", "Quà tặng"), option("FAMILY", "Ăn gia đình"),
                option("PERSONAL", "Ăn cá nhân"), option("OTHER", "Khác")));
        questions.add(single("people", "Bạn cần phục vụ cho bao nhiêu người?", true, false,
                option("ONE_TO_TWO", "1–2 người"), option("THREE_TO_FIVE", "3–5 người"),
                option("SIX_TO_TEN", "6–10 người"), option("ELEVEN_TO_TWENTY", "11–20 người"),
                option("OVER_TWENTY", "Trên 20 người")));
        questions.add(single("budget", "Ngân sách tối đa của bạn là bao nhiêu?", true, true,
                option("UNDER_TWO_HUNDRED", "Dưới 200.000đ"),
                option("TWO_HUNDRED_TO_FIVE_HUNDRED", "200.000–500.000đ"),
                option("FIVE_HUNDRED_TO_ONE_MILLION", "500.000–1.000.000đ"),
                option("OVER_ONE_MILLION", "Trên 1.000.000đ"),
                option("CUSTOM", "Nhập mức tối đa cụ thể")));
        questions.add(multiple("flavors", "Bạn thích những vị nào?", false, 0, 4, false,
                option("CHOCOLATE", "Chocolate"), option("VANILLA", "Vanilla"),
                option("FRUITY", "Trái cây"), option("CREAMY", "Kem béo"),
                option("COFFEE", "Cà phê"), option("MATCHA", "Matcha"),
                option("SAVORY", "Mặn"), option("ANY", "Không có ưu tiên")));
        questions.add(multiple("dietary", "Bạn có yêu cầu chế độ ăn nào không?", false, 0, 6, false,
                option("VEGAN", "Vegan"), option("VEGETARIAN", "Vegetarian"),
                option("GLUTEN_FREE", "Không gluten"), option("DAIRY_FREE", "Không sữa"),
                option("EGG_FREE", "Không trứng"), option("NUT_FREE", "Không hạt"),
                option("NONE", "Không có yêu cầu")));
        questions.add(multiple("allergens", "Bạn cần tránh thành phần dị ứng nào?", false, 0, 8, false,
                option("GLUTEN", "Gluten"), option("MILK", "Sữa"), option("EGG", "Trứng"),
                option("PEANUT", "Đậu phộng"), option("TREE_NUT", "Hạt cây"),
                option("SOY", "Đậu nành"), option("SESAME", "Mè"),
                option("NONE", "Không có dị ứng đã biết"), option("NOT_SURE", "Không chắc chắn")));
        questions.add(multiple("priorities", "Điều gì quan trọng nhất với bạn?", false, 0, 2, false,
                option("BUDGET", "Đúng ngân sách nhất"), option("PORTION", "Đủ khẩu phần"),
                option("VARIETY", "Đa dạng món"), option("LESS_SWEET", "Ít ngọt"),
                option("SHAREABLE", "Dễ chia sẻ"), option("GIFTABLE", "Phù hợp làm quà")));
        return questions;
    }

    private QuestionDefinition single(String id, String label, boolean required,
                                      boolean allowCustom, QuestionOption... options) {
        return new QuestionDefinition(id, label, "SINGLE", required, required ? 1 : 0, 1,
                allowCustom, List.of(options));
    }

    private QuestionDefinition multiple(String id, String label, boolean required,
                                        int min, int max, boolean allowCustom,
                                        QuestionOption... options) {
        return new QuestionDefinition(id, label, "MULTIPLE", required, min, max,
                allowCustom, List.of(options));
    }

    private QuestionOption option(String id, String label) {
        return new QuestionOption(id, label);
    }
}
