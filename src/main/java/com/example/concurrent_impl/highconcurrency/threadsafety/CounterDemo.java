package com.example.concurrent_impl.highconcurrency.threadsafety;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 【线程安全 - 计数器】多线程环境下的计数器实现对比
 *
 * 【业务背景】
 * 在实际项目中，经常需要统计访问量、点击量、订单数等指标，
 * 这些场景都需要在多线程环境下进行安全的计数操作。
 *
 * 【实现原理】
 * 1. 非线程安全：普通变量，存在竞态条件
 * 2. synchronized：悲观锁，保证原子性但性能差
 * 3. AtomicLong：CAS乐观锁，无锁实现
 * 4. LongAdder：分段锁，高并发下性能最优
 *
 * 【为什么这样写】
 * 1. 对比不同实现方式，理解线程安全的重要性
 * 2. 展示各种方案的性能差异
 * 3. 提供实际场景的选择建议
 *
 * 【不遵守的后果】
 * 1. 使用非线程安全的计数器：计数结果不准确，可能少于实际值
 * 2. 高并发下使用synchronized：性能瓶颈，响应时间长
 * 3. 不理解CAS原理：无法正确使用原子类
 *
 * 【正确示例】
 * 高并发场景使用LongAdder或AtomicLong
 *
 * 【错误示例】
 * 使用普通int或long作为计数器
 *
 * 【实际案例】
 * 1. 网站访问量统计
 * 2. 接口调用次数统计
 * 3. 秒杀商品已抢购数量
 *
 * @author concurrent_impl
 * @date 2024
 */
public class CounterDemo {

    // ==================== 不同实现的计数器 ====================

    /**
     * 非线程安全的计数器
     * 【问题】多线程环境下会出现数据不一致
     */
    private static int unsafeCount = 0;

    /**
     * 使用synchronized的计数器
     * 【特点】悲观锁，保证原子性，但性能较差
     */
    private static int syncCount = 0;

    /**
     * 使用AtomicLong的计数器
     * 【特点】CAS乐观锁，无锁实现，性能较好
     */
    private static final AtomicInteger atomicCount = new AtomicInteger(0);

    /**
     * 使用LongAdder的计数器
     * 【特点】分段锁，高并发下性能最优
     * 【原理】将一个值拆分成多个值，最后汇总
     */
    private static final LongAdder longAdderCount = new LongAdder();

    /**
     * 测试非线程安全的计数器
     *
     * 【问题演示】
     * 多个线程同时读取和修改同一个变量，会导致数据丢失
     *
     * @param threadCount 线程数
     * @param incrementCount 每个线程递增次数
     * @return 最终计数值
     */
    public static int testUnsafeCounter(int threadCount, int incrementCount) throws InterruptedException {
        unsafeCount = 0;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementCount; j++) {
                        // 【问题点】非原子操作
                        // 1. 读取当前值
                        // 2. 加1
                        // 3. 写回新值
                        // 这三步之间可能被其他线程打断
                        unsafeCount++;
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        return unsafeCount;
    }

