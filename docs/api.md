# Spring Cloud Demo API 文档

## 概述

本文档描述了 Spring Cloud Demo 项目中各个服务的 API 接口。

**基础 URL**: `http://localhost:8080` (通过 API Gateway)

**各服务独立端口**:
- User Service: `http://localhost:8081`
- Order Service: `http://localhost:8082`

---

## 用户服务 (User Service)

### 获取用户信息

**GET** `/users/{id}`

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户 ID |

**响应示例**:
```json
{
  "id": 1,
  "username": "john",
  "email": "john@example.com",
  "createdAt": "2026-01-01T00:00:00"
}
```

---

### 获取用户列表

**GET** `/users`

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 (默认 0) |
| size | Integer | 否 | 每页数量 (默认 10) |

**响应示例**:
```json
{
  "content": [
    {
      "id": 1,
      "username": "john",
      "email": "john@example.com"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 订单服务 (Order Service)

### 获取订单信息

**GET** `/orders/{id}`

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 订单 ID |

**响应示例**:
```json
{
  "id": 1,
  "userId": 1,
  "orderNo": "ORD20260317001",
  "amount": 99.99,
  "status": "PENDING",
  "createdAt": "2026-03-17T10:00:00"
}
```

---

### 获取订单列表

**GET** `/orders`

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 否 | 用户 ID (筛选) |
| status | String | 否 | 订单状态 |
| page | Integer | 否 | 页码 (默认 0) |
| size | Integer | 否 | 每页数量 (默认 10) |

**响应示例**:
```json
{
  "content": [
    {
      "id": 1,
      "userId": 1,
      "orderNo": "ORD20260317001",
      "amount": 99.99,
      "status": "PENDING"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 创建订单

**POST** `/orders`

**请求体**:
```json
{
  "userId": 1,
  "amount": 199.99,
  "items": [
    {
      "productId": 100,
      "quantity": 2,
      "price": 99.99
    }
  ]
}
```

**响应示例**:
```json
{
  "id": 2,
  "userId": 1,
  "orderNo": "ORD20260317002",
  "amount": 199.99,
  "status": "PENDING",
  "createdAt": "2026-03-17T11:00:00"
}
```

---

## 错误响应

### 错误响应格式

```json
{
  "error": "Not Found",
  "message": "User with id 999 not found",
  "status": 404,
  "timestamp": "2026-03-17T10:30:00"
}
```

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 通过 API Gateway 访问

所有服务都可以通过 API Gateway 统一访问：

| 服务 | 路径 | 示例 |
|------|------|------|
| User Service | /users/** | GET /users/1 |
| Order Service | /orders/** | GET /orders/1 |

---

最后更新: 2026-03-17
