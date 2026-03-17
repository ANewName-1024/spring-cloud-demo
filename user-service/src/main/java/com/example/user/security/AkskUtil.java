package com.example.user.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * AKSK 工具类 - 生成和验证 AccessKey/SecretKey
 */
@Component
public class AkskUtil {

    private static final int ACCESS_KEY_LENGTH = 16;
    private static final int SECRET_KEY_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成 AccessKey
     */
    public String generateAccessKey() {
        byte[] bytes = new byte[ACCESS_KEY_LENGTH];
        secureRandom.nextBytes(bytes);
        return "ak_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, ACCESS_KEY_LENGTH);
    }

    /**
     * 生成 SecretKey
     */
    public String generateSecretKey() {
        byte[] bytes = new byte[SECRET_KEY_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 验证 SecretKey
     */
    public boolean verifySecretKey(String rawSecretKey, String hashedSecretKey, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawSecretKey, hashedSecretKey);
    }

    /**
     * 对 SecretKey 进行哈希
     */
    public String hashSecretKey(String secretKey, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return passwordEncoder.encode(secretKey);
    }
}
