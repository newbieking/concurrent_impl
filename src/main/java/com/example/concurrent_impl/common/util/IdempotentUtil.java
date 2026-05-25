package com.example.concurrent_impl.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 【幂等工具类】防止重复提交的工具类
 * 
 * 【业务背景】
 * 在企业级项目中，由于网络抖动、用户重复点击等原因，
 * 同一个请求可能会被发送多次，需要保证接口的幂等性。
 * 
 * 【什么是幂等性】
 * 幂等性是指同一个操作执行一次和执行多次的效果相同。
 * 例如：支付接口，无论调用多少次，只会扣款一次。
 * 
 * 【为什么需要幂等性】
 * 1. 网络重试：网络超时后客户端可能会重试
 * 2. 用户重复点击：用户可能快速多次点击按钮
 * 3. 消息重复投递：消息队列可能重复投递消息
 * 4. 定时任务重复执行：定时任务可能被多次触发
 * 
 * 【实现原理】
 * 1. 客户端生成唯一的幂等键
 * 2. 服务端检查幂等键是否已存在
 * 3. 如果不存在，执行业务逻辑并记录幂等键
 * 4. 如果存在，说明是重复请求，直接返回
 * 
 * 【为什么这样写】
 * 1. 使用Redis存储幂等键：分布式环境下共享
 * 2. 设置过期时间：避免幂等键无限增长
 * 3. 使用SET NX命令：保证原子性
 * 4. 支持自定义幂等键：灵活性更强
 * 
 * 【不遵守的后果】
 * 1. 不做幂等处理：重复请求导致重复扣款、重复下单等问题
 * 2. 不设置过期时间：幂等键无限增长，占用存储空间
 * 3. 不使用原子操作：可能出现并发问题
 * 
 * 【正确示例】
 * 提交订单时检查幂等键：
 * if (idempotentUtil.checkAndSet("order:123")) {
 *     // 执行订单创建逻辑
 * } else {
 *     return "请勿重复提交";
 * }
 * 
 * 【错误示例】
 * 1. 直接执行业务：没有幂等保护
 * 2. 使用数据库唯一索引：性能差，且不支持分布式
 * 
 * 【实际案例】
 * 1. 订单创建接口：防止重复下单
 * 2. 支付接口：防止重复扣款
 * 3. 退款接口：防止重复退款
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * 幂等键前缀
     */
    private static final String IDEMPOTENT_PREFIX = "idempotent:";

    /**
     * 默认过期时间（秒）
     * 
     * 【为什么设置24小时】
     * 1. 足够覆盖大部分业务场景
     * 2. 避免幂等键无限增长
     * 3. 可以根据业务调整
     */
    private static final int DEFAULT_EXPIRE_SECONDS = 86400;

    /**
     * 检查并设置幂等键
     * 
     * 【使用场景】
     * 接口防重复提交
     * 
     * @param idempotentKey 幂等键
     * @return true-首次请求，false-重复请求
     */
    public boolean checkAndSet(String idempotentKey) {
        return checkAndSet(idempotentKey, DEFAULT_EXPIRE_SECONDS);
    }

    /**
     * 检查并设置幂等键（自定义过期时间）
     * 
     * @param idempotentKey 幂等键
     * @param expireSeconds 过期时间（秒）
     * @return true-首次请求，false-重复请求
     */
    public boolean checkAndSet(String idempotentKey, int expireSeconds) {
        String fullKey = IDEMPOTENT_PREFIX + idempotentKey;
        
        try {
            // 【关键点】使用SET NX命令
            // NX：只在key不存在时设置
            // 如果设置成功，说明是首次请求
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(fullKey, "1", expireSeconds, TimeUnit.SECONDS);
            
            if (Boolean.TRUE.equals(result)) {
                log.debug("幂等键设置成功（首次请求）: key={}", fullKey);
                return true;
            }
            
            log.debug("幂等键已存在（重复请求）: key={}", fullKey);
            return false;
        } catch (Exception e) {
            log.error("幂等键设置异常: key={}", fullKey, e);
            // 【容错处理】Redis异常时默认放行，避免影响正常业务
            return true;
        }
    }

    /**
     * 检查幂等键是否存在
     * 
     * 【使用场景】
     * 查询操作，不设置幂等键
     * 
     * @param idempotentKey 幂等键
     * @return 是否存在
     */
    public boolean exists(String idempotentKey) {
        String fullKey = IDEMPOTENT_PREFIX + idempotentKey;
        
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
        } catch (Exception e) {
            log.error("检查幂等键异常: key={}", fullKey, e);
            return false;
        }
    }

    /**
     * 删除幂等键
     * 
     * 【使用场景】
     * 业务执行失败后，允许重新提交
     * 
     * @param idempotentKey 幂等键
     */
    public void delete(String idempotentKey) {
        String fullKey = IDEMPOTENT_PREFIX + idempotentKey;
        
        try {
            redisTemplate.delete(fullKey);
            log.debug("删除幂等键: key={}", fullKey);
        } catch (Exception e) {
            log.error("删除幂等键异常: key={}", fullKey, e);
        }
    }

    /**
     * 生成幂等键
     * 
     * 【幂等键生成策略】
     * 1. 用户ID + 接口名 + 业务ID
     * 2. 请求参数的MD5
     * 3. 前端生成的唯一标识
     * 
     * @param userId 用户ID
     * @param interfaceName 接口名
     * @param bizId 业务ID
     * @return 幂等键
     */
    public String generateKey(Long userId, String interfaceName, String bizId) {
        return String.format("%d:%s:%s", userId, interfaceName, bizId);
    }

    /**
     * 生成幂等键（使用UUID）
     * 
     * 【使用场景】
     * 前端生成唯一标识，后端校验
     * 
     * @return UUID
     */
    public String generateKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * 带返回值的幂等操作
     * 
     * 【使用场景】
     * 需要返回业务数据的幂等操作
     * 
     * @param idempotentKey 幂等键
     * @param expireSeconds 过期时间
     * @param callback 业务回调
     * @param <T> 返回值类型
     * @return 业务返回值，重复请求返回null
     */
    public <T> T executeWithIdempotent(String idempotentKey, int expireSeconds, 
                                       java.util.function.Supplier<T> callback) {
        if (checkAndSet(idempotentKey, expireSeconds)) {
            try {
                return callback.get();
            } catch (Exception e) {
                // 【重要】业务执行失败，删除幂等键，允许重新提交
                delete(idempotentKey);
                throw e;
            }
        }
        return null;
    }
}
