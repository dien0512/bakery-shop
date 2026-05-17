package com.example.repository;

import com.example.entity.UserAddress;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserAddressRepository implements PanacheRepositoryBase<UserAddress, String> {

    public List<UserAddress> findByUserId(String userId) {
        return list("userId = ?1 order by isDefault desc, createdAt desc", userId);
    }

    public Optional<UserAddress> findByIdAndUserId(String id, String userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public Optional<UserAddress> findDefaultByUserId(String userId) {
        return find("userId = ?1 and isDefault = true", userId).firstResultOptional();
    }

    public long countByUserId(String userId) {
        return count("userId = ?1", userId);
    }

    public void clearDefaultForUser(String userId) {
        update("isDefault = false where userId = ?1 and isDefault = true", userId);
    }
}