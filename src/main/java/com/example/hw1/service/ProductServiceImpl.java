package com.example.hw1.service;

import java.time.Duration;
import java.util.Optional;

import com.example.hw1.cache.CachePolicy;
import com.example.hw1.cache.CacheValue;
import com.example.hw1.domain.Product;
import com.example.hw1.dto.CreateProductRequest;
import com.example.hw1.dto.ProductDetailResponse;
import com.example.hw1.dto.UpdateProductRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductCacheRepository productCacheRepository;
    private final ProductPersistenceService productPersistenceService;
    private final CachePolicy cachePolicy;
    private final long lockRetryMillis;
    private final int lockMaxRetries;

    public ProductServiceImpl(
            ProductCacheRepository productCacheRepository,
            ProductPersistenceService productPersistenceService,
            CachePolicy cachePolicy,
            @Value("${app.cache.lock-retry-millis}") long lockRetryMillis,
            @Value("${app.cache.lock-max-retries}") int lockMaxRetries) {
        this.productCacheRepository = productCacheRepository;
        this.productPersistenceService = productPersistenceService;
        this.cachePolicy = cachePolicy;
        this.lockRetryMillis = lockRetryMillis;
        this.lockMaxRetries = lockMaxRetries;
    }

    @Override
    public ProductDetailResponse create(CreateProductRequest request) {
        return toResponse(productPersistenceService.create(request), "MASTER_DB");
    }

    @Override
    public ProductDetailResponse getById(Long id) {
        Optional<CacheValue> cached = productCacheRepository.get(id);
        if (cached.isPresent()) {
            return fromCacheValue(id, cached.get());
        }

        if (productCacheRepository.tryLock(id)) {
            try {
                return loadFromReadDatabaseAndBackfill(id);
            } finally {
                productCacheRepository.unlock(id);
            }
        }

        for (int attempt = 0; attempt < lockMaxRetries; attempt++) {
            sleepQuietly();
            Optional<CacheValue> retryCached = productCacheRepository.get(id);
            if (retryCached.isPresent()) {
                return fromCacheValue(id, retryCached.get());
            }
        }

        return loadFromReadDatabaseAndBackfill(id);
    }

    @Override
    public ProductDetailResponse update(Long id, UpdateProductRequest request) {
        Product product = productPersistenceService.update(id, request);
        productCacheRepository.evict(id);
        return toResponse(product, "MASTER_DB");
    }

    @Override
    public ProductDetailResponse inspectRoute(Long id) {
        Optional<Product> product = productPersistenceService.findByIdForRead(id);
        return product.map(value -> toResponse(value, "SLAVE_DB"))
                .orElseGet(() -> nullResponse(id, "NULL_CACHE"));
    }

    @Override
    public ProductDetailResponse warmCache(Long id) {
        Optional<Product> product = productPersistenceService.findByIdForRead(id);
        if (product.isEmpty()) {
            productCacheRepository.putNull(id, cachePolicy.nullValueTtl());
            return nullResponse(id, "NULL_CACHE");
        }
        productCacheRepository.put(id, product.get(), cachePolicy.detailTtl());
        return toResponse(product.get(), "SLAVE_DB");
    }

    Duration nullValueTtl() {
        return cachePolicy.nullValueTtl();
    }

    private ProductDetailResponse loadFromReadDatabaseAndBackfill(Long id) {
        Optional<Product> product = productPersistenceService.findByIdForRead(id);
        if (product.isEmpty()) {
            productCacheRepository.putNull(id, cachePolicy.nullValueTtl());
            return nullResponse(id, "NULL_CACHE");
        }

        productCacheRepository.put(id, product.get(), cachePolicy.detailTtl());
        return toResponse(product.get(), "SLAVE_DB");
    }

    private ProductDetailResponse fromCacheValue(Long id, CacheValue cacheValue) {
        if (cacheValue.nullCache()) {
            return nullResponse(id, "NULL_CACHE");
        }
        return toResponse(cacheValue.product(), "CACHE");
    }

    private ProductDetailResponse toResponse(Product product, String source) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getDescription(),
                product.getStatus(),
                source,
                false,
                product.getCreateTime(),
                product.getUpdateTime());
    }

    private ProductDetailResponse nullResponse(Long id, String source) {
        return new ProductDetailResponse(
                id,
                null,
                null,
                null,
                null,
                null,
                source,
                true,
                null,
                null);
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(lockRetryMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
