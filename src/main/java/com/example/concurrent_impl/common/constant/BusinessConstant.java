package com.example.concurrent_impl.common.constant;

/**
 * 【业务常量】定义系统中使用的常量
 * 
 * 【业务背景】
 * 在企业级项目中，需要集中管理常量，避免魔法数字和字符串分散在代码各处。
 * 
 * 【为什么这样写】
 * 1. 集中管理常量，便于维护和修改
 * 2. 使用有意义的名称，提高代码可读性
 * 3. 避免魔法数字和字符串，减少拼写错误
 * 4. 支持类型检查，编译时发现错误
 * 
 * 【不遵守的后果】
 * 1. 魔法数字散布：代码可读性差，难以理解业务含义
 * 2. 字符串硬编码：容易拼写错误，且难以修改
 * 3. 不统一常量定义：同一含义的值在不同地方定义不同
 * 
 * 【正确示例】
 * 使用常量：if (status == BusinessConstant.ORDER_STATUS_PENDING)
 * 
 * 【错误示例】
 * 使用魔法数字：if (status == 0)
 * 使用魔法字符串：if (type.equals("order"))
 */
public final class BusinessConstant {

    private BusinessConstant() {
        // 私有构造方法，防止实例化
    }

    // ==================== 订单相关常量 ====================
    
    /**
     * 订单号前缀
     * 【为什么需要前缀】
     * 1. 便于识别业务类型
     * 2. 避免不同业务的订单号冲突
     */
    public static final String ORDER_NO_PREFIX = "ORD";

    /**
     * 订单超时时间（分钟）
     * 【为什么设置超时时间】
     * 1. 避免占用库存资源
     * 2. 提高库存周转率
     */
    public static final int ORDER_TIMEOUT_MINUTES = 30;

    /**
     * 订单最大重试次数
     * 【为什么限制重试次数】
     * 1. 避免无限重试导致系统资源耗尽
     * 2. 保证系统的稳定性
     */
    public static final int ORDER_MAX_RETRY_COUNT = 3;

    // ==================== 库存相关常量 ====================
    
    /**
     * 库存锁定时间（秒）
     * 【为什么需要锁定时间】
     * 1. 防止库存被长时间占用
     * 2. 超时后自动释放库存
     */
    public static final int STOCK_LOCK_TIMEOUT_SECONDS = 900; // 15分钟

    /**
     * 库存缓存key前缀
     */
    public static final String STOCK_CACHE_PREFIX = "stock:";

    /**
     * 库存锁key前缀
     */
    public static final String STOCK_LOCK_PREFIX = "stock_lock:";

    // ==================== 缓存相关常量 ====================
    
    /**
     * 默认缓存过期时间（秒）
     * 【为什么设置过期时间】
     * 1. 避免缓存数据与数据库数据不一致
     * 2. 释放内存空间
     */
    public static final int DEFAULT_CACHE_EXPIRE_SECONDS = 3600; // 1小时

    /**
     * 缓存空值过期时间（秒）
     * 【为什么缓存空值】
     * 防止缓存穿透，避免恶意请求直接打到数据库
     */
    public static final int CACHE_NULL_EXPIRE_SECONDS = 60; // 1分钟

    /**
     * 缓存随机过期时间范围（秒）
     * 【为什么添加随机值】
     * 防止缓存雪崩，避免大量key同时失效
     */
    public static final int CACHE_RANDOM_EXPIRE_SECONDS = 300; // 5分钟

    // ==================== 限流相关常量 ====================
    
    /**
     * 默认限流QPS
     * 【为什么设置默认QPS】
     * 保护系统不被过载
     */
    public static final int DEFAULT_RATE_LIMIT_QPS = 100;

    /**
     * 限流key前缀
     */
    public static final String RATE_LIMIT_PREFIX = "rate_limit:";

    // ==================== 幂等相关常量 ====================
    
    /**
     * 幂等键过期时间（秒）
     * 【为什么设置过期时间】
     * 1. 避免幂等键占用过多存储空间
     * 2. 超时后允许重新提交
     */
    public static final int IDEMPOTENT_KEY_EXPIRE_SECONDS = 86400; // 24小时

    /**
     * 幂等键前缀
     */
    public static final String IDEMPOTENT_PREFIX = "idempotent:";

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "token:";

    // ==================== 分布式锁相关常量 ====================
    
    /**
     * 分布式锁默认超时时间（秒）
     * 【为什么设置超时时间】
     * 1. 防止死锁
     * 2. 超时后自动释放锁
     */
    public static final int DISTRIBUTED_LOCK_TIMEOUT_SECONDS = 30;

    /**
     * 分布式锁等待时间（毫秒）
     */
    public static final long DISTRIBUTED_LOCK_WAIT_MILLIS = 100;

    /**
     * 分布式锁重试次数
     */
    public static final int DISTRIBUTED_LOCK_RETRY_COUNT = 3;

    // ==================== 消息队列相关常量 ====================
    
    /**
     * 消息最大重试次数
     * 【为什么限制重试次数】
     * 避免消息无限重试导致队列阻塞
     */
    public static final int MQ_MAX_RETRY_COUNT = 3;

    /**
     * 消息重试间隔（秒）
     */
    public static final int MQ_RETRY_INTERVAL_SECONDS = 60;

    /**
     * 订单队列名称
     */
    public static final String ORDER_QUEUE_NAME = "order.queue";

    /**
     * 订单交换机名称
     */
    public static final String ORDER_EXCHANGE_NAME = "order.exchange";

    /**
     * 订单路由键
     */
    public static final String ORDER_ROUTING_KEY = "order.create";

    // ==================== 用户相关常量 ====================
    
    /**
     * 用户缓存key前缀
     */
    public static final String USER_CACHE_PREFIX = "user:";

    /**
     * 用户会话key前缀
     */
    public static final String USER_SESSION_PREFIX = "session:";

    /**
     * 密码错误最大次数
     * 【为什么限制错误次数】
     * 防止暴力破解
     */
    public static final int PASSWORD_MAX_ERROR_COUNT = 5;

    /**
     * 账户锁定时间（分钟）
     */
    public static final int ACCOUNT_LOCK_MINUTES = 30;

    // ==================== 分页相关常量 ====================
    
    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页大小
     * 【为什么限制最大每页大小】
     * 防止一次查询过多数据导致内存溢出或数据库慢查询
     */
    public static final int MAX_PAGE_SIZE = 100;
}
