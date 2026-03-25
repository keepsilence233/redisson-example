package com.example.redisson.pubsub.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息实体类
 * 用于封装发布订阅的消息内容
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一标识
     */
    private String id;

    /**
     * 主题名称
     */
    private String topic;

    /**
     * 消息负载（可以是任何可序列化的对象）
     */
    private Object payload;

    /**
     * 时间戳
     */
    private Long timestamp;
}
