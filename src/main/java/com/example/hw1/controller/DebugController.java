package com.example.hw1.controller;

import com.example.hw1.dto.ProductDetailResponse;
import com.example.hw1.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final ProductService productService;

    public DebugController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/db-route/{id}")
    public ProductDetailResponse getRoute(@PathVariable Long id) {
        return productService.inspectRoute(id);
    }

    @PostMapping("/cache/warm/{id}")
    public ProductDetailResponse warmCache(@PathVariable Long id) {
        return productService.warmCache(id);
    }
}
