package com.example.concurrent_impl.transactionsafety.pitfall;

/**
 * 【事务安全 - 事务失效场景】常见事务问题汇总
 *
 * 【业务背景】
 * 在使用@Transactional注解时，有很多场景会导致事务失效，
 * 了解这些场景有助于避免数据不一致问题。
 *
 * @author concurrent_impl
 * @date 2024
 */
public class TransactionFailDemo {

    /**
     * 演示事务失效场景
     */
    public static void demonstrate() {
        System.out.println("========== 事务失效场景汇总 ==========");
        System.out.println();

        // 场景1：非public方法
        System.out.println("【场景1】非public方法");
        System.out.println("@Transactional");
        System.out.println("private void doSomething() { ... } // 事务失效");
        System.out.println("原因：Spring AOP基于代理，只能代理public方法");
        System.out.println("解决：将方法改为public");
        System.out.println();

        // 场景2：自调用
        System.out.println("【场景2】自调用");
        System.out.println("public void methodA() {");
        System.out.println("    methodB(); // 事务失效");
        System.out.println("}");
        System.out.println("@Transactional");
        System.out.println("public void methodB() { ... }");
        System.out.println("原因：自调用不经过代理对象");
        System.out.println("解决：1. 注入自身代理 2. 使用AopContext.currentProxy() 3. 拆分到不同类");
        System.out.println();

        // 场景3：异常被捕获
        System.out.println("【场景3】异常被捕获");
        System.out.println("@Transactional");
        System.out.println("public void doSomething() {");
        System.out.println("    try {");
        System.out.println("        // 业务逻辑");
        System.out.println("    } catch (Exception e) {");
        System.out.println("        // 异常被吞掉，事务不会回滚");
        System.out.println("        log.error(\"error\", e);");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("原因：Spring只能在抛出异常时回滚");
        System.out.println("解决：1. 不捕获异常 2. 手动回滚 TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()");
        System.out.println();

        // 场景4：异常类型不匹配
        System.out.println("【场景4】异常类型不匹配");
        System.out.println("@Transactional");
        System.out.println("public void doSomething() throws Exception {");
        System.out.println("    throw new Exception(\"error\"); // 不会回滚");
        System.out.println("}");
        System.out.println("原因：默认只回滚RuntimeException和Error");
        System.out.println("解决：@Transactional(rollbackFor = Exception.class)");
        System.out.println();

        // 场景5：数据库不支持事务
        System.out.println("【场景5】数据库不支持事务");
        System.out.println("如：MySQL的MyISAM引擎不支持事务");
        System.out.println("解决：使用InnoDB引擎");
        System.out.println();

        // 场景6：没有被Spring管理
        System.out.println("【场景6】没有被Spring管理");
        System.out.println("UserService userService = new UserService(); // 不走代理");
        System.out.println("userService.create(); // 事务失效");
        System.out.println("原因：手动创建的对象没有被Spring管理");
        System.out.println("解决：使用@Autowired注入");
        System.out.println();

        // 场景7：多线程调用
        System.out.println("【场景7】多线程调用");
        System.out.println("@Transactional");
        System.out.println("public void doSomething() {");
        System.out.println("    new Thread(() -> {");
        System.out.println("        // 在新线程中操作数据库，不在同一个事务中");
        System.out.println("    }).start();");
        System.out.println("}");
        System.out.println("原因：新线程不在原事务上下文中");
        System.out.println("解决：1. 使用编程式事务 2. 使用分布式事务");
        System.out.println();

        // 场景8：传播行为设置不当
        System.out.println("【场景8】传播行为设置不当");
        System.out.println("@Transactional(propagation = Propagation.NOT_SUPPORTED)");
        System.out.println("public void doSomething() { ... }");
        System.out.println("原因：NOT_SUPPORTED以非事务方式执行");
        System.out.println("解决：根据业务选择合适的传播行为");
        System.out.println();

        // 场景9：rollbackFor设置不当
        System.out.println("【场景9】rollbackFor设置不当");
        System.out.println("@Transactional(rollbackFor = BusinessException.class)");
        System.out.println("public void doSomething() {");
        System.out.println("    throw new RuntimeException(\"error\"); // 不会回滚");
        System.out.println("}");
        System.out.println("原因：只对BusinessException回滚");
        System.out.println("解决：@Transactional(rollbackFor = Exception.class)");
    }

    /**
     * 演示最佳实践
     */
    public static void demonstrateBestPractices() {
        System.out.println("========== 事务最佳实践 ==========");
        System.out.println();
        System.out.println("【推荐配置】");
        System.out.println("@Transactional(");
        System.out.println("    propagation = Propagation.REQUIRED,");
        System.out.println("    isolation = Isolation.READ_COMMITTED,");
        System.out.println("    timeout = 30,");
        System.out.println("    rollbackFor = Exception.class");
        System.out.println(")");
        System.out.println();
        System.out.println("【原则】");
        System.out.println("1. 事务方法必须是public");
        System.out.println("2. 避免自调用");
        System.out.println("3. 不要捕获异常（或手动回滚）");
        System.out.println("4. 设置rollbackFor = Exception.class");
        System.out.println("5. 事务范围尽量小");
        System.out.println("6. 避免长事务");
        System.out.println("7. 查询方法设置readOnly = true");
        System.out.println();
        System.out.println("【长事务优化】");
        System.out.println("1. 将查询移到事务外");
        System.out.println("2. 使用编程式事务控制范围");
        System.out.println("3. 异步处理非关键操作");
        System.out.println("4. 设置合理的超时时间");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrate();
        System.out.println("\n");
        demonstrateBestPractices();
    }
}
