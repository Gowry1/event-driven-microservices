package com.example.productservice.controller;

import com.example.productservice.entity.Category;
import com.example.productservice.entity.Product;
import com.example.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        return new ResponseEntity<>(productService.createCategory(category), HttpStatus.CREATED);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @PostMapping("/categories/{categoryId}")
    public ResponseEntity<Product> createProduct(@PathVariable String categoryId, @RequestBody Product product) {
        return new ResponseEntity<>(productService.createProduct(categoryId, product), HttpStatus.CREATED);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PutMapping("/{productId}/stock")
    public ResponseEntity<Product> updateStock(@PathVariable String productId, @RequestParam Integer quantityChange) {
        return ResponseEntity.ok(productService.updateStock(productId, quantityChange));
    }
}
