package com.example.concurrent_impl.highconcurrency.threadsafety;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【线程安全 - ThreadLocal】ThreadLocal使用场景与内存泄漏
 *
 * 【业务背景】
 * 在多线程环境下，有些变量需要每个线程独立一份，如：
 * 1. 用户会话信息
 * 2. 请求上下文
 * 3. 日期格式化工具
 * 4. 数据库连接
 *
 * 【实现原理】
 * ThreadLocal为每个线程提供一个独立的变量副本，
 * 线程之间互不影响，实现线程隔离。
 *
 * 【为什么这样写】
 * 1. 展示ThreadLocal的基本使用
 * 2. 演示线程隔离的效果
 * 3. 说明内存泄漏的风险和预防
 *
 * 【不遵守的后果】
 * 1. 不使用ThreadLocal：多线程共享变量导致数据混乱
 * 2. 不清理ThreadLocal：内存泄漏，可能导致OOM
 * 3. 在线程池中使用ThreadLocal不清理：数据污染
 *
 * 【正确示例】
 * 使用try-finally确保ThreadLocal被清理
 *
 * 【错误示例】
 * 使用ThreadLocal后不调用remove()
 *
 * 【实际案例】
 * 1. Spring的RequestContextHolder
 * 2. MyBatis的SqlSessionHolder
 * 3. 用户登录信息传递
 *
 * @author concurrent_impl
 * @date 2024
 */
public class ThreadLocalDemo {

    /**
     * 用户信息ThreadLocal
     *
     * 【使用场景】
     * 在请求处理过程中，保存当前登录用户信息
     * 避免在方法间传递用户对象
     */
    private static final ThreadLocal<String> userContext = new ThreadLocal<>();

    /**
     * 日期格式化ThreadLocal
     *
     * 【为什么使用ThreadLocal】
     * SimpleDateFormat是非线程安全的，
     * 使用ThreadLocal为每个线程提供独立的实例
     */
    private static final ThreadLocal<SimpleDateFormat> dateFormat =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    /**
     * 请求ID ThreadLocal
     *
     * 【使用场景】
     * 用于日志追踪，同一个请求的所有日志使用相同的请求ID
     */
    private static final ThreadLocal<String> requestId = new ThreadLocal<>();

    /**
     * 线程安全的计数器（用于演示）
     */
    private static final AtomicInteger counter = new AtomicInteger(0);

    /**
     * 设置用户信息
     *
     * @param username 用户名
     */
    public static void setUser(String username) {
        userContext.set(username);
    }

    /**
     * 获取用户信息
     *
     * @return 用户名
     */
    public static String getUser() {
        return userContext.get();
    }

    /**
     * 清理用户信息
     *
     * 【重要】必须在请求结束时调用，防止内存泄漏
     */
    public static void clearUser() {
        userContext.remove();
    }

    /**
     * 格式化日期（线程安全）
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDate(Date date) {
        // 【关键点】每个线程使用独立的SimpleDateFormat实例
        return dateFormat.get().format(date);
    }

    /**
     * 设置请求ID
     *
     * @param id 请求ID
     */
    public static void setRequestId(String id) {
        requestId.set(id);
    }

    /**
     * 获取请求ID
     *
     * @return 请求ID
     */
    public static String getRequestId() {
        return requestId.get();
    }

    /**
     * 清理请求ID
     */
    public static void clearRequestId() {
        requestId.remove();
    }

