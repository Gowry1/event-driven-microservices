package com.example.productservice.service;

import com.example.dto.ProductStockUpdatedEvent;
import com.example.productservice.entity.Category;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PRODUCT_EVENTS_TOPIC = "product-events";

    @Transactional
    public Category createCategory(Category category) {
        category.setCategoryId(UUID.randomUUID().toString());
        Category savedCategory = categoryRepository.save(category);
        log.info("Category created: {}", savedCategory.getName());
        return savedCategory;
    }

    @Transactional
    public Product createProduct(String categoryId, Product product) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + categoryId));
        
        product.setProductId(UUID.randomUUID().toString());
        product.setCategory(category);
        
        Product savedProduct = productRepository.save(product);
        log.info("Product created: {} in Category {}", savedProduct.getName(), category.getName());
        return savedProduct;
    }

    @Transactional
    public Product updateStock(String productId, Integer quantityChange) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
        
        int newStock = product.getStock() + quantityChange;
        if (newStock < 0) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
        }
        
        product.setStock(newStock);
        if (newStock == 0) {
            product.setStatus("OUT_OF_STOCK");
        } else {
            product.setStatus("ACTIVE");
        }
        
        Product savedProduct = productRepository.save(product);
        log.info("Product stock updated: {} (New Stock: {})", savedProduct.getName(), savedProduct.getStock());

        // Publish stock update event to Kafka
        ProductStockUpdatedEvent event = new ProductStockUpdatedEvent(
                savedProduct.getProductId(),
                savedProduct.getStock()
        );
        
        try {
            kafkaTemplate.send(PRODUCT_EVENTS_TOPIC, savedProduct.getProductId(), event);
            log.info("Successfully published ProductStockUpdatedEvent to topic {}: {}", PRODUCT_EVENTS_TOPIC, event);
        } catch (Exception e) {
            log.error("Failed to publish ProductStockUpdatedEvent to Kafka: {}", e.getMessage(), e);
        }

        return savedProduct;
    }

    public Product getProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}
