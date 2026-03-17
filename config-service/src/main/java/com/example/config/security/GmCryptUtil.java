package com.example.config.security;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密工具类（简化版）
 * 使用 AES-256-GCM
 */
@Component
public class GmCryptUtil {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    /**
     * AES-GCM 加密
     */
    public String encrypt(String data, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            
            // 生成随机 IV
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec);
            
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // IV + 加密数据
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * AES-GCM 解密
     */
    public String decrypt(String encryptedData, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            
            byte[] data = Base64.getDecoder().decode(encryptedData);
            
            // 提取 IV
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, iv.length);
            
            // 提取加密数据
            byte[] encrypted = new byte[data.length - IV_LENGTH];
            System.arraycopy(data, IV_LENGTH, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 生成随机密钥 (256位)
     */
    public String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("密钥生成失败", e);
        }
    }

    /**
     * SM3 摘要（使用 JDK 摘要）
     */
    public String sm3Digest(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("摘要计算失败", e);
        }
    }

    /**
     * SM3 验证
     */
    public boolean sm3Verify(String data, String expectedDigest) {
        String actualDigest = sm3Digest(data);
        return actualDigest.equals(expectedDigest);
    }

    /**
     * SM3 密钥派生 (KDF)
     */
    public String sm3Kdf(String password, String salt, int keyLength) {
        String combined = password + salt;
        StringBuilder key = new StringBuilder();
        int counter = 1;

        while (key.length() < keyLength) {
            String hash = sm3Digest(combined + counter);
            key.append(hash);
            counter++;
        }

        return key.substring(0, keyLength);
    }
}
