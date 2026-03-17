# 代码审查 Issues

## 高优先级 (High)

### 1. Gateway JWT Secret 配置为空
- **文件**: `gateway/src/main/resources/application.yml`
- **问题**: `jwt.secret: ${JWT_SECRET:}` 默认值为空，可能导致 Token 验证失败
- **建议**: 设置默认值或确保环境变量已配置

### 2. 数据库密码仍存在配置中
- **文件**: `config-service/src/main/resources/application.yml`
- **问题**: `password: NewPass2024` 硬编码在配置文件中
- **建议**: 使用环境变量 `${DB_PASSWORD}`

---

## 中优先级 (Medium)

### 3. 缺少统一的异常处理
- **问题**: 各服务缺少全局异常处理
- **建议**: 添加 `@ControllerAdvice` 统一异常处理

### 4. 日志输出问题
- **问题**: 可能存在 `System.out.println`
- **建议**: 使用日志框架

### 5. 缺少单元测试
- **问题**: 没有单元测试
- **建议**: 添加 JUnit 测试

---

## 低优先级 (Low)

### 6. 代码注释
- **问题**: 部分类缺少注释
- **建议**: 添加 Javadoc 注释

### 7. 硬编码文本
- **文件**: 多处
- **问题**: 错误消息、状态码硬编码
- **建议**: 使用常量或枚举

### 8. TODO 注释
- **问题**: 存在未完成的代码
- **建议**: 完成或记录在 Issues 中

---

## 建议修复优先级

1. 首先修复 Issue #1 (JWT Secret)
2. 然后修复 Issue #2 (数据库密码)
3. 添加全局异常处理
4. 添加单元测试
