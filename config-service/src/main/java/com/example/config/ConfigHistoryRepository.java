package com.example.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConfigHistoryRepository extends JpaRepository<ConfigHistory, Long> {
    
    List<ConfigHistory> findByConfigIdOrderByOperationTimeDesc(Long configId);
    
    List<ConfigHistory> findByConfigKeyOrderByOperationTimeDesc(String configKey);
    
    // ========== 定时任务需要的方法 ==========
    
    int deleteByCreateTimeBefore(LocalDateTime dateTime);
    
    @Query("SELECT COUNT(h) FROM ConfigHistory h WHERE h.createTime < :dateTime")
    int countByCreateTimeBefore(LocalDateTime dateTime);
    
    List<ConfigHistory> findByCreateTimeBefore(LocalDateTime dateTime);
    
    @Modifying
    @Query("DELETE FROM ConfigHistory WHERE isDeleted = 1")
    int deleteInvalidConfigs();
}
