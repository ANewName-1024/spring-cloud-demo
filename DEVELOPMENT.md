# 开发规范

## 代码规范

### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | `UserService`, `AuthController` |
| 方法名 | lowerCamelCase | `getUser()`, `saveConfig()` |
| 变量名 | lowerCamelCase | `userName`, `accessToken` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 包名 | 全小写 | `com.example.user` |

### Java 代码规范

```java
// ✅ 正确示例
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}

// ❌ 避免
@Service
public class userService {  // 类名小写
    public void GET_USER() {  // 方法名全大写
        // ...
    }
}
```

### Controller 规范

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    // 使用具体的请求方法注解
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<User> createUser(@RequestBody @Valid CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }
}
```

### Service 规范

```java
@Service
@Transactional(rollbackFor = Exception.class)
public class UserService {
    
    // 业务逻辑必须添加事务
    public User createUser(CreateUserRequest request) {
        // 参数校验
        validateRequest(request);
        
        // 业务处理
        // ...
        
        return user;
    }
    
    private void validateRequest(CreateUserRequest request) {
        if (request.getUsername() == null) {
            throw new IllegalArgumentException("用户名不能为空");
        }
    }
}
```

### Entity 规范

```java
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    
    // Getters and Setters
}
```

## Git 提交规范

### 提交信息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式 |
| `refactor` | 重构 |
| `test` | 测试 |
| `chore` | 构建/工具 |

### 示例

```bash
# 正确示例
git commit -m "feat(user): 添加用户注册功能"
git commit -m "fix(auth): 修复 Token 过期问题"
git commit -m "docs: 更新 API 文档"

# 错误示例
git commit -m "update"  # 不明确
git commit -m "fix bug"  # 小写
git commit -m "添加功能"  # 中文
```

## 安全规范

### 敏感信息

- ❌ 禁止硬编码密码、密钥、Token
- ✅ 使用环境变量或配置中心
- ✅ 敏感信息加入 `.gitignore`

```yaml
# ✅ 正确
spring:
  datasource:
    password: ${DB_PASSWORD}

# ❌ 错误
spring:
  datasource:
    password: mySecretPassword
```

### 密码安全

- 密码必须加密存储（BCrypt）
- 密码强度验证（8位+数字+字母）
- 弱密码字典检测

### 接口鉴权

- 敏感接口必须鉴权
- 使用 Gateway 统一认证
- 豁免路径需谨慎配置

## 注释规范

### 类注释

```java
/**
 * 用户服务
 * 负责用户的 CRUD 操作和认证
 *
 * @author developer
 * @since 2024-01-01
 */
public class UserService {
```

### 方法注释

```java
/**
 * 根据 ID 获取用户
 *
 * @param id 用户 ID
 * @return 用户信息
 * @throws UserNotFoundException 用户不存在
 */
public User getUserById(Long id) {
```

## 测试规范

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void shouldCreateUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("test");
        request.setPassword("Password123");
        
        // When
        User user = userService.createUser(request);
        
        // Then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo("test");
    }
}
```

## 日志规范

```java
// ✅ 正确
log.info("用户登录成功: {}", username);
log.error("数据库连接失败", e);

// ❌ 避免
System.out.println("用户登录");  // 使用日志
log.debug(e.getMessage());  // 异常必须记录堆栈
```

## 配置规范

### 环境配置

```bash
# 开发环境
.env.dev
# 生产环境
.env.prod
```

### 配置优先级

```
命令行参数 > 环境变量 > application.yml > application-default.yml
```
