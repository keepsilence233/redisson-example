package com.example.redisson.service;

import com.example.redisson.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductService 单元测试（使用真实的产品数据库 Map）
 */
class ProductServiceTest {

    private ProductService productService;

    @BeforeEach
    void setUp() {
        // 注意：这里使用真实的 ConcurrentHashMap 模拟数据库
        // 实际项目中可以使用 @DataJpaTest 或 TestContainers 进行集成测试
        Map<Long, Product> testDatabase = new ConcurrentHashMap<>();
        testDatabase.put(1L, new Product(1L, "Product-1", 100));
        testDatabase.put(2L, new Product(2L, "Product-2", 200));
        testDatabase.put(3L, new Product(3L, "Product-3", 300));

        // 由于 ProductService 依赖 RedissonClient，这里仅测试纯 Java 逻辑部分
        // 完整的集成测试需要启动 Redis 服务
    }

    @Test
    void testProductEquals() {
        // Given
        Product product1 = new Product(1L, "Product-1", 100);
        Product product2 = new Product(1L, "Product-1", 100);
        Product product3 = new Product(2L, "Product-2", 200);

        // When & Then
        assertEquals(product1, product2);
        assertNotEquals(product1, product3);
    }

    @Test
    void testProductHashCode() {
        // Given
        Product product1 = new Product(1L, "Product-1", 100);
        Product product2 = new Product(1L, "Product-1", 100);

        // When & Then
        assertEquals(product1.hashCode(), product2.hashCode());
    }

    @Test
    void testProductToString() {
        // Given
        Product product = new Product(1L, "Product-1", 100);

        // When
        String result = product.toString();

        // Then
        assertTrue(result.contains("Product-1"));
        assertTrue(result.contains("100"));
    }
}
