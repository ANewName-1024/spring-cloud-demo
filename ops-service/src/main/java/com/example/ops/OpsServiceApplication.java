package com.example.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Ops 运维服务启动类
 * 
 * 功能：
 * - 服务健康监控
 * - 日志聚合
 * - 链路追踪
 * - 告警管理
 */
@SpringBootApplication
@EnableDiscoveryClient
public class OpsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpsServiceApplication.class, args);
    }
}
