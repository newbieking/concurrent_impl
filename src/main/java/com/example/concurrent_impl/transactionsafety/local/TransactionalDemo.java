package com.example.concurrent_impl.transactionsafety.local;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 【事务安全 - @Transactional】声明式事务使用示例
 *
 * 【业务背景】
 * 在企业级项目中，很多业务操作需要保证事务一致性，
 * 如：订单创建需要同时扣减库存和余额。
 *
 * 【实现原理】
 * Spring通过AOP代理实现声明式事务：
 * 1. 方法执行前开启事务
 * 2. 方法正常执行完成后提交事务
 * 3. 方法抛出异常时回滚事务
 *
 * 【为什么这样写】
 * 1. 声明式事务代码简洁，无侵入
 * 2. 支持事务传播行为
 * 3. 支持事务隔离级别
 * 4. 支持超时和只读设置
 *
 * 【不遵守的后果】
 * 1. 不使用事务：数据不一致
 * 2. 事务范围太大：性能差
 * 3. 事务范围太小：数据不一致
 *
 * 【正确示例】
 * 在Service层使用@Transactional注解
 *
 * 【错误示例】
 * 在Controller层使用@Transactional
 *
 * 【实际案例】
 * 1. 订单创建（扣减库存 + 创建订单 + 扣减余额）
 * 2. 转账（扣减A账户 + 增加B账户）
 * 3. 退款（更新订单状态 + 恢复库存 + 恢复余额）
 *
 * @author concurrent_impl
 * @date 2024
 */
public class TransactionalDemo {

    /**
     * 模拟账户
     */
    @Data
    public static class Account {
        private Long id;
        private String username;
        private BigDecimal balance;
        private Integer version;
    }

    /**
     * 模拟订单
     */
    @Data
    public static class Order {
        private Long id;
        private String orderNo;
        private Long userId;
        private BigDecimal amount;
        private Integer status; // 0-待支付 1-已支付 2-已取消
    }

    /**
     * 演示@Transactional的基本使用
     */
    public static void demonstrateBasicUsage() {
        System.out.println("========== @Transactional基本使用演示 ==========");
        System.out.println();
        System.out.println("【基本用法】");
        System.out.println("@Transactional");
        System.out.println("public void createOrder(OrderRequest request) {");
        System.out.println("    // 1. 扣减库存");
        System.out.println("    productService.deductStock(request.getProductId(), request.getQuantity());");
        System.out.println("    ");
        System.out.println("    // 2. 创建订单");
        System.out.println("    Order order = orderService.create(request);");
        System.out.println("    ");
        System.out.println("    // 3. 扣减余额");
        System.out.println("    accountService.deductBalance(request.getUserId(), order.getAmount());");
        System.out.println("}");
        System.out.println();
        System.out.println("【效果】");
        System.out.println("如果任何一步失败，所有操作都会回滚");
    }

    /**
     * 演示@Transactional的属性配置
     */
    public static void demonstrateAttributes() {
        System.out.println("========== @Transactional属性配置演示 ==========");
        System.out.println();
        System.out.println("【常用属性】");
        System.out.println();
        System.out.println("1. propagation - 事务传播行为");
        System.out.println("   @Transactional(propagation = Propagation.REQUIRED)");
        System.out.println("   说明：如果当前存在事务，加入该事务；否则创建新事务");
        System.out.println();
        System.out.println("2. isolation - 事务隔离级别");
        System.out.println("   @Transactional(isolation = Isolation.READ_COMMITTED)");
        System.out.println("   说明：读已提交，避免脏读");
        System.out.println();
        System.out.println("3. timeout - 超时时间（秒）");
        System.out.println("   @Transactional(timeout = 30)");
        System.out.println("   说明：事务执行超过30秒自动回滚");
        System.out.println();
        System.out.println("4. readOnly - 是否只读");
        System.out.println("   @Transactional(readOnly = true)");
        System.out.println("   说明：只读事务，可以进行优化");
        System.out.println();
        System.out.println("5. rollbackFor - 回滚异常");
        System.out.println("   @Transactional(rollbackFor = Exception.class)");
        System.out.println("   说明：所有异常都回滚（默认只回滚RuntimeException）");
        System.out.println();
        System.out.println("6. noRollbackFor - 不回滚异常");
        System.out.println("   @Transactional(noRollbackFor = BusinessException.class)");
        System.out.println("   说明：BusinessException不回滚");
    }

