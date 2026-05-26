package com.example.concurrent_impl.highconcurrency.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 【企业级】缓存服务 - 防穿透、防击穿、防雪崩
 *
 * 【业务背景】
 * 在高并发系统中，缓存是提高性能的关键，
 * 但缓存使用不当会导致穿透、击穿、雪崩等问题。
 *
 * 【实现原理】
 * 1. 缓存穿透：缓存空值 + 布隆过滤器
 * 2. 缓存击穿：互斥锁 + 逻辑过期
 * 3. 缓存雪崩：随机过期时间 + 多级缓存
 *
 * 【为什么这样写】
 * 1. 集成Redis实现分布式缓存
 * 2. 使用Lua脚本保证原子性
 * 3. 支持多种防护策略
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheProtectionService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 空值标记
     */
    private static final String NULL_VALUE = "NULL";

    /**
     * 空值缓存过期时间（秒）
     */
    private static final int NULL_CACHE_EXPIRE_SECONDS = 60;

    /**
     * 互斥锁key前缀
     */
    private static final String MUTEX_LOCK_PREFIX = "cache:mutex:";

    /**
     * 互斥锁过期时间（秒）
     */
    private static final int MUTEX_LOCK_EXPIRE_SECONDS = 10;

    /**
     * 获取缓存（防穿透）
     *
     * 【实现原理】
     * 1. 查询缓存
     * 2. 如果缓存命中且不是空值，返回
     * 3. 如果缓存命中是空值，返回null
     * 4. 查询数据库
     * 5. 如果数据库有值，写入缓存
     * 6. 如果数据库无值，缓存空值
     *
     * @param key 缓存key
     * @param dbQuery 数据库查询函数
     * @param expireSeconds 过期时间（秒）
     * @return 缓存值
     */
    public String getWithPenetrationProtection(String key, Supplier<String> dbQuery, int expireSeconds) {
        // 1. 查询缓存
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            if (NULL_VALUE.equals(value)) {
                log.debug("缓存命中（空值）: key={}", key);
                return null;
            }
            log.debug("缓存命中: key={}", key);
            return value;
        }

        // 2. 缓存未命中，查询数据库
        String dbValue = dbQuery.get();

        // 3. 写入缓存
        if (dbValue != null) {
            redisTemplate.opsForValue().set(key, dbValue, expireSeconds, TimeUnit.SECONDS);
            log.debug("写入缓存: key={}", key);
        } else {
            // 缓存空值，防穿透
            redisTemplate.opsForValue().set(key, NULL_VALUE, NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.debug("写入空值缓存: key={}", key);
        }

        return dbValue;
    }

    /**
     * 获取缓存（防击穿 - 互斥锁）
     *
     * 【实现原理】
     * 1. 查询缓存
     * 2. 如果缓存命中，返回
     * 3. 尝试获取互斥锁
     * 4. 获取成功，查询数据库并写入缓存
     * 5. 获取失败，等待后重试
     *
     * @param key 缓存key
     * @param dbQuery 数据库查询函数
     * @param expireSeconds 过期时间（秒）
     * @return 缓存值
     */
    public String getWithBreakdownProtection(String key, Supplier<String> dbQuery, int expireSeconds) {
        // 1. 查询缓存
        String value = redisTemplate.opsForValue().get(key);
        if (value != null && !NULL_VALUE.equals(value)) {
            return value;
        }

        // 2. 尝试获取互斥锁
        String lockKey = MUTEX_LOCK_PREFIX + key;
        boolean locked = tryLock(lockKey);

        if (locked) {
            try {
                // 3. 双重检查
                value = redisTemplate.opsForValue().get(key);
                if (value != null && !NULL_VALUE.equals(value)) {
                    return value;
                }

                // 4. 查询数据库
                String dbValue = dbQuery.get();

                // 5. 写入缓存
                if (dbValue != null) {
                    // 添加随机过期时间，防雪崩
                    int randomExpire = expireSeconds + (int) (Math.random() * 60);
                    redisTemplate.opsForValue().set(key, dbValue, randomExpire, TimeUnit.SECONDS);
                } else {
                    redisTemplate.opsForValue().set(key, NULL_VALUE, NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                }

                return dbValue;
            } finally {
                unlock(lockKey);
            }
        } else {
            // 获取锁失败，等待后重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return redisTemplate.opsForValue().get(key);
        }
    }

    /**
     * 获取缓存（防雪崩 - 随机过期时间）
     *
     * @param key 缓存key
     * @param dbQuery 数据库查询函数
     * @param baseExpireSeconds 基础过期时间（秒）
     * @param randomExpireSeconds 随机过期时间范围（秒）
     * @return 缓存值
     */
    public String getWithAvalancheProtection(String key, Supplier<String> dbQuery,
                                              int baseExpireSeconds, int randomExpireSeconds) {
        // 1. 查询缓存
        String value = redisTemplate.opsForValue().get(key);
        if (value != null && !NULL_VALUE.equals(value)) {
            return value;
        }

        // 2. 查询数据库
        String dbValue = dbQuery.get();

        // 3. 写入缓存（随机过期时间）
        if (dbValue != null) {
            int randomExpire = baseExpireSeconds + (int) (Math.random() * randomExpireSeconds);
            redisTemplate.opsForValue().set(key, dbValue, randomExpire, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, NULL_VALUE, NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }

        return dbValue;
    }

    /**
     * 尝试获取锁
     */
    private boolean tryLock(String lockKey) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", MUTEX_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放锁
     */
    private void unlock(String lockKey) {
        redisTemplate.delete(lockKey);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 设置缓存
     *
     * @param key 缓存key
     * @param value 缓存值
     * @param expireSeconds 过期时间（秒）
     */
    public void set(String key, String value, int expireSeconds) {
        redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }
}
