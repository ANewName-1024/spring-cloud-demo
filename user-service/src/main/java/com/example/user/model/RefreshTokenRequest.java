package com.example.user.model;

import lombok.Data;

/**
 * Token 刷新请求
 */
@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
