package com.example.user.api;

import com.example.user.model.*;
import com.example.user.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 API 实现
 * 根据 AuthApi 接口自动生成实现
 */
@RestController
public class AuthApiController implements AuthApi {

    @Autowired
    private AuthService authService;

    @Override
    public ResponseEntity<com.example.user.entity.User> register(RegisterRequest request) {
        try {
            com.example.user.entity.User user = authService.register(
                convertToEntity(request)
            );
            return ResponseEntity.ok(convertToModel(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        try {
            com.example.user.dto.LoginResponse response = authService.login(
                convertToLoginRequest(request)
            );
            return ResponseEntity.ok(convertToLoginResponse(response));
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @Override
    public ResponseEntity<LoginResponse> refreshToken(RefreshTokenRequest request) {
        try {
            com.example.user.dto.LoginResponse response = authService.refreshToken(
                request.getRefreshToken()
            );
            return ResponseEntity.ok(convertToLoginResponse(response));
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @Override
    public ResponseEntity<Void> logout() {
        // 实现退出登录逻辑
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<User> getCurrentUser() {
        // 实现获取当前用户逻辑
        return ResponseEntity.ok(new User());
    }

    // ==================== 转换方法 ====================

    private com.example.user.dto.RegisterRequest convertToEntity(RegisterRequest request) {
        com.example.user.dto.RegisterRequest dto = new com.example.user.dto.RegisterRequest();
        dto.setUsername(request.getUsername());
        dto.setEmail(request.getEmail());
        dto.setPassword(request.getPassword());
        dto.setInviteCode(request.getInviteCode());
        return dto;
    }

    private com.example.user.dto.LoginRequest convertToLoginRequest(LoginRequest request) {
        com.example.user.dto.LoginRequest dto = new com.example.user.dto.LoginRequest();
        dto.setUsername(request.getUsername());
        dto.setPassword(request.getPassword());
        return dto;
    }

    private LoginResponse convertToLoginResponse(com.example.user.dto.LoginResponse response) {
        return new LoginResponse(
            response.getToken(),
            response.getRefreshToken(),
            response.getUsername(),
            response.getEmail(),
            response.getUserId(),
            response.getTokenExpiresIn(),
            response.getRefreshTokenExpiresIn()
        );
    }

    private User convertToModel(com.example.user.entity.User entity) {
        User model = new User();
        model.setId(entity.getId());
        model.setUsername(entity.getUsername());
        model.setEmail(entity.getEmail());
        model.setEnabled(entity.getEnabled());
        model.setCreatedAt(entity.getCreatedAt());
        return model;
    }
}
