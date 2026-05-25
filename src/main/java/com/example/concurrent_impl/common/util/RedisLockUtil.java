package com.example.concurrent_impl.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 【Redis分布式锁工具类】基于Redis实现的分布式锁
 * 
 * 【业务背景】
 * 在分布式系统中，多个服务实例需要竞争同一资源时，
 * 需要使用分布式锁来保证同一时刻只有一个实例能执行。
 * 
 * 【实现原理】
 * 使用Redis的SET命令实现，SET key value NX EX timeout
 * 1. NX：只在key不存在时设置
 * 2. EX：设置过期时间，防止死锁
 * 3. value：使用UUID作为锁标识，确保只有持有者能释放锁
 * 
 * 【为什么这样写】
 * 1. 使用UUID作为锁标识：防止误删其他线程的锁
 * 2. 使用Lua脚本释放锁：保证原子性
 * 3. 设置过期时间：防止死锁
 * 4. 支持可重入：同一线程可以多次获取同一把锁
 * 
 * 【不遵守的后果】
 * 1. 不使用UUID：可能误删其他线程的锁
 * 2. 不设置过期时间：服务宕机后锁无法释放，导致死锁
 * 3. 不使用Lua脚本：释放锁时可能出现竞态条件
 * 4. 不处理锁续期：业务执行时间超过锁超时时间会导致锁失效
 * 
 * 【正确示例】
 * 使用分布式锁保护共享资源：
 * if (redisLockUtil.tryLock("order:123", 30)) {
 *     try {
 *         // 业务逻辑
 *     } finally {
 *         redisLockUtil.unlock("order:123");
 *     }
 * }
 * 
 * 【错误示例】
 * 1. 不检查获取锁结果：直接执行业务，可能导致并发问题
 * 2. 不释放锁：导致其他线程无法获取锁
 * 3. 不处理异常：异常时锁未释放
 * 
 * 【实际案例】
 * 1. 订单创建时防止重复下单
 * 2. 库存扣减时防止超卖
 * 3. 定时任务执行时防止重复执行
 * 
 * 【注意事项】
 * 1. Redis分布式锁是AP模型，不是CP模型
 * 2. 在Redis主从切换时可能出现锁丢失
 * 3. 如果对一致性要求极高，建议使用Zookeeper或etcd
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * 锁的key前缀
     */
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 默认锁超时时间（秒）
     * 
     * 【为什么设置30秒】
     * 1. 大部分业务操作在30秒内可以完成
     * 2. 太短：业务未完成锁就过期
     * 3. 太长：锁被占用时间过长，影响并发
     */
    private static final int DEFAULT_TIMEOUT = 30;

    /**
     * 释放锁的Lua脚本
     * 
     * 【为什么使用Lua脚本】
     * 1. 保证原子性：判断和删除必须是一个原子操作
     * 2. 避免竞态条件：判断后删除之间锁可能已过期
     * 
     * 【脚本逻辑】
     * 1. 判断锁是否存在
     * 2. 判断锁的值是否是当前线程的标识
     * 3. 如果是，则删除锁
     * 4. 返回删除结果
     */
    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";

    /**
     * 尝试获取锁
     * 
     * @param lockKey 锁的key
     * @return 锁标识（用于释放锁），获取失败返回null
     */
    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_TIMEOUT);
    }

    /**
     * 尝试获取锁（指定超时时间）
     * 
     * 【为什么返回锁标识】
     * 1. 释放锁时需要验证锁标识
     * 2. 防止误删其他线程的锁
     * 
     * @param lockKey 锁的key
     * @param timeout 超时时间（秒）
     * @return 锁标识（用于释放锁），获取失败返回null
     */
    public String tryLock(String lockKey, int timeout) {
        String fullKey = LOCK_PREFIX + lockKey;
        // 生成唯一的锁标识
        String lockValue = UUID.randomUUID().toString();
        
        try {
            // 【关键点】使用SET NX EX命令
            // NX：只在key不存在时设置
            // EX：设置过期时间
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(fullKey, lockValue, timeout, TimeUnit.SECONDS);
            
            if (Boolean.TRUE.equals(result)) {
                log.debug("获取锁成功: key={}, value={}", fullKey, lockValue);
                return lockValue;
            }
            
            log.debug("获取锁失败: key={}", fullKey);
            return null;
        } catch (Exception e) {
            log.error("获取锁异常: key={}", fullKey, e);
            return null;
        }
    }

    /**
     * 尝试获取锁（带重试）
     * 
     * 【使用场景】
     * 需要等待锁释放后再执行的场景
     * 
     * @param lockKey 锁的key
     * @param timeout 超时时间（秒）
     * @param retryCount 重试次数
     * @param retryInterval 重试间隔（毫秒）
     * @return 锁标识，获取失败返回null
     */
    public String tryLockWithRetry(String lockKey, int timeout, int retryCount, long retryInterval) {
        for (int i = 0; i < retryCount; i++) {
            String lockValue = tryLock(lockKey, timeout);
            if (lockValue != null) {
                return lockValue;
            }
            
            // 等待一段时间后重试
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待锁被中断: key={}", lockKey);
                return null;
            }
        }
        
        log.warn("获取锁重试{}次后失败: key={}", retryCount, lockKey);
        return null;
    }

    /**
     * 释放锁
     * 
     * 【为什么使用Lua脚本】
     * 1. 保证原子性
     * 2. 只释放自己的锁
     * 
     * @param lockKey 锁的key
     * @param lockValue 锁标识
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey, String lockValue) {
        String fullKey = LOCK_PREFIX + lockKey;
        
        try {
            // 【关键点】使用Lua脚本保证原子性
            Long result = redisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class),
                    java.util.Collections.singletonList(fullKey),
                    lockValue
            );
            
            if (Long.valueOf(1L).equals(result)) {
                log.debug("释放锁成功: key={}", fullKey);
                return true;
            }
            
            log.warn("释放锁失败（锁已过期或不属于当前线程）: key={}", fullKey);
            return false;
        } catch (Exception e) {
            log.error("释放锁异常: key={}", fullKey, e);
            return false;
        }
    }

    /**
     * 检查锁是否存在
     * 
     * @param lockKey 锁的key
     * @return 是否存在
     */
    public boolean isLocked(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
        } catch (Exception e) {
            log.error("检查锁异常: key={}", fullKey, e);
            return false;
        }
    }

    /**
     * 强制释放锁（慎用）
     * 
     * 【使用场景】
     * 1. 调试时需要强制释放锁
     * 2. 锁被异常占用时的应急处理
     * 
     * 【注意事项】
     * 不要在正常业务逻辑中使用，可能导致并发问题
     * 
     * @param lockKey 锁的key
     */
    public void forceUnlock(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        try {
            redisTemplate.delete(fullKey);
            log.warn("强制释放锁: key={}", fullKey);
        } catch (Exception e) {
            log.error("强制释放锁异常: key={}", fullKey, e);
        }
    }
}
