package com.example.user.api;

import com.example.user.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 API 接口
 * 根据 user-service-api.yaml 自动生成
 */
@Tag(name = "认证", description = "用户认证相关接口")
public interface AuthApi {

    /**
     * 用户注册
     * POST /user/auth/register
     */
    @Operation(summary = "用户注册", description = "创建新用户账户")
    @ApiResponse(responseCode = "200", description = "注册成功", content = @Content)
    @ApiResponse(responseCode = "400", description = "请求参数错误", content = @Content)
    ResponseEntity<User> register(RegisterRequest request);

    /**
     * 用户登录
     * POST /user/auth/login
     */
    @Operation(summary = "用户登录", description = "用户登录获取 JWT Token")
    @ApiResponse(responseCode = "200", description = "登录成功", content = @Content)
    @ApiResponse(responseCode = "401", description = "用户名或密码错误", content = @Content)
    ResponseEntity<LoginResponse> login(LoginRequest request);

    /**
     * 刷新 Token
     * POST /user/auth/refresh
     */
    @Operation(summary = "刷新 Token", description = "使用 Refresh Token 获取新的 Access Token")
    @ApiResponse(responseCode = "200", description = "刷新成功", content = @Content)
    @ApiResponse(responseCode = "401", description = "Token 无效或已过期", content = @Content)
    ResponseEntity<LoginResponse> refreshToken(RefreshTokenRequest request);

    /**
     * 退出登录
     * POST /user/auth/logout
     */
    @Operation(summary = "退出登录", description = "撤销当前用户的所有 Token")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "退出成功", content = @Content)
    ResponseEntity<Void> logout();

    /**
     * 获取当前用户
     * GET /user/auth/me
     */
    @Operation(summary = "获取当前用户", description = "获取已登录用户的详细信息")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content)
    @ApiResponse(responseCode = "401", description = "未登录", content = @Content)
    ResponseEntity<User> getCurrentUser();
}
