package com.example.redisson.pubsub.controller;

import com.example.redisson.pubsub.entity.PublishRequest;
import com.example.redisson.pubsub.entity.SubscriptionRequest;
import com.example.redisson.pubsub.listener.ConsoleMessageListener;
import com.example.redisson.pubsub.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 消息发布订阅控制器
 * <p>
 * 提供基于 Redisson RTopic 和 RPatternTopic 的发布订阅功能的 REST API 接口。
 * 支持精确主题订阅、模式订阅（通配符匹配）、消息发布、订阅管理等功能。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final ConsoleMessageListener consoleMessageListener;

    /**
     * 构造函数注入依赖。
     * <p>
     * 使用 Spring 构造函数注入机制自动装配服务组件和默认监听器。
     * </p>
     *
     * @param messageService         消息服务，处理发布订阅业务逻辑
     * @param consoleMessageListener 控制台消息监听器，用于接收并打印订阅消息
     */
    public MessageController(MessageService messageService, ConsoleMessageListener consoleMessageListener) {
        this.messageService = messageService;
        this.consoleMessageListener = consoleMessageListener;
    }

    /**
     * 发布消息到指定主题。
     * <p>
     * 将消息发布到 Redis 指定主题，所有订阅该主题的客户端将收到此消息。
     * 消息会被封装为 {@link com.example.redisson.pubsub.entity.Message} 格式，包含唯一 ID、主题、负载和时间戳。
     * </p>
     *
     * @param request 消息发布请求，包含主题名称和消息内容
     * @return HTTP 200 成功响应
     * @throws IllegalArgumentException 当主题名称为空或空白时抛出
     */
    @PostMapping("/publish")
    public ResponseEntity<Void> publishMessage(@RequestBody PublishRequest request) {
        log.info("收到发布请求: 主题={}, 内容={}", request.getTopic(), request.getMessage());

        if (request.getTopic() == null || request.getTopic().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        messageService.publish(request.getTopic(), request.getMessage());
        return ResponseEntity.ok().build();
    }

    /**
     * 订阅指定主题（精确匹配）。
     * <p>
     * 订阅 Redis 指定主题，当有消息发布到该主题时，控制台监听器将接收并打印消息。
     * 使用 {@link org.redisson.api.RTopic} 实现精确主题匹配。
     * </p>
     *
     * @param request 订阅请求，包含要订阅的主题名称
     * @return HTTP 200 成功响应
     * @throws IllegalArgumentException 当主题名称为空或空白时抛出
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribeTopic(@RequestBody SubscriptionRequest request) {
        log.info("收到订阅请求: 主题={}", request.getTopic());

        if (request.getTopic() == null || request.getTopic().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        messageService.subscribe(request.getTopic(), consoleMessageListener);
        return ResponseEntity.ok().build();
    }

    /**
     * 订阅主题模式（通配符匹配）。
     * <p>
     * 订阅主题模式，支持通配符匹配（如 {@code events.*.login}、{@code notifications.*}）。
     * 使用 {@link org.redisson.api.RPatternTopic} 实现模式匹配订阅。
     * 当有消息发布到匹配模式的主题时，控制台监听器将接收并打印消息。
     * </p>
     *
     * @param request 订阅请求，包含要订阅的主题模式
     * @return HTTP 200 成功响应
     * @throws IllegalArgumentException 当模式为空或空白时抛出
     */
    @PostMapping("/pattern-subscribe")
    public ResponseEntity<Void> patternSubscribe(@RequestBody SubscriptionRequest request) {
        log.info("收到模式订阅请求: 模式={}", request.getPattern());

        if (request.getPattern() == null || request.getPattern().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        messageService.patternSubscribe(
                request.getPattern(),
                (pattern, channel, msg) -> consoleMessageListener.onMessage(channel, msg)
        );
        return ResponseEntity.ok().build();
    }

    /**
     * 取消订阅指定主题。
     * <p>
     * 取消对指定主题的订阅，移除所有该主题的监听器。
     * </p>
     *
     * @param topic 要取消订阅的主题名称
     * @return HTTP 200 成功响应
     */
    @DeleteMapping("/unsubscribe/{topic}")
    public ResponseEntity<Void> unsubscribeTopic(@PathVariable("topic") String topic) {
        log.info("收到取消订阅请求: 主题={}", topic);
        messageService.unsubscribe(topic);
        return ResponseEntity.ok().build();
    }

    /**
     * 取消订阅指定主题模式。
     * <p>
     * 取消对指定主题模式的订阅，移除所有该模式的监听器。
     * </p>
     *
     * @param pattern 要取消订阅的主题模式
     * @return HTTP 200 成功响应
     */
    @DeleteMapping("/pattern-unsubscribe/{pattern}")
    public ResponseEntity<Void> patternUnsubscribe(@PathVariable("pattern") String pattern) {
        log.info("收到取消模式订阅请求: 模式={}", pattern);
        messageService.patternUnsubscribe(pattern);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取所有已订阅的主题列表。
     * <p>
     * 返回当前已订阅的所有精确匹配主题的名称集合。
     * </p>
     *
     * @return HTTP 200 响应，包含已订阅主题名称的 Set 集合
     */
    @GetMapping("/subscriptions/topics")
    public ResponseEntity<java.util.Set<String>> getSubscribedTopics() {
        java.util.Set<String> topics = messageService.getSubscribedTopics();
        return ResponseEntity.ok(topics);
    }

    /**
     * 获取所有已订阅的主题模式列表。
     * <p>
     * 返回当前已订阅的所有模式（通配符匹配）的名称集合。
     * </p>
     *
     * @return HTTP 200 响应，包含已订阅模式名称的 Set 集合
     */
    @GetMapping("/subscriptions/patterns")
    public ResponseEntity<java.util.Set<String>> getSubscribedPatterns() {
        java.util.Set<String> patterns = messageService.getSubscribedPatterns();
        return ResponseEntity.ok(patterns);
    }

    /**
     * 获取发布订阅统计信息。
     * <p>
     * 返回当前订阅状态的统计信息，包括主题数量和模式数量。
     * </p>
     *
     * @return HTTP 200 响应，包含统计信息的 Map
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = Map.of(
                "topicCount", messageService.getSubscribedTopics().size(),
                "patternCount", messageService.getSubscribedPatterns().size(),
                "totalSubscriptions", messageService.getSubscribedTopics().size() + messageService.getSubscribedPatterns().size()
        );
        return ResponseEntity.ok(stats);
    }
}
