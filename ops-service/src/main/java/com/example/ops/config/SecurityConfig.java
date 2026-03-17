package com.example.ops.config;

import com.example.ops.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 * Gateway 统一认证后，各服务信任 Gateway 传递的请求头
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 认证接口公开
                .requestMatchers("/ops/auth/login", "/ops/auth/register").permitAll()
                // 健康检查
                .requestMatchers("/actuator/**", "/ops/health").permitAll()
                // 监控数据 (需要 ops:view 权限)
                .requestMatchers("/ops/metrics").hasAuthority("ops:view")
                // 告警管理 (需要 ops:manage 权限)
                .requestMatchers("/ops/alerts/**").hasAuthority("ops:manage")
                // 日志级别管理 (需要 admin 权限)
                .requestMatchers("/ops/loggers/**").hasAuthority("admin:manage")
                // 其他接口需要认证
                .requestMatchers("/ops/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
