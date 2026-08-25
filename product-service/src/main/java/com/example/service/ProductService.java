package com.example.service;

import com.example.dto.request.CategoryRequest;
import com.example.dto.request.ConsultationFilterRequest;
import com.example.dto.request.ProductRequest;
import com.example.dto.response.CategoryResponse;
import com.example.dto.response.PageResponse;
import com.example.dto.response.ProductResponse;
import com.example.entity.Category;
import com.example.entity.Product;
import com.example.exception.BadRequestException;
import com.example.exception.ConflictException;
import com.example.exception.NotFoundException;
import com.example.mapper.ProductMapper;
import com.example.repository.CategoryRepository;
import com.example.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);

    @Inject ProductMapper productMapper;
    @Inject ProductRepository productRepository;
    @Inject CategoryRepository categoryRepository;

    // ─── Categories ───────────────────────────────────────────────────────────

    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.listAll();
        return productMapper.toCategoryResponseList(categories);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        if (categoryRepository.existsByName(req.name)) {
            throw new ConflictException("Category '" + req.name + "' already exists");
        }

        Category category = Category.builder()
                .id(UUID.randomUUID().toString())
                .name(req.name.trim())
                .description(req.description)
                .build();

        categoryRepository.persist(category);
        categoryRepository.flush();
        LOG.infof("Category created: %s (%s)", category.getName(), category.getId());
        return productMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(String id, CategoryRequest req) {
        Category category = categoryRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (req.name != null && !req.name.equalsIgnoreCase(category.getName())) {
            if (categoryRepository.existsByName(req.name)) {
                throw new ConflictException("Category '" + req.name + "' already exists");
            }
            category.setName(req.name.trim());
        }
        if (req.description != null) category.setDescription(req.description);

        return productMapper.toResponse(category);
    }

    @Transactional
    public void deleteCategory(String id) {
        Category category = categoryRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        long productCount = productRepository.count("categoryId = ?1", id);
        if (productCount > 0) {
            throw new ConflictException("Cannot delete category with " + productCount + " existing product(s)");
        }

        categoryRepository.delete(category);
        LOG.infof("Category deleted: %s", id);
    }

    // ─── Products ─────────────────────────────────────────────────────────────

    public PageResponse<ProductResponse> getProducts(int page, int size, String categoryId, String keyword) {
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        if (page < 0) page = 0;

        List<Product> products;
        long total;

        if (keyword != null && !keyword.isBlank()) {
            products = Product.searchByName(keyword, page, size);
            total = Product.countByName(keyword);
        } else if (categoryId != null && !categoryId.isBlank()) {
            // validate category exists
            categoryRepository.findByIdOptional(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            products = Product.findByCategory(categoryId, page, size);
            total = Product.countByCategory(categoryId);
        } else {
            products = Product.findActive(page, size);
            total = Product.countActive();
        }

        List<ProductResponse> data = enrichWithCategoryName(products);
        return new PageResponse<>(data, page, size, total);
    }

    public ProductResponse getProduct(String id) {
        Product product = productRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return enrichWithCategoryName(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest req) {
        if (req.categoryId != null && !req.categoryId.isBlank()) {
            categoryRepository.findByIdOptional(req.categoryId)
                    .orElseThrow(() -> new BadRequestException("Category not found"));
        }

        int stock = req.stock != null ? req.stock : 0;
        Product.Status status = stock == 0 ? Product.Status.OUT_OF_STOCK : Product.Status.ACTIVE;

        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .name(req.name.trim())
                .price(req.price)
                .stock(stock)
                .description(req.description)
                .portionCount(req.portionCount != null ? req.portionCount : 1)
                .ingredients(req.ingredients)
                .flavorTags(normalizeTags(req.flavorTags))
                .dietaryTags(normalizeTags(req.dietaryTags))
                .allergens(normalizeTags(req.allergens))
                .allergenInfoComplete(Boolean.TRUE.equals(req.allergenInfoComplete))
                .categoryId(req.categoryId)
                .status(status)
                .build();

        productRepository.persist(product);
        productRepository.flush();
        LOG.infof("Product created: %s (%s)", product.getName(), product.getId());
        return enrichWithCategoryName(product);
    }

    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest req) {
        Product product = productRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        if (req.categoryId != null && !req.categoryId.isBlank()) {
            categoryRepository.findByIdOptional(req.categoryId)
                    .orElseThrow(() -> new BadRequestException("Category not found"));
            product.setCategoryId(req.categoryId);
        }
        if (req.name != null) product.setName(req.name.trim());
        if (req.price != null) product.setPrice(req.price);
        if (req.description != null) product.setDescription(req.description);
        if (req.portionCount != null) product.setPortionCount(req.portionCount);
        if (req.ingredients != null) product.setIngredients(req.ingredients);
        if (req.flavorTags != null) product.setFlavorTags(normalizeTags(req.flavorTags));
        if (req.dietaryTags != null) product.setDietaryTags(normalizeTags(req.dietaryTags));
        if (req.allergens != null) product.setAllergens(normalizeTags(req.allergens));
        if (req.allergenInfoComplete != null) product.setAllergenInfoComplete(req.allergenInfoComplete);
        if (req.stock != null) {
            product.setStock(req.stock);
            product.setStatus(req.stock == 0 ? Product.Status.OUT_OF_STOCK : Product.Status.ACTIVE);
        }

        return enrichWithCategoryName(product);
    }

    @Transactional
    public void deleteProduct(String id) {
        Product product = productRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        product.setDeletedAt(LocalDateTime.now());
        product.setStatus(Product.Status.DISABLED);
        LOG.infof("Product soft-deleted: %s", id);
    }

    @Transactional
    public ProductResponse adjustStock(String id, int delta) {
        Product product = productRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        int newStock = product.getStock() + delta;
        if (newStock < 0) {
            throw new ConflictException("Insufficient stock. Current: " + product.getStock() + ", delta: " + delta);
        }

        product.setStock(newStock);
        product.setStatus(newStock == 0 ? Product.Status.OUT_OF_STOCK : Product.Status.ACTIVE);
        LOG.infof("Stock adjusted for product %s: %d -> %d", id, product.getStock() - delta, newStock);
        return enrichWithCategoryName(product);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private ProductResponse enrichWithCategoryName(Product product) {
        ProductResponse response = productMapper.toResponse(product);
        if (product.getCategoryId() != null) {
            categoryRepository.findByIdOptional(product.getCategoryId())
                    .ifPresent(cat -> response.categoryName = cat.getName());
        }
        return response;
    }

    private List<ProductResponse> enrichWithCategoryName(List<Product> products) {
        return products.stream()
                .map(this::enrichWithCategoryName)
                .toList();
    }

    /**
     * Returns only currently purchasable products. Filtering is deliberately
     * deterministic and happens before any model call.
     */
    public List<ProductResponse> findConsultationCandidates(ConsultationFilterRequest filter) {
        List<Product> products = productRepository.find("status = ?1 and stock > 0", Product.Status.ACTIVE).list();
        Set<String> excludedAllergens = normalizeTags(filter.excludedAllergens == null
                ? Set.of() : Set.copyOf(filter.excludedAllergens));
        Set<String> dietaryRequirements = normalizeTags(filter.dietaryRequirements == null
                ? Set.of() : Set.copyOf(filter.dietaryRequirements));
        Set<String> flavorPreferences = normalizeTags(filter.flavors == null
                ? Set.of() : Set.copyOf(filter.flavors));
        boolean allergyFilter = "CONFIRMED".equalsIgnoreCase(filter.allergyCertainty)
                && !excludedAllergens.isEmpty();

        return products.stream()
                .filter(product -> filter.budgetMax == null || product.getPrice().compareTo(filter.budgetMax) <= 0)
                .filter(product -> dietaryRequirements.isEmpty()
                        || product.getDietaryTags().containsAll(dietaryRequirements))
                .filter(product -> !allergyFilter
                        || (product.isAllergenInfoComplete()
                        && java.util.Collections.disjoint(product.getAllergens(), excludedAllergens)))
                .filter(product -> flavorPreferences.isEmpty()
                        || flavorPreferences.contains("ANY")
                        || !java.util.Collections.disjoint(product.getFlavorTags(), flavorPreferences))
                .map(this::enrichWithCategoryName)
                .toList();
    }

    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) return new HashSet<>();
        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(tag -> tag.trim().toUpperCase())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }
}
