package com.example.concurrent_impl;

import com.example.concurrent_impl.highconcurrency.ratelimit.service.RedisRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 【企业级】限流服务测试
 */
@SpringBootTest
public class RedisRateLimitServiceTest {

    @Autowired
    private RedisRateLimitService rateLimitService;

    /**
     * 测试滑动窗口限流
     */
    @Test
    public void testSlidingWindowRateLimit() {
        String key = "test:sliding:" + System.currentTimeMillis();

        // 前10个请求应该通过
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.slidingWindowRateLimit(key, 1000, 10),
                    "第" + (i + 1) + "个请求应该通过");
        }

        // 第11个请求应该被限流
        assertFalse(rateLimitService.slidingWindowRateLimit(key, 1000, 10),
                "第11个请求应该被限流");
    }

    /**
     * 测试固定窗口限流
     */
    @Test
    public void testFixedWindowRateLimit() {
        String key = "test:fixed:" + System.currentTimeMillis();

        // 前5个请求应该通过
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.fixedWindowRateLimit(key, 5, 60),
                    "第" + (i + 1) + "个请求应该通过");
        }

        // 第6个请求应该被限流
        assertFalse(rateLimitService.fixedWindowRateLimit(key, 5, 60),
                "第6个请求应该被限流");
    }

    /**
     * 测试令牌桶限流
     */
    @Test
    public void testTokenBucketRateLimit() {
        String key = "test:bucket:" + System.currentTimeMillis();

        // 前5个请求应该通过
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.tokenBucketRateLimit(key, 5, 10),
                    "第" + (i + 1) + "个请求应该通过");
        }

        // 等待令牌生成
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 应该可以继续请求
        assertTrue(rateLimitService.tokenBucketRateLimit(key, 5, 10),
                "等待后应该可以继续请求");
    }
}
