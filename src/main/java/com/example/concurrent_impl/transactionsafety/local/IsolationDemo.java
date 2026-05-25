package com.example.concurrent_impl.transactionsafety.local;

/**
 * 【事务安全 - 隔离级别】事务隔离级别演示
 *
 * 【业务背景】
 * 在并发环境下，多个事务同时执行可能导致数据问题，
 * 事务隔离级别定义了事务之间的隔离程度。
 *
 * 【隔离级别】
 * 1. READ_UNCOMMITTED：读未提交，可能出现脏读
 * 2. READ_COMMITTED：读已提交，避免脏读
 * 3. REPEATABLE_READ：可重复读，避免脏读和不可重复读（MySQL默认）
 * 4. SERIALIZABLE：串行化，避免所有问题，但性能最差
 *
 * 【并发问题】
 * 1. 脏读：读取到其他事务未提交的数据
 * 2. 不可重复读：同一事务中，两次读取结果不同
 * 3. 幻读：同一事务中，两次查询结果集不同
 *
 * @author concurrent_impl
 * @date 2024
 */
public class IsolationDemo {

    /**
     * 演示脏读问题
     *
     * 【场景】
     * 事务A读取到事务B未提交的数据
     * 如果事务B回滚，事务A读取的数据就是无效的
     */
    public static void demonstrateDirtyRead() {
        System.out.println("========== 脏读问题演示 ==========");
        System.out.println();
        System.out.println("【场景】");
        System.out.println("时间点1: 事务A读取余额=1000");
        System.out.println("时间点2: 事务B更新余额=500（未提交）");
        System.out.println("时间点3: 事务A读取余额=500（脏读）");
        System.out.println("时间点4: 事务B回滚，余额恢复为1000");
        System.out.println("结果: 事务A读取到的500是无效数据");
        System.out.println();
        System.out.println("【解决方案】");
        System.out.println("使用READ_COMMITTED或更高隔离级别");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("@Transactional(isolation = Isolation.READ_COMMITTED)");
        System.out.println("public BigDecimal getBalance(Long userId) { ... }");
    }

    /**
     * 演示不可重复读问题
     *
     * 【场景】
     * 同一事务中，两次读取同一数据结果不同
     */
    public static void demonstrateNonRepeatableRead() {
        System.out.println("========== 不可重复读问题演示 ==========");
        System.out.println();
        System.out.println("【场景】");
        System.out.println("时间点1: 事务A读取余额=1000");
        System.out.println("时间点2: 事务B更新余额=500并提交");
        System.out.println("时间点3: 事务A读取余额=500（不可重复读）");
        System.out.println("结果: 同一事务中，两次读取结果不同");
        System.out.println();
        System.out.println("【解决方案】");
        System.out.println("使用REPEATABLE_READ或更高隔离级别");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("@Transactional(isolation = Isolation.REPEATABLE_READ)");
        System.out.println("public void transfer(Long fromId, Long toId, BigDecimal amount) { ... }");
    }

    /**
     * 演示幻读问题
     *
     * 【场景】
     * 同一事务中，两次查询结果集不同
     */
    public static void demonstratePhantomRead() {
        System.out.println("========== 幻读问题演示 ==========");
        System.out.println();
        System.out.println("【场景】");
        System.out.println("时间点1: 事务A查询订单数量=10");
        System.out.println("时间点2: 事务B插入新订单并提交");
        System.out.println("时间点3: 事务A查询订单数量=11（幻读）");
        System.out.println("结果: 同一事务中，两次查询结果集不同");
        System.out.println();
        System.out.println("【解决方案】");
        System.out.println("使用SERIALIZABLE隔离级别");
        System.out.println("或使用悲观锁 SELECT ... FOR UPDATE");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("@Transactional(isolation = Isolation.SERIALIZABLE)");
        System.out.println("public void batchProcess() { ... }");
    }

    /**
     * 演示隔离级别选择指南
     */
    public static void demonstrateSelectionGuide() {
        System.out.println("========== 隔离级别选择指南 ==========");
        System.out.println();
        System.out.println("【隔离级别对比】");
        System.out.println("┌─────────────────────┬────────┬──────────┬──────────┬──────────┐");
        System.out.println("│ 隔离级别            │ 脏读   │ 不可重复读 │ 幻读     │ 性能     │");
        System.out.println("├─────────────────────┼────────┼──────────┼──────────┼──────────┤");
        System.out.println("│ READ_UNCOMMITTED    │ 可能   │ 可能     │ 可能     │ 最好     │");
        System.out.println("│ READ_COMMITTED      │ 不会   │ 可能     │ 可能     │ 较好     │");
        System.out.println("│ REPEATABLE_READ     │ 不会   │ 不会     │ 可能     │ 一般     │");
        System.out.println("│ SERIALIZABLE        │ 不会   │ 不会     │ 不会     │ 最差     │");
        System.out.println("└─────────────────────┴────────┴──────────┴──────────┴──────────┘");
        System.out.println();
        System.out.println("【选择建议】");
        System.out.println("1. 大部分场景使用READ_COMMITTED（Oracle默认）");
        System.out.println("2. 需要一致性读使用REPEATABLE_READ（MySQL默认）");
        System.out.println("3. 对一致性要求极高使用SERIALIZABLE");
        System.out.println("4. 不建议使用READ_UNCOMMITTED");
        System.out.println();
        System.out.println("【MySQL默认隔离级别】");
        System.out.println("REPEATABLE_READ");
        System.out.println();
        System.out.println("【查看当前隔离级别】");
        System.out.println("SELECT @@transaction_isolation;");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrateDirtyRead();
        System.out.println("\n");
        demonstrateNonRepeatableRead();
        System.out.println("\n");
        demonstratePhantomRead();
        System.out.println("\n");
        demonstrateSelectionGuide();
    }
}
