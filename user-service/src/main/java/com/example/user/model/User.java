package com.example.user.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户模型
 */
@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private Set<Role> roles;
}
