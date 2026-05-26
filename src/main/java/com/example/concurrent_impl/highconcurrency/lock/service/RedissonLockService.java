package com.example.concurrent_impl.highconcurrency.lock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 【企业级】Redisson分布式锁服务
 *
 * 【业务背景】
 * 在分布式系统中，多个服务实例需要竞争同一资源时，
 * 需要使用分布式锁来保证同一时刻只有一个实例能执行。
 *
 * 【Redisson优势】
 * 1. 支持可重入锁：同一线程可以多次获取锁
 * 2. 支持锁续期：Watch Dog机制自动续期
 * 3. 支持公平锁：按请求顺序获取锁
 * 4. 支持读写锁：读锁共享，写锁独占
 * 5. 支持联锁：同时获取多个锁
 * 6. 支持红锁：Redis集群环境下的分布式锁
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonLockService {

    private final RedissonClient redissonClient;

    /**
     * 获取可重入锁
     *
     * 【特点】
     * 1. 同一线程可以多次获取锁
     * 2. 支持锁续期（Watch Dog）
     * 3. 非公平锁
     *
     * @param lockKey 锁的key
     * @return RLock对象
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 获取公平锁
     *
     * 【特点】
     * 1. 按请求顺序获取锁
     * 2. 避免线程饥饿
     * 3. 性能略低于非公平锁
     *
     * @param lockKey 锁的key
     * @return RLock对象
     */
    public RLock getFairLock(String lockKey) {
        return redissonClient.getFairLock(lockKey);
    }

    /**
     * 获取读写锁
     *
     * 【特点】
     * 1. 读锁：多个线程可以同时持有
     * 2. 写锁：独占锁
     * 3. 读写互斥
     *
     * @param lockKey 锁的key
     * @return RReadWriteLock对象
     */
    public RReadWriteLock getReadWriteLock(String lockKey) {
        return redissonClient.getReadWriteLock(lockKey);
    }

    /**
     * 尝试获取锁（带超时）
     *
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param leaseTime 持有时间
     * @param unit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            boolean success = lock.tryLock(waitTime, leaseTime, unit);
            if (success) {
                log.info("获取锁成功: lockKey={}", lockKey);
            } else {
                log.warn("获取锁失败: lockKey={}", lockKey);
            }
            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: lockKey={}", lockKey, e);
            return false;
        }
    }

    /**
     * 尝试获取锁（使用Watch Dog自动续期）
     *
     * 【Watch Dog机制】
     * 1. 默认锁过期时间30秒
     * 2. 每10秒续期一次
     * 3. 业务完成后自动释放锁
     *
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param unit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLockWithWatchDog(String lockKey, long waitTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            boolean success = lock.tryLock(waitTime, -1, unit);
            if (success) {
                log.info("获取锁成功（Watch Dog）: lockKey={}", lockKey);
            } else {
                log.warn("获取锁失败: lockKey={}", lockKey);
            }
            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: lockKey={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        RLock lock = getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("释放锁成功: lockKey={}", lockKey);
        }
    }

    /**
     * 在锁保护下执行任务
     *
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param leaseTime 持有时间
     * @param unit 时间单位
     * @param task 任务
     * @param <T> 返回值类型
     * @return 任务执行结果
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, LockTask<T> task) {
        RLock lock = getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(waitTime, leaseTime, unit);
            if (!locked) {
                throw new RuntimeException("获取锁失败: " + lockKey);
            }

            log.info("执行任务（带锁）: lockKey={}", lockKey);
            return task.execute();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁被中断: " + lockKey, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放锁: lockKey={}", lockKey);
            }
        }
    }

    /**
     * 在锁保护下执行任务（使用Watch Dog）
     *
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param unit 时间单位
     * @param task 任务
     * @param <T> 返回值类型
     * @return 任务执行结果
     */
    public <T> T executeWithWatchDog(String lockKey, long waitTime, TimeUnit unit, LockTask<T> task) {
        RLock lock = getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(waitTime, -1, unit);
            if (!locked) {
                throw new RuntimeException("获取锁失败: " + lockKey);
            }

            log.info("执行任务（Watch Dog）: lockKey={}", lockKey);
            return task.execute();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁被中断: " + lockKey, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放锁: lockKey={}", lockKey);
            }
        }
    }

    /**
     * 在读写锁保护下执行读操作
     *
     * @param lockKey 锁的key
     * @param task 读任务
     * @param <T> 返回值类型
     * @return 任务执行结果
     */
    public <T> T executeWithReadLock(String lockKey, long waitTime, TimeUnit unit, LockTask<T> task) {
        RReadWriteLock rwLock = getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();
        boolean locked = false;

        try {
            locked = readLock.tryLock(waitTime, unit);
            if (!locked) {
                throw new RuntimeException("获取读锁失败: " + lockKey);
            }

            log.info("执行读任务: lockKey={}", lockKey);
            return task.execute();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取读锁被中断: " + lockKey, e);
        } finally {
            if (locked && readLock.isHeldByCurrentThread()) {
                readLock.unlock();
                log.info("释放读锁: lockKey={}", lockKey);
            }
        }
    }

    /**
     * 在读写锁保护下执行写操作
     *
     * @param lockKey 锁的key
     * @param task 写任务
     * @param <T> 返回值类型
     * @return 任务执行结果
     */
    public <T> T executeWithWriteLock(String lockKey, long waitTime, TimeUnit unit, LockTask<T> task) {
        RReadWriteLock rwLock = getReadWriteLock(lockKey);
        RLock writeLock = rwLock.writeLock();
        boolean locked = false;

        try {
            locked = writeLock.tryLock(waitTime, unit);
            if (!locked) {
                throw new RuntimeException("获取写锁失败: " + lockKey);
            }

            log.info("执行写任务: lockKey={}", lockKey);
            return task.execute();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取写锁被中断: " + lockKey, e);
        } finally {
            if (locked && writeLock.isHeldByCurrentThread()) {
                writeLock.unlock();
                log.info("释放写锁: lockKey={}", lockKey);
            }
        }
    }

    /**
     * 检查锁是否被锁定
     *
     * @param lockKey 锁的key
     * @return 是否被锁定
     */
    public boolean isLocked(String lockKey) {
        RLock lock = getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * 检查当前线程是否持有锁
     *
     * @param lockKey 锁的key
     * @return 是否持有锁
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        RLock lock = getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 锁任务接口
     */
    @FunctionalInterface
    public interface LockTask<T> {
        T execute();
    }
}
