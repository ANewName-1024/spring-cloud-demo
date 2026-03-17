package com.example.user.service;

import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.RegisterRequest;
import com.example.user.entity.Permission;
import com.example.user.entity.Role;
import com.example.user.entity.User;
import com.example.user.repository.PermissionRepository;
import com.example.user.repository.RoleRepository;
import com.example.user.repository.UserRepository;
import com.example.user.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // 是否启用注册邀请码验证（可通过配置切换）
    @Value("${auth.register.invite-code-required:false}")
    private boolean inviteCodeRequired;

    // 预设的邀请码（生产环境应存入数据库）
    @Value("${auth.register.invite-codes:}")
    private String inviteCodes;

    // 密码强度正则：至少8位，包含数字和字母
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$");

    /**
     * 用户注册
     */
    @Transactional
    public User register(RegisterRequest request) {
        // 验证必填字段
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        // 检查用户名是否存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 验证邀请码（如果启用）
        if (inviteCodeRequired) {
            String inviteCode = request.getInviteCode();
            if (inviteCode == null || !isValidInviteCode(inviteCode)) {
                throw new RuntimeException("邀请码无效");
            }
        }

        // 验证密码强度
        if (!isPasswordStrong(request.getPassword())) {
            throw new RuntimeException("密码强度不足，需至少8位，包含数字和字母");
        }

        // 验证用户名格式（防止SQL注入和特殊字符）
        if (!isValidUsername(request.getUsername())) {
            throw new RuntimeException("用户名格式不正确，只允许字母、数字、下划线");
        }

        User user = new User(
            request.getUsername(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword())
        );

        // 默认分配 USER 角色
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

    /**
     * 验证邀请码
     */
    private boolean isValidInviteCode(String code) {
        if (inviteCodes == null || inviteCodes.isEmpty()) {
            return false;
        }
        String[] codes = inviteCodes.split(",");
        for (String c : codes) {
            if (c.trim().equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证密码强度
     */
    private boolean isPasswordStrong(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 验证用户名格式
     */
    private boolean isValidUsername(String username) {
        return Pattern.matches("^[a-zA-Z0-9_]{3,20}$", username);
    }

    /**
     * 用户登录
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

        Set<String> roles = new java.util.HashSet<>();
        for (Role role : user.getRoles()) {
            roles.add(role.getName());
        }

        String token = jwtUtil.generateToken(
            user.getUsername(),
            user.getId(),
            roles,
            user.getPermissions()
        );

        return new LoginResponse(token, user.getUsername(), user.getEmail(), user.getId());
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
