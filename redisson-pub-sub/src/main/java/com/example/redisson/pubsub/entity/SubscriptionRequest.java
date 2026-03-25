package com.example.redisson.pubsub.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订阅请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    /**
     * 主题名称（精确匹配）
     */
    private String topic;

    /**
     * 主题模式（通配符匹配，如 events.*.login）
     */
    private String pattern;
}
