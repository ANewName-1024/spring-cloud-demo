package com.example.user.service;

import com.example.user.entity.Permission;
import com.example.user.entity.Role;
import com.example.user.repository.PermissionRepository;
import com.example.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService 单元测试")
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleService roleService;

    private Role adminRole;
    private Role userRole;
    private Permission userReadPermission;
    private Permission userWritePermission;

    @BeforeEach
    void setUp() {
        // 创建权限
        userReadPermission = new Permission();
        userReadPermission.setId(1L);
        userReadPermission.setName("user:read");
        userReadPermission.setDescription("读取用户");

        userWritePermission = new Permission();
        userWritePermission.setId(2L);
        userWritePermission.setName("user:write");
        userWritePermission.setDescription("写入用户");

        // 创建角色
        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ADMIN");
        adminRole.setDescription("管理员");
        adminRole.setPermissions(new HashSet<>(Set.of(userReadPermission, userWritePermission)));

        userRole = new Role();
        userRole.setId(2L);
        userRole.setName("USER");
        userRole.setDescription("普通用户");
        userRole.setPermissions(new HashSet<>(Set.of(userReadPermission)));
    }

    @Nested
    @DisplayName("角色查询测试")
    class QueryTests {

        @Test
        @DisplayName("查询所有角色")
        void shouldFindAllRoles() {
            // Given
            List<Role> roles = List.of(adminRole, userRole);
            when(roleRepository.findAll()).thenReturn(roles);

            // When
            List<Role> result = roleService.getAllRoles();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Role::getName)
                    .containsExactlyInAnyOrder("ADMIN", "USER");
        }

        @Test
        @DisplayName("根据名称查询角色")
        void shouldFindRoleByName() {
            // Given
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

            // When
            Optional<Role> result = roleService.getRoleByName("ADMIN");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("查询角色权限")
        void shouldGetRolePermissions() {
            // Given
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

            // When
            Set<String> permissions = roleService.getRolePermissions("ADMIN");

            // Then
            assertThat(permissions).contains("user:read", "user:write");
        }

        @Test
        @DisplayName("查询不存在的角色")
        void shouldReturnEmptyWhenRoleNotFound() {
            // Given
            when(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

            // When
            Optional<Role> result = roleService.getRoleByName("NONEXISTENT");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("角色创建测试")
    class CreateTests {

        @Test
        @DisplayName("创建新角色")
        void shouldCreateRole() {
            // Given
            Role newRole = new Role();
            newRole.setName("OPERATOR");
            newRole.setDescription("运营人员");

            when(roleRepository.findByName("OPERATOR")).thenReturn(Optional.empty());
            when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
                Role r = invocation.getArgument(0);
                r.setId(3L);
                return r;
            });

            // When
            Role result = roleService.createRole(newRole);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("OPERATOR");
            verify(roleRepository).save(newRole);
        }

        @Test
        @DisplayName("创建角色失败 - 角色名已存在")
        void shouldFailWhenRoleNameExists() {
            // Given
            Role newRole = new Role();
            newRole.setName("ADMIN");

            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

            // When & Then
            assertThatThrownBy(() -> roleService.createRole(newRole))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("角色已存在");
        }
    }

    @Nested
    @DisplayName("角色权限管理测试")
    class PermissionTests {

        @Test
        @DisplayName("为角色添加权限")
        void shouldAddPermissionToRole() {
            // Given
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(permissionRepository.findByName("user:write")).thenReturn(Optional.of(userWritePermission));
            when(roleRepository.save(any(Role.class))).thenReturn(userRole);

            // When
            Role result = roleService.addPermissionToRole("USER", "user:write");

            // Then
            assertThat(result.getPermissions()).contains(userWritePermission);
            verify(roleRepository).save(userRole);
        }

        @Test
        @DisplayName("为角色移除权限")
        void shouldRemovePermissionFromRole() {
            // Given
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(roleRepository.save(any(Role.class))).thenReturn(adminRole);

            // When
            Role result = roleService.removePermissionFromRole("ADMIN", "user:write");

            // Then
            assertThat(result.getPermissions()).doesNotContain(userWritePermission);
            verify(roleRepository).save(adminRole);
        }

        @Test
        @DisplayName("批量添加权限")
        void shouldAddMultiplePermissions() {
            // Given
            Permission configRead = new Permission();
            configRead.setName("config:read");
            
            Permission configWrite = new Permission();
            configWrite.setName("config:write");

            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(permissionRepository.findByName("config:read")).thenReturn(Optional.of(configRead));
            when(permissionRepository.findByName("config:write")).thenReturn(Optional.of(configWrite));
            when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Role result = roleService.addPermissionsToRole("ADMIN", Set.of("config:read", "config:write"));

            // Then
            assertThat(result.getPermissions()).hasSize(4);
        }

        @Test
        @DisplayName("检查角色是否有权限")
        void shouldCheckRoleHasPermission() {
            // Given
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

            // When
            boolean hasRead = roleService.hasPermission("USER", "user:read");
            boolean hasWrite = roleService.hasPermission("USER", "user:write");

            // Then
            assertThat(hasRead).isTrue();
            assertThat(hasWrite).isFalse();
        }
    }

    @Nested
    @DisplayName("角色删除测试")
    class DeleteTests {

        @Test
        @DisplayName("删除角色")
        void shouldDeleteRole() {
            // Given
            when(roleRepository.findByName("OPERATOR")).thenReturn(Optional.empty());
            doNothing().when(roleRepository).deleteByName("OPERATOR");

            // When
            roleService.deleteRole("OPERATOR");

            // Then
            verify(roleRepository).deleteByName("OPERATOR");
        }

        @Test
        @DisplayName("删除角色失败 - 角色不存在")
        void shouldFailWhenRoleNotFound() {
            // Given
            when(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roleService.deleteRole("NONEXISTENT"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("角色不存在");
        }
    }

    @Nested
    @DisplayName("获取用户角色测试")
    class UserRoleTests {

        @Test
        @DisplayName("获取用户的所有角色名称")
        void shouldGetUserRoles() {
            // Given
            Role admin = new Role();
            admin.setName("ADMIN");
            Role user = new Role();
            user.setName("USER");

            Set<Role> roles = Set.of(admin, user);

            // When
            Set<String> roleNames = RoleService.getUserRoles(roles);

            // Then
            assertThat(roleNames).containsExactlyInAnyOrder("ADMIN", "USER");
        }

        @Test
        @DisplayName("获取用户的权限集合")
        void shouldGetUserPermissions() {
            // Given
            Permission p1 = new Permission();
            p1.setName("user:read");
            Permission p2 = new Permission();
            p2.setName("user:write");
            
            Role role = new Role();
            role.setPermissions(Set.of(p1, p2));

            // When
            Set<String> permissions = RoleService.getUserPermissions(role);

            // Then
            assertThat(permissions).containsExactlyInAnyOrder("user:read", "user:write");
        }
    }
}
