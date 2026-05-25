package com.example.concurrent_impl.transactionsafety.pitfall;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 【事务安全 - 死锁】死锁场景与解决方案
 *
 * 【业务背景】
 * 在并发环境下，多个事务竞争同一资源可能导致死锁，
 * 了解死锁的原因和解决方案有助于避免系统问题。
 *
 * 【死锁条件】
 * 1. 互斥：资源只能被一个事务持有
 * 2. 占有并等待：持有资源的事务等待其他资源
 * 3. 不可抢占：已持有的资源不能被其他事务抢占
 * 4. 循环等待：多个事务形成循环等待
 *
 * @author concurrent_impl
 * @date 2024
 */
public class DeadLockDemo {

    /**
     * 演示死锁场景
     */
    public static void demonstrateDeadlock() {
        System.out.println("========== 死锁场景演示 ==========");
        System.out.println();
        System.out.println("【场景】转账死锁");
        System.out.println();
        System.out.println("事务A：从账户1转账到账户2");
        System.out.println("1. 锁定账户1");
        System.out.println("2. 等待锁定账户2");
        System.out.println();
        System.out.println("事务B：从账户2转账到账户1");
        System.out.println("1. 锁定账户2");
        System.out.println("2. 等待锁定账户1");
        System.out.println();
        System.out.println("结果：A等B释放账户2，B等A释放账户1，形成死锁");
        System.out.println();
        System.out.println("【SQL示例】");
        System.out.println("-- 事务A");
        System.out.println("UPDATE account SET balance = balance - 100 WHERE id = 1; -- 锁定账户1");
        System.out.println("UPDATE account SET balance = balance + 100 WHERE id = 2; -- 等待账户2");
        System.out.println();
        System.out.println("-- 事务B");
        System.out.println("UPDATE account SET balance = balance - 100 WHERE id = 2; -- 锁定账户2");
        System.out.println("UPDATE account SET balance = balance + 100 WHERE id = 1; -- 等待账户1");
    }

    /**
     * 演示死锁解决方案
     */
    public static void demonstrateSolutions() {
        System.out.println("========== 死锁解决方案 ==========");
        System.out.println();
        System.out.println("【方案1】按固定顺序获取锁");
        System.out.println("总是先锁ID小的账户");
        System.out.println();
        System.out.println("```java");
        System.out.println("public void transfer(int fromId, int toId, BigDecimal amount) {");
        System.out.println("    int firstId = Math.min(fromId, toId);");
        System.out.println("    int secondId = Math.max(fromId, toId);");
        System.out.println("    ");
        System.out.println("    // 先锁ID小的");
        System.out.println("    lock(firstId);");
        System.out.println("    try {");
        System.out.println("        lock(secondId);");
        System.out.println("        try {");
        System.out.println("            // 执行转账");
        System.out.println("        } finally {");
        System.out.println("            unlock(secondId);");
        System.out.println("        }");
        System.out.println("    } finally {");
        System.out.println("        unlock(firstId);");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```");
        System.out.println();
        System.out.println("【方案2】使用tryLock设置超时");
        System.out.println("```java");
        System.out.println("public void transfer(int fromId, int toId, BigDecimal amount) {");
        System.out.println("    boolean locked1 = tryLock(fromId, 5, TimeUnit.SECONDS);");
        System.out.println("    if (!locked1) {");
        System.out.println("        throw new RuntimeException(\"获取锁超时\");");
        System.out.println("    }");
        System.out.println("    try {");
        System.out.println("        boolean locked2 = tryLock(toId, 5, TimeUnit.SECONDS);");
        System.out.println("        if (!locked2) {");
        System.out.println("            throw new RuntimeException(\"获取锁超时\");");
        System.out.println("        }");
        System.out.println("        try {");
        System.out.println("            // 执行转账");
        System.out.println("        } finally {");
        System.out.println("            unlock(toId);");
        System.out.println("        }");
        System.out.println("    } finally {");
        System.out.println("        unlock(fromId);");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```");
        System.out.println();
        System.out.println("【方案3】使用数据库死锁检测");
        System.out.println("MySQL会自动检测死锁，并回滚其中一个事务");
        System.out.println("-- 查看死锁信息");
        System.out.println("SHOW ENGINE INNODB STATUS;");
        System.out.println();
        System.out.println("【方案4】减少事务持有锁的时间");
        System.out.println("1. 事务尽量短");
        System.out.println("2. 避免在事务中执行耗时操作");
        System.out.println("3. 将非关键操作移到事务外");
    }

    /**
     * 演示死锁检测
     */
    public static void demonstrateDeadlockDetection() {
        System.out.println("========== 死锁检测 ==========");
        System.out.println();
        System.out.println("【MySQL死锁检测】");
        System.out.println("1. 查看死锁日志");
        System.out.println("   SHOW ENGINE INNODB STATUS;");
        System.out.println();
        System.out.println("2. 查看当前事务");
        System.out.println("   SELECT * FROM information_schema.INNODB_TRX;");
        System.out.println();
        System.out.println("3. 查看当前锁");
        System.out.println("   SELECT * FROM information_schema.INNODB_LOCKS;");
        System.out.println();
        System.out.println("4. 查看锁等待");
        System.out.println("   SELECT * FROM information_schema.INNODB_LOCK_WAITS;");
        System.out.println();
        System.out.println("【死锁预防】");
        System.out.println("1. 按固定顺序访问资源");
        System.out.println("2. 保持事务简短");
        System.out.println("3. 使用合理的索引");
        System.out.println("4. 避免大事务");
        System.out.println("5. 设置锁等待超时");
        System.out.println("   SET innodb_lock_wait_timeout = 10;");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrateDeadlock();
        System.out.println("\n");
        demonstrateSolutions();
        System.out.println("\n");
        demonstrateDeadlockDetection();
    }
}
