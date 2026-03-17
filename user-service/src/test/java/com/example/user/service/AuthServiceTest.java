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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("Test@123456"));
        testUser.setEnabled(true);
        testUser.setRoles(new HashSet<>());
        testUser.setPermissions(new HashSet<>());

        // 创建测试角色
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("USER");
        testRole.setDescription("普通用户");
        testRole.setPermissions(new HashSet<>());
    }

    @Nested
    @DisplayName("用户注册测试")
    class RegisterTests {

        @Test
        @DisplayName("注册成功 - 正常用户")
        void shouldRegisterSuccessfully() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("New@123456");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(testRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(2L);
                return u;
            });

            // When
            User result = authService.register(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getEmail()).isEqualTo("new@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("注册失败 - 用户名已存在")
        void shouldFailWhenUsernameExists() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("existinguser");
            request.setEmail("test@example.com");
            request.setPassword("Test@123456");

            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("用户名已存在");
        }

        @Test
        @DisplayName("注册失败 - 用户名为空")
        void shouldFailWhenUsernameIsEmpty() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("");
            request.setEmail("test@example.com");
            request.setPassword("Test@123456");

            // When & Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("用户名不能为空");
        }

        @Test
        @DisplayName("注册失败 - 密码太简单")
        void shouldFailWhenPasswordIsWeak() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("test@example.com");
            request.setPassword("123456"); // 弱密码

            when(userRepository.existsByUsername("newuser")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("密码太简单");
        }

        @Test
        @DisplayName("注册失败 - 密码缺少数字")
        void shouldFailWhenPasswordMissingNumber() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("test@example.com");
            request.setPassword("abcdefgh"); // 没有数字

            when(userRepository.existsByUsername("newuser")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("密码必须包含数字");
        }

        @Test
        @DisplayName("注册失败 - 密码缺少字母")
        void shouldFailWhenPasswordMissingLetter() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("test@example.com");
            request.setPassword("12345678"); // 没有字母

            when(userRepository.existsByUsername("newuser")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("密码必须包含字母");
        }

        @Test
        @DisplayName("注册失败 - 用户名格式错误")
        void shouldFailWhenUsernameFormatInvalid() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("ab"); // 小于3位
            request.setEmail("test@example.com");
            request.setPassword("Test@123456");

            when(userRepository.existsByUsername("ab")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("用户名格式不正确");
        }

        @Test
        @DisplayName("注册成功 - 常见弱密码应被拒绝")
        void shouldRejectCommonWeakPasswords() {
            // Given
            String[] weakPasswords = {"password", "123456", "admin123", "qwerty123"};

            for (String weakPassword : weakPasswords) {
                RegisterRequest request = new RegisterRequest();
                request.setUsername("user" + weakPassword);
                request.setEmail("test@example.com");
                request.setPassword(weakPassword);

                when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);

                // When & Then
                assertThatThrownBy(() -> authService.register(request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("密码太简单");

                reset(userRepository);
            }
        }
    }

    @Nested
    @DisplayName("用户登录测试")
    class LoginTests {

        @Test
        @DisplayName("登录成功")
        void shouldLoginSuccessfully() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("Test@123456");

            Set<String> roles = Set.of("USER");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(jwtUtil.generateToken(eq("testuser"), eq(1L), any(), any()))
                    .thenReturn("access-token-xyz");

            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken("refresh-token-xyz");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token-xyz");
            assertThat(response.getRefreshToken()).isNotNull();
            assertThat(response.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("登录失败 - 用户名错误")
        void shouldFailWhenUsernameWrong() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("wronguser");
            request.setPassword("Test@123456");

            when(userRepository.findByUsername("wronguser")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("用户名或密码错误");
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void shouldFailWhenPasswordWrong() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("Wrong@123456");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("用户名或密码错误");
        }

        @Test
        @DisplayName("登录失败 - 用户被禁用")
        void shouldFailWhenUserDisabled() {
            // Given
            testUser.setEnabled(false);
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("Test@123456");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("账号已被禁用");
        }
    }

    @Nested
    @DisplayName("Token 刷新测试")
    class RefreshTokenTests {

        @Test
        @DisplayName("刷新 Token 成功")
        void shouldRefreshTokenSuccessfully() {
            // Given
            String refreshTokenValue = "valid-refresh-token";
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(refreshTokenValue);
            refreshToken.setUser(testUser);
            refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
            refreshToken.setRevoked(false);

            when(refreshTokenRepository.findByToken(refreshTokenValue))
                    .thenReturn(Optional.of(refreshToken));
            when(jwtUtil.generateToken(any(), any(), any(), any()))
                    .thenReturn("new-access-token");

            RefreshToken newRefreshToken = new RefreshToken();
            newRefreshToken.setToken("new-refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenReturn(newRefreshToken);

            // When
            LoginResponse response = authService.refreshToken(refreshTokenValue);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("刷新 Token 失败 - Token 无效")
        void shouldFailWhenRefreshTokenInvalid() {
            // Given
            when(refreshTokenRepository.findByToken("invalid-token"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.refreshToken("invalid-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Refresh Token 无效");
        }

        @Test
        @DisplayName("刷新 Token 失败 - Token 已过期")
        void shouldFailWhenRefreshTokenExpired() {
            // Given
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken("expired-token");
            refreshToken.setUser(testUser);
            refreshToken.setExpiresAt(LocalDateTime.now().minusDays(1)); // 已过期
            refreshToken.setRevoked(false);

            when(refreshTokenRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(refreshToken));

            // When & Then
            assertThatThrownBy(() -> authService.refreshToken("expired-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("已过期");
        }

        @Test
        @DisplayName("刷新 Token 失败 - Token 已撤销")
        void shouldFailWhenRefreshTokenRevoked() {
            // Given
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken("revoked-token");
            refreshToken.setUser(testUser);
            refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
            refreshToken.setRevoked(true); // 已撤销

            when(refreshTokenRepository.findByToken("revoked-token"))
                    .thenReturn(Optional.of(refreshToken));

            // When & Then
            assertThatThrownBy(() -> authService.refreshToken("revoked-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("已撤销");
        }
    }

    @Nested
    @DisplayName("权限检查测试")
    class PermissionTests {

        @Test
        @DisplayName("检查用户权限 - 有权限")
        void shouldReturnTrueWhenUserHasPermission() {
            // Given
            testUser.getPermissions().add("user:read");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            boolean hasPermission = authService.hasPermission("testuser", "user:read");

            // Then
            assertThat(hasPermission).isTrue();
        }

        @Test
        @DisplayName("检查用户权限 - 无权限")
        void shouldReturnFalseWhenUserNoPermission() {
            // Given
            testUser.getPermissions().add("user:read");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            boolean hasPermission = authService.hasPermission("testuser", "user:write");

            // Then
            assertThat(hasPermission).isFalse();
        }

        @Test
        @DisplayName("检查用户权限 - 用户不存在")
        void shouldReturnFalseWhenUserNotFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When
            boolean hasPermission = authService.hasPermission("nonexistent", "user:read");

            // Then
            assertThat(hasPermission).isFalse();
        }

        @Test
        @DisplayName("检查用户角色 - 有角色")
        void shouldReturnTrueWhenUserHasRole() {
            // Given
            testUser.getRoles().add(testRole);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            boolean hasRole = authService.hasRole("testuser", "USER");

            // Then
            assertThat(hasRole).isTrue();
        }

        @Test
        @DisplayName("检查用户角色 - 无角色")
        void shouldReturnFalseWhenUserNoRole() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            boolean hasRole = authService.hasRole("testuser", "ADMIN");

            // Then
            assertThat(hasRole).isFalse();
        }
    }
}
