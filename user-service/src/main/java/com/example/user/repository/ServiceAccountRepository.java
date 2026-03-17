package com.example.user.repository;

import com.example.user.entity.ServiceAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceAccountRepository extends JpaRepository<ServiceAccount, Long> {
    Optional<ServiceAccount> findByAccessKey(String accessKey);
    boolean existsByAccessKey(String accessKey);
    boolean existsByName(String name);
}
