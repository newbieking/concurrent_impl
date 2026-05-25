package com.example.concurrent_impl.dataconsistency.strong;

/**
 * 【数据一致性 - 强一致性】强一致性实现
 *
 * 【业务背景】
 * 在资金等关键业务中，需要保证强一致性，
 * 不能出现数据不一致的情况。
 *
 * 【实现方案】
 * 1. 数据库乐观锁
 * 2. 分布式锁
 * 3. 两阶段提交
 * 4. Raft/Paxos协议
 *
 * 【为什么这样写】
 * 1. 保证数据准确性
 * 2. 避免资金损失
 * 3. 满足监管要求
 *
 * 【不遵守的后果】
 * 1. 不做强一致：资金不一致
 * 2. 使用最终一致：可能出现中间状态
 * 3. 不做校验：数据错误
 *
 * 【正确示例】
 * 使用分布式锁 + 数据库事务
 *
 * 【错误示例】
 * 只使用缓存，不落库
 *
 * 【实际案例】
 * 1. 转账
 * 2. 支付
 * 3. 退款
 *
 * @author concurrent_impl
 * @date 2024
 */
public class StrongConsistencyDemo {

    /**
     * 演示强一致性方案
     */
    public static void demonstrate() {
        System.out.println("========== 强一致性方案演示 ==========");
        System.out.println();
        System.out.println("【方案1】数据库乐观锁");
        System.out.println("适用场景：单库事务");
        System.out.println("实现方式：使用version字段");
        System.out.println();
        System.out.println("【方案2】分布式锁");
        System.out.println("适用场景：分布式环境");
        System.out.println("实现方式：Redis/Zookeeper");
        System.out.println();
        System.out.println("【方案3】两阶段提交（2PC）");
        System.out.println("适用场景：跨库事务");
        System.out.println("实现方式：事务协调者");
        System.out.println();
        System.out.println("【方案4】Raft/Paxos协议");
        System.out.println("适用场景：分布式共识");
        System.out.println("实现方式：多数派同意");
    }

    /**
     * 演示转账强一致性
     */
    public static void demonstrateTransfer() {
        System.out.println("========== 转账强一致性演示 ==========");
        System.out.println();
        System.out.println("【场景】A转账100元给B");
        System.out.println();
        System.out.println("【方案】分布式锁 + 数据库事务");
        System.out.println();
        System.out.println("```java");
        System.out.println("@Transactional(rollbackFor = Exception.class)");
        System.out.println("public void transfer(Long fromId, Long toId, BigDecimal amount) {");
        System.out.println("    // 1. 获取分布式锁（按ID顺序获取，避免死锁）");
        System.out.println("    Long firstId = Math.min(fromId, toId);");
        System.out.println("    Long secondId = Math.max(fromId, toId);");
        System.out.println("    ");
        System.out.println("    String lock1 = redisLockUtil.tryLock(\"account:\" + firstId, 10);");
        System.out.println("    try {");
        System.out.println("        String lock2 = redisLockUtil.tryLock(\"account:\" + secondId, 10);");
        System.out.println("        try {");
        System.out.println("            // 2. 检查余额");
        System.out.println("            Account from = accountMapper.selectById(fromId);");
        System.out.println("            if (from.getBalance().compareTo(amount) < 0) {");
        System.out.println("                throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);");
        System.out.println("            }");
        System.out.println("            ");
        System.out.println("            // 3. 扣减A账户（乐观锁）");
        System.out.println("            int rows = accountMapper.deductBalance(fromId, amount, from.getVersion());");
        System.out.println("            if (rows == 0) {");
        System.out.println("                throw new BusinessException(ErrorCode.DB_ERROR, \"扣减失败，请重试\");");
        System.out.println("            }");
        System.out.println("            ");
        System.out.println("            // 4. 增加B账户（乐观锁）");
        System.out.println("            Account to = accountMapper.selectById(toId);");
        System.out.println("            rows = accountMapper.addBalance(toId, amount, to.getVersion());");
        System.out.println("            if (rows == 0) {");
        System.out.println("                throw new BusinessException(ErrorCode.DB_ERROR, \"增加失败，请重试\");");
        System.out.println("            }");
        System.out.println("            ");
        System.out.println("            // 5. 记录流水");
        System.out.println("            accountLogService.save(fromId, toId, amount);");
        System.out.println("            ");
        System.out.println("        } finally {");
        System.out.println("            redisLockUtil.unlock(\"account:\" + secondId, lock2);");
        System.out.println("        }");
        System.out.println("    } finally {");
        System.out.println("        redisLockUtil.unlock(\"account:\" + firstId, lock1);");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```");
    }

    /**
     * 演示强一致性 vs 最终一致性
     */
    public static void demonstrateComparison() {
        System.out.println("========== 强一致性 vs 最终一致性 ==========");
        System.out.println();
        System.out.println("【强一致性】");
        System.out.println("优点：数据准确，不会出现不一致");
        System.out.println("缺点：性能差，可用性低");
        System.out.println("适用：资金、库存等关键业务");
        System.out.println();
        System.out.println("【最终一致性】");
        System.out.println("优点：性能好，可用性高");
        System.out.println("缺点：可能出现中间状态");
        System.out.println("适用：日志、通知等非关键业务");
        System.out.println();
        System.out.println("【选择建议】");
        System.out.println("1. 资金相关：强一致性");
        System.out.println("2. 库存相关：强一致性");
        System.out.println("3. 订单状态：强一致性");
        System.out.println("4. 日志记录：最终一致性");
        System.out.println("5. 消息通知：最终一致性");
        System.out.println("6. 数据同步：最终一致性");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrate();
        System.out.println("\n");
        demonstrateTransfer();
        System.out.println("\n");
        demonstrateComparison();
    }
}
