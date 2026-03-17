package com.example.config.service;

import com.example.config.entity.ConfigUser;
import com.example.config.repository.ConfigUserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 配置服务认证服务
 */
@Service
public class ConfigAuthService {

    private final ConfigUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${config.jwt.secret:ConfigServiceJWTSecretKey2024ChangeInProduction}")
    private String jwtSecret;

    @Value("${config.jwt.expiration:86400000}")
    private Long jwtExpiration;

    // 弱密码字典
    private static final Set<String> WEAK_PASSWORDS = new HashSet<>(Arrays.asList(
            // 常见弱密码
            "123456", "password", "12345678", "qwerty", "123456789",
            "12345", "1234", "111111", "1234567", "dragon",
            "123123", "baseball", "abc123", "football", "monkey",
            "letmein", "shadow", "master", "666666", "qwertyuiop",
            "123321", "mustang", "1234567890", "michael", "654321",
            "superman", "1qaz2wsx", "7777777", "121212", "000000",
            "qazwsx", "123qwe", "killer", "trustno1", "jordan",
            "jennifer", "zxcvbnm", "asdfgh", "hunter", "buster",
            "soccer", "harley", "batman", "andrew", "tigger",
            "sunshine", "iloveyou", "2000", "charlie", "robert",
            "thomas", "hockey", "ranger", "daniel", "starwars",
            "klaster", "112233", "george", "computer", " michael",
            "john", "essex", "michelle", "ginger", "maggie",
            "159753", "aaaaaa", "ginger", "christin", " Kimberly",
            "schumacher", "taylor", "matrix", "mobilemail", "monitor",
            "monitoring", "montana", "moon", "moscow",
            // 常见模式
            "password1", "password123", "admin123", "welcome123",
            "changeme", "default", "root", "toor", "passw0rd",
            "p@ssword", "p@ssw0rd", "passw0rd!", "Admin@123",
            "Admin123", "admin@123", "Root@123", "Test@123",
            // 键盘模式
            "1q2w3e4r", "1qaz2wsx", "qweasdzxc", "asdfghjkl",
            "zxcvbnm", "1qaz@WSX", "qwe@123", "asd@123",
            // 纯数字变体
            "000000", "111111", "222222", "333333", "444444",
            "555555", "666666", "777777", "888888", "999999",
            "0123456789", "9876543210", "1357924680", "2468013579",
            // 公司常见
            "company123", "password1!", "P@ssword1!", "Welcome1!",
            // 季节日期
            "Summer2024", "Spring2024", "Winter2024", "Autumn2024",
            "2024Pass", "2024Password", "Pass2024",
            // 包含用户名
            "admin123", "adminadmin", "user123", "test123"
    ));

    public ConfigAuthService(ConfigUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 验证密码强度
     */
    public void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("密码长度至少8位");
        }

        // 检查是否包含数字
        if (!password.matches(".*\\d.*")) {
            throw new RuntimeException("密码必须包含数字");
        }

        // 检查是否包含字母
        if (!password.matches(".*[a-zA-Z].*")) {
            throw new RuntimeException("密码必须包含字母");
        }

        // 检查弱密码
        String lowerPassword = password.toLowerCase();
        if (WEAK_PASSWORDS.contains(lowerPassword)) {
            throw new RuntimeException("密码太简单，请使用更复杂的密码");
        }

        // 检查键盘连续字符
        if (hasSequentialChars(password)) {
            throw new RuntimeException("密码不能包含连续字符");
        }

        // 检查重复字符
        if (hasRepeatedChars(password)) {
            throw new RuntimeException("密码不能包含过多重复字符");
        }
    }

    /**
     * 检查键盘连续字符
     */
    private boolean hasSequentialChars(String password) {
        String lower = password.toLowerCase();
        String[] sequences = {
                "1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm",
                "abcdefghijklmnopqrstuvwxyz", "0123456789",
                "qazwsxedc", "plmoknijb", "ujmki"
        };

        for (String seq : sequences) {
            for (int i = 0; i < seq.length() - 2; i++) {
                String sub = seq.substring(i, i + 3);
                if (lower.contains(sub)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查重复字符
     */
    private boolean hasRepeatedChars(String password) {
        // 检查连续3个以上相同字符
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) &&
                    password.charAt(i) == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 用户登录
     */
    public LoginResponse login(String username, String password) {
        ConfigUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!user.getEnabled()) {
            throw new RuntimeException("账号已被禁用");
        }

        String token = generateToken(user);
        return new LoginResponse(token, user.getUsername(), user.getId());
    }

    /**
     * 用户注册
     */
    public ConfigUser register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        // 验证密码强度
        validatePasswordStrength(password);

        ConfigUser user = new ConfigUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setEnabled(true);
        user.setCreatedTime(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        ConfigUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 验证新密码强度
        validatePasswordStrength(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedTime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 重置密码（管理员）
     */
    public void resetPassword(String username, String newPassword) {
        ConfigUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证新密码强度
        validatePasswordStrength(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedTime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 获取用户信息
     */
    public Optional<ConfigUser> getUser(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 生成 JWT Token
     */
    private String generateToken(ConfigUser user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 初始化默认管理员（使用强密码）
     */
    public void initDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            // 生成随机强密码
            String randomPassword = generateSecurePassword();
            
            ConfigUser admin = new ConfigUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(randomPassword));
            admin.setEmail("admin@example.com");
            admin.setEnabled(true);
            admin.setCreatedTime(LocalDateTime.now());
            userRepository.save(admin);
            
            // 打印初始密码（仅首次）
            System.out.println("========================================");
            System.out.println("默认管理员账户已创建:");
            System.out.println("用户名: admin");
            System.out.println("初始密码: " + randomPassword);
            System.out.println("请首次登录后立即修改密码!");
            System.out.println("========================================");
        }
    }

    /**
     * 生成安全随机密码
     */
    private String generateSecurePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        
        // 确保包含大写字母、小写字母、数字
        password.append("A");
        password.append("a");
        password.append("1");
        password.append("!");
        
        // 填充剩余位
        for (int i = 4; i < 16; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 打乱顺序
        List<Character> list = new ArrayList<>();
        for (char c : password.toString().toCharArray()) {
            list.add(c);
        }
        Collections.shuffle(list);
        
        StringBuilder result = new StringBuilder();
        for (char c : list) {
            result.append(c);
        }
        
        return result.toString();
    }

    public static class LoginResponse {
        private String token;
        private String username;
        private Long userId;

        public LoginResponse(String token, String username, Long userId) {
            this.token = token;
            this.username = username;
            this.userId = userId;
        }

        public String getToken() { return token; }
        public String getUsername() { return username; }
        public Long getUserId() { return userId; }
    }
}
