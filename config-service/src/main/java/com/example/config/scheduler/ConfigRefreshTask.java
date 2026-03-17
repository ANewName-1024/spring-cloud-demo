package com.example.config.scheduler;

import com.example.config.ConfigHistoryRepository;
import com.example.config.ConfigService;
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
 * 配置刷新定时任务
 * 
 * 功能：
 * 1. 定期刷新配置缓存
 * 2. 清理过期配置历史
 * 3. 同步最新配置到各服务
 */
@Component
@ConditionalOnProperty(name = "scheduler.config-refresh.enabled", havingValue = "true", matchIfMissing = true)
public class ConfigRefreshTask {

    private static final Logger logger = LoggerFactory.getLogger(ConfigRefreshTask.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private ConfigHistoryRepository configHistoryRepository;

    @Autowired
    private EncryptionService encryptionService;

    /**
     * 配置刷新任务
     * 默认每 5 分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5 分钟
    public void refreshConfig() {
        logger.debug("执行配置刷新任务...");
        
        try {
            // 1. 刷新配置缓存
            configService.refreshConfigCache();
            
            // 2. 验证加密密钥
            encryptionService.verifyMasterKey();
            
            logger.debug("配置刷新完成");
        } catch (Exception e) {
            logger.error("配置刷新失败", e);
        }
    }

    /**
     * 清理过期配置历史
     * 默认每天凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredHistory() {
        logger.info("开始清理过期配置历史...");
        
        try {
            LocalDateTime expiredDate = LocalDateTime.now().minusDays(90); // 保留 90 天
            int deleted = configHistoryRepository.deleteByCreateTimeBefore(expiredDate);
            
            logger.info("清理完成，删除 {} 条过期记录", deleted);
        } catch (Exception e) {
            logger.error("清理过期配置历史失败", e);
        }
    }

    /**
     * 配置同步任务
     * 将配置推送到各服务
     * 默认每 10 分钟执行一次
     */
    @Scheduled(fixedRate = 600000) // 10 分钟
    public void syncConfigToServices() {
        logger.debug("开始同步配置到各服务...");
        
        try {
            // 获取所有注册服务
            Map<String, String> services = configService.getRegisteredServices();
            
            for (Map.Entry<String, String> entry : services.entrySet()) {
                String serviceName = entry.getKey();
                String serviceUrl = entry.getValue();
                
                // 推送配置到各服务
                boolean success = configService.pushConfigToService(serviceName, serviceUrl);
                
                if (success) {
                    logger.debug("配置推送成功: {}", serviceName);
                } else {
                    logger.warn("配置推送失败: {}", serviceName);
                }
            }
            
            logger.debug("配置同步完成");
        } catch (Exception e) {
            logger.error("配置同步失败", e);
        }
    }

    /**
     * 配置变更检测任务
     * 检测 Git 仓库配置变更
     * 默认每 1 分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 1 分钟
    public void checkConfigChanges() {
        try {
            boolean hasChanges = configService.checkGitConfigChanges();
            
            if (hasChanges) {
                logger.info("检测到配置变更，触发刷新...");
                refreshConfig();
            }
        } catch (Exception e) {
            logger.error("配置变更检测失败", e);
        }
    }
}
