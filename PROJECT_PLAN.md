# 企业级并发与安全代码实现示例项目计划

## 项目概述

本项目是一个企业级代码示例集合，展示实际项目中常见的高并发、业务安全、事务安全、数据一致性等场景的最佳实践代码实现。每个示例都包含详细的中文注释，解释：
- **为什么这样写**：设计原理和最佳实践
- **不遵守的后果**：可能出现的问题和风险
- **实际案例**：真实业务场景中的应用
- **对比示例**：正确 vs 错误的实现方式

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                      客户端层 (Client)                      │
│              Web / Mobile / API Consumer                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    API 网关层 (Gateway)                      │
│              限流 / 鉴权 / 日志 / 路由                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   应用服务层 (Application)                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ 订单服务    │  │ 库存服务    │  │ 支付服务    │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ 用户服务    │  │ 通知服务    │  │ 账户服务    │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    基础设施层 (Infrastructure)               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ MySQL       │  │ Redis       │  │ RabbitMQ    │         │
│  │ (持久化)    │  │ (缓存)      │  │ (消息队列)  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

## 场景分类与模块设计

### 1. 高并发场景 (High Concurrency)

#### 1.1 线程安全 (Thread Safety)
- **场景**: 多线程环境下的共享资源访问
- **示例**:
  - 线程安全的计数器实现
  - 线程安全的缓存实现
  - ThreadLocal 使用场景
- **关键注释点**:
  - 为什么使用 `AtomicLong` 而不是 `volatile`
  - `synchronized` vs `ReentrantLock` 的选择
  - ThreadLocal 内存泄漏风险

#### 1.2 锁机制 (Lock Mechanism)
- **场景**: 并发控制与资源竞争
- **示例**:
  - 乐观锁 (CAS 实现)
  - 悲观锁 (数据库行锁)
  - 分布式锁 (Redis/Zookeeper)
- **关键注释点**:
  - 乐观锁适用场景：读多写少
  - 悲观锁适用场景：写多冲突多
  - 分布式锁的超时与续期问题

#### 1.3 限流 (Rate Limiting)
- **场景**: 保护系统防止过载
- **示例**:
  - 令牌桶算法 (Token Bucket)
  - 漏桶算法 (Leaky Bucket)
  - 滑动窗口限流
  - 分布式限流
- **关键注释点**:
  - 不同算法的适用场景
  - 限流参数的合理设置
  - 限流后的降级策略

#### 1.4 缓存策略 (Cache Strategy)
- **场景**: 提升系统性能
- **示例**:
  - 缓存穿透防护
  - 缓存击穿防护
  - 缓存雪崩防护
  - 缓存与数据库一致性
- **关键注释点**:
  - 布隆过滤器的使用
  - 互斥锁防止缓存击穿
  - 随机过期时间防止雪崩

### 2. 业务安全场景 (Business Security)

#### 2.1 防重复提交 (Idempotency)
- **场景**: 防止用户重复操作
- **示例**:
  - 基于 Token 的防重复提交
  - 基于幂等键的防重复
  - 基于状态机的防重复
- **关键注释点**:
  - 幂等性设计的重要性
  - 分布式环境下的幂等实现
  - 超时与重试的处理

#### 2.2 参数校验 (Validation)
- **场景**: 输入数据安全
- **示例**:
  - JSR-303 注解校验
  - 自定义校验器
  - 分组校验
  - 嵌套对象校验
- **关键注释点**:
  - 为什么不能只依赖前端校验
  - 校验失败的安全处理
  - SQL 注入防护

#### 2.3 权限控制 (Authorization)
- **场景**: 资源访问控制
- **示例**:
  - RBAC 模型实现
  - 数据权限控制
  - 接口权限控制
- **关键注释点**:
  - 最小权限原则
  - 权限缓存策略
  - 权限变更的实时性

### 3. 事务安全场景 (Transaction Safety)

#### 3.1 本地事务 (Local Transaction)
- **场景**: 单数据库事务
- **示例**:
  - 声明式事务 (`@Transactional`)
  - 编程式事务
  - 事务传播行为
  - 事务隔离级别
- **关键注释点**:
  - `@Transactional` 失效的常见原因
  - 传播行为的选择策略
  - 隔离级别的性能影响

#### 3.2 分布式事务 (Distributed Transaction)
- **场景**: 跨服务事务
- **示例**:
  - Saga 模式实现
  - TCC (Try-Confirm-Cancel) 模式
  - 本地消息表
  - 事务消息 (RocketMQ)
