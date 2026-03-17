package com.example.ops.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MetricsServiceTest {

    @Autowired
    private MetricsService metricsService;

    @Test
    void testRecordRequest() {
        Map<String, Object> requests = (Map<String, Object>) metricsService.getMetrics().get("requests");
        long before = (Long) requests.get("total");
        
        metricsService.recordRequest();
        
        Map<String, Object> requestsAfter = (Map<String, Object>) metricsService.getMetrics().get("requests");
        long after = (Long) requestsAfter.get("total");
        assertEquals(before + 1, after);
    }

    @Test
    void testRecordError() {
        Map<String, Object> requests = (Map<String, Object>) metricsService.getMetrics().get("requests");
        long before = (Long) requests.get("errors");
        
        metricsService.recordError();
        
        Map<String, Object> requestsAfter = (Map<String, Object>) metricsService.getMetrics().get("requests");
        long after = (Long) requestsAfter.get("errors");
        assertEquals(before + 1, after);
    }

    @Test
    void testGetMetrics() {
        var metrics = metricsService.getMetrics();
        
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("requests"));
        assertTrue(metrics.containsKey("memory"));
        assertTrue(metrics.containsKey("system"));
        assertTrue(metrics.containsKey("threads"));
    }

    @Test
    void testGetLoggerLevels() {
        var loggers = metricsService.getLoggerLevels();
        
        assertNotNull(loggers);
        assertTrue(loggers.containsKey("root"));
        assertTrue(loggers.containsKey("com.example"));
    }
}
