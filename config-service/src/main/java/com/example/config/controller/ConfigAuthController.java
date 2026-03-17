package com.example.config.controller;

import com.example.config.entity.ConfigUser;
import com.example.config.service.ConfigAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 配置服务认证控制器
 */
@RestController
@RequestMapping("/config/auth")
public class ConfigAuthController {

    @Autowired
    private ConfigAuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            ConfigAuthService.LoginResponse response = authService.login(username, password);
            
            return ResponseEntity.ok(Map.of(
                    "token", response.getToken(),
                    "username", response.getUsername(),
                    "userId", response.getUserId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String email = request.get("email");
            
            ConfigUser user = authService.register(username, password, email);
            
            return ResponseEntity.ok(Map.of(
                    "message", "注册成功",
                    "userId", user.getId(),
                    "username", user.getUsername()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String username = authService.getUsernameFromToken(token);
            
            return authService.getUser(username)
                    .map(user -> ResponseEntity.ok(Map.of(
                            "username", user.getUsername(),
                            "email", user.getEmail() != null ? user.getEmail() : ""
                    )))
                    .orElse(ResponseEntity.status(401).body(Map.of("error", "用户不存在")));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "无效的令牌"));
        }
    }

    /**
     * 验证 Token
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            boolean valid = authService.validateToken(token);
            
            if (valid) {
                String username = authService.getUsernameFromToken(token);
                return ResponseEntity.ok(Map.of("valid", true, "username", username));
            } else {
                return ResponseEntity.ok(Map.of("valid", false));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false, "error", e.getMessage()));
        }
    }
}
