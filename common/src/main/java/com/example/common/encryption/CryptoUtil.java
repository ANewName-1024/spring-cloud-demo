package com.example.common.encryption;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 行业标准加密工具类
 * 
 * 推荐算法:
 * - 对称加密: AES-256-GCM (NIST 推荐)
 * - 密码哈希: bcrypt (OWSAP 推荐)
 * - 密钥派生: PBKDF2 (NIST SP 800-132)
 */
@Component
public class CryptoUtil {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    
    // PBKDF2 参数
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int PBKDF2_KEY_LENGTH = 256;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    // BCrypt 强度
    private static final int BCRYPT_STRENGTH = 12;

    // ==================== 对称加密 (AES-256-GCM) ====================
    
    /**
     * 加密数据 - AES-256-GCM
     * @param data 待加密数据
     * @param key Base64编码的32字节密钥
     * @return Base64编码的密文 (IV + 密文)
     */
    public String encrypt(String data, String key) {
        if (data == null || key == null) {
            return null;
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("密钥必须为32字节");
            }
            
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
     * 解密数据 - AES-256-GCM
     * @param encryptedData Base64编码的密文
     * @param key Base64编码的32字节密钥
     * @return 明文
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
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 生成随机密钥 - AES-256
     * @return Base64编码的32字节密钥
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

    // ==================== 密码哈希 (bcrypt) ====================
    
    /**
     * bcrypt 哈希密码
     * @param password 原始密码
     * @return bcrypt 哈希值 (包含salt)
     */
    public String hashPassword(String password) {
        try {
            org.mindrot.jbcrypt.BCrypt.Password bcryptPassword = 
                org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt(BCRYPT_STRENGTH));
            return new String(bcryptPassword);
        } catch (Exception e) {
            throw new RuntimeException("密码哈希失败", e);
        }
    }

    /**
     * 验证密码
     * @param password 原始密码
     * @param hashedPassword 存储的哈希值
     * @return 是否匹配
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        try {
            return org.mindrot.jbcrypt.BCrypt.checkpw(password, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 密钥派生 (PBKDF2) ====================
    
    /**
     * PBKDF2 密钥派生
     * @param password 密码
     * @param salt 盐值
     * @return 派生的密钥 (Base64编码)
     */
    public String deriveKey(String password, String salt) {
        try {
            KeySpec spec = new PBEKeySpec(
                password.toCharArray(), 
                salt.getBytes(StandardCharsets.UTF_8), 
                PBKDF2_ITERATIONS, 
                PBKDF2_KEY_LENGTH
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] key = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(key);
        } catch (Exception e) {
            throw new RuntimeException("密钥派生失败", e);
        }
    }

    /**
     * 生成随机盐值
     * @return Base64编码的盐值
     */
    public String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // ==================== 兼容旧接口 ====================
    
    /**
     * SM3 摘要 (兼容旧接口，实际使用 SHA-256)
     * @deprecated 使用 hashPassword 代替
     */
    @Deprecated
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
     * SM3 验证 (兼容旧接口)
     * @deprecated 使用 verifyPassword 代替
     */
    @Deprecated
    public boolean sm3Verify(String data, String expectedDigest) {
        String actualDigest = sm3Digest(data);
        return actualDigest.equals(expectedDigest);
    }

    /**
     * SM4 密钥派生 (兼容旧接口，使用 PBKDF2)
     * @deprecated 使用 deriveKey 代替
     */
    @Deprecated
    public String sm4Kdf(String password, String salt, int keyLength) {
        return deriveKey(password, salt);
    }
}
