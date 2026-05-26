package com.example.concurrent_impl.highconcurrency.threadsafety.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 【企业级】线程安全服务
 *
 * 【业务背景】
 * 在实际项目中，经常需要统计访问量、点击量、订单数等指标，
 * 这些场景都需要在多线程环境下进行安全的计数操作。
 *
 * 【实现原理】
 * 1. AtomicInteger：CAS乐观锁，无锁实现
 * 2. LongAdder：分段锁，高并发下性能最优
 * 3. synchronized：悲观锁，保证原子性但性能差
 *
 * 【为什么这样写】
 * 1. 提供多种线程安全的计数器实现
 * 2. 根据业务场景选择合适的实现
 * 3. 支持性能对比测试
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
public class ThreadSafetyService {

    /**
     * 原子计数器（使用AtomicInteger）
     *
     * 【适用场景】
     * 中等并发，需要精确计数
     */
    private final AtomicInteger atomicCounter = new AtomicInteger(0);

    /**
     * 高性能计数器（使用LongAdder）
     *
     * 【适用场景】
     * 高并发统计，如访问量、点击量
     */
    private final LongAdder longAdderCounter = new LongAdder();

    /**
     * 使用AtomicInteger递增
     *
     * @return 递增后的值
     */
    public int incrementAtomic() {
        return atomicCounter.incrementAndGet();
    }

    /**
     * 使用AtomicInteger递增指定次数
     *
     * @param times 递增次数
     * @return 最终值
     */
    public int incrementAtomic(int times) {
        for (int i = 0; i < times; i++) {
            atomicCounter.incrementAndGet();
        }
        return atomicCounter.get();
    }

    /**
     * 使用LongAdder递增
     */
    public void incrementLongAdder() {
        longAdderCounter.increment();
    }

    /**
     * 使用LongAdder递增指定次数
     *
     * @param times 递增次数
     * @return 最终值
     */
    public long incrementLongAdder(int times) {
        for (int i = 0; i < times; i++) {
            longAdderCounter.increment();
        }
        return longAdderCounter.sum();
    }

    /**
     * 获取AtomicInteger当前值
     */
    public int getAtomicValue() {
        return atomicCounter.get();
    }

    /**
     * 获取LongAdder当前值
     */
    public long getLongAdderValue() {
        return longAdderCounter.sum();
    }

    /**
     * 重置计数器
     */
    public void reset() {
        atomicCounter.set(0);
        longAdderCounter.reset();
    }

    /**
     * 多线程并发递增测试
     *
     * @param threadCount 线程数
     * @param incrementCount 每个线程递增次数
     * @return 测试结果
     */
    public CounterTestResult concurrentIncrementTest(int threadCount, int incrementCount) throws InterruptedException {
        log.info("开始并发测试: threadCount={}, incrementCount={}", threadCount, incrementCount);

        // 重置计数器
        reset();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch1 = new CountDownLatch(threadCount);

        // 测试AtomicInteger
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    incrementAtomic(incrementCount);
                } finally {
                    latch1.countDown();
                }
            });
        }
        latch1.await();
        long atomicTime = System.currentTimeMillis() - startTime;
        int atomicResult = getAtomicValue();

        // 测试LongAdder
        reset();
        CountDownLatch latch2 = new CountDownLatch(threadCount);
        startTime = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    incrementLongAdder(incrementCount);
                } finally {
                    latch2.countDown();
                }
            });
        }
        latch2.await();
        long longAdderTime = System.currentTimeMillis() - startTime;
        long longAdderResult = getLongAdderValue();

        executor.shutdown();

        CounterTestResult result = new CounterTestResult();
        result.setThreadCount(threadCount);
        result.setIncrementCount(incrementCount);
        result.setExpectedCount((long) threadCount * incrementCount);
        result.setAtomicResult(atomicResult);
        result.setAtomicTimeMs(atomicTime);
        result.setLongAdderResult(longAdderResult);
        result.setLongAdderTimeMs(longAdderTime);

        log.info("并发测试完成: atomicResult={}, atomicTime={}ms, longAdderResult={}, longAdderTime={}ms",
                atomicResult, atomicTime, longAdderResult, longAdderTime);

        return result;
    }

    /**
     * 测试结果
     */
    @lombok.Data
    public static class CounterTestResult {
        private int threadCount;
        private int incrementCount;
        private long expectedCount;
        private int atomicResult;
        private long atomicTimeMs;
        private long longAdderResult;
        private long longAdderTimeMs;
    }
}
