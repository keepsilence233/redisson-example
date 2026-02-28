package com.example.redisson.service;

import org.redisson.api.RHyperLogLog;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class VisitService {

    private final RedissonClient redissonClient;
    
    // Key prefix for a product or store visit count
    private static final String VISIT_KEY_PREFIX = "visit:product:";

    public VisitService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Record a visit for a specific target (product or store).
     *
     * @param targetId The ID of the product or store being visited.
     * @param userId The ID or IP of the user visiting.
     */  
    public void recordVisit(String targetId, String userId) {
        String key = VISIT_KEY_PREFIX + targetId;
        RHyperLogLog<String> hyperLogLog = redissonClient.getHyperLogLog(key);
        hyperLogLog.add(userId);
    }

    /**
     * Get the estimated visit count for a specific target.
     *
     * @param targetId The ID of the product or store.
     * @return The estimated number of unique visitors.
     */
    public long getVisitCount(String targetId) {
        String key = VISIT_KEY_PREFIX + targetId;
        RHyperLogLog<String> hyperLogLog = redissonClient.getHyperLogLog(key);
        return hyperLogLog.count();
    }
}