    /**
     * 演示ThreadLocal的线程隔离效果
     *
     * 【测试方法】
     * 多个线程同时设置和获取ThreadLocal变量，
     * 验证每个线程只能访问自己的数据
     */
    public static void demonstrateThreadIsolation() throws InterruptedException {
        System.out.println("========== ThreadLocal线程隔离演示 ==========");

        ExecutorService executor = Executors.newFixedThreadPool(3);
        AtomicInteger threadCounter = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            final int threadId = threadCounter.incrementAndGet();
            executor.submit(() -> {
                // 每个线程设置不同的用户
                String username = "user_" + threadId;
                setUser(username);

                // 模拟业务处理
                System.out.println(Thread.currentThread().getName() +
                        " 设置用户: " + username);

                // 模拟调用其他方法获取用户
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                String currentUser = getUser();
                System.out.println(Thread.currentThread().getName() +
                        " 获取用户: " + currentUser);

                // 验证数据隔离
                if (!username.equals(currentUser)) {
                    System.err.println("ERROR: 数据污染!");
                }

                // 【重要】清理ThreadLocal
                clearUser();
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        System.out.println();
    }

    /**
     * 演示SimpleDateFormat的线程安全问题
     *
     * 【问题】
     * SimpleDateFormat是非线程安全的，
     * 多线程共享同一个实例会导致日期格式化错误
     */
    public static void demonstrateDateFormatProblem() throws InterruptedException {
        System.out.println("========== SimpleDateFormat线程安全问题演示 ==========");

        // 【错误示例】共享SimpleDateFormat实例
        SimpleDateFormat unsafeFormat = new SimpleDateFormat("yyyy-MM-dd");
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    // 多线程同时格式化日期
                    String result = unsafeFormat.format(new Date());
                    // 验证格式是否正确
                    if (result == null || !result.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        System.out.println("共享SimpleDateFormat错误次数: " + errorCount.get());

        // 【正确示例】使用ThreadLocal
        System.out.println("\n使用ThreadLocal后的结果:");
        executor = Executors.newFixedThreadPool(10);
        errorCount.set(0);

        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    // 使用ThreadLocal获取线程独立的SimpleDateFormat
                    String result = formatDate(new Date());
                    if (result == null || !result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        System.out.println("使用ThreadLocal错误次数: " + errorCount.get());
        System.out.println();
    }

    /**
     * 演示内存泄漏风险
     *
     * 【内存泄漏原因】
     * ThreadLocal变量存储在Thread的ThreadLocalMap中，
     * 如果不调用remove()，变量会一直存在，导致内存泄漏。
     *
     * 【泄漏场景】
     * 1. 线程池中的线程会复用，ThreadLocal变量不会自动清理
     * 2. Web应用中，请求线程被回收但ThreadLocal变量还在
     */
    public static void demonstrateMemoryLeak() {
        System.out.println("========== 内存泄漏风险演示 ==========");
        System.out.println();
        System.out.println("【内存泄漏场景】");
        System.out.println("1. 线程池中的线程会复用");
        System.out.println("2. 如果不调用remove()，ThreadLocal变量会一直存在");
        System.out.println("3. 可能导致内存泄漏和数据污染");
        System.out.println();
        System.out.println("【预防措施】");
        System.out.println("1. 使用try-finally确保remove()被调用");
        System.out.println("2. 在拦截器/过滤器中统一清理");
        System.out.println("3. 使用WeakReference（但不能完全避免泄漏）");
        System.out.println();
        System.out.println("【正确代码示例】");
        System.out.println("```java");
        System.out.println("try {");
        System.out.println("    userContext.set(currentUser);");
        System.out.println("    // 业务逻辑");
        System.out.println("} finally {");
        System.out.println("    userContext.remove(); // 必须清理");
        System.out.println("}");
        System.out.println("```");
    }

    /**
     * 演示请求上下文传递
     *
     * 【使用场景】
     * 在Web应用中，将请求信息传递给整个调用链
     */
    public static void demonstrateRequestContext() throws InterruptedException {
        System.out.println("========== 请求上下文传递演示 ==========");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 0; i < 3; i++) {
            final int requestNo = i + 1;
            executor.submit(() -> {
                try {
                    // 模拟请求开始，设置请求ID
                    String reqId = "REQ_" + System.currentTimeMillis() + "_" + requestNo;
                    setRequestId(reqId);

                    System.out.println(Thread.currentThread().getName() +
                            " 开始处理请求: " + reqId);

                    // 模拟调用服务层
                    serviceMethod();

                    System.out.println(Thread.currentThread().getName() +
                            " 请求处理完成: " + reqId);

                } finally {
                    // 【重要】清理ThreadLocal
                    clearRequestId();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        System.out.println();
    }

    /**
     * 模拟服务层方法
     */
    private static void serviceMethod() {
        // 在服务层可以获取请求ID，用于日志记录
        String reqId = getRequestId();
        System.out.println("  服务层处理中，请求ID: " + reqId);

        // 模拟调用DAO层
        daoMethod();
    }

    /**
     * 模拟DAO层方法
     */
    private static void daoMethod() {
        String reqId = getRequestId();
        System.out.println("    DAO层执行SQL，请求ID: " + reqId);
    }

    /**
     * 主方法 - 运行所有演示
     */
    public static void main(String[] args) throws InterruptedException {
        // 演示线程隔离
        demonstrateThreadIsolation();

        // 演示日期格式化问题
        demonstrateDateFormatProblem();

        // 演示内存泄漏风险
        demonstrateMemoryLeak();

        // 演示请求上下文传递
        demonstrateRequestContext();
    }
}
