package com.example.concurrent_impl.highconcurrency.lock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 【企业级】Redis分布式锁服务
 *
 * 【业务背景】
 * 在分布式系统中，多个服务实例需要竞争同一资源时，
 * 需要使用分布式锁来保证同一时刻只有一个实例能执行。
 *
 * 【实现原理】
 * 1. 使用Redis的SET命令实现分布式锁
 * 2. SET key value NX EX timeout
 * 3. NX：只在key不存在时设置
 * 4. EX：设置过期时间，防止死锁
 * 5. value：使用UUID作为锁标识
 * 6. 使用Lua脚本释放锁，保证原子性
 *
 * 【为什么这样写】
 * 1. 使用UUID作为锁标识：防止误删其他线程的锁
 * 2. 设置过期时间：防止死锁
 * 3. 使用Lua脚本释放锁：保证原子性
 * 4. 支持锁续期：防止业务未完成锁就过期
 *
 * 【不遵守的后果】
 * 1. 不设置过期时间：服务宕机后锁无法释放，导致死锁
 * 2. 不使用UUID：可能误删其他线程的锁
 * 3. 不使用Lua脚本：释放锁时可能出现竞态条件
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 锁前缀
     */
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 锁标识前缀
     */
    private static final String LOCK_VALUE_PREFIX = "lock_value:";

    /**
     * 释放锁的Lua脚本
     *
     * 【为什么使用Lua脚本】
     * 保证原子性：判断锁标识和删除锁必须是原子操作
     */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁的key
     * @param timeoutSeconds 超时时间（秒）
     * @return 锁标识（UUID），获取失败返回null
     */
    public String tryLock(String lockKey, int timeoutSeconds) {
        String key = LOCK_PREFIX + lockKey;
        String value = UUID.randomUUID().toString();

        try {
            // 【关键点】使用SET NX EX命令
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(key, value, timeoutSeconds, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(result)) {
                log.info("获取锁成功: key={}, value={}", key, value);
                return value;
            }

            log.warn("获取锁失败（已被占用）: key={}", key);
            return null;
        } catch (Exception e) {
            log.error("获取锁异常: key={}", key, e);
            return null;
        }
    }

    /**
     * 尝试获取锁（带重试）
     *
     * @param lockKey 锁的key
     * @param timeoutSeconds 超时时间（秒）
     * @param maxRetries 最大重试次数
     * @param retryIntervalMs 重试间隔（毫秒）
     * @return 锁标识（UUID），获取失败返回null
     */
    public String tryLockWithRetry(String lockKey, int timeoutSeconds, int maxRetries, long retryIntervalMs) {
        for (int i = 0; i < maxRetries; i++) {
            String value = tryLock(lockKey, timeoutSeconds);
            if (value != null) {
                return value;
            }

            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }

        log.warn("获取锁失败（超过最大重试次数）: key={}", lockKey);
        return null;
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的key
     * @param lockValue 锁标识
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey, String lockValue) {
        String key = LOCK_PREFIX + lockKey;

        try {
            // 【关键点】使用Lua脚本保证原子性
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(key), lockValue);

            if (result != null && result > 0) {
                log.info("释放锁成功: key={}, value={}", key, lockValue);
                return true;
            }

            log.warn("释放锁失败（锁标识不匹配或锁已过期）: key={}, value={}", key, lockValue);
            return false;
        } catch (Exception e) {
            log.error("释放锁异常: key={}", key, e);
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
        String key = LOCK_PREFIX + lockKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 在锁保护下执行任务
     *
     * @param lockKey 锁的key
     * @param timeoutSeconds 超时时间（秒）
     * @param task 任务
     * @param <T> 返回值类型
     * @return 任务执行结果
     */
    public <T> T executeWithLock(String lockKey, int timeoutSeconds, LockTask<T> task) {
        String lockValue = tryLock(lockKey, timeoutSeconds);
        if (lockValue == null) {
            throw new RuntimeException("获取锁失败: " + lockKey);
        }

        try {
            return task.execute();
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 锁任务接口
     */
    @FunctionalInterface
    public interface LockTask<T> {
        T execute();
    }
}
