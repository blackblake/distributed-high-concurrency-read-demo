package com.example.hw1.seckill.controller;

import java.time.Duration;

import com.example.hw1.seckill.dto.SeckillRequest;
import com.example.hw1.seckill.dto.SeckillResult;
import com.example.hw1.seckill.service.SeckillService;
import com.example.hw1.seckill.service.StockService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;
    private final StockService stockService;
    private final int defaultStockTtlHours;

    public SeckillController(SeckillService seckillService,
                             StockService stockService,
                             @Value("${app.seckill.stock-init-ttl-hours:24}") int defaultStockTtlHours) {
        this.seckillService = seckillService;
        this.stockService = stockService;
        this.defaultStockTtlHours = defaultStockTtlHours;
    }

    @PostMapping("/orders")
    public SeckillResult seckill(@Valid @RequestBody SeckillRequest request) {
        return seckillService.seckill(request);
    }

    @GetMapping("/stock/{productId}")
    public Long currentStock(@PathVariable Long productId) {
        return stockService.currentStock(productId);
    }

    /** Admin-style helper to prime Redis stock for a product. */
    @PostMapping("/stock/{productId}")
    public String initStock(@PathVariable Long productId,
                            @RequestParam long quantity,
                            @RequestParam(required = false) Integer ttlHours) {
        Duration ttl = Duration.ofHours(ttlHours == null ? defaultStockTtlHours : ttlHours);
        stockService.initStock(productId, quantity, ttl);
        return "stock initialised: product=" + productId + " qty=" + quantity;
    }
}
