package com.example.user.controller;

import com.example.user.entity.Permission;
import com.example.user.entity.Role;
import com.example.user.entity.User;
import com.example.user.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器 - 管理员专用
 */
@RestController
@RequestMapping("/user/admin")
@PreAuthorize("hasAuthority('admin:manage')")
public class AdminController {

    @Autowired
    private RoleService roleService;

    /**
     * 初始化默认权限和角色
     */
    @PostMapping("/init")
    public ResponseEntity<?> init() {
        roleService.initDefaultPermissions();
        roleService.initDefaultRoles();
        return ResponseEntity.ok(Map.of("message", "初始化完成"));
    }

    /**
     * 获取所有角色
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * 获取所有权限
     */
    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }

    /**
     * 创建角色
     */
    @PostMapping("/roles")
    public ResponseEntity<?> createRole(@RequestBody Map<String, String> request) {
        try {
            Role role = roleService.createRole(request.get("name"), request.get("description"));
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 创建权限
     */
    @PostMapping("/permissions")
    public ResponseEntity<?> createPermission(@RequestBody Map<String, String> request) {
        try {
            Permission permission = roleService.createPermission(request.get("name"), request.get("description"));
            return ResponseEntity.ok(permission);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 为角色添加权限
     */
    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<?> addPermissionToRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        try {
            Role role = roleService.addPermissionToRole(roleId, permissionId);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 为用户分配角色
     */
    @PostMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<?> assignRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        try {
            User user = roleService.assignRoleToUser(userId, roleId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
