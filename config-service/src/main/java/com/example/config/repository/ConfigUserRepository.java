package com.example.config.repository;

import com.example.config.entity.ConfigUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigUserRepository extends JpaRepository<ConfigUser, Long> {
    Optional<ConfigUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
