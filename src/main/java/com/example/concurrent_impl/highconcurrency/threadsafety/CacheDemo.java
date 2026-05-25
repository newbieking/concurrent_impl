package com.example.concurrent_impl.highconcurrency.threadsafety;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 【线程安全 - 缓存】线程安全的本地缓存实现
 *
 * 【业务背景】
 * 在实际项目中，经常需要使用本地缓存来提高性能，
 * 如缓存配置信息、热点数据、计算结果等。
 * 多线程环境下，缓存的读写需要保证线程安全。
 *
 * 【实现原理】
 * 1. HashMap：非线程安全，高并发下可能出现死循环
 * 2. Hashtable：线程安全，但性能差（全表锁）
 * 3. ConcurrentHashMap：分段锁，高并发下性能最优
 * 4. ReadWriteLock：读写分离，读多写少场景最优
 *
 * 【为什么这样写】
 * 1. 对比不同缓存实现的线程安全性和性能
 * 2. 展示读写锁的使用场景
 * 3. 提供实际场景的选择建议
 *
 * 【不遵守的后果】
 * 1. 使用HashMap：高并发下可能出现死循环、数据丢失
 * 2. 使用Hashtable：性能差，成为瓶颈
 * 3. 不使用读写锁：读多写少场景性能浪费
 *
 * 【正确示例】
 * 读多写少场景使用ReadWriteLock
 * 一般场景使用ConcurrentHashMap
 *
 * 【错误示例】
 * 使用HashMap或Collections.synchronizedMap
 *
 * 【实际案例】
 * 1. 缓存系统配置
 * 2. 缓存热点商品信息
 * 3. 缓存用户会话信息
 *
 * @author concurrent_impl
 * @date 2024
 */
public class CacheDemo {

    /**
     * 使用ConcurrentHashMap实现的线程安全缓存
     *
     * 【特点】
     * 1. 线程安全：内部使用分段锁
     * 2. 高性能：读操作无锁，写操作只锁对应的段
     * 3. 支持高并发读写
     *
     * 【适用场景】
     * 读写都比较频繁的场景
     */
    private static final ConcurrentHashMap<String, Object> concurrentCache = new ConcurrentHashMap<>();

    /**
     * 使用ReadWriteLock实现的缓存
     *
     * 【特点】
     * 1. 读写分离：多个读线程可以同时访问
     * 2. 写操作独占：写时阻塞所有读写
     * 3. 适合读多写少的场景
     *
     * 【适用场景】
     * 读操作远多于写操作的场景
     */
    private static final Map<String, Object> rwLockCache = new ConcurrentHashMap<>();
    private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * 使用ConcurrentHashMap的put操作
     *
     * 【为什么线程安全】
     * ConcurrentHashMap内部使用CAS + synchronized实现
     * 1. 对于空桶，使用CAS插入
     * 2. 对于非空桶，使用synchronized锁住链表头
     *
     * @param key 缓存key
     * @param value 缓存value
     */
    public static void putConcurrent(String key, Object value) {
        // 【关键点】ConcurrentHashMap的put是线程安全的
        concurrentCache.put(key, value);
    }

    /**
     * 使用ConcurrentHashMap的get操作
     *
     * 【为什么线程安全】
     * get操作是无锁的，利用了volatile的可见性
     *
     * @param key 缓存key
     * @return 缓存value
     */
    public static Object getConcurrent(String key) {
        // 【关键点】ConcurrentHashMap的get是无锁的
        return concurrentCache.get(key);
    }

