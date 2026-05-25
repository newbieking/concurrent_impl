package com.example.concurrent_impl.highconcurrency.lock;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【锁机制 - 分布式锁】Redis分布式锁实现示例
 *
 * 【业务背景】
 * 在分布式系统中，多个服务实例需要竞争同一资源时，
 * 需要使用分布式锁来保证同一时刻只有一个实例能执行。
 *
 * 【实现原理】
 * 使用Redis的SET命令实现分布式锁：
 * 1. SET key value NX EX timeout
 * 2. NX：只在key不存在时设置
 * 3. EX：设置过期时间，防止死锁
 * 4. value：使用UUID作为锁标识
 *
 * 【为什么这样写】
 * 1. 使用UUID作为锁标识：防止误删其他线程的锁
 * 2. 设置过期时间：防止死锁
 * 3. 使用Lua脚本释放锁：保证原子性
 *
 * 【不遵守的后果】
 * 1. 不设置过期时间：服务宕机后锁无法释放，导致死锁
 * 2. 不使用UUID：可能误删其他线程的锁
 * 3. 不使用Lua脚本：释放锁时可能出现竞态条件
 *
 * 【正确示例】
 * 使用分布式锁保护共享资源
 *
 * 【错误示例】
 * 不检查获取锁结果，直接执行业务
 *
 * 【实际案例】
 * 1. 订单创建时防止重复下单
 * 2. 库存扣减时防止超卖
 * 3. 定时任务执行时防止重复执行
 *
 * 【分布式锁方案对比】
 * 1. Redis：性能好，但主从切换可能丢失锁
 * 2. Zookeeper：一致性好，但性能稍差
 * 3. etcd：兼顾性能和一致性
 *
 * @author concurrent_impl
 * @date 2024
 */
public class DistributedLockDemo {

    /**
     * 模拟Redis分布式锁
     *
     * 【注意】
     * 这里使用AtomicBoolean模拟Redis的SET NX命令
     * 实际项目中应使用RedisTemplate
     */
    private static final AtomicBoolean lockKey = new AtomicBoolean(false);
    private static String lockValue = null;

    /**
     * 模拟共享资源
     */
    private static final AtomicInteger sharedResource = new AtomicInteger(100);

    /**
     * 成功获取锁的次数
     */
    private static final AtomicInteger successCount = new AtomicInteger(0);

    /**
     * 失败获取锁的次数
     */
    private static final AtomicInteger failCount = new AtomicInteger(0);

    /**
     * 尝试获取分布式锁
     *
     * 【实现原理】
     * 模拟Redis的SET key value NX EX timeout命令
     *
     * @param key 锁的key
     * @param timeoutSeconds 超时时间（秒）
     * @return 锁标识（UUID），获取失败返回null
     */
    public static String tryLock(String key, int timeoutSeconds) {
        String value = UUID.randomUUID().toString();

        // 【关键点】模拟SET NX命令
        // 只在key不存在时设置
        boolean locked = lockKey.compareAndSet(false, true);

        if (locked) {
            lockValue = value;
            System.out.println(Thread.currentThread().getName() +
                    " 获取锁成功: " + value);
            return value;
        }

        System.out.println(Thread.currentThread().getName() +
                " 获取锁失败（已被占用）");
        return null;
    }

    /**
     * 释放分布式锁
     *
     * 【实现原理】
     * 模拟Lua脚本：判断锁标识是否匹配，匹配则删除
     *
     * @param key 锁的key
     * @param value 锁标识
     * @return 是否释放成功
     */
    public static boolean unlock(String key, String value) {
        // 【关键点】验证锁标识
        if (value != null && value.equals(lockValue)) {
            lockKey.set(false);
            lockValue = null;
            System.out.println(Thread.currentThread().getName() +
                    " 释放锁成功: " + value);
            return true;
        }

        System.out.println(Thread.currentThread().getName() +
                " 释放锁失败（锁标识不匹配或锁已过期）");
        return false;
    }

