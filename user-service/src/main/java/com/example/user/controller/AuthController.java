package com.example.user.controller;

import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.RegisterRequest;
import com.example.user.entity.User;
import com.example.user.entity.ServiceAccount;
import com.example.user.service.AuthService;
import com.example.user.service.ServiceAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证控制器 - 包含人机用户和机机账户
 */
@RestController
@RequestMapping("/user/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ServiceAccountService serviceAccountService;

    // ==================== 人机用户接口 ====================

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);
            return ResponseEntity.ok(new MessageResponse("注册成功", user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new MessageResponse("未登录"));
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            User user = authService.getCurrentUser(authentication.getName());
            return ResponseEntity.ok(Map.of(
                "type", "user",
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles().stream().map(r -> r.getName()).toList(),
                "permissions", user.getPermissions()
            ));
        } else if (principal instanceof com.example.user.entity.ServiceAccount) {
            com.example.user.entity.ServiceAccount sa = (com.example.user.entity.ServiceAccount) principal;
            String ak = sa.getAccessKey();
            String maskedAk = ak != null && ak.length() > 8 
                ? ak.substring(0, 4) + "****" + ak.substring(ak.length() - 4)
                : "****";
            return ResponseEntity.ok(Map.of(
                "type", "service_account",
                "id", sa.getId(),
                "name", sa.getName(),
                "accessKey", maskedAk,
                "roles", sa.getRoles().stream().map(r -> r.getName()).toList(),
                "permissions", sa.getPermissions(),
                "lastUsedAt", sa.getLastUsedAt() != null ? sa.getLastUsedAt().toString() : null
            ));
        }
        
        return ResponseEntity.ok(Map.of("username", authentication.getName()));
    }

    /**
     * 检查权限
     */
    @GetMapping("/check/{permission}")
    public ResponseEntity<?> checkPermission(
            @PathVariable String permission,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new MessageResponse("未登录"));
        }
        boolean hasPermission = authService.hasPermission(authentication.getName(), permission);
        return ResponseEntity.ok(new MessageResponse(hasPermission ? "有权限" : "无权限", hasPermission));
    }

    // ==================== 机机账户接口 ====================

    /**
     * 机机账户登录（使用 AKSK）
     */
    @PostMapping("/ak/login")
    public ResponseEntity<?> loginWithAksk(@RequestBody Map<String, String> request) {
        try {
            String accessKey = request.get("accessKey");
            String secretKey = request.get("secretKey");
            
            if (accessKey == null || secretKey == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("缺少 accessKey 或 secretKey"));
            }
            
            ServiceAccountService.AkskLoginResponse response = serviceAccountService.loginWithAksk(accessKey, secretKey);
            return ResponseEntity.ok(Map.of(
                "token", response.getToken(),
                "accessKey", response.getAccessKey(),
                "name", response.getName(),
                "accountId", response.getAccountId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * 创建机机账户（需要管理员权限）
     */
    @PostMapping("/ak/create")
    @PreAuthorize("hasAuthority('admin:manage')")
    public ResponseEntity<?> createServiceAccount(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            String roleName = request.get("roleName");
            
            if (name == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("缺少 name 参数"));
            }
            
            ServiceAccount account = serviceAccountService.createServiceAccount(name, description, roleName);
            
            // 返回时包含生成的 secretKey（只返回一次）
            return ResponseEntity.ok(Map.of(
                "id", account.getId(),
                "name", account.getName(),
                "accessKey", account.getAccessKey(),
                "secretKey", account.getSecretKeyHash(), // 这里是明文
                "description", account.getDescription() != null ? account.getDescription() : "",
                "createdAt", account.getCreatedAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * 轮转 SecretKey
     */
    @PostMapping("/ak/{accountId}/rotate")
    @PreAuthorize("hasAuthority('admin:manage')")
    public ResponseEntity<?> rotateSecretKey(@PathVariable Long accountId) {
        try {
            String newSecretKey = serviceAccountService.rotateSecretKey(accountId);
            return ResponseEntity.ok(Map.of(
                "message", "SecretKey 轮转成功",
                "secretKey", newSecretKey
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * 通过 AccessKey 轮转 SecretKey - 必须验证当前密钥
     */
    @PostMapping("/ak/rotate")
    public ResponseEntity<?> rotateSecretKeyByAccessKey(@RequestBody Map<String, String> request) {
        try {
            String accessKey = request.get("accessKey");
            String currentSecretKey = request.get("secretKey");
            
            if (accessKey == null || currentSecretKey == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("缺少 accessKey 或 secretKey 参数"));
            }
            
            // 必须验证当前密钥才能轮转，防止越权
            serviceAccountService.loginWithAksk(accessKey, currentSecretKey);
            
            String newSecretKey = serviceAccountService.rotateSecretKeyByAccessKey(accessKey);
            return ResponseEntity.ok(Map.of(
                "message", "SecretKey 轮转成功",
                "secretKey", newSecretKey
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * 获取所有机机账户（管理员）
     */
    @GetMapping("/ak/list")
    @PreAuthorize("hasAuthority('admin:manage')")
    public ResponseEntity<?> listServiceAccounts() {
        List<ServiceAccount> accounts = serviceAccountService.getAllServiceAccounts();
        return ResponseEntity.ok(accounts.stream().map(a -> {
            String ak = a.getAccessKey();
            String maskedAk = ak != null && ak.length() > 8 
                ? ak.substring(0, 4) + "****" + ak.substring(ak.length() - 4)
                : "****";
            return Map.of(
                "id", a.getId(),
                "name", a.getName(),
                "accessKey", maskedAk,
                "description", a.getDescription() != null ? a.getDescription() : "",
                "enabled", a.getEnabled(),
                "createdAt", a.getCreatedAt().toString(),
                "lastUsedAt", a.getLastUsedAt() != null ? a.getLastUsedAt().toString() : "",
                "secretKeyRotatedAt", a.getSecretKeyRotatedAt() != null ? a.getSecretKeyRotatedAt().toString() : "",
                "roles", a.getRoles().stream().map(r -> r.getName()).toList()
            );
        }).toList());
    }

    /**
     * 启用/禁用机机账户
     */
    @PostMapping("/ak/{accountId}/toggle")
    @PreAuthorize("hasAuthority('admin:manage')")
    public ResponseEntity<?> toggleServiceAccount(@PathVariable Long accountId, @RequestBody Map<String, Boolean> request) {
        try {
            boolean enabled = request.get("enabled");
            ServiceAccount account = serviceAccountService.setEnabled(accountId, enabled);
            return ResponseEntity.ok(Map.of(
                "id", account.getId(),
                "enabled", account.getEnabled()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * 简单消息响应
     */
    public static class MessageResponse {
        private String message;
        private Object data;

        public MessageResponse(String message) {
            this.message = message;
        }

        public MessageResponse(String message, Object data) {
            this.message = message;
            this.data = data;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}
