package com.example.config.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 密钥管理服务
 * 支持软件根密钥和硬件根密钥
 */
@Service
public class KeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(KeyManagementService.class);

    @Value("${encryption.root-key-type:SOFTWARE}")
    private String rootKeyType;

    @Value("${encryption.root-key-file:}")
    private String rootKeyFile;

    @Value("${encryption.working-key-rotation-days:90}")
    private int workingKeyRotationDays;

    private String rootKey;
    private String workingKey;
    private String workingKeyId;
    private LocalDateTime workingKeyCreatedTime;
    private boolean isHardwareKeyAvailable = false;

    private final GmCryptUtil gmCryptUtil;

    public KeyManagementService(GmCryptUtil gmCryptUtil) {
        this.gmCryptUtil = gmCryptUtil;
    }

    @PostConstruct
    public void init() {
        if ("HARDWARE".equalsIgnoreCase(rootKeyType)) {
            initHardwareKey();
        } else {
            initSoftwareKey();
        }
    }

    // ==================== 根密钥管理 ====================

    private void initSoftwareKey() {
        if (rootKeyFile != null && !rootKeyFile.isEmpty()) {
            try {
                rootKey = loadRootKeyFromFile(rootKeyFile);
                log.info("从文件加载根密钥成功");
            } catch (Exception e) {
                log.warn("无法加载根密钥文件，生成新的根密钥");
                generateSoftwareRootKey();
            }
        } else {
            generateSoftwareRootKey();
        }
    }

    private void initHardwareKey() {
        try {
            isHardwareKeyAvailable = checkHardwareKeyAvailable();
            if (isHardwareKeyAvailable) {
                log.info("硬件根密钥可用");
            } else {
                log.warn("硬件密钥设备不可用，回退到软件根密钥");
                rootKeyType = "SOFTWARE";
                initSoftwareKey();
            }
        } catch (Exception e) {
            log.error("硬件密钥初始化失败", e);
            rootKeyType = "SOFTWARE";
            initSoftwareKey();
        }
    }

    public void generateSoftwareRootKey() {
        rootKey = gmCryptUtil.generateKey();
        log.info("生成新的软件根密钥");
        
        if (rootKeyFile != null && !rootKeyFile.isEmpty()) {
            try {
                saveRootKeyToFile(rootKeyFile, rootKey);
            } catch (Exception e) {
                log.error("保存根密钥失败", e);
            }
        }
    }

    // ==================== 工作密钥管理 ====================

    public String generateWorkingKey() {
        workingKey = gmCryptUtil.generateKey();
        workingKeyId = generateKeyId();
        workingKeyCreatedTime = LocalDateTime.now();
        
        log.info("生成新的工作密钥, ID: {}", workingKeyId);
        return workingKey;
    }

    public void rotateWorkingKey() {
        generateWorkingKey();
        log.info("工作密钥已轮换, 新 ID: {}", workingKeyId);
    }

    public String getActiveWorkingKey() {
        if (workingKey == null) {
            generateWorkingKey();
        }
        
        if (workingKeyCreatedTime != null) {
            LocalDateTime expiryDate = workingKeyCreatedTime.plusDays(workingKeyRotationDays);
            if (LocalDateTime.now().isAfter(expiryDate)) {
                log.info("工作密钥已过期，进行轮换");
                rotateWorkingKey();
            }
        }
        
        return workingKey;
    }

    public String getWorkingKeyId() {
        if (workingKeyId == null) {
            getActiveWorkingKey();
        }
        return workingKeyId;
    }

    // ==================== 加密/解密 ====================

    public String encrypt(String data) {
        String key = getActiveWorkingKey();
        return gmCryptUtil.encrypt(data, key);
    }

    public String decrypt(String encryptedData) {
        String key = getActiveWorkingKey();
        return gmCryptUtil.decrypt(encryptedData, key);
    }

    public String encryptWithRootKey(String data) {
        if (rootKey == null) {
            throw new RuntimeException("根密钥未初始化");
        }
        
        if (isHardwareKeyAvailable) {
            return encryptWithHardware(data);
        }
        
        return gmCryptUtil.encrypt(data, rootKey);
    }

    public String decryptWithRootKey(String encryptedData) {
        if (rootKey == null) {
            throw new RuntimeException("根密钥未初始化");
        }
        
        if (isHardwareKeyAvailable) {
            return decryptWithHardware(encryptedData);
        }
        
        return gmCryptUtil.decrypt(encryptedData, rootKey);
    }

    // ==================== 硬件密钥 ====================

    public boolean isHardwareKeyAvailable() {
        return isHardwareKeyAvailable;
    }

    private boolean checkHardwareKeyAvailable() {
        return false;
    }

    public String encryptWithHardware(String data) {
        if (!isHardwareKeyAvailable) {
            throw new RuntimeException("硬件密钥不可用");
        }
        throw new UnsupportedOperationException("硬件加密待实现");
    }

    public String decryptWithHardware(String encryptedData) {
        if (!isHardwareKeyAvailable) {
            throw new RuntimeException("硬件密钥不可用");
        }
        throw new UnsupportedOperationException("硬件解密待实现");
    }

    // ==================== 密钥备份/恢复 ====================

    public String backupRootKey(String password) {
        if (rootKey == null) {
            throw new RuntimeException("根密钥不存在");
        }
        String derivedKey = gmCryptUtil.sm3Kdf(password, "backup", 32);
        return gmCryptUtil.encrypt(rootKey, derivedKey);
    }

    public void restoreRootKey(String encryptedRootKey, String password) {
        String derivedKey = gmCryptUtil.sm3Kdf(password, "backup", 32);
        rootKey = gmCryptUtil.decrypt(encryptedRootKey, derivedKey);
        
        if (rootKeyFile != null && !rootKeyFile.isEmpty()) {
            try {
                saveRootKeyToFile(rootKeyFile, rootKey);
            } catch (Exception e) {
                log.error("恢复根密钥保存失败", e);
            }
        }
    }

    // ==================== 工具方法 ====================

    private String generateKeyId() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String loadRootKeyFromFile(String path) throws Exception {
        return null;
    }

    private void saveRootKeyToFile(String path, String key) throws Exception {
        // TODO: 保存到文件
    }

    public KeyStatus getKeyStatus() {
        return new KeyStatus(
                rootKeyType,
                isHardwareKeyAvailable,
                workingKeyId,
                workingKeyCreatedTime,
                workingKeyRotationDays
        );
    }

    public static class KeyStatus {
        private String rootKeyType;
        private boolean hardwareKeyAvailable;
        private String workingKeyId;
        private LocalDateTime workingKeyCreatedTime;
        private int rotationDays;

        public KeyStatus(String rootKeyType, boolean hardwareKeyAvailable,
                        String workingKeyId, LocalDateTime workingKeyCreatedTime,
                        int rotationDays) {
            this.rootKeyType = rootKeyType;
            this.hardwareKeyAvailable = hardwareKeyAvailable;
            this.workingKeyId = workingKeyId;
            this.workingKeyCreatedTime = workingKeyCreatedTime;
            this.rotationDays = rotationDays;
        }

        public String getRootKeyType() { return rootKeyType; }
        public boolean isHardwareKeyAvailable() { return hardwareKeyAvailable; }
        public String getWorkingKeyId() { return workingKeyId; }
        public LocalDateTime getWorkingKeyCreatedTime() { return workingKeyCreatedTime; }
        public int getRotationDays() { return rotationDays; }
    }
}
