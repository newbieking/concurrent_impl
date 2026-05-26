package com.example.concurrent_impl.businesssecurity.idempotent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 【企业级】幂等服务
 *
 * 【业务背景】
 * 在企业级项目中，由于网络抖动、用户重复点击等原因，
 * 同一个请求可能会被发送多次，需要保证接口的幂等性。
 *
 * 【实现原理】
 * 1. Token方式：页面加载时生成Token，提交时验证并删除
 * 2. 业务键方式：根据业务特征生成唯一键
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Token前缀
     */
    private static final String TOKEN_PREFIX = "idempotent:token:";

    /**
     * 业务键前缀
     */
    private static final String KEY_PREFIX = "idempotent:key:";

    /**
     * Token过期时间（秒）
     */
    private static final int TOKEN_EXPIRE_SECONDS = 300; // 5分钟

    /**
     * 业务键过期时间（秒）
     */
    private static final int KEY_EXPIRE_SECONDS = 5; // 5秒

    /**
     * 生成Token
     *
     * @param userId 用户ID
     * @return Token
     */
    public String generateToken(String userId) {
        String token = UUID.randomUUID().toString();
        String key = TOKEN_PREFIX + userId + ":" + token;

        redisTemplate.opsForValue().set(key, token, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("生成Token: userId={}, token={}", userId, token);

        return token;
    }

    /**
     * 验证并删除Token
     *
     * @param userId 用户ID
     * @param token Token
     * @return true-验证通过，false-重复提交
     */
    public boolean validateAndRemoveToken(String userId, String token) {
        String key = TOKEN_PREFIX + userId + ":" + token;

        // 使用Lua脚本保证原子性
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "  return redis.call('del', KEYS[1]) " +
                "else " +
                "  return 0 " +
                "end";

        Long result = redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                java.util.Collections.singletonList(key),
                token
        );

        boolean success = result != null && result > 0;
        if (success) {
            log.info("Token验证通过: userId={}, token={}", userId, token);
        } else {
            log.warn("Token验证失败（重复提交）: userId={}, token={}", userId, token);
        }

        return success;
    }

    /**
     * 检查业务键是否存在
     *
     * @param businessKey 业务键
     * @return true-存在（重复），false-不存在（首次）
     */
    public boolean existsKey(String businessKey) {
        String key = KEY_PREFIX + businessKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置业务键
     *
     * @param businessKey 业务键
     * @return true-设置成功，false-已存在（重复）
     */
    public boolean setKey(String businessKey) {
        String key = KEY_PREFIX + businessKey;

        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", KEY_EXPIRE_SECONDS, TimeUnit.SECONDS);

        boolean success = Boolean.TRUE.equals(result);
        if (success) {
            log.info("设置业务键成功: key={}", businessKey);
        } else {
            log.warn("业务键已存在（重复提交）: key={}", businessKey);
        }

        return success;
    }

    /**
     * 设置业务键（带结果存储）
     *
     * @param businessKey 业务键
     * @param result 结果
     */
    public void setKeyWithResult(String businessKey, String result) {
        String key = KEY_PREFIX + businessKey;
        redisTemplate.opsForValue().set(key, result, KEY_EXPIRE_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 获取业务键的结果
     *
     * @param businessKey 业务键
     * @return 结果
     */
    public String getKeyResult(String businessKey) {
        String key = KEY_PREFIX + businessKey;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除业务键
     *
     * @param businessKey 业务键
     */
    public void removeKey(String businessKey) {
        String key = KEY_PREFIX + businessKey;
        redisTemplate.delete(key);
        log.info("删除业务键: key={}", businessKey);
    }
}
