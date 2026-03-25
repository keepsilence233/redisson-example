package com.example.redisson.pubsub.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息发布请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublishRequest {

    /**
     * 主题名称
     */
    private String topic;

    /**
     * 消息内容
     */
    private Object message;
}