    /**
     * 使用ReadWriteLock的put操作
     *
     * 【为什么使用写锁】
     * 写操作需要独占，防止并发写导致数据不一致
     *
     * @param key 缓存key
     * @param value 缓存value
     */
    public static void putWithRWLock(String key, Object value) {
        // 【关键点】获取写锁
        rwLock.writeLock().lock();
        try {
            rwLockCache.put(key, value);
        } finally {
            // 【重要】必须在finally中释放锁
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 使用ReadWriteLock的get操作
     *
     * 【为什么使用读锁】
     * 读操作可以并发，使用读锁允许多个线程同时读
     *
     * @param key 缓存key
     * @return 缓存value
     */
    public static Object getWithRWLock(String key) {
        // 【关键点】获取读锁
        rwLock.readLock().lock();
        try {
            return rwLockCache.get(key);
        } finally {
            // 【重要】必须在finally中释放锁
            rwLock.readLock().unlock();
        }
    }

    /**
     * 缓存初始化（模拟从数据库加载数据）
     *
     * @param size 缓存大小
     */
    public static void initCache(int size) {
        concurrentCache.clear();
        rwLockCache.clear();
        for (int i = 0; i < size; i++) {
            String key = "key_" + i;
            Object value = "value_" + i;
            concurrentCache.put(key, value);
            rwLockCache.put(key, value);
        }
    }

    /**
     * 性能对比测试
     *
     * 【测试方法】
     * 模拟读多写少的场景，比较不同实现的性能
     *
     * @param cacheSize 缓存大小
     * @param readCount 每个线程读取次数
     * @param writeCount 每个线程写入次数
     * @param threadCount 线程数
     */
    public static void performanceComparison(int cacheSize, int readCount, int writeCount, int threadCount)
            throws InterruptedException {
        System.out.println("========== 缓存性能对比测试 ==========");
        System.out.println("缓存大小: " + cacheSize + ", 线程数: " + threadCount);
        System.out.println("读次数: " + readCount + ", 写次数: " + writeCount);
        System.out.println();

        // 初始化缓存
        initCache(cacheSize);

        // 测试ConcurrentHashMap
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch1 = new CountDownLatch(threadCount);

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            final int conThreadId = i;
            executor.submit(() -> {
                try {
                    // 读操作
                    for (int j = 0; j < readCount; j++) {
                        String key = "key_" + (j % cacheSize);
                        getConcurrent(key);
                    }
                    // 写操作
                    for (int j = 0; j < writeCount; j++) {
                        String key = "key_" + (j % cacheSize);
                        putConcurrent(key, "thread_" + conThreadId + "_value_" + j);
                    }
                } finally {
                    latch1.countDown();
                }
            });
        }
        latch1.await();
        long concurrentTime = System.currentTimeMillis() - start;
        System.out.println("ConcurrentHashMap: 耗时=" + concurrentTime + "ms");

        // 测试ReadWriteLock
        initCache(cacheSize);
        CountDownLatch latch2 = new CountDownLatch(threadCount);

        start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            final int rwThreadId = i;
            executor.submit(() -> {
                try {
                    // 读操作
                    for (int j = 0; j < readCount; j++) {
                        String key = "key_" + (j % cacheSize);
                        getWithRWLock(key);
                    }
                    // 写操作
                    for (int j = 0; j < writeCount; j++) {
                        String key = "key_" + (j % cacheSize);
                        putWithRWLock(key, "thread_" + rwThreadId + "_value_" + j);
                    }
                } finally {
                    latch2.countDown();
                }
            });
        }
        latch2.await();
        long rwLockTime = System.currentTimeMillis() - start;
        System.out.println("ReadWriteLock: 耗时=" + rwLockTime + "ms");

        executor.shutdown();

        System.out.println();
        System.out.println("【结论】");
        System.out.println("1. ConcurrentHashMap在读写都频繁时性能更好");
        System.out.println("2. ReadWriteLock在读多写少时性能更好");
        System.out.println("3. 实际选择需要根据业务场景进行测试");
    }

    /**
     * 主方法 - 运行性能对比测试
     */
    public static void main(String[] args) throws InterruptedException {
        // 场景1：读多写少（读:写 = 100:1）
        System.out.println("【场景1】读多写少（100线程，10000读，100写）");
        performanceComparison(1000, 10000, 100, 100);

        System.out.println("\n");

        // 场景2：读写均衡（读:写 = 10:1）
        System.out.println("【场景2】读写均衡（100线程，1000读，100写）");
        performanceComparison(1000, 1000, 100, 100);
    }
}
