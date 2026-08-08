package com.example.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders",
    indexes = @Index(name = "idx_orders_user", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order extends PanacheEntityBase {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "payment_status", length = 10)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "payment_method", length = 10)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "source", length = 20, nullable = false)
    private OrderSource source = OrderSource.DIRECT;

    @Column(name = "ai_recommendation_id", length = 36)
    private String aiRecommendationId;

    @Column(name = "shipping_address", length = 255)
    private String shippingAddress;

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public enum OrderStatus { PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED }
    public enum PaymentStatus { UNPAID, PAID, FAILED }
    public enum PaymentMethod { COD, VNPAY }
    public enum OrderSource { DIRECT, AI_CONCIERGE }

    public static List<Order> findByUserId(String userId, int page, int size) {
        return find("userId = ?1", Sort.by("createdAt").descending(), userId)
                .page(Page.of(page, size)).list();
    }
    public static long countByUserId(String userId) { return count("userId = ?1", userId); }
    public static List<Order> findAllPaged(int page, int size) {
        return findAll(Sort.by("createdAt").descending()).page(Page.of(page, size)).list();
    }
    public static long countAll() { return count(); }
    public static List<Order> findByStatus(OrderStatus status, int page, int size) {
        return find("status = ?1", Sort.by("createdAt").descending(), status)
                .page(Page.of(page, size)).list();
    }
    public static long countByStatus(OrderStatus status) { return count("status = ?1", status); }
}
