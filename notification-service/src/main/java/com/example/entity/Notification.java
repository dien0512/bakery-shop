package com.example.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notifications",
        indexes = @Index(name = "idx_notifications_user", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification extends PanacheEntityBase {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 10)
    private NotificationType type = NotificationType.EMAIL;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 10)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Builder.Default
    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum NotificationType { EMAIL, SMS }
    public enum NotificationStatus { PENDING, SENT, FAILED }

    // ── Factory ──────────────────────────────────────────────────────────────

    public static Notification create(String userId, NotificationType type,
                                      String title, String content) {
        return Notification.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public static List<Notification> findByUserId(String userId, int page, int size) {
        return find("userId = ?1", Sort.by("createdAt").descending(), userId)
                .page(Page.of(page, size)).list();
    }

    public static long countByUserId(String userId) {
        return count("userId = ?1", userId);
    }

    public static List<Notification> findAllPaged(int page, int size) {
        return findAll(Sort.by("createdAt").descending())
                .page(Page.of(page, size)).list();
    }

    public static long countAll() { return count(); }

    public static List<Notification> findPending(int limit) {
        return find("status = ?1", Sort.by("createdAt").ascending(),
                NotificationStatus.PENDING)
                .page(Page.of(0, limit)).list();
    }

    public static List<Notification> findFailed(int maxRetry) {
        return find("status = ?1 and retryCount < ?2",
                Sort.by("createdAt").ascending(),
                NotificationStatus.FAILED, maxRetry)
                .list();
    }
}
