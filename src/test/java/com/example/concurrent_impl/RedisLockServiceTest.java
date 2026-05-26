package com.example.concurrent_impl;

import com.example.concurrent_impl.highconcurrency.lock.service.RedisLockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 【企业级】分布式锁服务测试
 */
@SpringBootTest
public class RedisLockServiceTest {

    @Autowired
    private RedisLockService lockService;

    /**
     * 测试获取和释放锁
     */
    @Test
    public void testTryLockAndUnlock() {
        String lockKey = "test:lock:" + System.currentTimeMillis();

        // 获取锁
        String lockValue = lockService.tryLock(lockKey, 10);
        assertNotNull(lockValue, "应该成功获取锁");

        // 检查锁是否存在
        assertTrue(lockService.isLocked(lockKey), "锁应该存在");

        // 释放锁
        boolean unlocked = lockService.unlock(lockKey, lockValue);
        assertTrue(unlocked, "应该成功释放锁");

        // 检查锁是否已释放
        assertFalse(lockService.isLocked(lockKey), "锁应该已释放");
    }

    /**
     * 测试锁互斥
     */
    @Test
    public void testLockMutex() {
        String lockKey = "test:mutex:" + System.currentTimeMillis();

        // 第一个线程获取锁
        String lockValue1 = lockService.tryLock(lockKey, 10);
        assertNotNull(lockValue1, "第一个线程应该成功获取锁");

        // 第二个线程尝试获取锁（应该失败）
        String lockValue2 = lockService.tryLock(lockKey, 10);
        assertNull(lockValue2, "第二个线程应该获取锁失败");

        // 释放第一个线程的锁
        lockService.unlock(lockKey, lockValue1);

        // 第三个线程尝试获取锁（应该成功）
        String lockValue3 = lockService.tryLock(lockKey, 10);
        assertNotNull(lockValue3, "第三个线程应该成功获取锁");

        lockService.unlock(lockKey, lockValue3);
    }

    /**
     * 测试带重试的锁获取
     */
    @Test
    public void testTryLockWithRetry() {
        String lockKey = "test:retry:" + System.currentTimeMillis();

        // 第一个线程获取锁
        String lockValue1 = lockService.tryLock(lockKey, 10);
        assertNotNull(lockValue1, "第一个线程应该成功获取锁");

        // 第二个线程带重试获取锁（应该失败）
        String lockValue2 = lockService.tryLockWithRetry(lockKey, 10, 3, 100);
        assertNull(lockValue2, "第二个线程应该获取锁失败");

        lockService.unlock(lockKey, lockValue1);
    }

    /**
     * 测试锁保护下的任务执行
     */
    @Test
    public void testExecuteWithLock() {
        String lockKey = "test:execute:" + System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger(0);

        // 执行任务
        Integer result = lockService.executeWithLock(lockKey, 10, () -> {
            counter.incrementAndGet();
            return 42;
        });

        assertEquals(42, result, "任务应该返回正确结果");
        assertEquals(1, counter.get(), "任务应该只执行一次");
    }
}
