package com.example.hw1.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.hw1.dto.CreateProductRequest;
import com.example.hw1.dto.ProductDetailResponse;
import com.example.hw1.dto.UpdateProductRequest;
import com.example.hw1.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping("/{id}")
    @SentinelResource(value = "product-detail",
            blockHandler = "onDetailBlocked",
            fallback = "onDetailFallback")
    public ProductDetailResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PutMapping("/{id}")
    public ProductDetailResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    public ProductDetailResponse onDetailBlocked(Long id, BlockException ex) {
        return new ProductDetailResponse(id, null, null, null,
                "访问过于频繁，限流中", "RATE_LIMITED",
                "RATE_LIMITED", true, null, null);
    }

    public ProductDetailResponse onDetailFallback(Long id, Throwable t) {
        return new ProductDetailResponse(id, null, null, null,
                "商品详情暂不可用", "DEGRADED",
                "DEGRADED", true, null, null);
    }
}