- **关键注释点**:
  - CAP 理论与选择
  - 最终一致性的实现
  - 补偿事务的设计

#### 3.3 事务陷阱 (Transaction Pitfalls)
- **场景**: 常见事务问题
- **示例**:
  - 事务失效场景演示
  - 长事务优化
  - 死锁处理
- **关键注释点**:
  - 非 public 方法事务失效
  - 自调用事务失效
  - 异常类型与回滚

### 4. 数据一致性场景 (Data Consistency)

#### 4.1 最终一致性 (Eventual Consistency)
- **场景**: 分布式系统数据同步
- **示例**:
  - 消息队列异步同步
  - 定时任务补偿
  - Binlog 监听同步
- **关键注释点**:
  - 消息丢失的处理
  - 消息重复的处理
  - 消息顺序的保证

#### 4.2 强一致性 (Strong Consistency)
- **场景**: 资金等关键业务
- **示例**:
  - 数据库乐观锁
  - 分布式锁
  - 两阶段提交
- **关键注释点**:
  - 性能与一致性的权衡
  - 强一致性的实现成本
  - 适用场景的选择

#### 4.3 数据版本控制 (Data Versioning)
- **场景**: 并发更新控制
- **示例**:
  - 乐观锁版本号
  - CAS 操作
  - 时钟向量
- **关键注释点**:
  - 版本冲突的处理策略
  - 重试机制的设计
  - 用户体验的考虑

## 目录结构设计

