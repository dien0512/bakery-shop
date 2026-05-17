package com.example.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products",
    indexes = {
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_status", columnList = "status")
    })
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends PanacheEntityBase {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(length = 150, nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer stock = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id", length = 36)
    private String categoryId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20)
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum Status {
        ACTIVE, OUT_OF_STOCK, DISABLED
    }

    // ── Static finders ───────────────────────────────────────────────────────

    public static List<Product> findActive(int page, int size) {
        return find("status != ?1", Sort.by("createdAt").descending(), Status.DISABLED)
                .page(Page.of(page, size))
                .list();
    }

    public static long countActive() {
        return count("status != ?1", Status.DISABLED);
    }

    public static List<Product> findByCategory(String categoryId, int page, int size) {
        return find("categoryId = ?1 and status != ?2",
                Sort.by("createdAt").descending(), categoryId, Status.DISABLED)
                .page(Page.of(page, size))
                .list();
    }

    public static long countByCategory(String categoryId) {
        return count("categoryId = ?1 and status != ?2", categoryId, Status.DISABLED);
    }

    public static List<Product> searchByName(String keyword, int page, int size) {
        return find("lower(name) like ?1 and status != ?2",
                Sort.by("createdAt").descending(),
                "%" + keyword.toLowerCase() + "%", Status.DISABLED)
                .page(Page.of(page, size))
                .list();
    }

    public static long countByName(String keyword) {
        return count("lower(name) like ?1 and status != ?2",
                "%" + keyword.toLowerCase() + "%", Status.DISABLED);
    }
}
