package com.example.hw1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import com.example.hw1.cache.CachePolicy;
import com.example.hw1.cache.CacheValue;
import com.example.hw1.domain.Product;
import com.example.hw1.dto.ProductDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceLockingTest {

    @Mock
    private ProductCacheRepository productCacheRepository;

    @Mock
    private ProductPersistenceService productPersistenceService;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(
                productCacheRepository,
                productPersistenceService,
                new CachePolicy(30, 10, 2, 10),
                1,
                2);
    }

    @Test
    void shouldReturnCacheValueAfterRetryWhenLockIsHeldByAnotherThread() {
        Product product = sampleProduct(1L);
        when(productCacheRepository.get(1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(CacheValue.of(product)));
        when(productCacheRepository.tryLock(1L)).thenReturn(false);

        ProductDetailResponse response = productService.getById(1L);

        assertEquals("CACHE", response.source());
        verify(productPersistenceService, never()).findByIdForRead(any());
    }

    @Test
    void shouldFallbackToDatabaseWhenLockRetryStillMissesCache() {
        Product product = sampleProduct(1L);
        when(productCacheRepository.get(1L)).thenReturn(Optional.empty());
        when(productCacheRepository.tryLock(1L)).thenReturn(false);
        when(productPersistenceService.findByIdForRead(1L)).thenReturn(Optional.of(product));

        ProductDetailResponse response = productService.getById(1L);

        assertEquals("SLAVE_DB", response.source());
        verify(productCacheRepository).put(any(), any(), any());
    }

    private Product sampleProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("Mechanical Keyboard");
        product.setPrice(new BigDecimal("499.00"));
        product.setStock(80);
        product.setDescription("Hot-sale keyboard for cache demo");
        product.setStatus("ONLINE");
        product.setCreateTime(LocalDateTime.of(2026, 3, 17, 10, 0, 0));
        product.setUpdateTime(LocalDateTime.of(2026, 3, 17, 10, 0, 0));
        return product;
    }
}
