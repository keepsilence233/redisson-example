package com.example.redisson.pubsub.listener;

import com.example.redisson.pubsub.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.listener.MessageListener;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 控制台消息监听器
 * 用于演示接收发布订阅消息，将消息打印到控制台
 */
@Slf4j
@Component
public class ConsoleMessageListener implements MessageListener<Message> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void onMessage(CharSequence channel, Message message) {
        String timestamp = DATE_FORMAT.format(new Date(message.getTimestamp()));
        log.info("=========================================");
        log.info("🔔 收到新消息");
        log.info("   📌 ID: {}", message.getId());
        log.info("   📂 Channel: {}", channel);
        log.info("   🏷️ Topic: {}", message.getTopic());
        log.info("   ⏰ 时间: {}", timestamp);
        log.info("   💬 内容: {}", message.getPayload());
        log.info("=========================================");
    }
}
