package com.example.user.entity;

import com.example.common.listener.EntityEncryptListener;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 机机账户（Service Account）- 用于系统间调用
 */
@Entity
@Table(name = "service_accounts")
@EntityListeners(EntityEncryptListener.class)
public class ServiceAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "access_key", unique = true)
    private String accessKey;

    @Column(name = "secret_key_hash")
    private String secretKeyHash;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "secret_key_rotated_at")
    private LocalDateTime secretKeyRotatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "service_account_roles",
        joinColumns = @JoinColumn(name = "service_account_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private java.util.Set<Role> roles = new java.util.HashSet<>();

    public ServiceAccount() {}

    public ServiceAccount(String name, String description) {
        this.name = name;
        this.description = description;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKeyHash() { return secretKeyHash; }
    public void setSecretKeyHash(String secretKeyHash) { this.secretKeyHash = secretKeyHash; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public LocalDateTime getSecretKeyRotatedAt() { return secretKeyRotatedAt; }
    public void setSecretKeyRotatedAt(LocalDateTime secretKeyRotatedAt) { this.secretKeyRotatedAt = secretKeyRotatedAt; }
    public java.util.Set<Role> getRoles() { return roles; }
    public void setRoles(java.util.Set<Role> roles) { this.roles = roles; }

    /**
     * 获取机机账户所有权限
     */
    public java.util.Set<String> getPermissions() {
        java.util.Set<String> permissions = new java.util.HashSet<>();
        for (Role role : roles) {
            for (Permission permission : role.getPermissions()) {
                permissions.add(permission.getName());
            }
        }
        return permissions;
    }
}
