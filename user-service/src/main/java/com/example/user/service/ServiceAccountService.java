package com.example.user.service;

import com.example.user.entity.Role;
import com.example.user.entity.ServiceAccount;
import com.example.user.repository.RoleRepository;
import com.example.user.repository.ServiceAccountRepository;
import com.example.user.security.AkskUtil;
import com.example.user.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 机机账户服务
 */
@Service
public class ServiceAccountService {

    @Autowired
    private ServiceAccountRepository serviceAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AkskUtil akskUtil;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建机机账户
     */
    @Transactional
    public ServiceAccount createServiceAccount(String name, String description, String roleName) {
        if (serviceAccountRepository.existsByName(name)) {
            throw new RuntimeException("机机账户名称已存在");
        }

        ServiceAccount account = new ServiceAccount(name, description);
        
        // 生成 AKSK
        String accessKey = akskUtil.generateAccessKey();
        String secretKey = akskUtil.generateSecretKey();
        
        account.setAccessKey(accessKey);
        account.setSecretKeyHash(akskUtil.hashSecretKey(secretKey, passwordEncoder));
        
        // 分配角色
        if (roleName != null) {
            roleRepository.findByName(roleName).ifPresent(account.getRoles()::add);
        }
        
        ServiceAccount saved = serviceAccountRepository.save(account);
        
        // 返回时附带生成的 secretKey（只返回一次）
        saved.setSecretKeyHash(secretKey); // 临时存储明文供返回
        
        return saved;
    }

    /**
     * 使用 AKSK 登录
     */
    public AkskLoginResponse loginWithAksk(String accessKey, String secretKey) {
        ServiceAccount account = serviceAccountRepository.findByAccessKey(accessKey)
                .orElseThrow(() -> new RuntimeException("AccessKey 无效"));

        if (!account.getEnabled()) {
            throw new RuntimeException("机机账户已被禁用");
        }

        if (!akskUtil.verifySecretKey(secretKey, account.getSecretKeyHash(), passwordEncoder)) {
            throw new RuntimeException("SecretKey 无效");
        }

        // 更新最后使用时间
        account.setLastUsedAt(LocalDateTime.now());
        serviceAccountRepository.save(account);

        // 生成 Token
        Set<String> roles = Set.of(account.getName()); // 使用账户名称作为角色标识
        String token = jwtUtil.generateToken(
            account.getName(),
            account.getId(),
            roles,
            account.getPermissions()
        );

        return new AkskLoginResponse(token, accessKey, account.getName(), account.getId());
    }

    /**
     * 轮转 SecretKey
     */
    @Transactional
    public String rotateSecretKey(Long accountId) {
        ServiceAccount account = serviceAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("机机账户不存在"));

        // 生成新的 SecretKey
        String newSecretKey = akskUtil.generateSecretKey();
        account.setSecretKeyHash(akskUtil.hashSecretKey(newSecretKey, passwordEncoder));
        account.setSecretKeyRotatedAt(LocalDateTime.now());
        
        serviceAccountRepository.save(account);
        
        return newSecretKey;
    }

    /**
     * 轮转指定账户的 SecretKey（通过 AccessKey）
     */
    @Transactional
    public String rotateSecretKeyByAccessKey(String accessKey) {
        ServiceAccount account = serviceAccountRepository.findByAccessKey(accessKey)
                .orElseThrow(() -> new RuntimeException("AccessKey 无效"));
        
        return rotateSecretKey(account.getId());
    }

    /**
     * 获取所有机机账户
     */
    public java.util.List<ServiceAccount> getAllServiceAccounts() {
        return serviceAccountRepository.findAll();
    }

    /**
     * 获取机机账户详情
     */
    public ServiceAccount getServiceAccount(Long id) {
        return serviceAccountRepository.findById(id).orElse(null);
    }

    /**
     * 启用/禁用机机账户
     */
    @Transactional
    public ServiceAccount setEnabled(Long id, boolean enabled) {
        ServiceAccount account = serviceAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("机机账户不存在"));
        account.setEnabled(enabled);
        return serviceAccountRepository.save(account);
    }

    /**
     * 为机机账户分配角色
     */
    @Transactional
    public ServiceAccount assignRole(Long accountId, Long roleId) {
        ServiceAccount account = serviceAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("机机账户不存在"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        
        account.getRoles().add(role);
        return serviceAccountRepository.save(account);
    }

    /**
     * AKSK 登录响应
     */
    public static class AkskLoginResponse {
        private String token;
        private String accessKey;
        private String name;
        private Long accountId;

        public AkskLoginResponse(String token, String accessKey, String name, Long accountId) {
            this.token = token;
            this.accessKey = accessKey;
            this.name = name;
            this.accountId = accountId;
        }

        public String getToken() { return token; }
        public String getAccessKey() { return accessKey; }
        public String getName() { return name; }
        public Long getAccountId() { return accountId; }
    }
}
