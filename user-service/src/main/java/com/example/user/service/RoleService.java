package com.example.user.service;

import com.example.user.entity.Permission;
import com.example.user.entity.Role;
import com.example.user.entity.User;
import com.example.user.repository.PermissionRepository;
import com.example.user.repository.RoleRepository;
import com.example.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色管理服务
 */
@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建角色
     */
    public Role createRole(String name, String description) {
        if (roleRepository.existsByName(name)) {
            throw new RuntimeException("角色已存在");
        }
        Role role = new Role(name, description);
        return roleRepository.save(role);
    }

    /**
     * 为角色添加权限
     */
    @Transactional
    public Role addPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("权限不存在"));
        
        role.getPermissions().add(permission);
        return roleRepository.save(role);
    }

    /**
     * 为用户分配角色
     */
    @Transactional
    public User assignRoleToUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    /**
     * 获取所有角色
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * 获取所有权限
     */
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * 创建权限
     */
    public Permission createPermission(String name, String description) {
        if (permissionRepository.existsByName(name)) {
            throw new RuntimeException("权限已存在");
        }
        Permission permission = new Permission(name, description);
        return permissionRepository.save(permission);
    }

    /**
     * 初始化默认权限
     */
    @Transactional
    public void initDefaultPermissions() {
        String[][] defaultPerms = {
            {"user:read", "读取用户信息"},
            {"user:write", "创建/修改用户"},
            {"user:delete", "删除用户"},
            {"admin:manage", "管理员权限"},
            {"order:read", "读取订单"},
            {"order:write", "创建/修改订单"},
            {"product:read", "读取商品"},
            {"product:write", "管理商品"}
        };

        for (String[] perm : defaultPerms) {
            if (!permissionRepository.existsByName(perm[0])) {
                permissionRepository.save(new Permission(perm[0], perm[1]));
            }
        }
    }

    /**
     * 初始化默认角色
     */
    @Transactional
    public void initDefaultRoles() {
        // 创建 ADMIN 角色
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = new Role("ADMIN", "管理员");
            // ADMIN 拥有所有权限
            adminRole.setPermissions(new HashSet<>(permissionRepository.findAll()));
            roleRepository.save(adminRole);
        }

        // 创建 USER 角色
        if (!roleRepository.existsByName("USER")) {
            Role userRole = new Role("USER", "普通用户");
            // USER 只有基本权限
            permissionRepository.findByName("user:read").ifPresent(p -> {
                userRole.getPermissions().add(p);
            });
            roleRepository.save(userRole);
        }

        // 创建 MANAGER 角色
        if (!roleRepository.existsByName("MANAGER")) {
            Role managerRole = new Role("MANAGER", "经理");
            // MANAGER 有订单和商品权限
            permissionRepository.findByName("order:read").ifPresent(p -> managerRole.getPermissions().add(p));
            permissionRepository.findByName("order:write").ifPresent(p -> managerRole.getPermissions().add(p));
            permissionRepository.findByName("product:read").ifPresent(p -> managerRole.getPermissions().add(p));
            permissionRepository.findByName("product:write").ifPresent(p -> managerRole.getPermissions().add(p));
            roleRepository.save(managerRole);
        }
    }
}
