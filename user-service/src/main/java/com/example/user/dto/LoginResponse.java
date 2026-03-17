package com.example.user.dto;

/**
 * 登录响应
 */
public class LoginResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String email;
    private Long userId;
    private Long tokenExpiresIn;  // Access Token 过期时间(秒)
    private Long refreshTokenExpiresIn;  // Refresh Token 过期时间(秒)

    public LoginResponse() {}

    public LoginResponse(String token, String refreshToken, String username, String email, Long userId, 
                        Long tokenExpiresIn, Long refreshTokenExpiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
        this.email = email;
        this.userId = userId;
        this.tokenExpiresIn = tokenExpiresIn;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTokenExpiresIn() { return tokenExpiresIn; }
    public void setTokenExpiresIn(Long tokenExpiresIn) { this.tokenExpiresIn = tokenExpiresIn; }
    public Long getRefreshTokenExpiresIn() { return refreshTokenExpiresIn; }
    public void setRefreshTokenExpiresIn(Long refreshTokenExpiresIn) { this.refreshTokenExpiresIn = refreshTokenExpiresIn; }
}
