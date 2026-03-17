package com.example.common.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CryptoUtil 加密工具测试")
class CryptoUtilTest {

    private CryptoUtil cryptoUtil;
    private String testKey;

    @BeforeEach
    void setUp() {
        cryptoUtil = new CryptoUtil();
        testKey = cryptoUtil.generateKey(); // 生成32字节密钥
    }

    @Nested
    @DisplayName("对称加密测试 (AES-256-GCM)")
    class EncryptionTests {

        @Test
        @DisplayName("加密解密 - 正常数据")
        void shouldEncryptAndDecrypt() {
            // Given
            String plaintext = "Hello, World! 你好世界！";

            // When
            String ciphertext = cryptoUtil.encrypt(plaintext, testKey);
            String decrypted = cryptoUtil.decrypt(ciphertext, testKey);

            // Then
            assertThat(ciphertext).isNotEqualTo(plaintext);
            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("加密 - 每次生成不同的密文")
        void shouldGenerateDifferentCiphertextEachTime() {
            // Given
            String plaintext = "Same plaintext";

            // When
            String ciphertext1 = cryptoUtil.encrypt(plaintext, testKey);
            String ciphertext2 = cryptoUtil.encrypt(plaintext, testKey);

            // Then
            assertThat(ciphertext1).isNotEqualTo(ciphertext2); // 随机 IV 导致不同
        }

        @Test
        @DisplayName("解密 - 错误密钥应失败")
        void shouldFailWithWrongKey() {
            // Given
            String plaintext = "Secret message";
            String ciphertext = cryptoUtil.encrypt(plaintext, testKey);
            String wrongKey = cryptoUtil.generateKey();

            // When & Then
            assertThatThrownBy(() -> cryptoUtil.decrypt(ciphertext, wrongKey))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("加密 - 空值应返回空")
        void shouldHandleNullInput() {
            // When
            String encrypted = cryptoUtil.encrypt(null, testKey);
            String decrypted = cryptoUtil.decrypt(null, testKey);

            // Then
            assertThat(encrypted).isNull();
            assertThat(decrypted).isNull();
        }

        @Test
        @DisplayName("加密 - 特殊字符")
        void shouldHandleSpecialCharacters() {
            // Given
            String[] testCases = {
                    "密码: password123",
                    "emoji: 😀🎉🔥",
                    "符号: !@#$%^&*()",
                    "换行:\n\t\r",
                    "unicode: 中文日本語한국어"
            };

            for (String plaintext : testCases) {
                // When
                String ciphertext = cryptoUtil.encrypt(plaintext, testKey);
                String decrypted = cryptoUtil.decrypt(ciphertext, testKey);

                // Then
                assertThat(decrypted).isEqualTo(plaintext);
            }
        }

        @Test
        @DisplayName("密钥长度验证")
        void shouldRejectInvalidKeyLength() {
            // Given
            String shortKey = "YWJjZA=="; // 4字节
            String longKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamts"; // 错误长度

            // When & Then
            assertThatThrownBy(() -> cryptoUtil.encrypt("test", shortKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("32字节");
        }
    }

    @Nested
    @DisplayName("密码哈希测试 (bcrypt)")
    class PasswordHashTests {

        @Test
        @DisplayName("哈希密码")
        void shouldHashPassword() {
            // Given
            String password = "MySecure@123";

            // When
            String hash = cryptoUtil.hashPassword(password);

            // Then
            assertThat(hash).isNotEqualTo(password);
            assertThat(hash.length()).isGreaterThan(50);
        }

        @Test
        @DisplayName("验证密码 - 正确密码")
        void shouldVerifyCorrectPassword() {
            // Given
            String password = "MySecure@123";
            String hash = cryptoUtil.hashPassword(password);

            // When
            boolean result = cryptoUtil.verifyPassword(password, hash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("验证密码 - 错误密码")
        void shouldRejectWrongPassword() {
            // Given
            String password = "MySecure@123";
            String wrongPassword = "Wrong@123";
            String hash = cryptoUtil.hashPassword(password);

            // When
            boolean result = cryptoUtil.verifyPassword(wrongPassword, hash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("同一密码不同哈希")
        void shouldGenerateDifferentHashesForSamePassword() {
            // Given
            String password = "MySecure@123";

            // When
            String hash1 = cryptoUtil.hashPassword(password);
            String hash2 = cryptoUtil.hashPassword(password);

            // Then
            assertThat(hash1).isNotEqualTo(hash2); // bcrypt 自动生成不同 salt
        }

        @Test
        @DisplayName("验证空密码")
        void shouldHandleEmptyPassword() {
            // Given
            String password = "";
            String hash = cryptoUtil.hashPassword(password);

            // When
            boolean result = cryptoUtil.verifyPassword(password, hash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("验证空哈希")
        void shouldRejectEmptyHash() {
            // When
            boolean result = cryptoUtil.verifyPassword("password", "");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("密钥派生测试 (PBKDF2)")
    class KeyDerivationTests {

        @Test
        @DisplayName("派生密钥")
        void shouldDeriveKey() {
            // Given
            String password = "MyPassword@123";
            String salt = cryptoUtil.generateSalt();

            // When
            String derivedKey = cryptoUtil.deriveKey(password, salt);

            // Then
            assertThat(derivedKey).isNotNull();
            assertThat(derivedKey.length()).isGreaterThan(20);
        }

        @Test
        @DisplayName("相同输入派生相同密钥")
        void shouldDeriveSameKeyWithSameInputs() {
            // Given
            String password = "MyPassword@123";
            String salt = "fixed-salt-value";

            // When
            String key1 = cryptoUtil.deriveKey(password, salt);
            String key2 = cryptoUtil.deriveKey(password, salt);

            // Then
            assertThat(key1).isEqualTo(key2);
        }

        @Test
        @DisplayName("不同盐值派生不同密钥")
        void shouldDeriveDifferentKeyWithDifferentSalt() {
            // Given
            String password = "MyPassword@123";

            // When
            String key1 = cryptoUtil.deriveKey(password, "salt1");
            String key2 = cryptoUtil.deriveKey(password, "salt2");

            // Then
            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("生成随机盐值")
        void shouldGenerateSalt() {
            // When
            String salt1 = cryptoUtil.generateSalt();
            String salt2 = cryptoUtil.generateSalt();

            // Then
            assertThat(salt1).isNotNull();
            assertThat(salt2).isNotNull();
            assertThat(salt1).isNotEqualTo(salt2);
        }
    }

    @Nested
    @DisplayName("密钥生成测试")
    class KeyGenerationTests {

        @Test
        @DisplayName("生成随机密钥")
        void shouldGenerateRandomKey() {
            // When
            String key1 = cryptoUtil.generateKey();
            String key2 = cryptoUtil.generateKey();

            // Then
            assertThat(key1).isNotNull();
            assertThat(key2).isNotNull();
            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("密钥长度为32字节")
        void shouldGenerate256BitKey() {
            // When
            String key = cryptoUtil.generateKey();

            // Then
            byte[] keyBytes = java.util.Base64.getDecoder().decode(key);
            assertThat(keyBytes.length).isEqualTo(32); // 256 bits
        }
    }

    @Nested
    @DisplayName("兼容接口测试")
    class LegacyTests {

        @Test
        @DisplayName("sm3Digest 应返回 SHA-256 哈希")
        void shouldWorkAsSm3Digest() {
            // Given
            String data = "test data";

            // When
            String hash = cryptoUtil.sm3Digest(data);

            // Then
            assertThat(hash).isNotNull();
            assertThat(hash.length()).isEqualTo(44); // Base64 encoded SHA-256
        }

        @Test
        @DisplayName("sm4Kdf 应使用 PBKDF2")
        void shouldWorkAsSm4Kdf() {
            // Given
            String password = "test";
            String salt = "salt";

            // When
            String key = cryptoUtil.sm4Kdf(password, salt, 32);

            // Then
            assertThat(key).isNotNull();
        }
    }
}
