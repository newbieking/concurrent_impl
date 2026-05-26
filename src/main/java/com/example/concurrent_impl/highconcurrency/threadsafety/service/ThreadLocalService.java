package com.example.concurrent_impl.highconcurrency.threadsafety.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【企业级】ThreadLocal服务
 *
 * 【业务背景】
 * 在多线程环境下，有些变量需要每个线程独立一份，如：
 * 1. 用户会话信息
 * 2. 请求上下文
 * 3. 日期格式化工具
 *
 * 【实现原理】
 * ThreadLocal为每个线程提供一个独立的变量副本，
 * 线程之间互不影响，实现线程隔离。
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
public class ThreadLocalService {

    /**
     * 用户上下文ThreadLocal
     *
     * 【使用场景】
     * 在请求处理过程中，保存当前登录用户信息
     */
    private static final ThreadLocal<String> userContext = new ThreadLocal<>();

    /**
     * 请求ID ThreadLocal
     *
     * 【使用场景】
     * 用于日志追踪，同一个请求的所有日志使用相同的请求ID
     */
    private static final ThreadLocal<String> requestId = new ThreadLocal<>();

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
     * 设置用户信息
     *
     * @param username 用户名
     */
    public void setUser(String username) {
        userContext.set(username);
        log.debug("设置用户上下文: username={}", username);
    }

    /**
     * 获取用户信息
     *
     * @return 用户名
     */
    public String getUser() {
        return userContext.get();
    }

    /**
     * 清理用户信息
     *
     * 【重要】必须在请求结束时调用，防止内存泄漏
     */
    public void clearUser() {
        userContext.remove();
        log.debug("清理用户上下文");
    }

    /**
     * 设置请求ID
     *
     * @param id 请求ID
     */
    public void setRequestId(String id) {
        requestId.set(id);
    }

    /**
     * 获取请求ID
     *
     * @return 请求ID
     */
    public String getRequestId() {
        return requestId.get();
    }

    /**
     * 清理请求ID
     */
    public void clearRequestId() {
        requestId.remove();
    }

    /**
     * 格式化日期（线程安全）
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public String formatDate(Date date) {
        return dateFormat.get().format(date);
    }

    /**
     * 清理所有ThreadLocal
     *
     * 【使用场景】
     * 请求结束时统一清理
     */
    public void clearAll() {
        clearUser();
        clearRequestId();
    }
}
