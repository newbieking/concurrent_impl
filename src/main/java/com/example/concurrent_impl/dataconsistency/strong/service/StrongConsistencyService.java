package com.example.concurrent_impl.dataconsistency.strong.service;

import com.example.concurrent_impl.entity.User;
import com.example.concurrent_impl.mapper.UserMapper;
import com.example.concurrent_impl.highconcurrency.lock.service.RedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 【企业级】强一致性服务
 *
 * 【业务背景】
 * 在资金等关键业务中，需要保证强一致性，
 * 不能出现数据不一致的情况。
 *
 * 【实现方案】
 * 1. 分布式锁：防止并发操作
 * 2. 数据库乐观锁：保证数据一致性
 * 3. 事务：保证操作原子性
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrongConsistencyService {

    private final UserMapper userMapper;
    private final RedisLockService redisLockService;

    /**
     * 转账操作（强一致性）
     *
     * 【实现原理】
     * 1. 获取分布式锁（按ID顺序，避免死锁）
     * 2. 检查余额
     * 3. 扣减转出账户（乐观锁）
     * 4. 增加转入账户（乐观锁）
     * 5. 记录流水
     * 6. 释放锁
     *
     * @param fromUserId 转出用户ID
     * @param toUserId 转入用户ID
     * @param amount 转账金额
     * @return 是否转账成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean transfer(Long fromUserId, Long toUserId, BigDecimal amount) {
        // 按ID顺序获取锁，避免死锁
        Long firstId = Math.min(fromUserId, toUserId);
        Long secondId = Math.max(fromUserId, toUserId);

        String lockKey1 = "transfer:" + firstId;
        String lockKey2 = "transfer:" + secondId;

        String lockValue1 = redisLockService.tryLock(lockKey1, 30);
        if (lockValue1 == null) {
            log.error("获取锁失败: lockKey={}", lockKey1);
            return false;
        }

        try {
            String lockValue2 = redisLockService.tryLock(lockKey2, 30);
            if (lockValue2 == null) {
                log.error("获取锁失败: lockKey={}", lockKey2);
                return false;
            }

            try {
                return doTransfer(fromUserId, toUserId, amount);
            } finally {
                redisLockService.unlock(lockKey2, lockValue2);
            }
        } finally {
            redisLockService.unlock(lockKey1, lockValue1);
        }
    }

    /**
     * 执行转账（内部方法）
     */
    private boolean doTransfer(Long fromUserId, Long toUserId, BigDecimal amount) {
        // 1. 查询转出用户
        User fromUser = userMapper.findById(fromUserId).orElse(null);
        if (fromUser == null) {
            log.error("转出用户不存在: userId={}", fromUserId);
            return false;
        }

        // 2. 检查余额
        if (fromUser.getBalance().compareTo(amount) < 0) {
            log.warn("余额不足: userId={}, balance={}, requested={}", fromUserId, fromUser.getBalance(), amount);
            return false;
        }

        // 3. 查询转入用户
        User toUser = userMapper.findById(toUserId).orElse(null);
        if (toUser == null) {
            log.error("转入用户不存在: userId={}", toUserId);
            return false;
        }

        // 4. 扣减转出用户余额（乐观锁）
        int rows = userMapper.deductBalance(fromUserId, amount, fromUser.getVersion());
        if (rows == 0) {
            log.warn("转出余额扣减失败（版本冲突）: userId={}", fromUserId);
            return false;
        }

        // 5. 重新查询转入用户（获取最新版本号）
        toUser = userMapper.findById(toUserId).orElse(null);
        if (toUser == null) {
            throw new RuntimeException("转入用户不存在");
        }

        // 6. 增加转入用户余额（乐观锁）
        rows = userMapper.addBalance(toUserId, amount, toUser.getVersion());
        if (rows == 0) {
            log.warn("转入余额增加失败（版本冲突）: userId={}", toUserId);
            throw new RuntimeException("转账失败");
        }

        log.info("转账成功: fromUserId={}, toUserId={}, amount={}", fromUserId, toUserId, amount);
        return true;
    }
}
