package com.example.redisson.controller;

import com.example.redisson.entity.Product;
import com.example.redisson.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口控制器
 * 提供商品信息的查询和更新接口，用于演示 Redisson 缓存一致性
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    /**
     * 构造方法，注入 ProductService
     *
     * @param productService 商品服务实现类
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 根据商品 ID 获取商品信息
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    @GetMapping("/{id}")
    public Product getProduct(@PathVariable("id") Long id) {
        return productService.getProduct(id);
    }

    /**
     * 更新商品信息
     *
     * @param product 包含更新内容的商品对象
     * @return 更新后的商品信息
     */
    @PostMapping
    public Product updateProduct(@RequestBody Product product) {
        return productService.updateProduct(product);
    }
}
