package com.example.redisson.pubsub.service;

import com.example.redisson.pubsub.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RPatternTopic;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.listener.PatternMessageListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 消息服务类
 * 负责处理消息的发布和订阅，使用 Redisson RTopic 和 RPatternTopic 实现
 */
@Slf4j
@Service
public class MessageService {

    private final RedissonClient redissonClient;

    /**
     * 监听器管理：topic -> 监听器ID映射
     */
    private final Map<String, Integer> topicListeners = new ConcurrentHashMap<>();

    /**
     * 模式监听器管理：pattern -> 监听器ID映射
     */
    private final Map<String, Integer> patternListeners = new ConcurrentHashMap<>();

    /**
     * 生成唯一监听器标识的计数器
     */
    private final AtomicInteger listenerCounter = new AtomicInteger(0);

    /**
     * 构造方法，注入 Redisson 客户端
     *
     * @param redissonClient Redisson 客户端实例
     */
    public MessageService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 发布消息到指定主题
     *
     * @param topic   主题名称
     * @param payload 消息负载
     */
    public void publish(String topic, Object payload) {
        log.info("发布消息到主题: {}，内容: {}", topic, payload);

        RTopic rTopic = redissonClient.getTopic(topic);
        Message message = new Message(
                java.util.UUID.randomUUID().toString(),
                topic,
                payload,
                System.currentTimeMillis()
        );
        rTopic.publish(message);

        log.info("消息发布成功: {}", message.getId());
    }

    /**
     * 订阅指定主题（精确匹配）
     *
     * @param topic    主题名称
     * @param listener 消息监听器
     */
    public void subscribe(String topic, MessageListener<Message> listener) {
        log.info("订阅主题: {}", topic);

        RTopic rTopic = redissonClient.getTopic(topic);
        int listenerId = rTopic.addListener(Message.class, listener);

        String key = generateListenerKey("topic", topic);
        topicListeners.put(key, listenerId);

        log.info("主题订阅成功: {} (监听器ID: {})", topic, listenerId);
    }

    /**
     * 取消订阅指定主题
     *
     * @param topic 主题名称
     */
    public void unsubscribe(String topic) {
        log.info("取消订阅主题: {}", topic);

        RTopic rTopic = redissonClient.getTopic(topic);
        String prefix = "topic-" + topic + "-";

        topicListeners.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                rTopic.removeListener(entry.getValue());
                log.info("已移除监听器: {}", entry.getValue());
                return true;
            }
            return false;
        });

        log.info("主题取消订阅完成: {}", topic);
    }

    /**
     * 订阅主题模式（通配符匹配）
     * 支持模式如: events.*.login, notifications.*
     *
     * @param pattern  主题模式
     * @param listener 模式消息监听器
     */
    public void patternSubscribe(String pattern, PatternMessageListener<Message> listener) {
        log.info("订阅模式: {}", pattern);

        RPatternTopic patternTopic = redissonClient.getPatternTopic(pattern);
        int listenerId = patternTopic.addListener(Message.class, listener);

        String key = generateListenerKey("pattern", pattern);
        patternListeners.put(key, listenerId);

        log.info("模式订阅成功: {} (监听器ID: {})", pattern, listenerId);
    }

    /**
     * 取消订阅主题模式
     *
     * @param pattern 模式
     */
    public void patternUnsubscribe(String pattern) {
        log.info("取消订阅模式: {}", pattern);

        RPatternTopic patternTopic = redissonClient.getPatternTopic(pattern);
        String prefix = "pattern-" + pattern + "-";

        patternListeners.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                patternTopic.removeListener(entry.getValue());
                log.info("已移除监听器: {}", entry.getValue());
                return true;
            }
            return false;
        });

        log.info("模式取消订阅完成: {}", pattern);
    }

    /**
     * 获取所有已订阅的主题
     *
     * @return 主题列表
     */
    public java.util.Set<String> getSubscribedTopics() {
        return topicListeners.keySet().stream()
                .map(key -> key.substring(key.indexOf("-") + 1, key.lastIndexOf("-")))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 获取所有已订阅的模式
     *
     * @return 模式列表
     */
    public java.util.Set<String> getSubscribedPatterns() {
        return patternListeners.keySet().stream()
                .map(key -> key.substring(key.indexOf("-") + 1, key.lastIndexOf("-")))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 生成监听器唯一键
     *
     * @param type    监听器类型 (topic/pattern)
     * @param subject 主题或模式
     * @return 唯一键
     */
    private String generateListenerKey(String type, String subject) {
        int id = listenerCounter.incrementAndGet();
        return type + "-" + subject + "-" + id;
    }
}
