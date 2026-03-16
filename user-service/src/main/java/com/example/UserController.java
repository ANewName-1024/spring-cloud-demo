package com.example;

import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final List<User> users = Arrays.asList(
        new User(1L, "Alice", "alice@example.com"),
        new User(2L, "Bob", "bob@example.com"),
        new User(3L, "Charlie", "charlie@example.com")
    );

    @GetMapping
    public List<User> getAllUsers() {
        return users;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return users.stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}
