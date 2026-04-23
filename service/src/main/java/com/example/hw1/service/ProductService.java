package com.example.hw1.service;

import com.example.hw1.dto.CreateProductRequest;
import com.example.hw1.dto.ProductDetailResponse;
import com.example.hw1.dto.UpdateProductRequest;

public interface ProductService {

    ProductDetailResponse create(CreateProductRequest request);

    ProductDetailResponse getById(Long id);

    ProductDetailResponse update(Long id, UpdateProductRequest request);

    ProductDetailResponse inspectRoute(Long id);

    ProductDetailResponse warmCache(Long id);
}
