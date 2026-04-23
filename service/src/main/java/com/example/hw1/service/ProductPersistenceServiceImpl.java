package com.example.hw1.service;

import java.util.Optional;

import com.example.hw1.domain.Product;
import com.example.hw1.dto.CreateProductRequest;
import com.example.hw1.dto.UpdateProductRequest;
import com.example.hw1.mapper.ProductMapper;
import com.example.hw1.routing.ReadOnlyRoute;
import org.springframework.stereotype.Service;

@Service
public class ProductPersistenceServiceImpl implements ProductPersistenceService {

    private final ProductMapper productMapper;

    public ProductPersistenceServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public Product create(CreateProductRequest request) {
        Product product = toProduct(request);
        productMapper.insert(product);
        return productMapper.findById(product.getId());
    }

    @Override
    public Product update(Long id, UpdateProductRequest request) {
        Product product = toProduct(request);
        product.setId(id);
        int affected = productMapper.update(product);
        if (affected == 0) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        return productMapper.findById(id);
    }

    @Override
    @ReadOnlyRoute
    public Optional<Product> findByIdForRead(Long id) {
        return Optional.ofNullable(productMapper.findById(id));
    }

    @Override
    public Optional<Product> findByIdForWrite(Long id) {
        return Optional.ofNullable(productMapper.findById(id));
    }

    private Product toProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setDescription(request.description());
        product.setStatus(request.status());
        return product;
    }

    private Product toProduct(UpdateProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setDescription(request.description());
        product.setStatus(request.status());
        return product;
    }
}
