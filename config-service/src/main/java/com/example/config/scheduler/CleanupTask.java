package com.example.config.scheduler;

import com.example.config.ConfigHistory;
import com.example.config.ConfigHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据清理定时任务
 * 
 * 功能：
 * 1. 清理过期日志
 * 2. 清理临时文件
 * 3. 清理无效配置
 */
@Component
@ConditionalOnProperty(name = "scheduler.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class CleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(CleanupTask.class);

    @Autowired
    private ConfigHistoryRepository configHistoryRepository;

    /**
     * 清理配置历史
     * 默认每天凌晨执行
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanConfigHistory() {
        logger.info("开始清理配置历史...");
        
        try {
            // 清理 90 天前的历史
            LocalDateTime threshold = LocalDateTime.now().minusDays(90);
            List<ConfigHistory> expired = configHistoryRepository.findByCreateTimeBefore(threshold);
            
            if (!expired.isEmpty()) {
                configHistoryRepository.deleteAll(expired);
                logger.info("清理完成，删除 {} 条配置历史", expired.size());
            } else {
                logger.info("无需清理的配置历史");
            }
        } catch (Exception e) {
            logger.error("清理配置历史失败", e);
        }
    }

    /**
     * 清理临时文件
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanTempFiles() {
        logger.debug("开始清理临时文件...");
        
        try {
            // 清理临时配置文件
            int cleaned = cleanupTempFiles("/tmp/config-service");
            
            if (cleaned > 0) {
                logger.info("清理完成，删除 {} 个临时文件", cleaned);
            }
        } catch (Exception e) {
            logger.error("清理临时文件失败", e);
        }
    }

    /**
     * 清理无效配置
     * 每天中午 12 点执行
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void cleanInvalidConfigs() {
        logger.info("开始清理无效配置...");
        
        try {
            // 清理无效的配置
            int cleaned = configHistoryRepository.deleteInvalidConfigs();
            
            logger.info("清理完成，删除 {} 个无效配置", cleaned);
        } catch (Exception e) {
            logger.error("清理无效配置失败", e);
        }
    }

    /**
     * 清理临时文件
     */
    private int cleanupTempFiles(String tempDir) {
        // 实现临时文件清理逻辑
        return 0;
    }
}
