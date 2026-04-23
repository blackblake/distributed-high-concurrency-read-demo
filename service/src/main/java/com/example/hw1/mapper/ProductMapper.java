package com.example.hw1.mapper;

import com.example.hw1.domain.Product;

public interface ProductMapper {

    int insert(Product product);

    int update(Product product);

    Product findById(Long id);
}
