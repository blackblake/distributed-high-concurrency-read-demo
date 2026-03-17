package com.example.hw1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import com.example.hw1.cache.CachePolicy;
import com.example.hw1.cache.CacheValue;
import com.example.hw1.domain.Product;
import com.example.hw1.dto.CreateProductRequest;
import com.example.hw1.dto.ProductDetailResponse;
import com.example.hw1.dto.UpdateProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceCachingTest {

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
                100,
                2);
    }

    @Test
    void shouldReturnCacheHitWhenProductExistsInRedis() {
        when(productCacheRepository.get(1L)).thenReturn(Optional.of(CacheValue.of(sampleProduct(1L))));

        ProductDetailResponse response = productService.getById(1L);

        assertEquals("CACHE", response.source());
        assertEquals("Mechanical Keyboard", response.name());
    }

    @Test
    void shouldReturnNullCacheWhenNullMarkerExistsInRedis() {
        when(productCacheRepository.get(99L)).thenReturn(Optional.of(CacheValue.nullMarker()));

        ProductDetailResponse response = productService.getById(99L);

        assertEquals("NULL_CACHE", response.source());
        assertTrue(response.nullCache());
    }

    @Test
    void shouldLoadFromSlaveAndBackfillCacheWhenCacheMissOccurs() {
        Product product = sampleProduct(1L);
        when(productCacheRepository.get(1L)).thenReturn(Optional.empty());
        when(productCacheRepository.tryLock(1L)).thenReturn(true);
        when(productPersistenceService.findByIdForRead(1L)).thenReturn(Optional.of(product));

        ProductDetailResponse response = productService.getById(1L);

        assertEquals("SLAVE_DB", response.source());
        verify(productCacheRepository).put(eq(1L), eq(product), any());
        verify(productCacheRepository).unlock(1L);
    }

    @Test
    void shouldStoreNullMarkerWhenProductDoesNotExist() {
        when(productCacheRepository.get(99L)).thenReturn(Optional.empty());
        when(productCacheRepository.tryLock(99L)).thenReturn(true);
        when(productPersistenceService.findByIdForRead(99L)).thenReturn(Optional.empty());

        ProductDetailResponse response = productService.getById(99L);

        assertEquals("NULL_CACHE", response.source());
        assertTrue(response.nullCache());
        verify(productCacheRepository).putNull(99L, productService.nullValueTtl());
        verify(productCacheRepository).unlock(99L);
    }

    @Test
    void shouldEvictCacheAfterUpdate() {
        Product updated = sampleProduct(1L);
        when(productPersistenceService.update(eq(1L), any(UpdateProductRequest.class))).thenReturn(updated);

        ProductDetailResponse response = productService.update(1L, new UpdateProductRequest(
                "Mechanical Keyboard",
                new BigDecimal("499.00"),
                60,
                "Updated",
                "ONLINE"));

        assertEquals("MASTER_DB", response.source());
        verify(productCacheRepository).evict(1L);
    }

    @Test
    void shouldCreateProductThroughMaster() {
        Product created = sampleProduct(2L);
        when(productPersistenceService.create(any(CreateProductRequest.class))).thenReturn(created);

        ProductDetailResponse response = productService.create(new CreateProductRequest(
                "Wireless Mouse",
                new BigDecimal("199.00"),
                120,
                "Low latency office mouse",
                "ONLINE"));

        assertEquals("MASTER_DB", response.source());
        assertEquals(2L, response.id());
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
