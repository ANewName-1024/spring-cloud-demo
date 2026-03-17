package com.example.user.security;

import com.example.user.entity.ServiceAccount;
import com.example.user.entity.User;
import com.example.user.repository.ServiceAccountRepository;
import com.example.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器 - 支持人机用户和机机账户
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceAccountRepository serviceAccountRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = getJwtFromRequest(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            String subject = jwtUtil.getUsernameFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            // 尝试查找人机用户
            User user = userRepository.findByUsername(subject).orElse(null);
            
            // 如果找不到用户，尝试机机账户
            ServiceAccount serviceAccount = null;
            if (user == null) {
                serviceAccount = serviceAccountRepository.findById(userId).orElse(null);
            }

            if (user != null && user.getEnabled()) {
                // 人机用户认证
                authenticateUser(user, request);
            } else if (serviceAccount != null && serviceAccount.getEnabled()) {
                // 机机账户认证
                authenticateServiceAccount(serviceAccount, request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(User user, HttpServletRequest request) {
        Set<String> permissions = user.getPermissions();
        Set<String> roles = user.getRoles().stream()
                .map(r -> "ROLE_" + r.getName().toUpperCase())
                .collect(Collectors.toSet());

        Set<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(p -> new SimpleGrantedAuthority(p))
                .collect(Collectors.toSet());
        
        roles.stream()
                .map(r -> new SimpleGrantedAuthority(r))
                .forEach(authorities::add);

        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(user, "N/A", authorities);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateServiceAccount(ServiceAccount serviceAccount, HttpServletRequest request) {
        Set<String> permissions = serviceAccount.getPermissions();
        
        // 机机账户使用特殊前缀标识
        Set<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(p -> new SimpleGrantedAuthority("SERVICE_" + p))
                .collect(Collectors.toSet());
        
        // 添加账户名称作为标识
        authorities.add(new SimpleGrantedAuthority("SERVICE_ACCOUNT:" + serviceAccount.getName()));

        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(serviceAccount, "N/A", authorities);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
