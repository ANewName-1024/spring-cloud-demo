package com.example.user.service;

import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.RegisterRequest;
import com.example.user.entity.Permission;
import com.example.user.entity.RefreshToken;
import com.example.user.entity.Role;
import com.example.user.entity.User;
import com.example.user.repository.PermissionRepository;
import com.example.user.repository.RefreshTokenRepository;
import com.example.user.repository.RoleRepository;
import com.example.user.repository.UserRepository;
import com.example.user.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 认证服务
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Access Token 过期时间（毫秒），默认 30 分钟
    @Value("${jwt.expiration:1800000}")
    private Long accessTokenExpiration;

    // Refresh Token 过期时间（毫秒），默认 7 天
    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration;

    // 是否启用注册邀请码验证
    @Value("${auth.register.invite-code-required:false}")
    private boolean inviteCodeRequired;

    // 预设的邀请码
    @Value("${auth.register.invite-codes:}")
    private String inviteCodes;

    // 密码强度正则
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$");

    /**
     * 用户注册
     */
    @Transactional
    public User register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        if (inviteCodeRequired) {
            String inviteCode = request.getInviteCode();
            if (inviteCode == null || !isValidInviteCode(inviteCode)) {
                throw new RuntimeException("邀请码无效");
            }
        }

        if (!isPasswordStrong(request.getPassword())) {
            throw new RuntimeException("密码强度不足，需至少8位，包含数字和字母");
        }

        if (!isValidUsername(request.getUsername())) {
            throw new RuntimeException("用户名格式不正确，只允许字母、数字、下划线");
        }

        User user = new User(
            request.getUsername(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword())
        );

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role newRole = new Role("USER", "普通用户");
                    Set<Permission> perms = new HashSet<>();
                    perms.add(permissionRepository.findByName("user:read")
                            .orElseGet(() -> permissionRepository.save(new Permission("user:read", "读取用户"))));
                    newRole.setPermissions(perms);
                    return roleRepository.save(newRole);
                });

        user.getRoles().add(userRole);
        return userRepository.save(user);
    }

    private boolean isValidInviteCode(String code) {
        if (inviteCodes == null || inviteCodes.isEmpty()) return false;
        for (String c : inviteCodes.split(",")) {
            if (c.trim().equals(code)) return true;
        }
        return false;
    }

    private boolean isPasswordStrong(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    private boolean isValidUsername(String username) {
        return Pattern.matches("^[a-zA-Z0-9_]{3,20}$", username);
    }

    /**
     * 用户登录 - 生成 Access Token 和 Refresh Token
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!user.getEnabled()) {
            throw new RuntimeException("账号已被禁用");
        }

        // 生成 Access Token
        Set<String> roles = new HashSet<>();
        for (Role role : user.getRoles()) {
            roles.add(role.getName());
        }
        String accessToken = jwtUtil.generateToken(
            user.getUsername(),
            user.getId(),
            roles,
            user.getPermissions()
        );

        // 生成 Refresh Token
        String refreshTokenValue = UUID.randomUUID().toString().replace("-", "");
        RefreshToken refreshToken = new RefreshToken(refreshTokenValue, user, 
            LocalDateTime.now().plusNanos(refreshTokenExpiration * 1_000_000));
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(
            accessToken,
            refreshTokenValue,
            user.getUsername(),
            user.getEmail(),
            user.getId(),
            accessTokenExpiration / 1000,  // 转换为秒
            refreshTokenExpiration / 1000
        );
    }

    /**
     * 使用 Refresh Token 刷新 Access Token
     */
    public LoginResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh Token 无效"));

        if (!refreshToken.isValid()) {
            throw new RuntimeException("Refresh Token 已过期或已撤销");
        }

        User user = refreshToken.getUser();
        if (!user.getEnabled()) {
            throw new RuntimeException("账号已被禁用");
        }

        // 撤销旧的 Refresh Token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // 生成新的 Access Token
        Set<String> roles = new HashSet<>();
        for (Role role : user.getRoles()) {
            roles.add(role.getName());
        }
        String accessToken = jwtUtil.generateToken(
            user.getUsername(),
            user.getId(),
            roles,
            user.getPermissions()
        );

        // 生成新的 Refresh Token
        String newRefreshTokenValue = UUID.randomUUID().toString().replace("-", "");
        RefreshToken newRefreshToken = new RefreshToken(newRefreshTokenValue, user,
            LocalDateTime.now().plusNanos(refreshTokenExpiration * 1_000_000));
        refreshTokenRepository.save(newRefreshToken);

        return new LoginResponse(
            accessToken,
            newRefreshTokenValue,
            user.getUsername(),
            user.getEmail(),
            user.getId(),
            accessTokenExpiration / 1000,
            refreshTokenExpiration / 1000
        );
    }

    /**
     * 撤销 Refresh Token
     */
    @Transactional
    public void revokeToken(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    /**
     * 撤销用户所有 Refresh Token（退出登录）
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * 清理过期 Token
     */
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredOrRevoked(LocalDateTime.now());
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public boolean hasPermission(String username, String permission) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        return user.getPermissions().contains(permission);
    }

    public boolean hasRole(String username, String roleName) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
    }
}