```
concurrent_impl/
├── PROJECT_PLAN.md                           # 项目计划文档
├── README.md                                  # 项目说明文档
├── pom.xml                                    # Maven 配置
├── src/
│   ├── main/
│   │   ├── java/com/example/concurrent_impl/
│   │   │   ├── ConcurrentImplApplication.java
│   │   │   │
│   │   │   ├── common/                        # 公共模块
│   │   │   │   ├── config/                    # 配置类
│   │   │   │   │   ├── RedisConfig.java       # Redis 配置
│   │   │   │   │   ├── RabbitMQConfig.java    # RabbitMQ 配置
│   │   │   │   │   └── AsyncConfig.java       # 异步线程池配置
│   │   │   │   │
│   │   │   │   ├── constant/                  # 常量定义
│   │   │   │   │   └── BusinessConstant.java
│   │   │   │   │
│   │   │   │   ├── enums/                     # 枚举定义
│   │   │   │   │   ├── ErrorCode.java
│   │   │   │   │   └── OrderStatus.java
│   │   │   │   │
│   │   │   │   ├── exception/                 # 异常定义
│   │   │   │   │   ├── BusinessException.java
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   │
│   │   │   │   ├── response/                  # 响应封装
│   │   │   │   │   └── ApiResult.java
│   │   │   │   │
│   │   │   │   └── util/                      # 工具类
│   │   │   │       ├── RedisLockUtil.java
│   │   │   │       ├── IdempotentUtil.java
│   │   │   │       └── SnowflakeIdUtil.java
│   │   │   │
│   │   │   ├── highconcurrency/               # 高并发场景
│   │   │   │   ├── threadsafety/              # 线程安全
│   │   │   │   │   ├── CounterDemo.java
│   │   │   │   │   ├── CacheDemo.java
│   │   │   │   │   └── ThreadLocalDemo.java
│   │   │   │   │
│   │   │   │   ├── lock/                      # 锁机制
│   │   │   │   │   ├── OptimisticLockDemo.java
│   │   │   │   │   ├── PessimisticLockDemo.java
│   │   │   │   │   └── DistributedLockDemo.java
│   │   │   │   │
│   │   │   │   ├── ratelimit/                 # 限流
│   │   │   │   │   ├── TokenBucketDemo.java
│   │   │   │   │   ├── LeakyBucketDemo.java
│   │   │   │   │   └── SlidingWindowDemo.java
│   │   │   │   │
│   │   │   │   └── cache/                     # 缓存策略
│   │   │   │       ├── CachePenetrationDemo.java
│   │   │   │       ├── CacheBreakdownDemo.java
│   │   │   │       └── CacheAvalancheDemo.java
│   │   │   │
│   │   │   ├── businesssecurity/              # 业务安全场景
│   │   │   │   ├── idempotent/                # 防重复提交
│   │   │   │   │   ├── TokenIdempotentDemo.java
│   │   │   │   │   ├── KeyIdempotentDemo.java
│   │   │   │   │   └── annotation/
│   │   │   │   │       └── Idempotent.java
│   │   │   │   │
│   │   │   │   ├── validation/                # 参数校验
│   │   │   │   │   ├── ValidationDemo.java
│   │   │   │   │   └── validator/
│   │   │   │   │       └── MobileValidator.java
│   │   │   │   │
│   │   │   │   └── permission/                # 权限控制
│   │   │   │       ├── RBACDemo.java
│   │   │   │       └── DataPermissionDemo.java
│   │   │   │
│   │   │   ├── transactionsafety/             # 事务安全场景
│   │   │   │   ├── local/                     # 本地事务
│   │   │   │   │   ├── TransactionalDemo.java
│   │   │   │   │   ├── PropagationDemo.java
│   │   │   │   │   └── IsolationDemo.java
│   │   │   │   │
│   │   │   │   ├── distributed/               # 分布式事务
│   │   │   │   │   ├── SagaDemo.java
│   │   │   │   │   ├── TCCDemo.java
│   │   │   │   │   └── LocalMessageDemo.java
│   │   │   │   │
│   │   │   │   └── pitfall/                   # 事务陷阱
│   │   │   │       ├── TransactionFailDemo.java
│   │   │   │       └── DeadLockDemo.java
│   │   │   │
│   │   │   ├── dataconsistency/               # 数据一致性场景
│   │   │   │   ├── eventual/                  # 最终一致性
│   │   │   │   │   ├── MessageQueueDemo.java
│   │   │   │   │   ├── ScheduledTaskDemo.java
│   │   │   │   │   └── BinlogSyncDemo.java
│   │   │   │   │
│   │   │   │   ├── strong/                    # 强一致性
│   │   │   │   │   ├── OptimisticLockDemo.java
│   │   │   │   │   └── DistributedLockDemo.java
│   │   │   │   │
│   │   │   │   └── versioning/                # 数据版本控制
│   │   │   │       └── VersionControlDemo.java
│   │   │   │
│   │   │   └── controller/                    # API 接口层
│   │   │       ├── OrderController.java
│   │   │       ├── InventoryController.java
│   │   │       └── PaymentController.java
│   │   │
│   │   ├── resources/
│   │   │   ├── application.yml                # 主配置文件
│   │   │   ├── application-dev.yml            # 开发环境配置
│   │   │   ├── db/
│   │   │   │   └── schema.sql                 # 数据库初始化脚本
│   │   │   └── logback-spring.xml             # 日志配置
│   │   │
│   │   └── test/                              # 测试代码
│   │       └── java/com/example/concurrent_impl/
│   │           └── ...
│   │
│   └── docs/                                  # 文档目录
│       ├── 01-highconcurrency.md              # 高并发场景文档
│       ├── 02-businesssecurity.md             # 业务安全场景文档
│       ├── 03-transactionsafety.md            # 事务安全场景文档
│       ├── 04-dataconsistency.md              # 数据一致性场景文档
│       └── best-practices.md                  # 最佳实践总结
```

## 业务场景设计

### 示例业务：电商订单系统

为了展示各种场景，我们设计一个简化的电商订单系统：

```
用户下单流程:
┌──────┐    ┌──────┐    ┌──────┐    ┌──────┐    ┌──────┐
│ 浏览 │ -> │ 加购 │ -> │ 下单 │ -> │ 支付 │ -> │ 完成 │
└──────┘    └──────┘    └──────┘    └──────┘    └──────┘
                │           │           │
                ▼           ▼           ▼
           ┌────────┐  ┌────────┐  ┌────────┐
           │ 库存   │  │ 订单   │  │ 支付   │
           │ 服务   │  │ 服务   │  │ 服务   │
           └────────┘  └────────┘  └────────┘
                │           │           │
                ▼           ▼           ▼
           ┌────────────────────────────────┐
           │         数据库 / 缓存          │
           └────────────────────────────────┘
```

### 数据库表设计

