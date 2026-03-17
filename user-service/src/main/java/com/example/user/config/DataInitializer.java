package com.example.user.config;

import com.example.user.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器 - 启动时初始化默认权限和角色
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleService roleService;

    @Override
    public void run(String... args) throws Exception {
        // 初始化默认权限
        roleService.initDefaultPermissions();
        // 初始化默认角色
        roleService.initDefaultRoles();
    }
}
