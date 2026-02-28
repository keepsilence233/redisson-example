package com.example.redisson.controller;

import com.example.redisson.service.VisitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/visit")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    /**
     * Record a new visit for a target ID.
     *
     * @param targetId The ID of the product or store (e.g., product-1)
     * @param userId The ID of the visiting user
     * @return Confirmation message
     */
    @PostMapping("/{targetId}")
    public ResponseEntity<String> recordVisit(
            @PathVariable("targetId") String targetId,
            @RequestParam String userId) {
        
        visitService.recordVisit(targetId, userId);
        return ResponseEntity.ok("Visit recorded for user: " + userId + " on target: " + targetId);
    }

    /**
     * Get the estimated visit count for a target ID.
     *
     * @param targetId The ID of the product or store
     * @return The estimated number of unique visitors
     */
    @GetMapping("/{targetId}/count")
    public ResponseEntity<Long> getVisitCount(@PathVariable String targetId) {
        long count = visitService.getVisitCount(targetId);
        return ResponseEntity.ok(count);
    }
}
