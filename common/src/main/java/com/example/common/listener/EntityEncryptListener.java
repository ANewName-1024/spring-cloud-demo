package com.example.common.listener;

import com.example.common.encryption.GmCryptUtil;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * 实体加密监听器
 * 在实体保存前自动加密敏感字段，读取后自动解密
 */
@Component
public class EntityEncryptListener {

    private static final Logger logger = LoggerFactory.getLogger(EntityEncryptListener.class);

    @Autowired
    private GmCryptUtil cryptUtil;

    /**
     * 加密密钥 (从配置读取)
     */
    @Value("${encryption.key:}")
    private String encryptionKey;

    /**
     * 需要加密的字段列表
     */
    private static final List<String> ENCRYPTED_FIELDS = Arrays.asList(
            "password",        // 用户密码
            "token",          // Token
            "secretKeyHash",  // 密钥哈希
            "clientSecret"    // 客户端密钥
    );

    /**
     * PrePersist - 保存前加密
     */
    @PrePersist
    public void prePersist(Object entity) {
        encryptFields(entity);
    }

    /**
     * PreUpdate - 更新前加密
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        encryptFields(entity);
    }

    /**
     * PostLoad - 加载后解密
     */
    @PostLoad
    public void postLoad(Object entity) {
        decryptFields(entity);
    }

    /**
     * 加密敏感字段
     */
    private void encryptFields(Object entity) {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            logger.warn("加密密钥未配置，跳过加密");
            return;
        }

        Class<?> clazz = entity.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (isEncryptedField(field.getName())) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value != null && value instanceof String) {
                        String encrypted = cryptUtil.encrypt((String) value, encryptionKey);
                        field.set(entity, encrypted);
                        logger.debug("加密字段: {}.{}", clazz.getSimpleName(), field.getName());
                    }
                } catch (Exception e) {
                    logger.error("加密字段失败: {}.{}", clazz.getSimpleName(), field.getName(), e);
                }
            }
        }
    }

    /**
     * 解密敏感字段
     */
    private void decryptFields(Object entity) {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            logger.warn("加密密钥未配置，跳过解密");
            return;
        }

        Class<?> clazz = entity.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (isEncryptedField(field.getName())) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value != null && value instanceof String) {
                        String decrypted = cryptUtil.decrypt((String) value, encryptionKey);
                        field.set(entity, decrypted);
                        logger.debug("解密字段: {}.{}", clazz.getSimpleName(), field.getName());
                    }
                } catch (Exception e) {
                    logger.error("解密字段失败: {}.{}", clazz.getSimpleName(), field.getName(), e);
                }
            }
        }
    }

    /**
     * 判断字段是否需要加密
     */
    private boolean isEncryptedField(String fieldName) {
        return ENCRYPTED_FIELDS.contains(fieldName.toLowerCase());
    }
}
