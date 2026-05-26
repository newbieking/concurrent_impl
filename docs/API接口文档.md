# API接口文档

## 1. 概述

本文档描述了企业级并发与安全示例项目的API接口。

**基础信息**：
- 基础路径：`/api`
- 请求格式：`application/json`
- 响应格式：`application/json`
- 认证方式：JWT Token

## 2. 通用响应格式

### 2.1 成功响应

```json
{
    "code": 200,
    "message": "success",
    "data": {}
}
```

### 2.2 错误响应

```json
{
    "code": 400,
    "message": "参数错误",
    "data": null
}
```

### 2.3 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 业务冲突（如重复提交） |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |
| 60001 | 用户不存在 |
| 60002 | 密码错误 |
| 60003 | 账户已锁定 |
| 60101 | 商品不存在 |
| 60102 | 库存不足 |
| 60201 | 订单不存在 |
| 60202 | 订单状态异常 |

## 3. 订单接口

### 3.1 创建订单

**接口说明**：创建新订单

**请求信息**：
- URL：`POST /api/orders`
- 限流：每IP每秒10次

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |
| productId | Long | 是 | 商品ID |
| quantity | Integer | 是 | 数量 |

**请求示例**：
```json
{
    "userId": 1001,
    "productId": 2001,
    "quantity": 1
}
```

**响应示例**：
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "orderNo": "ORD17152345678901234",
        "userId": 1001,
        "productId": 2001,
        "quantity": 1,
        "totalAmount": 99.99,
        "status": 0,
        "createdAt": "2024-05-09 10:30:00"
    }
}
```

**错误响应**：
```json
{
    "code": 60102,
    "message": "库存不足",
    "data": null
}
```

### 3.2 支付订单

**接口说明**：支付订单

**请求信息**：
- URL：`POST /api/orders/{orderNo}/pay`

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | String | 是 | 订单号 |

**响应示例**：
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "orderNo": "ORD17152345678901234",
        "status": 1,
        "payTime": "2024-05-09 10:35:00"
    }
}
```

**错误响应**：
```json
{
    "code": 60004,
    "message": "余额不足",
    "data": null
}
```

### 3.3 查询订单

**接口说明**：查询订单详情

**请求信息**：
- URL：`GET /api/orders/{orderNo}`

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | String | 是 | 订单号 |

**响应示例**：
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "orderNo": "ORD17152345678901234",
        "userId": 1001,
        "productId": 2001,
        "quantity": 1,
        "totalAmount": 99.99,
        "status": 1,
        "createdAt": "2024-05-09 10:30:00",
        "payTime": "2024-05-09 10:35:00"
    }
}
```

## 4. 用户接口

### 4.1 获取用户列表

**接口说明**：获取所有用户

**请求信息**：
- URL：`GET /api/orders/users`

**响应示例**：
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 1,
            "username": "zhangsan",
            "phone": "13800138000",
            "balance": 10000.00
        }
    ]
}
```

### 4.2 获取用户详情

**接口说明**：获取用户详情

**请求信息**：
- URL：`GET /api/users/{id}`

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户ID |

## 5. 商品接口

### 5.1 获取商品列表

**接口说明**：获取所有商品

**请求信息**：
- URL：`GET /api/orders/products`

**响应示例**：
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 1,
            "name": "iPhone 15",
            "price": 6999.00,
            "stock": 100
        }
    ]
}
```

### 5.2 获取商品详情

**接口说明**：获取商品详情

**请求信息**：
- URL：`GET /api/products/{id}`

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 商品ID |

## 6. 消息统计接口

### 6.1 获取消息统计

**接口说明**：获取本地消息表统计信息

**请求信息**：
- URL：`GET /api/orders/messages/statistics`

**响应示例**：
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "pendingCount": 10,
        "sentCount": 100,
        "confirmedCount": 95,
        "failedCount": 5
    }
}
```

## 7. 接口限流说明

### 7.1 限流策略

| 接口 | 限流策略 | 限制 |
|------|----------|------|
| POST /api/orders | 滑动窗口 | 10次/秒/IP |
| POST /api/orders/{orderNo}/pay | 固定窗口 | 5次/秒/用户 |
| GET /api/orders/* | 不限流 | - |

### 7.2 限流响应

当触发限流时，返回以下响应：

```json
{
    "code": 429,
    "message": "请求过于频繁，请稍后再试",
    "data": null
}
```

## 8. 幂等说明

### 8.1 支持幂等的接口

| 接口 | 幂等策略 | 过期时间 |
|------|----------|----------|
| POST /api/orders | 幂等键 | 5秒 |
| POST /api/orders/{orderNo}/pay | 幂等键 | 5秒 |

### 8.2 幂等响应

当重复提交时，返回以下响应：

```json
{
    "code": 409,
    "message": "请勿重复提交",
    "data": null
}
```

---

**文档版本**：v1.0  
**最后更新**：2024年  
**维护人员**：开发团队