    /**
     * 使用分布式锁执行任务
     *
     * @param taskName 任务名称
     */
    public static void executeWithLock(String taskName) {
        String lockKey = "task:" + taskName;
        String lockValue = null;

        try {
            // 【关键点1】获取锁
            lockValue = tryLock(lockKey, 30);

            if (lockValue == null) {
                failCount.incrementAndGet();
                System.out.println(Thread.currentThread().getName() +
                        " 无法获取锁，跳过任务: " + taskName);
                return;
            }

            // 【关键点2】执行业务逻辑
            successCount.incrementAndGet();
            System.out.println(Thread.currentThread().getName() +
                    " 开始执行任务: " + taskName);

            // 模拟业务处理
            int currentValue = sharedResource.get();
            if (currentValue > 0) {
                // 模拟处理时间
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                sharedResource.decrementAndGet();
                System.out.println(Thread.currentThread().getName() +
                        " 任务完成: " + taskName + "，剩余资源: " + sharedResource.get());
            }

        } finally {
            // 【关键点3】释放锁
            if (lockValue != null) {
                unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 演示分布式锁的效果
     */
    public static void demonstrateDistributedLock() throws InterruptedException {
        System.out.println("========== 分布式锁演示 ==========");
        System.out.println("初始资源: " + sharedResource.get());
        System.out.println();

        // 重置
        sharedResource.set(10);
        successCount.set(0);
        failCount.set(0);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 模拟多个任务同时执行
        for (int i = 0; i < threadCount; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    executeWithLock("task_" + taskId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println();
        System.out.println("成功获取锁: " + successCount.get() + " 次");
        System.out.println("失败获取锁: " + failCount.get() + " 次");
        System.out.println("剩余资源: " + sharedResource.get());
    }

    /**
     * 演示锁超时重试机制
     *
     * 【使用场景】
     * 获取锁失败后，等待一段时间重试
     */
    public static void demonstrateRetryMechanism() throws InterruptedException {
        System.out.println("========== 锁超时重试机制演示 ==========");
        System.out.println();

        // 重置
        sharedResource.set(5);
        successCount.set(0);
        failCount.set(0);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // 【重试机制】最多重试3次，每次等待100ms
                    for (int retry = 0; retry < 3; retry++) {
                        String lockValue = tryLock("retry_task", 30);
                        if (lockValue != null) {
                            try {
                                successCount.incrementAndGet();
                                System.out.println(Thread.currentThread().getName() +
                                        " 第" + (retry + 1) + "次尝试获取锁成功");
                                // 执行业务
                                Thread.sleep(200);
                                sharedResource.decrementAndGet();
                            } finally {
                                unlock("retry_task", lockValue);
                            }
                            return;
                        }

                        // 等待后重试
                        System.out.println(Thread.currentThread().getName() +
                                " 第" + (retry + 1) + "次尝试获取锁失败，等待重试...");
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    failCount.incrementAndGet();
                    System.out.println(Thread.currentThread().getName() +
                            " 重试3次后仍然失败");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println();
        System.out.println("成功: " + successCount.get() + " 次");
        System.out.println("失败: " + failCount.get() + " 次");
        System.out.println("剩余资源: " + sharedResource.get());
    }

    /**
     * 演示分布式锁的注意事项
     */
    public static void demonstrateBestPractices() {
        System.out.println("========== 分布式锁最佳实践 ==========");
        System.out.println();
        System.out.println("【Redis分布式锁实现要点】");
        System.out.println("1. 使用SET key value NX EX timeout命令");
        System.out.println("2. value使用UUID，保证锁的唯一性");
        System.out.println("3. 设置合理的超时时间，防止死锁");
        System.out.println("4. 使用Lua脚本释放锁，保证原子性");
        System.out.println();
        System.out.println("【Redis分布式锁的风险】");
        System.out.println("1. 主从切换可能丢失锁");
        System.out.println("2. 锁过期但业务未完成");
        System.out.println("3. 网络分区导致锁失效");
        System.out.println();
        System.out.println("【解决方案】");
        System.out.println("1. 使用RedLock算法（多节点）");
        System.out.println("2. 使用锁续期机制（Watch Dog）");
        System.out.println("3. 对一致性要求高使用Zookeeper/etcd");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("```java");
        System.out.println("// 获取锁");
        System.out.println("String lockValue = UUID.randomUUID().toString();");
        System.out.println("Boolean result = redisTemplate.opsForValue()");
        System.out.println("    .setIfAbsent(lockKey, lockValue, 30, TimeUnit.SECONDS);");
        System.out.println("if (Boolean.TRUE.equals(result)) {");
        System.out.println("    try {");
        System.out.println("        // 执行业务逻辑");
        System.out.println("    } finally {");
        System.out.println("        // 释放锁（Lua脚本）");
        System.out.println("        String script = \"if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end\";");
        System.out.println("        redisTemplate.execute(script, lockKey, lockValue);");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```");
    }

    /**
     * 主方法 - 运行演示
     */
    public static void main(String[] args) throws InterruptedException {
        demonstrateDistributedLock();
        System.out.println("\n");
        demonstrateRetryMechanism();
        System.out.println("\n");
        demonstrateBestPractices();
    }
}
