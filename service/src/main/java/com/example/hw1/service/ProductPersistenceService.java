package com.example.hw1.service;

import java.util.Optional;

import com.example.hw1.domain.Product;
import com.example.hw1.dto.CreateProductRequest;
import com.example.hw1.dto.UpdateProductRequest;

public interface ProductPersistenceService {

    Product create(CreateProductRequest request);

    Product update(Long id, UpdateProductRequest request);

    Optional<Product> findByIdForRead(Long id);

    Optional<Product> findByIdForWrite(Long id);
}
