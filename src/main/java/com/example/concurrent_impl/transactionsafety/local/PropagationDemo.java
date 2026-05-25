package com.example.concurrent_impl.transactionsafety.local;

/**
 * 【事务安全 - 传播行为】事务传播行为演示
 *
 * 【业务背景】
 * 在复杂业务中，方法之间会相互调用，
 * 事务传播行为定义了事务如何在方法之间传递。
 *
 * 【传播行为类型】
 * 1. REQUIRED：如果当前存在事务，加入该事务；否则创建新事务
 * 2. REQUIRES_NEW：总是创建新事务，如果当前存在事务，挂起当前事务
 * 3. SUPPORTS：如果当前存在事务，加入该事务；否则以非事务方式执行
 * 4. NOT_SUPPORTED：以非事务方式执行，如果当前存在事务，挂起当前事务
 * 5. MANDATORY：如果当前存在事务，加入该事务；否则抛出异常
 * 6. NEVER：以非事务方式执行，如果当前存在事务，抛出异常
 * 7. NESTED：如果当前存在事务，创建嵌套事务；否则创建新事务
 *
 * 【为什么需要了解传播行为】
 * 1. 不同场景需要不同的传播行为
 * 2. 错误的传播行为可能导致数据不一致
 * 3. 理解传播行为有助于设计事务边界
 *
 * @author concurrent_impl
 * @date 2024
 */
public class PropagationDemo {

    /**
     * 演示REQUIRED传播行为
     *
     * 【特点】
     * 1. 如果当前存在事务，加入该事务
     * 2. 如果当前不存在事务，创建新事务
     * 3. 最常用的传播行为
     *
     * 【使用场景】
     * 大部分业务方法
     */
    public static void demonstrateRequired() {
        System.out.println("========== REQUIRED传播行为演示 ==========");
        System.out.println();
        System.out.println("【场景】");
        System.out.println("methodA() {");
        System.out.println("    // 开启事务");
        System.out.println("    methodB(); // 加入methodA的事务");
        System.out.println("    // 提交事务");
        System.out.println("}");
        System.out.println();
        System.out.println("【特点】");
        System.out.println("1. methodB和methodA在同一个事务中");
        System.out.println("2. methodB失败，methodA也会回滚");
        System.out.println("3. 最常用的传播行为");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("@Transactional(propagation = Propagation.REQUIRED)");
        System.out.println("public void methodB() { ... }");
    }

    /**
     * 演示REQUIRES_NEW传播行为
     *
     * 【特点】
     * 1. 总是创建新事务
     * 2. 如果当前存在事务，挂起当前事务
     * 3. 新事务独立提交和回滚
     *
     * 【使用场景】
     * 日志记录、审计等需要独立事务的场景
     */
    public static void demonstrateRequiresNew() {
        System.out.println("========== REQUIRES_NEW传播行为演示 ==========");
        System.out.println();
        System.out.println("【场景】");
        System.out.println("methodA() {");
        System.out.println("    // 开启事务1");
        System.out.println("    methodB(); // 挂起事务1，开启事务2");
        System.out.println("    // 事务1继续");
        System.out.println("    // 提交事务1");
        System.out.println("}");
        System.out.println();
        System.out.println("【特点】");
        System.out.println("1. methodB在独立的事务中执行");
        System.out.println("2. methodB失败，不影响methodA的事务");
        System.out.println("3. 适用于日志记录、审计等场景");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("@Transactional(propagation = Propagation.REQUIRES_NEW)");
        System.out.println("public void saveLog() { ... }");
    }

    /**
     * 演示NESTED传播行为
     *
     * 【特点】
     * 1. 如果当前存在事务，创建嵌套事务
     * 2. 嵌套事务可以独立回滚
     * 3. 外层事务回滚，嵌套事务也会回滚
     *
     * 【使用场景】
     * 需要部分回滚的场景
     */
    public static void demonstrateNested() {
        System.out.println("========== NESTED传播行为演示 ==========");
        System.out.println();
        System.out.println("【场景】");
        System.out.println("methodA() {");
        System.out.println("    // 开启事务");
        System.out.println("    try {");
        System.out.println("        methodB(); // 创建嵌套事务（保存点）");
        System.out.println("    } catch (Exception e) {");
        System.out.println("        // methodB回滚，但methodA继续");
        System.out.println("    }");
        System.out.println("    // 继续其他操作");
        System.out.println("    // 提交事务");
        System.out.println("}");
        System.out.println();
        System.out.println("【特点】");
        System.out.println("1. 嵌套事务可以独立回滚");
        System.out.println("2. 外层事务回滚，嵌套事务也会回滚");
        System.out.println("3. 适用于需要部分回滚的场景");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("@Transactional(propagation = Propagation.NESTED)");
        System.out.println("public void methodB() { ... }");
    }

    /**
     * 演示传播行为选择指南
     */
    public static void demonstrateSelectionGuide() {
        System.out.println("========== 传播行为选择指南 ==========");
        System.out.println();
        System.out.println("【选择原则】");
        System.out.println("1. REQUIRED：大部分场景使用，如果当前有事务就加入");
        System.out.println("2. REQUIRES_NEW：需要独立事务的场景，如日志记录");
        System.out.println("3. NESTED：需要部分回滚的场景");
        System.out.println("4. SUPPORTS：查询方法，可以有事务也可以没有");
        System.out.println("5. NOT_SUPPORTED：不需要事务的场景");
        System.out.println("6. MANDATORY：必须在事务中调用");
        System.out.println("7. NEVER：不能在事务中调用");
        System.out.println();
        System.out.println("【常见错误】");
        System.out.println("1. 查询方法使用REQUIRED：不必要的事务开销");
        System.out.println("2. 日志记录使用REQUIRED：日志失败导致业务回滚");
        System.out.println("3. 嵌套调用使用REQUIRES_NEW：事务频繁创建和销毁");
        System.out.println();
        System.out.println("【最佳实践】");
        System.out.println("1. 查询方法使用@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)");
        System.out.println("2. 日志记录使用@Transactional(propagation = Propagation.REQUIRES_NEW)");
        System.out.println("3. 业务方法使用@Transactional(propagation = Propagation.REQUIRED)");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrateRequired();
        System.out.println("\n");
        demonstrateRequiresNew();
        System.out.println("\n");
        demonstrateNested();
        System.out.println("\n");
        demonstrateSelectionGuide();
    }
}
