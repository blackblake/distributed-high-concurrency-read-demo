package com.example.hw1.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.hw1.dto.CreateProductRequest;
import com.example.hw1.dto.ProductDetailResponse;
import com.example.hw1.dto.UpdateProductRequest;
import com.example.hw1.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;

@WebMvcTest(ProductController.class)
class ProductControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void shouldCreateProductAndReturnCreatedResource() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "Mechanical Keyboard",
                new BigDecimal("499.00"),
                80,
                "Hot-sale keyboard for cache demo",
                "ONLINE");

        given(productService.create(any(CreateProductRequest.class))).willReturn(sampleResponse(1L, "MASTER_DB"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.source").value("MASTER_DB"))
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"));
    }

    @Test
    void shouldGetProductDetailWithSourceMetadata() throws Exception {
        given(productService.getById(1L)).willReturn(sampleResponse(1L, "CACHE"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.source").value("CACHE"))
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    void shouldUpdateProductAndReturnUpdatedDetail() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest(
                "Mechanical Keyboard Pro",
                new BigDecimal("599.00"),
                66,
                "Updated description",
                "ONLINE");

        given(productService.update(eq(1L), any(UpdateProductRequest.class))).willReturn(sampleResponse(1L, "MASTER_DB"));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.source").value("MASTER_DB"))
                .andExpect(jsonPath("$.price").value(499.00));
    }

    private ProductDetailResponse sampleResponse(Long id, String source) {
        return new ProductDetailResponse(
                id,
                "Mechanical Keyboard",
                new BigDecimal("499.00"),
                80,
                "Hot-sale keyboard for cache demo",
                "ONLINE",
                source,
                false,
                LocalDateTime.of(2026, 3, 17, 10, 0, 0),
                LocalDateTime.of(2026, 3, 17, 10, 0, 0));
    }
}