```sql
-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    balance DECIMAL(10,2) DEFAULT 0,
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 商品表
CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status TINYINT DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已发货 3-已完成 4-已取消',
    idempotent_key VARCHAR(64) COMMENT '幂等键',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 订单明细表
CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(100),
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 账户流水表
CREATE TABLE account_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    type TINYINT COMMENT '1-扣款 2-退款',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 消息表（本地消息表模式）
CREATE TABLE message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL UNIQUE,
    content TEXT,
    status TINYINT DEFAULT 0 COMMENT '0-待发送 1-已发送 2-已确认',
    retry_count INT DEFAULT 0,
    next_retry_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 实现阶段计划

### Phase 1: 基础框架搭建 (Day 1-2)

#### 任务清单：
- [ ] 创建项目目录结构
- [ ] 配置 application.yml (MySQL, Redis, RabbitMQ)
- [ ] 创建数据库初始化脚本
- [ ] 实现公共模块：
  - [ ] 统一响应封装 `ApiResult`
  - [ ] 全局异常处理 `GlobalExceptionHandler`
  - [ ] 业务异常定义 `BusinessException`
  - [ ] 错误码枚举 `ErrorCode`
- [ ] 配置 Redis 连接池
- [ ] 配置 RabbitMQ 连接
- [ ] 配置异步线程池

### Phase 2: 高并发场景实现 (Day 3-5)

#### 2.1 线程安全模块
- [ ] `CounterDemo.java` - 展示不同计数器实现的线程安全性
- [ ] `CacheDemo.java` - 线程安全的本地缓存实现
- [ ] `ThreadLocalDemo.java` - ThreadLocal 使用场景与内存泄漏

#### 2.2 锁机制模块
- [ ] `OptimisticLockDemo.java` - 数据库乐观锁实现
- [ ] `PessimisticLockDemo.java` - 数据库悲观锁实现
- [ ] `DistributedLockDemo.java` - Redis 分布式锁实现

#### 2.3 限流模块
- [ ] `TokenBucketDemo.java` - 令牌桶限流算法
- [ ] `LeakyBucketDemo.java` - 漏桶限流算法
- [ ] `SlidingWindowDemo.java` - 滑动窗口限流
- [ ] `RateLimitInterceptor.java` - 限流注解与拦截器

#### 2.4 缓存策略模块
- [ ] `CachePenetrationDemo.java` - 缓存穿透防护
- [ ] `CacheBreakdownDemo.java` - 缓存击穿防护
- [ ] `CacheAvalancheDemo.java` - 缓存雪崩防护

### Phase 3: 业务安全场景实现 (Day 6-7)

#### 3.1 防重复提交模块
- [ ] `Idempotent.java` - 幂等注解定义
- [ ] `IdempotentAspect.java` - 幂等切面实现
- [ ] `TokenIdempotentDemo.java` - Token 方式防重复
- [ ] `KeyIdempotentDemo.java` - 幂等键方式防重复

#### 3.2 参数校验模块
- [ ] `ValidationDemo.java` - JSR-303 校验示例
- [ ] `MobileValidator.java` - 自定义手机号校验器
- [ ] `ValidationGroup.java` - 分组校验示例

#### 3.3 权限控制模块
- [ ] `RBACDemo.java` - RBAC 权限模型实现
- [ ] `DataPermissionDemo.java` - 数据权限控制

### Phase 4: 事务安全场景实现 (Day 8-10)

#### 4.1 本地事务模块
- [ ] `TransactionalDemo.java` - @Transactional 使用示例
- [ ] `PropagationDemo.java` - 事务传播行为演示
- [ ] `IsolationDemo.java` - 事务隔离级别演示

#### 4.2 分布式事务模块
- [ ] `SagaDemo.java` - Saga 模式实现
- [ ] `TCCDemo.java` - TCC 模式实现
- [ ] `LocalMessageDemo.java` - 本地消息表实现

#### 4.3 事务陷阱模块
- [ ] `TransactionFailDemo.java` - 事务失效场景汇总
- [ ] `DeadLockDemo.java` - 死锁场景与解决

### Phase 5: 数据一致性场景实现 (Day 11-12)

#### 5.1 最终一致性模块
- [ ] `MessageQueueDemo.java` - 消息队列异步同步
- [ ] `ScheduledTaskDemo.java` - 定时任务补偿
- [ ] `BinlogSyncDemo.java` - Binlog 监听同步

#### 5.2 强一致性模块
- [ ] `StrongConsistencyDemo.java` - 强一致性实现

#### 5.3 数据版本控制模块
- [ ] `VersionControlDemo.java` - 乐观锁版本控制

### Phase 6: API 接口层与测试 (Day 13-14)

#### 6.1 API 接口
- [ ] `OrderController.java` - 订单相关接口
- [ ] `InventoryController.java` - 库存相关接口
- [ ] `PaymentController.java` - 支付相关接口

#### 6.2 测试用例
- [ ] 并发测试用例
- [ ] 事务测试用例
- [ ] 幂等测试用例

### Phase 7: 文档完善 (Day 15)

- [ ] README.md - 项目说明
- [ ] 场景文档编写
- [ ] 最佳实践总结
- [ ] 代码注释完善

## 代码注释规范

每个示例类和方法都应包含以下注释：

```java
/**
 * 【场景名称】具体场景描述
 * 
 * 【业务背景】
 * 描述该场景在实际业务中的应用
 * 
 * 【实现原理】
 * 解释实现的技术原理
 * 
 * 【为什么这样写】
 * 1. 原因1
 * 2. 原因2
 * 
 * 【不遵守的后果】
 * 1. 后果1 - 具体描述
 * 2. 后果2 - 具体描述
 * 
 * 【正确示例】
 * 展示正确的实现方式
 * 
 * 【错误示例】
 * 展示错误的实现方式及其问题
 * 
 * 【实际案例】
 * 真实项目中的应用案例
 * 
 * @author xxx
 * @date xxx
 */
