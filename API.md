# API 接口文档

## 基础信息

| 项目 | 值 |
|------|-----|
| 基础 URL | `http://localhost:8080` |
| 数据格式 | JSON |
| 编码 | UTF-8 |

---

## 认证接口

### 1. 用户注册

**请求**
```http
POST /user/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Password123",
  "inviteCode": "optional-invite-code"
}
```

**响应**
```json
{
  "message": "注册成功",
  "data": 1
}
```

---

### 2. 用户登录

**请求**
```http
POST /user/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "Password123"
}
```

**响应**
```json
{
  "token": "eyJhbGc...",
  "username": "testuser",
  "email": "test@example.com",
  "userId": 1
}
```

---

### 3. 机机账户登录 (AKSK)

**请求**
```http
POST /user/auth/ak/login
Content-Type: application/json

{
  "accessKey": "ak_xxxxxxxxxxxx",
  "secretKey": "yyyyyyyyyyyy"
}
```

**响应**
```json
{
  "token": "eyJhbGc...",
  "accessKey": "ak_xxxxxxxxxxxx",
  "name": "service-account-name",
  "accountId": 1
}
```

---

### 4. 获取当前用户信息

**请求**
```http
GET /user/auth/me
Authorization: Bearer <token>
```

**响应**
```json
{
  "type": "user",
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["USER"],
  "permissions": ["user:read"]
}
```

---

### 5. 创建机机账户

**请求**
```http
POST /user/auth/ak/create
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "payment-service",
  "description": "支付服务账户",
  "roleName": "MANAGER"
}
```

**响应**
```json
{
  "id": 1,
  "name": "payment-service",
  "accessKey": "ak_xxxxxxxxxxxx",
  "secretKey": "yyyyyyyyyyyy",
  "description": "支付服务账户",
  "createdAt": "2024-01-01T00:00:00"
}
```

> ⚠️ SecretKey 仅返回一次，请妥善保存！

---

### 6. 轮转 SecretKey

**请求**
```http
POST /user/auth/ak/{accountId}/rotate
Authorization: Bearer <admin-token>
```

**响应**
```json
{
  "message": "SecretKey 轮转成功",
  "secretKey": "new-secret-key"
}
```

---

### 7. 获取所有机机账户

**请求**
```http
GET /user/auth/ak/list
Authorization: Bearer <admin-token>
```

**响应**
```json
[
  {
    "id": 1,
    "name": "payment-service",
    "accessKey": "akxx****xxxx",
    "enabled": true,
    "roles": ["MANAGER"]
  }
]
```

---

## 用户管理接口

### 8. 获取用户列表

**请求**
```http
GET /user/list
Authorization: Bearer <token>
```

**响应**
```json
[
  {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "enabled": true
  }
]
```

---

### 9. 获取单个用户

**请求**
```http
GET /user/{id}
Authorization: Bearer <token>
```

**响应**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "enabled": true
}
```

---

### 10. 创建用户

**请求**
```http
POST /user/add
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "username": "newuser",
  "email": "new@example.com",
  "password": "Password123"
}
```

**响应**
```json
{
  "id": 2,
  "username": "newuser",
  "email": "new@example.com",
  "tempPassword": "abcd1234"
}
```

---

### 11. 删除用户

**请求**
```http
DELETE /user/{id}
Authorization: Bearer <admin-token>
```

**响应**
```json
{
  "message": "用户已删除"
}
```

---

## 管理员接口

### 12. 获取所有角色

**请求**
```http
GET /user/admin/roles
Authorization: Bearer <admin-token>
```

---

### 13. 获取所有权限

**请求**
```http
GET /user/admin/permissions
Authorization: Bearer <admin-token>
```

---

### 14. 创建角色

**请求**
```http
POST /user/admin/roles
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "CUSTOMER",
  "description": "客户角色"
}
```

---

### 15. 为用户分配角色

**请求**
```http
POST /user/admin/users/{userId}/roles/{roleId}
Authorization: Bearer <admin-token>
```

---

## 权限检查

### 16. 检查权限

**请求**
```http
GET /user/auth/check/{permission}
Authorization: Bearer <token>
```

**响应**
```json
{
  "message": "有权限",
  "data": true
}
```

---

## 错误码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 认证示例

### cURL

```bash
# 登录
curl -X POST http://localhost:8080/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Password123"}'

# 使用 Token 访问
curl http://localhost:8080/user/auth/me \
  -H "Authorization: Bearer eyJhbGc..."
```

### JavaScript

```javascript
// 登录
const login = async () => {
  const res = await fetch('/user/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'test', password: 'Password123' })
  });
  const { token } = await res.json();
  return token;
};

// 带 Token 请求
const getUser = async (token) => {
  const res = await fetch('/user/auth/me', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return await res.json();
};
```
