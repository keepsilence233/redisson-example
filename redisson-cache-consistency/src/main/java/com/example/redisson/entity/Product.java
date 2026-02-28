package com.example.redisson.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品实体类
 * 用于演示缓存中的数据对象，必须实现 Serializable 接口以便序列化存储到 Redis
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    /**
     * 商品 ID（主键）
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品库存
     */
    private Integer stock;
}
