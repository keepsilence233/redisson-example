package com.example.redisson.visit.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RHyperLogLog;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 访问服务类
 * 基于 Redis HyperLogLog 实现独立访客（UV）统计
 */
@Slf4j
@Service
public class VisitService {

    private final RedissonClient redissonClient;

    /**
     * 访问统计键前缀
     */
    private static final String VISIT_KEY_PREFIX = "visit:hll:";

    /**
     * 构造方法，初始化 Redisson 客户端
     *
     * @param redissonClient Redisson 客户端实例
     */
    public VisitService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 记录一次访问
     * HyperLogLog 会自动去重，相同 userId 多次访问只计一次
     *
     * @param targetId 目标 ID（商品或店铺）
     * @param userId   用户 ID
     */
    public void recordVisit(String targetId, String userId) {
        String key = VISIT_KEY_PREFIX + targetId;
        RHyperLogLog<String> hyperLogLog = redissonClient.getHyperLogLog(key);
        hyperLogLog.add(userId);
        log.debug("Recorded visit for user: {} on target: {}", userId, targetId);
    }

    /**
     * 获取访问数量（估算值）
     * HyperLogLog 的统计存在约 0.81% 的标准误差
     *
     * @param targetId 目标 ID
     * @return 独立访客数量（估算值）
     */
    public long getVisitCount(String targetId) {
        String key = VISIT_KEY_PREFIX + targetId;
        RHyperLogLog<String> hyperLogLog = redissonClient.getHyperLogLog(key);
        long count = hyperLogLog.count();
        log.debug("Get visit count for target: {}: {}", targetId, count);
        return count;
    }
}
