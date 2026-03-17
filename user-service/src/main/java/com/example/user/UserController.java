package com.example.user.controller;

import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/list")
    public List<User> list() {
        return userRepository.findAll();
    }

    /**
     * 创建用户（需要管理员权限）
     */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('admin:manage')")
    public ResponseEntity<?> add(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");
        
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少 username 或 password 参数"));
        }
        
        // 使用随机密码并返回，让管理员分发给用户
        String randomPassword = UUID.randomUUID().toString().substring(0, 8);
        User user = new User(username, email, randomPassword);
        User saved = userRepository.save(user);
        
        return ResponseEntity.ok(Map.of(
            "id", saved.getId(),
            "username", saved.getUsername(),
            "email", saved.getEmail() != null ? saved.getEmail() : "",
            "tempPassword", randomPassword
        ));
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    /**
     * 获取指定用户信息
     */
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * 删除用户 - 需要管理员权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:manage')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "用户已删除"));
    }
}
