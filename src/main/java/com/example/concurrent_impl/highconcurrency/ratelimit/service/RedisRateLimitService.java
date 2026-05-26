package com.example.concurrent_impl.highconcurrency.ratelimit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 【企业级】Redis限流服务
 *
 * 【业务背景】
 * 在高并发系统中，需要对请求进行限流保护，
 * 防止系统过载导致服务不可用。
 *
 * 【实现原理】
 * 使用Redis + Lua脚本实现滑动窗口限流：
 * 1. 使用Sorted Set存储请求时间戳
 * 2. 使用Lua脚本保证原子性
 * 3. 支持滑动窗口和固定窗口
 *
 * 【为什么这样写】
 * 1. 使用Redis实现分布式限流
 * 2. 使用Lua脚本保证原子性
 * 3. 支持多种限流策略
 *
 * 【不遵守的后果】
 * 1. 不限流：系统过载，响应变慢甚至崩溃
 * 2. 限流太严：正常请求被拒绝，用户体验差
 * 3. 不使用分布式限流：单机限流无法应对集群场景
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimitService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 限流key前缀
     */
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * 滑动窗口限流Lua脚本
     *
     * 【脚本逻辑】
     * 1. 移除窗口外的请求记录
     * 2. 统计窗口内的请求数
     * 3. 如果请求数小于限制，添加新请求并返回成功
     * 4. 否则返回失败
     */
    private static final String SLIDING_WINDOW_SCRIPT =
            "local key = KEYS[1] " +
            "local window = tonumber(ARGV[1]) " +
            "local limit = tonumber(ARGV[2]) " +
            "local now = tonumber(ARGV[3]) " +
            "local windowStart = now - window " +
            "redis.call('zremrangebyscore', key, '-inf', windowStart) " +
            "local count = redis.call('zcard', key) " +
            "if count < limit then " +
            "  redis.call('zadd', key, now, now .. ':' .. math.random()) " +
            "  redis.call('expire', key, window / 1000 + 1) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";

    /**
     * 固定窗口限流Lua脚本
     */
    private static final String FIXED_WINDOW_SCRIPT =
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local expire = tonumber(ARGV[2]) " +
            "local count = redis.call('get', key) " +
            "if count == false then " +
            "  redis.call('set', key, 1) " +
            "  redis.call('expire', key, expire) " +
            "  return 1 " +
            "elseif tonumber(count) < limit then " +
            "  redis.call('incr', key) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";

    /**
     * 令牌桶限流Lua脚本
     */
    private static final String TOKEN_BUCKET_SCRIPT =
            "local key = KEYS[1] " +
            "local capacity = tonumber(ARGV[1]) " +
            "local rate = tonumber(ARGV[2]) " +
            "local now = tonumber(ARGV[3]) " +
            "local requested = tonumber(ARGV[4]) " +
            "local lastTime = redis.call('hget', key, 'last_time') or now " +
            "local tokens = redis.call('hget', key, 'tokens') or capacity " +
            "local elapsed = now - lastTime " +
            "tokens = math.min(capacity, tokens + elapsed * rate / 1000) " +
            "if tokens >= requested then " +
            "  tokens = tokens - requested " +
            "  redis.call('hset', key, 'tokens', tokens) " +
            "  redis.call('hset', key, 'last_time', now) " +
            "  redis.call('expire', key, 60) " +
            "  return 1 " +
            "else " +
            "  redis.call('hset', key, 'tokens', tokens) " +
            "  redis.call('hset', key, 'last_time', now) " +
            "  return 0 " +
            "end";

    /**
     * 滑动窗口限流
     *
     * @param key 限流key
     * @param windowMs 窗口大小（毫秒）
     * @param limit 窗口内允许的最大请求数
     * @return 是否允许通过
     */
    public boolean slidingWindowRateLimit(String key, long windowMs, int limit) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        long now = System.currentTimeMillis();

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(SLIDING_WINDOW_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(redisKey),
                    String.valueOf(windowMs), String.valueOf(limit), String.valueOf(now));

            boolean allowed = result != null && result > 0;
            if (!allowed) {
                log.warn("滑动窗口限流触发: key={}, limit={}", key, limit);
            }
            return allowed;
        } catch (Exception e) {
            log.error("滑动窗口限流异常: key={}", key, e);
            // 异常时放行，保证可用性
            return true;
        }
    }

    /**
     * 固定窗口限流
     *
     * @param key 限流key
     * @param limit 窗口内允许的最大请求数
     * @param windowSeconds 窗口大小（秒）
     * @return 是否允许通过
     */
    public boolean fixedWindowRateLimit(String key, int limit, int windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + "fixed:" + key;

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(FIXED_WINDOW_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(redisKey),
                    String.valueOf(limit), String.valueOf(windowSeconds));

            boolean allowed = result != null && result > 0;
            if (!allowed) {
                log.warn("固定窗口限流触发: key={}, limit={}", key, limit);
            }
            return allowed;
        } catch (Exception e) {
            log.error("固定窗口限流异常: key={}", key, e);
            return true;
        }
    }

    /**
     * 令牌桶限流
     *
     * @param key 限流key
     * @param capacity 桶容量
     * @param rate 令牌生成速率（每秒）
     * @return 是否允许通过
     */
    public boolean tokenBucketRateLimit(String key, int capacity, int rate) {
        String redisKey = RATE_LIMIT_PREFIX + "bucket:" + key;
        long now = System.currentTimeMillis();

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(redisKey),
                    String.valueOf(capacity), String.valueOf(rate), String.valueOf(now), "1");

            boolean allowed = result != null && result > 0;
            if (!allowed) {
                log.warn("令牌桶限流触发: key={}, capacity={}, rate={}", key, capacity, rate);
            }
            return allowed;
        } catch (Exception e) {
            log.error("令牌桶限流异常: key={}", key, e);
            return true;
        }
    }

    /**
     * 获取当前限流计数
     *
     * @param key 限流key
     * @return 当前计数
     */
    public long getCurrentCount(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        try {
            String count = redisTemplate.opsForValue().get(redisKey);
            return count != null ? Long.parseLong(count) : 0;
        } catch (Exception e) {
            log.error("获取限流计数异常: key={}", key, e);
            return 0;
        }
    }

    /**
     * 重置限流计数
     *
     * @param key 限流key
     */
    public void resetCount(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("重置限流计数异常: key={}", key, e);
        }
    }
}
