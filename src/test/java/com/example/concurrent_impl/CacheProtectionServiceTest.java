package com.example.concurrent_impl;

import com.example.concurrent_impl.highconcurrency.cache.service.CacheProtectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 【企业级】缓存保护服务测试
 */
@SpringBootTest
public class CacheProtectionServiceTest {

    @Autowired
    private CacheProtectionService cacheProtectionService;

    /**
     * 测试防穿透 - 缓存空值
     */
    @Test
    public void testPenetrationProtection() {
        String key = "test:penetration:" + System.currentTimeMillis();

        // 第一次查询（数据库返回null）
        String result1 = cacheProtectionService.getWithPenetrationProtection(
                key, () -> null, 60);
        assertNull(result1, "第一次查询应该返回null");

        // 第二次查询（应该命中空值缓存）
        String result2 = cacheProtectionService.getWithPenetrationProtection(
                key, () -> "should not be called", 60);
        assertNull(result2, "第二次查询应该返回null（命中空值缓存）");
    }

    /**
     * 测试防穿透 - 正常缓存
     */
    @Test
    public void testPenetrationProtectionWithRealData() {
        String key = "test:real:" + System.currentTimeMillis();

        // 第一次查询（数据库返回数据）
        String result1 = cacheProtectionService.getWithPenetrationProtection(
                key, () -> "test_value", 60);
        assertEquals("test_value", result1, "第一次查询应该返回数据库值");

        // 第二次查询（应该命中缓存）
        String result2 = cacheProtectionService.getWithPenetrationProtection(
                key, () -> "should not be called", 60);
        assertEquals("test_value", result2, "第二次查询应该返回缓存值");
    }

    /**
     * 测试防击穿 - 互斥锁
     */
    @Test
    public void testBreakdownProtection() {
        String key = "test:breakdown:" + System.currentTimeMillis();

        // 第一次查询
        String result1 = cacheProtectionService.getWithBreakdownProtection(
                key, () -> "test_value", 60);
        assertEquals("test_value", result1, "第一次查询应该返回数据库值");

        // 第二次查询（应该命中缓存）
        String result2 = cacheProtectionService.getWithBreakdownProtection(
                key, () -> "should not be called", 60);
        assertEquals("test_value", result2, "第二次查询应该返回缓存值");
    }

    /**
     * 测试防雪崩 - 随机过期时间
     */
    @Test
    public void testAvalancheProtection() {
        String key = "test:avalanche:" + System.currentTimeMillis();

        // 查询
        String result = cacheProtectionService.getWithAvalancheProtection(
                key, () -> "test_value", 60, 30);
        assertEquals("test_value", result, "查询应该返回数据库值");
    }

    /**
     * 测试删除缓存
     */
    @Test
    public void testDeleteCache() {
        String key = "test:delete:" + System.currentTimeMillis();

        // 设置缓存
        cacheProtectionService.set(key, "test_value", 60);

        // 删除缓存
        cacheProtectionService.delete(key);

        // 再次查询（应该查数据库）
        String result = cacheProtectionService.getWithPenetrationProtection(
                key, () -> "new_value", 60);
        assertEquals("new_value", result, "删除缓存后应该查询数据库");
    }
}
