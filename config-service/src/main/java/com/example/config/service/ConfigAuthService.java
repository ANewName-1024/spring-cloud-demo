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

    public ConfigAuthService(ConfigUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        ConfigUser user = new ConfigUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setEnabled(true);
        user.setCreatedTime(LocalDateTime.now());
        
        return userRepository.save(user);
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
     * 初始化默认管理员
     */
    public void initDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            ConfigUser admin = new ConfigUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setEmail("admin@example.com");
            admin.setEnabled(true);
            admin.setCreatedTime(LocalDateTime.now());
            userRepository.save(admin);
        }
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
