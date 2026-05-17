package com.example.repository;

import com.example.entity.Category;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoryRepository implements PanacheRepositoryBase<Category, String> {

    public boolean existsByName(String name) {
        return count("lower(name) = ?1", name.toLowerCase()) > 0;
    }
}
