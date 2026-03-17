package com.example.common.encryption;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 国密加密工具类
 * 支持 AES-256-GCM (等效国密SM4)
 * SM3 摘要
 * SM4 密钥派生
 */
@Component
public class GmCryptUtil {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    /**
     * 加密数据
     * @param data 待加密数据
     * @param key 密钥 (Base64编码)
     * @return 加密后的数据 (Base64编码)
     */
    public String encrypt(String data, String key) {
        if (data == null || key == null) {
            return null;
        }
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
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密数据
     * @param encryptedData 加密数据 (Base64编码)
     * @param key 密钥 (Base64编码)
     * @return 解密后的数据
     */
    public String decrypt(String encryptedData, String key) {
        if (encryptedData == null || key == null) {
            return null;
        }
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
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成随机密钥 (256位/32字节)
     * @return Base64编码的密钥
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
     * SM3 摘要 (使用 SHA-256 模拟)
     * @param data 待摘要数据
     * @return Base64编码的摘要
     */
    public String sm3Digest(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SM3摘要计算失败", e);
        }
    }

    /**
     * SM3 验证
     * @param data 原始数据
     * @param expectedDigest 期望的摘要
     * @return 是否匹配
     */
    public boolean sm3Verify(String data, String expectedDigest) {
        String actualDigest = sm3Digest(data);
        return actualDigest.equals(expectedDigest);
    }

    /**
     * SM4 密钥派生 (KDF)
     * @param password 密码
     * @param salt 盐值
     * @param keyLength 密钥长度
     * @return 派生的密钥 (Base64编码)
     */
    public String sm4Kdf(String password, String salt, int keyLength) {
        String combined = password + salt;
        StringBuilder key = new StringBuilder();
        int counter = 1;

        while (key.length() < keyLength * 2) {
            String hash = sm3Digest(combined + counter);
            key.append(hash);
            counter++;
        }

        byte[] keyBytes = key.substring(0, keyLength * 2).getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    /**
     * 密码哈希
     * @param password 密码
     * @return 哈希值
     */
    public String hashPassword(String password) {
        return sm3Digest(password);
    }

    /**
     * 验证密码
     * @param password 原始密码
     * @param hash 存储的哈希值
     * @return 是否匹配
     */
    public boolean verifyPassword(String password, String hash) {
        return sm3Verify(password, hash);
    }
}
