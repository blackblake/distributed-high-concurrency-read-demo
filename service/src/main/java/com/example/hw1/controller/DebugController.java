package com.example.hw1.controller;

import java.util.Map;

import com.example.hw1.dto.ProductDetailResponse;
import com.example.hw1.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final ProductService productService;
    private final String instanceId;

    public DebugController(ProductService productService,
                           @Value("${app.instance-id:local}") String instanceId) {
        this.productService = productService;
        this.instanceId = instanceId;
    }

    @GetMapping("/instance")
    public Map<String, String> whoAmI() {
        return Map.of("instanceId", instanceId);
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
