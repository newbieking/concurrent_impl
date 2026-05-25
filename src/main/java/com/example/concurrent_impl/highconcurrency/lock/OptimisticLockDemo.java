package com.example.concurrent_impl.highconcurrency.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【锁机制 - 乐观锁】数据库乐观锁实现示例
 *
 * 【业务背景】
 * 在电商秒杀场景中，大量用户同时抢购同一商品，
 * 需要保证库存扣减的原子性，防止超卖。
 *
 * 【实现原理】
 * 乐观锁假设不会发生冲突，只在提交更新时检查数据是否被修改。
 * 使用版本号机制：
 * 1. 读取数据时获取版本号
 * 2. 更新时带上版本号
 * 3. 如果版本号不匹配，说明数据被修改，更新失败
 *
 * 【为什么这样写】
 * 1. 乐观锁适用于读多写少的场景
 * 2. 不会阻塞其他线程，性能好
 * 3. 使用版本号比CAS更直观
 *
 * 【不遵守的后果】
 * 1. 不使用乐观锁：库存超卖，实际发货数量超过库存
 * 2. 不重试：用户体验差，明明有库存却提示失败
 * 3. 不限制重试次数：可能导致死循环
 *
 * 【正确示例】
 * 使用版本号 + 重试机制
 *
 * 【错误示例】
 * 直接 UPDATE stock = stock - 1（存在超卖风险）
 *
 * 【实际案例】
 * 1. 秒杀商品库存扣减
 * 2. 账户余额更新
 * 3. 订单状态更新
 *
 * @author concurrent_impl
 * @date 2024
 */
public class OptimisticLockDemo {

    /**
     * 模拟商品库存（使用AtomicInteger模拟数据库）
     */
    private static final AtomicInteger stock = new AtomicInteger(100);

    /**
     * 模拟版本号（使用AtomicInteger模拟数据库）
     */
    private static final AtomicInteger version = new AtomicInteger(0);

    /**
     * 成功扣减数量
     */
    private static final AtomicInteger successCount = new AtomicInteger(0);

    /**
     * 失败扣减数量
     */
    private static final AtomicInteger failCount = new AtomicInteger(0);

    /**
     * 获取当前库存
     */
    public static int getStock() {
        return stock.get();
    }

    /**
     * 获取当前版本号
     */
    public static int getVersion() {
        return version.get();
    }

    /**
     * 使用乐观锁扣减库存
     *
     * 【实现原理】
     * 1. 读取当前库存和版本号
     * 2. 检查库存是否充足
     * 3. 使用CAS更新库存和版本号
     * 4. 更新失败则重试
     *
     * @param quantity 扣减数量
     * @param maxRetries 最大重试次数
     * @return 是否扣减成功
     */
    public static boolean deductStockWithOptimisticLock(int quantity, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            // 【关键点1】读取当前库存和版本号
            int currentStock = stock.get();
            int currentVersion = version.get();

            // 【关键点2】检查库存是否充足
            if (currentStock < quantity) {
                System.out.println(Thread.currentThread().getName() +
                        " 库存不足，当前库存: " + currentStock);
                return false;
            }

            // 【关键点3】使用CAS更新库存
            // 同时更新库存和版本号，保证原子性
            int newStock = currentStock - quantity;
            int newVersion = currentVersion + 1;

            if (stock.compareAndSet(currentStock, newStock) &&
                    version.compareAndSet(currentVersion, newVersion)) {
                // 更新成功
                System.out.println(Thread.currentThread().getName() +
                        " 扣减成功，剩余库存: " + newStock +
                        "，版本号: " + newVersion);
                return true;
            }

            // 【关键点4】更新失败，重试
            System.out.println(Thread.currentThread().getName() +
                        " 扣减失败（并发冲突），第" + (i + 1) + "次重试");
        }

        System.out.println(Thread.currentThread().getName() +
                " 扣减失败，超过最大重试次数");
        return false;
    }

    /**
     * 不使用乐观锁扣减库存（错误示例）
     *
     * 【问题】
     * 存在竞态条件，可能导致超卖
     */
    public static boolean deductStockWithoutLock(int quantity) {
        // 【错误示例】非原子操作
        int currentStock = stock.get();
        if (currentStock < quantity) {
            return false;
        }
        // 在这行之前，其他线程可能已经修改了stock
        stock.set(currentStock - quantity);
        return true;
    }

    /**
     * 演示乐观锁的效果
     */
    public static void demonstrateOptimisticLock() throws InterruptedException {
        System.out.println("========== 乐观锁演示 ==========");
        System.out.println("初始库存: " + getStock());
        System.out.println("初始版本号: " + getVersion());
        System.out.println();

        // 重置
        stock.set(100);
        version.set(0);
        successCount.set(0);
        failCount.set(0);

        int threadCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 模拟200个用户同时抢购
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = deductStockWithOptimisticLock(1, 3);
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
        System.out.println("最终库存: " + getStock());
        System.out.println("最终版本号: " + getVersion());
        System.out.println();

        // 验证库存是否正确
        if (getStock() == 0 && successCount.get() == 100) {
            System.out.println("【验证通过】库存扣减正确，没有超卖");
        } else {
            System.out.println("【验证失败】库存扣减异常");
        }
    }

    /**
     * 演示不使用乐观锁的问题
     */
    public static void demonstrateWithoutLock() throws InterruptedException {
        System.out.println("========== 不使用乐观锁的问题演示 ==========");
        System.out.println("初始库存: " + getStock());
        System.out.println();

        // 重置
        stock.set(100);
        successCount.set(0);
        failCount.set(0);

        int threadCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 模拟200个用户同时抢购
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = deductStockWithoutLock(1);
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

        System.out.println("扣减成功: " + successCount.get() + " 次");
        System.out.println("扣减失败: " + failCount.get() + " 次");
        System.out.println("最终库存: " + getStock());
        System.out.println();

        // 验证是否超卖
        if (getStock() < 0) {
            System.out.println("【问题】出现超卖！库存为负数: " + getStock());
        } else if (successCount.get() > 100) {
            System.out.println("【问题】扣减次数超过库存！");
        }
    }

    /**
     * 主方法 - 运行演示
     */
    public static void main(String[] args) throws InterruptedException {
        // 先演示不使用乐观锁的问题
        demonstrateWithoutLock();

        System.out.println("\n");

        // 再演示使用乐观锁的正确性
        demonstrateOptimisticLock();
    }
}