    /**
     * 测试synchronized计数器
     *
     * 【实现原理】
     * 使用synchronized关键字保证同一时刻只有一个线程能修改计数器
     *
     * 【优点】
     * 1. 实现简单，易于理解
     * 2. 保证原子性和可见性
     *
     * 【缺点】
     * 1. 性能较差，线程需要竞争锁
     * 2. 高并发下成为瓶颈
     *
     * @param threadCount 线程数
     * @param incrementCount 每个线程递增次数
     * @return 最终计数值
     */
    public static int testSyncCounter(int threadCount, int incrementCount) throws InterruptedException {
        syncCount = 0;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementCount; j++) {
                        // 【关键点】使用synchronized保证原子性
                        synchronized (CounterDemo.class) {
                            syncCount++;
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        return syncCount;
    }

    /**
     * 测试AtomicInteger计数器
     *
     * 【实现原理】
     * 使用CAS（Compare And Swap）乐观锁机制
     * 1. 读取当前值
     * 2. 计算新值
     * 3. 使用CAS尝试更新
     * 4. 如果失败，重试
     *
     * 【优点】
     * 1. 无锁实现，性能好
     * 2. 不会导致线程阻塞
     *
     * 【缺点】
     * 1. 高并发下CAS可能频繁失败，导致CPU空转
     * 2. 只能保证单个变量的原子性
     *
     * @param threadCount 线程数
     * @param incrementCount 每个线程递增次数
     * @return 最终计数值
     */
    public static int testAtomicCounter(int threadCount, int incrementCount) throws InterruptedException {
        atomicCount.set(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementCount; j++) {
                        // 【关键点】使用原子操作
                        // 内部使用CAS实现，无需加锁
                        atomicCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        return atomicCount.get();
    }

    /**
     * 测试LongAdder计数器
     *
     * 【实现原理】
     * LongAdder将一个值拆分成多个值（Cell数组），
     * 每个线程操作自己的Cell，最后汇总结果。
     *
     * 【优点】
     * 1. 高并发下性能最优
     * 2. 减少了CAS竞争
     *
     * 【缺点】
     * 1. 统计时存在一定的延迟
     * 2. 内存占用稍大
     *
     * 【适用场景】
     * 统计类场景，对实时性要求不高
     *
     * @param threadCount 线程数
     * @param incrementCount 每个线程递增次数
     * @return 最终计数值
     */
    public static long testLongAdderCounter(int threadCount, int incrementCount) throws InterruptedException {
        longAdderCount.reset();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementCount; j++) {
                        // 【关键点】使用LongAdder
                        // 内部使用分段锁，高并发下性能更好
                        longAdderCount.increment();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        return longAdderCount.sum();
    }

    /**
     * 性能对比测试
     *
     * 【测试方法】
     * 使用相同数量的线程和递增次数，比较不同实现的耗时
     *
     * @param threadCount 线程数
     * @param incrementCount 每个线程递增次数
     */
    public static void performanceComparison(int threadCount, int incrementCount) throws InterruptedException {
        System.out.println("========== 性能对比测试 ==========");
        System.out.println("线程数: " + threadCount + ", 每线程递增次数: " + incrementCount);
        System.out.println("预期结果: " + (long) threadCount * incrementCount);
        System.out.println();

        // 测试非线程安全计数器
        long start = System.currentTimeMillis();
        int unsafeResult = testUnsafeCounter(threadCount, incrementCount);
        long unsafeTime = System.currentTimeMillis() - start;
        System.out.println("非线程安全: 结果=" + unsafeResult + ", 耗时=" + unsafeTime + "ms");

        // 测试synchronized计数器
        start = System.currentTimeMillis();
        int syncResult = testSyncCounter(threadCount, incrementCount);
        long syncTime = System.currentTimeMillis() - start;
        System.out.println("synchronized: 结果=" + syncResult + ", 耗时=" + syncTime + "ms");

        // 测试AtomicInteger计数器
        start = System.currentTimeMillis();
        int atomicResult = testAtomicCounter(threadCount, incrementCount);
        long atomicTime = System.currentTimeMillis() - start;
        System.out.println("AtomicInteger: 结果=" + atomicResult + ", 耗时=" + atomicTime + "ms");

        // 测试LongAdder计数器
        start = System.currentTimeMillis();
        long longAdderResult = testLongAdderCounter(threadCount, incrementCount);
        long longAdderTime = System.currentTimeMillis() - start;
        System.out.println("LongAdder: 结果=" + longAdderResult + ", 耗时=" + longAdderTime + "ms");

        System.out.println();
        System.out.println("【结论】");
        System.out.println("1. 非线程安全计数器结果不准确，存在数据丢失");
        System.out.println("2. synchronized性能最差，高并发下成为瓶颈");
        System.out.println("3. AtomicInteger性能较好，适合中等并发");
        System.out.println("4. LongAdder性能最优，适合高并发统计场景");
    }

    /**
     * 主方法 - 运行性能对比测试
     */
    public static void main(String[] args) throws InterruptedException {
        // 测试场景1：低并发
        System.out.println("【场景1】低并发（10线程，10000次）");
        performanceComparison(10, 10000);

        System.out.println("\n");

        // 测试场景2：中等并发
        System.out.println("【场景2】中等并发（100线程，10000次）");
        performanceComparison(100, 10000);

        System.out.println("\n");

        // 测试场景3：高并发
        System.out.println("【场景3】高并发（1000线程，10000次）");
        performanceComparison(1000, 10000);
    }
}
