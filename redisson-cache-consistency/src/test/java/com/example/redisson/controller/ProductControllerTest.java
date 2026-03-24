package com.example.redisson.controller;

import com.example.redisson.entity.Product;
import com.example.redisson.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ProductController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private ProductController productController;

    @BeforeEach
    void setUp() {
        productController = new ProductController(productService);
    }

    @Test
    void testGetProduct_Success() {
        // Given
        Long productId = 1L;
        Product expectedProduct = new Product(productId, "Test Product", 100);
        when(productService.getProduct(productId)).thenReturn(expectedProduct);

        // When
        Product result = productController.getProduct(productId);

        // Then
        assertNotNull(result);
        assertEquals(productId, result.getId());
        assertEquals("Test Product", result.getName());
        verify(productService, times(1)).getProduct(productId);
    }

    @Test
    void testGetProduct_NotFound() {
        // Given
        Long productId = 999L;
        when(productService.getProduct(productId)).thenReturn(null);

        // When
        Product result = productController.getProduct(productId);

        // Then
        assertNull(result);
        verify(productService, times(1)).getProduct(productId);
    }

    @Test
    void testUpdateProduct_Success() {
        // Given
        Product inputProduct = new Product(1L, "Updated Product", 200);
        Product returnedProduct = new Product(1L, "Updated Product", 200);
        when(productService.updateProduct(any(Product.class))).thenReturn(returnedProduct);

        // When
        Product result = productController.updateProduct(inputProduct);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Updated Product", result.getName());
        assertEquals(200, result.getStock());
        verify(productService, times(1)).updateProduct(any(Product.class));
    }

    @Test
    void testUpdateProduct_NullProduct() {
        // Given
        when(productService.updateProduct(null)).thenThrow(IllegalArgumentException.class);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> productController.updateProduct(null));
    }
}
