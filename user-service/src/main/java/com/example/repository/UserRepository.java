package com.example.repository;

import com.example.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, String> {

    // ─── Find by credentials ──────────────────────────────────────────────────

    public Optional<User> findByUsername(String username) {
        return find("username = ?1 AND deletedAt IS NULL", username).firstResultOptional();
    }

    public Optional<User> findByEmail(String email) {
        return find("email = ?1 AND deletedAt IS NULL", email).firstResultOptional();
    }

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return find("(username = ?1 OR email = ?1) AND deletedAt IS NULL", usernameOrEmail)
                .firstResultOptional();
    }

    public Optional<User> findActiveById(String id) {
        return find("id = ?1 AND deletedAt IS NULL", id).firstResultOptional();
    }

    // ─── Existence checks ─────────────────────────────────────────────────────

    public boolean existsByUsername(String username) {
        return count("username = ?1 AND deletedAt IS NULL", username) > 0;
    }

    public boolean existsByEmail(String email) {
        return count("email = ?1 AND deletedAt IS NULL", email) > 0;
    }

    public boolean existsByUsernameExcluding(String username, String excludedId) {
        return count("username = ?1 AND id != ?2 AND deletedAt IS NULL", username, excludedId) > 0;
    }

    public boolean existsByEmailExcluding(String email, String excludedId) {
        return count("email = ?1 AND id != ?2 AND deletedAt IS NULL", email, excludedId) > 0;
    }

    // ─── Admin queries ────────────────────────────────────────────────────────

    public List<User> findAllActive() {
        return list("deletedAt IS NULL ORDER BY createdAt DESC");
    }

    public List<User> findByStatus(User.Status status) {
        return list("status = ?1 AND deletedAt IS NULL ORDER BY createdAt DESC", status);
    }

    public List<User> searchByKeyword(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return list(
                "deletedAt IS NULL AND (LOWER(username) LIKE ?1 OR LOWER(email) LIKE ?1 OR LOWER(fullName) LIKE ?1)",
                pattern
        );
    }

    public long countActive() {
        return count("deletedAt IS NULL");
    }
}