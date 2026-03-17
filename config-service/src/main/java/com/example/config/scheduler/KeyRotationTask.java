package com.example.config.scheduler;

import com.example.config.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 密钥轮换定时任务
 * 
 * 功能：
 * 1. 定期轮换加密密钥
 * 2. 检查密钥有效期
 * 3. 告警即将过期的密钥
 */
@Component
@ConditionalOnProperty(name = "scheduler.key-rotation.enabled", havingValue = "true", matchIfMissing = true)
public class KeyRotationTask {

    private static final Logger logger = LoggerFactory.getLogger(KeyRotationTask.class);

    @Autowired
    private EncryptionService encryptionService;

    /**
     * 密钥轮换任务
     * 默认每天凌晨 2 点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void rotateKeys() {
        logger.info("开始密钥轮换任务...");
        
        try {
            // 1. 轮换数据加密密钥
            encryptionService.rotateDataKey();
            
            // 2. 记录轮换日志
            logger.info("密钥轮换完成，时间: {}", LocalDateTime.now());
        } catch (Exception e) {
            logger.error("密钥轮换失败", e);
        }
    }

    /**
     * 密钥健康检查
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1 小时
    public void checkKeyHealth() {
        logger.debug("开始密钥健康检查...");
        
        try {
            // 1. 检查主密钥状态
            Map<String, Object> healthStatus = encryptionService.checkKeyHealth();
            
            // 2. 检查即将过期的密钥
            int expiringKeys = (int) healthStatus.getOrDefault("expiringKeys", 0);
            
            if (expiringKeys > 0) {
                logger.warn("发现 {} 个即将过期的密钥", expiringKeys);
            }
            
            logger.debug("密钥健康检查完成");
        } catch (Exception e) {
            logger.error("密钥健康检查失败", e);
        }
    }

    /**
     * 密钥备份任务
     * 每周日凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void backupKeys() {
        logger.info("开始密钥备份任务...");
        
        try {
            // 1. 备份密钥到安全存储
            encryptionService.backupKeys();
            
            logger.info("密钥备份完成");
        } catch (Exception e) {
            logger.error("密钥备份失败", e);
        }
    }

    /**
     * 密钥版本清理
     * 每月 1 号凌晨 4 点执行
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void cleanOldKeyVersions() {
        logger.info("开始清理旧密钥版本...");
        
        try {
            // 保留最近 3 个版本
            int deleted = encryptionService.cleanOldKeyVersions(3);
            
            logger.info("清理完成，删除 {} 个旧密钥版本", deleted);
        } catch (Exception e) {
            logger.error("清理旧密钥版本失败", e);
        }
    }
}
