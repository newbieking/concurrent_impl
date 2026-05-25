package com.example.concurrent_impl.highconcurrency.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 【锁机制 - 悲观锁】数据库悲观锁实现示例
 *
 * 【业务背景】
 * 在高冲突场景中，多个线程同时修改同一数据，
 * 使用悲观锁可以保证数据的一致性。
 *
 * 【实现原理】
 * 悲观锁假设会发生冲突，在操作数据前先获取锁，
 * 其他线程必须等待锁释放才能操作数据。
 *
 * 【为什么这样写】
 * 1. 悲观锁适用于写多冲突多的场景
 * 2. 使用ReentrantLock模拟数据库悲观锁
 * 3. 展示悲观锁的使用方式和注意事项
 *
 * 【不遵守的后果】
 * 1. 不使用锁：数据不一致，出现脏读、幻读
 * 2. 锁粒度太大：性能差，成为瓶颈
 * 3. 不释放锁：死锁，系统无法继续运行
 *
 * 【正确示例】
 * 使用try-finally确保锁被释放
 *
 * 【错误示例】
 * 获取锁后不释放，或不在finally中释放
 *
 * 【实际案例】
 * 1. 账户余额更新（写多冲突多）
 * 2. 库存扣减（秒杀场景）
 * 3. 订单状态更新
 *
 * 【悲观锁 vs 乐观锁】
 * 悲观锁：
 * - 优点：保证数据一致性，适合高冲突场景
 * - 缺点：性能差，可能导致死锁
 *
 * 乐观锁：
 * - 优点：性能好，不会死锁
 * - 缺点：冲突多时重试频繁
 *
 * @author concurrent_impl
 * @date 2024
 */
public class PessimisticLockDemo {

    /**
     * 模拟账户余额
     */
    private static final AtomicInteger balance = new AtomicInteger(1000);

    /**
     * 悲观锁（使用ReentrantLock模拟）
     */
    private static final ReentrantLock lock = new ReentrantLock();

    /**
     * 成功扣减次数
     */
    private static final AtomicInteger successCount = new AtomicInteger(0);

    /**
     * 失败扣减次数
     */
    private static final AtomicInteger failCount = new AtomicInteger(0);

    /**
     * 获取当前余额
     */
    public static int getBalance() {
        return balance.get();
    }

    /**
     * 使用悲观锁扣减余额
     *
     * 【实现原理】
     * 1. 获取锁
     * 2. 检查余额是否充足
     * 3. 扣减余额
     * 4. 释放锁
     *
     * @param amount 扣减金额
     * @return 是否扣减成功
     */
    public static boolean deductWithPessimisticLock(int amount) {
        // 【关键点1】获取锁
        // tryLock()：尝试获取锁，失败返回false
        // lock()：阻塞等待获取锁
        lock.lock();
        try {
            // 【关键点2】检查余额
            int currentBalance = balance.get();
            if (currentBalance < amount) {
                System.out.println(Thread.currentThread().getName() +
                        " 余额不足，当前余额: " + currentBalance);
                return false;
            }

            // 【关键点3】扣减余额
            int newBalance = currentBalance - amount;
            balance.set(newBalance);

            System.out.println(Thread.currentThread().getName() +
                    " 扣减成功，金额: " + amount + "，剩余余额: " + newBalance);
            return true;

        } finally {
            // 【关键点4】释放锁（必须在finally中）
            lock.unlock();
        }
    }

    /**
     * 使用tryLock尝试获取锁（带超时）
     *
     * 【使用场景】
     * 不想无限等待锁，可以设置超时时间
     *
     * @param amount 扣减金额
     * @param timeoutMillis 超时时间（毫秒）
     * @return 是否扣减成功
     */
    public static boolean deductWithTryLock(int amount, long timeoutMillis) {
        try {
            // 【关键点】尝试获取锁，带超时
            boolean locked = lock.tryLock(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!locked) {
                System.out.println(Thread.currentThread().getName() +
                        " 获取锁超时");
                return false;
            }

            try {
                int currentBalance = balance.get();
                if (currentBalance < amount) {
                    return false;
                }
                balance.set(currentBalance - amount);
                return true;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 演示悲观锁的效果
     */
    public static void demonstratePessimisticLock() throws InterruptedException {
        System.out.println("========== 悲观锁演示 ==========");
        System.out.println("初始余额: " + getBalance());
        System.out.println();

        // 重置
        balance.set(1000);
        successCount.set(0);
        failCount.set(0);

        int threadCount = 200;
        int deductAmount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 模拟200个线程同时扣减
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = deductWithPessimisticLock(deductAmount);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println();
        System.out.println("扣减成功: " + successCount.get() + " 次");
        System.out.println("扣减失败: " + failCount.get() + " 次");
        System.out.println("最终余额: " + getBalance());
        System.out.println();

        // 验证余额是否正确
        int expectedBalance = 1000 - (successCount.get() * deductAmount);
        if (getBalance() == expectedBalance) {
            System.out.println("【验证通过】余额扣减正确");
        } else {
            System.out.println("【验证失败】余额扣减异常，期望: " + expectedBalance + "，实际: " + getBalance());
        }
    }

    /**
     * 演示死锁风险
     *
     * 【死锁条件】
     * 1. 互斥：资源只能被一个线程持有
     * 2. 占有并等待：持有资源的线程等待其他资源
     * 3. 不可抢占：已持有的资源不能被其他线程抢占
     * 4. 循环等待：多个线程形成循环等待
     *
     * 【预防措施】
     * 1. 按顺序获取锁
     * 2. 设置超时时间
     * 3. 使用tryLock
     */
    public static void demonstrateDeadlockRisk() {
        System.out.println("========== 死锁风险演示 ==========");
        System.out.println();
        System.out.println("【死锁场景】");
        System.out.println("线程A：获取锁1 -> 等待锁2");
        System.out.println("线程B：获取锁2 -> 等待锁1");
        System.out.println("结果：A等B释放锁2，B等A释放锁1，形成死锁");
        System.out.println();
        System.out.println("【预防措施】");
        System.out.println("1. 按固定顺序获取锁（如按ID大小）");
        System.out.println("2. 使用tryLock设置超时");
        System.out.println("3. 使用定时锁，超时自动释放");
        System.out.println();
        System.out.println("【正确示例】");
        System.out.println("```java");
        System.out.println("// 按顺序获取锁");
        System.out.println("if (lock1Id < lock2Id) {");
        System.out.println("    lock(lock1Id);");
        System.out.println("    lock(lock2Id);");
        System.out.println("} else {");
        System.out.println("    lock(lock2Id);");
        System.out.println("    lock(lock1Id);");
        System.out.println("}");
        System.out.println("```");
    }

    /**
     * 主方法 - 运行演示
     */
    public static void main(String[] args) throws InterruptedException {
        demonstratePessimisticLock();
        System.out.println("\n");
        demonstrateDeadlockRisk();
    }
}
