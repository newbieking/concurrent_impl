package com.example.concurrent_impl.highconcurrency.lock.service;

import com.example.concurrent_impl.entity.User;
import com.example.concurrent_impl.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 【企业级】悲观锁服务
 *
 * 【业务背景】
 * 在高冲突场景中，多个线程同时修改同一数据，
 * 使用悲观锁可以保证数据的一致性。
 *
 * 【实现原理】
 * 悲观锁假设会发生冲突，在操作数据前先获取锁，
 * 其他线程必须等待锁释放才能操作数据。
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PessimisticLockService {

    private final UserMapper userMapper;
    private final RedisLockService redisLockService;

    /**
     * 使用悲观锁扣减余额
     *
     * @param userId 用户ID
     * @param amount 扣减金额
     * @return 是否扣减成功
     */
    public boolean deductBalance(Long userId, BigDecimal amount) {
        String lockKey = "balance:" + userId;

        return redisLockService.executeWithLock(lockKey, 10, () -> {
            // 1. 查询用户信息
            User user = userMapper.findById(userId).orElse(null);
            if (user == null) {
                log.error("用户不存在: userId={}", userId);
                return false;
            }

            // 2. 检查余额
            if (user.getBalance().compareTo(amount) < 0) {
                log.warn("余额不足: userId={}, balance={}, requested={}", userId, user.getBalance(), amount);
                return false;
            }

            // 3. 扣减余额（使用乐观锁）
            int rows = userMapper.deductBalance(userId, amount, user.getVersion());
            if (rows > 0) {
                log.info("余额扣减成功: userId={}, amount={}, newBalance={}", userId, amount, user.getBalance().subtract(amount));
                return true;
            }

            log.warn("余额扣减失败（版本冲突）: userId={}", userId);
            return false;
        });
    }

    /**
     * 转账操作（使用分布式锁防死锁）
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

        String lockKey1 = "balance:" + firstId;
        String lockKey2 = "balance:" + secondId;

        String lockValue1 = redisLockService.tryLock(lockKey1, 10);
        if (lockValue1 == null) {
            log.error("获取锁失败: lockKey={}", lockKey1);
            return false;
        }

        try {
            String lockValue2 = redisLockService.tryLock(lockKey2, 10);
            if (lockValue2 == null) {
                log.error("获取锁失败: lockKey={}", lockKey2);
                return false;
            }

            try {
                // 执行转账
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

        // 4. 扣减转出用户余额
        int rows = userMapper.deductBalance(fromUserId, amount, fromUser.getVersion());
        if (rows == 0) {
            log.warn("转出余额扣减失败: userId={}", fromUserId);
            return false;
        }

        // 5. 增加转入用户余额
        rows = userMapper.addBalance(toUserId, amount, toUser.getVersion());
        if (rows == 0) {
            log.warn("转入余额增加失败: userId={}", toUserId);
            // 需要回滚转出操作（实际项目中使用事务）
            throw new RuntimeException("转账失败");
        }

        log.info("转账成功: fromUserId={}, toUserId={}, amount={}", fromUserId, toUserId, amount);
        return true;
    }
}