    /**
     * 演示事务失效场景
     */
    public static void demonstrateFailureScenarios() {
        System.out.println("========== 事务失效场景演示 ==========");
        System.out.println();
        System.out.println("【场景1】非public方法");
        System.out.println("@Transactional");
        System.out.println("private void doSomething() { ... } // 事务失效");
        System.out.println("原因：Spring AOP只能代理public方法");
        System.out.println();
        System.out.println("【场景2】自调用");
        System.out.println("public void methodA() {");
        System.out.println("    methodB(); // 事务失效");
        System.out.println("}");
        System.out.println("@Transactional");
        System.out.println("public void methodB() { ... }");
        System.out.println("原因：自调用不经过代理");
        System.out.println();
        System.out.println("【场景3】异常被捕获");
        System.out.println("@Transactional");
        System.out.println("public void doSomething() {");
        System.out.println("    try {");
        System.out.println("        // 业务逻辑");
        System.out.println("    } catch (Exception e) {");
        System.out.println("        // 异常被吞掉，事务不会回滚");
        System.out.println("    }");
        System.out.println("}");
        System.out.println();
        System.out.println("【场景4】异常类型不匹配");
        System.out.println("@Transactional");
        System.out.println("public void doSomething() throws Exception {");
        System.out.println("    throw new Exception(\"error\"); // 不会回滚");
        System.out.println("}");
        System.out.println("原因：默认只回滚RuntimeException");
        System.out.println("解决：@Transactional(rollbackFor = Exception.class)");
        System.out.println();
        System.out.println("【场景5】数据库不支持事务");
        System.out.println("如：MyISAM引擎不支持事务");
        System.out.println();
        System.out.println("【场景6】没有被Spring管理");
        System.out.println("如：手动new的对象，没有注入到Spring容器");
    }

    /**
     * 演示编程式事务
     */
    public static void demonstrateProgrammaticTransaction() {
        System.out.println("========== 编程式事务演示 ==========");
        System.out.println();
        System.out.println("【使用场景】");
        System.out.println("1. 需要更细粒度的事务控制");
        System.out.println("2. 需要在事务中执行异步操作");
        System.out.println("3. 需要手动控制事务提交和回滚");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("@Autowired");
        System.out.println("private TransactionTemplate transactionTemplate;");
        System.out.println();
        System.out.println("public void createOrder(OrderRequest request) {");
        System.out.println("    transactionTemplate.execute(status -> {");
        System.out.println("        try {");
        System.out.println("            // 扣减库存");
        System.out.println("            productService.deductStock(request.getProductId(), request.getQuantity());");
        System.out.println("            ");
        System.out.println("            // 创建订单");
        System.out.println("            Order order = orderService.create(request);");
        System.out.println("            ");
        System.out.println("            // 扣减余额");
        System.out.println("            accountService.deductBalance(request.getUserId(), order.getAmount());");
        System.out.println("            ");
        System.out.println("            return order;");
        System.out.println("        } catch (Exception e) {");
        System.out.println("            status.setRollbackOnly(); // 手动标记回滚");
        System.out.println("            throw e;");
        System.out.println("        }");
        System.out.println("    });");
        System.out.println("}");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrateBasicUsage();
        System.out.println("\n");
        demonstrateAttributes();
        System.out.println("\n");
        demonstrateFailureScenarios();
        System.out.println("\n");
        demonstrateProgrammaticTransaction();
    }
}