```

## 示例代码风格

```java
/**
 * 【高并发 - 乐观锁】商品库存扣减示例
 * 
 * 【业务背景】
 * 在电商秒杀场景中，大量用户同时抢购同一商品，需要保证库存扣减的原子性。
 * 
 * 【实现原理】
 * 使用数据库的乐观锁机制，通过版本号控制并发更新。
 * 
 * 【为什么这样写】
 * 1. 乐观锁适用于读多写少的场景，秒杀场景读操作远多于写操作
 * 2. 使用版本号比使用 CAS 更直观，便于问题排查
 * 3. 失败重试机制保证最终成功
 * 
 * 【不遵守的后果】
 * 1. 不使用乐观锁：库存超卖，实际发货数量超过库存
 * 2. 不重试：用户体验差，明明有库存却提示失败
 * 3. 不限制重试次数：可能导致死循环
 * 
 * 【正确示例】
 * 使用版本号 + 重试机制
 * 
 * 【错误示例】
 * 直接 UPDATE stock = stock - 1（存在超卖风险）
 * 
 * @param productId 商品ID
 * @param quantity 购买数量
 * @return 是否扣减成功
 */
public boolean deductStock(Long productId, Integer quantity) {
    // 【关键点1】获取当前版本号
    Product product = productMapper.selectById(productId);
    if (product.getStock() < quantity) {
        throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
    }
    
    // 【关键点2】使用版本号进行乐观锁更新
    int rows = productMapper.updateStock(productId, quantity, product.getVersion());
    
    // 【关键点3】更新失败说明有并发冲突，可以选择重试
    if (rows == 0) {
        log.warn("库存扣减并发冲突，productId={}, version={}", productId, product.getVersion());
        return false;
    }
    
    return true;
}
```

## 依赖说明

当前 pom.xml 已包含的依赖：

| 依赖 | 用途 |
|------|------|
| spring-boot-starter-web | Web 应用支持 |
| spring-boot-starter-data-redis | Redis 缓存支持 |
| spring-boot-starter-amqp | RabbitMQ 消息队列支持 |
| mysql-connector-j | MySQL 数据库驱动 |
| lombok | 简化代码 |
| springdoc-openapi | API 文档 |

## 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+
- Maven 3.6+

## 预期成果

完成本项目后，学习者将掌握：

1. **高并发处理能力**
   - 理解线程安全的核心概念
   - 掌握各种锁机制的使用场景
   - 学会限流与熔断策略
   - 掌握缓存设计的最佳实践

2. **业务安全意识**
   - 理解幂等性设计的重要性
   - 掌握参数校验的完整方案
   - 学会权限控制的实现方式

3. **事务安全能力**
   - 理解本地事务的各种陷阱
   - 掌握分布式事务的实现方案
   - 学会事务失败的处理策略

4. **数据一致性保障**
   - 理解强一致性与最终一致性的权衡
   - 掌握数据同步的各种方案
   - 学会版本控制与冲突处理

---

> 📝 **说明**: 本计划文档将随项目开发持续更新完善。
