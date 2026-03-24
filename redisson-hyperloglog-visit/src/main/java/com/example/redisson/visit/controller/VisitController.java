package com.example.redisson.visit.controller;

import com.example.redisson.visit.service.VisitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 访问统计控制器
 * 基于 Redis HyperLogLog 实现 UV 统计
 */
@RestController
@RequestMapping("/visit")
public class VisitController {

    private final VisitService visitService;

    /**
     * 构造方法，注入 VisitService
     *
     * @param visitService 访问服务实现类
     */
    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    /**
     * 记录一次访问
     *
     * @param targetId 目标 ID（商品或店铺 ID）
     * @param userId   用户 ID
     * @return 确认消息
     */
    @PostMapping("/{targetId}")
    public ResponseEntity<String> recordVisit(
            @PathVariable("targetId") String targetId,
            @RequestParam("userId") String userId) {

        visitService.recordVisit(targetId, userId);
        return ResponseEntity.ok("Visit recorded for user: " + userId + " on target: " + targetId);
    }

    /**
     * 获取访问数量（估算值）
     *
     * @param targetId 目标 ID
     * @return 独立访客数量
     */
    @GetMapping("/{targetId}/count")
    public ResponseEntity<Long> getVisitCount(@PathVariable("targetId") String targetId) {
        long count = visitService.getVisitCount(targetId);
        return ResponseEntity.ok(count);
    }
}
