package com.example.redisson.visit.controller;

import com.example.redisson.visit.service.VisitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * VisitController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class VisitControllerTest {

    @Mock
    private VisitService visitService;

    private VisitController visitController;

    @BeforeEach
    void setUp() {
        visitController = new VisitController(visitService);
    }

    @Test
    void testRecordVisit_Success() {
        // Given
        String targetId = "product-1";
        String userId = "user-123";
        doNothing().when(visitService).recordVisit(targetId, userId);

        // When
        ResponseEntity<String> response = visitController.recordVisit(targetId, userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Visit recorded"));
        assertTrue(response.getBody().contains(userId));
        assertTrue(response.getBody().contains(targetId));
        verify(visitService, times(1)).recordVisit(targetId, userId);
    }

    @Test
    void testRecordVisit_MultipleVisits() {
        // Given
        String targetId = "product-1";
        doNothing().when(visitService).recordVisit(anyString(), anyString());

        // When
        visitController.recordVisit(targetId, "user-1");
        visitController.recordVisit(targetId, "user-2");
        visitController.recordVisit(targetId, "user-3");

        // Then
        verify(visitService, times(3)).recordVisit(eq(targetId), anyString());
    }

    @Test
    void testGetVisitCount_Success() {
        // Given
        String targetId = "product-1";
        long expectedCount = 100L;
        when(visitService.getVisitCount(targetId)).thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response = visitController.getVisitCount(targetId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedCount, response.getBody());
        verify(visitService, times(1)).getVisitCount(targetId);
    }

    @Test
    void testGetVisitCount_Zero() {
        // Given
        String targetId = "new-product";
        when(visitService.getVisitCount(targetId)).thenReturn(0L);

        // When
        ResponseEntity<Long> response = visitController.getVisitCount(targetId);

        // Then
        assertNotNull(response);
        assertEquals(0L, response.getBody());
    }
}
