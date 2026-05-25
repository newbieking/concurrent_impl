package com.example.concurrent_impl.transactionsafety.distributed;

/**
 * 【事务安全 - TCC模式】分布式事务实现
 *
 * 【业务背景】
 * TCC（Try-Confirm-Cancel）是一种分布式事务模式，
 * 适用于对一致性要求较高的场景。
 *
 * 【TCC模式原理】
 * 1. Try：预留资源，检查业务条件
 * 2. Confirm：确认提交，执行业务操作
 * 3. Cancel：取消操作，释放预留资源
 *
 * 【TCC vs Saga】
 * TCC：
 * - 优点：隔离性好，不会读到中间状态
 * - 缺点：业务侵入性强，需要实现三个接口
 *
 * Saga：
 * - 优点：业务侵入性小
 * - 缺点：隔离性差，可能读到中间状态
 *
 * 【为什么这样写】
 * 1. 支持资源预留
 * 2. 隔离性好
 * 3. 适用于高一致性场景
 *
 * 【不遵守的后果】
 * 1. 不预留资源：并发问题
 * 2. 不处理空回滚：悬挂问题
 * 3. 不处理幂等：重复提交问题
 *
 * 【正确示例】
 * 使用TCC模式，实现三个阶段
 *
 * 【错误示例】
 * 只实现Try和Confirm，不实现Cancel
 *
 * 【实际案例】
 * 1. 转账（冻结金额 -> 扣减 -> 解冻）
 * 2. 库存扣减（预扣 -> 确认 -> 释放）
 * 3. 优惠券使用（锁定 -> 核销 -> 解锁）
 *
 * @author concurrent_impl
 * @date 2024
 */
public class TCCDemo {

    /**
     * TCC接口定义
     */
    public interface TCCAction<T> {

        /**
         * Try阶段：预留资源
         *
         * @param params 参数
         * @return 是否成功
         */
        boolean tryAction(T params);

        /**
         * Confirm阶段：确认提交
         *
         * @param params 参数
         * @return 是否成功
         */
        boolean confirmAction(T params);

        /**
         * Cancel阶段：取消操作
         *
         * @param params 参数
         * @return 是否成功
         */
        boolean cancelAction(T params);
    }

    /**
     * 账户余额TCC实现
     */
    public static class AccountTCCAction implements TCCAction<AccountTCCAction.AccountParams> {

        @lombok.Data
        @lombok.AllArgsConstructor
        public static class AccountParams {
            private Long userId;
            private java.math.BigDecimal amount;
        }

        @Override
        public boolean tryAction(AccountParams params) {
            System.out.println("  [Try] 冻结金额: 用户=" + params.getUserId() +
                    ", 金额=" + params.getAmount());
            // 检查余额是否充足
            // 冻结金额
            return true;
        }

        @Override
        public boolean confirmAction(AccountParams params) {
            System.out.println("  [Confirm] 扣减金额: 用户=" + params.getUserId() +
                    ", 金额=" + params.getAmount());
            // 实际扣减金额
            // 释放冻结
            return true;
        }

        @Override
        public boolean cancelAction(AccountParams params) {
            System.out.println("  [Cancel] 解冻金额: 用户=" + params.getUserId() +
                    ", 金额=" + params.getAmount());
            // 释放冻结金额
            return true;
        }
    }

    /**
     * 库存TCC实现
     */
    public static class StockTCCAction implements TCCAction<StockTCCAction.StockParams> {

        @lombok.Data
        @lombok.AllArgsConstructor
        public static class StockParams {
            private Long productId;
            private Integer quantity;
        }

        @Override
        public boolean tryAction(StockParams params) {
            System.out.println("  [Try] 预扣库存: 商品=" + params.getProductId() +
                    ", 数量=" + params.getQuantity());
            // 检查库存是否充足
            // 预扣库存（锁定）
            return true;
        }

        @Override
        public boolean confirmAction(StockParams params) {
            System.out.println("  [Confirm] 确认扣减: 商品=" + params.getProductId() +
                    ", 数量=" + params.getQuantity());
            // 实际扣减库存
            // 释放锁定
            return true;
        }

        @Override
        public boolean cancelAction(StockParams params) {
            System.out.println("  [Cancel] 释放库存: 商品=" + params.getProductId() +
                    ", 数量=" + params.getQuantity());
            // 释放锁定库存
            return true;
        }
    }

    /**
     * TCC事务协调者
     */
    @SuppressWarnings("unchecked")
    public static class TCCCoordinator {

        /**
         * 执行TCC事务
         */
        public static <T> boolean execute(String transactionId,
                                       TCCAction<T> tryAction, T tryParams,
                                       TCCAction<T> confirmAction, T confirmParams,
                                       TCCAction<T> cancelAction, T cancelParams) {
            System.out.println("开始TCC事务: " + transactionId);

            try {
                // Try阶段
                System.out.println("Try阶段:");
                boolean tryResult = tryAction.tryAction(tryParams);
                if (!tryResult) {
                    System.out.println("Try失败，执行Cancel:");
                    cancelAction.cancelAction(cancelParams);
                    return false;
                }

                // Confirm阶段
                System.out.println("Confirm阶段:");
                boolean confirmResult = confirmAction.confirmAction(confirmParams);
                if (!confirmResult) {
                    System.out.println("Confirm失败，执行Cancel:");
                    cancelAction.cancelAction(cancelParams);
                    return false;
                }

                System.out.println("TCC事务成功: " + transactionId);
                return true;

            } catch (Exception e) {
                System.out.println("TCC事务异常，执行Cancel:");
                cancelAction.cancelAction(cancelParams);
                return false;
            }
        }
    }

    /**
     * 演示TCC模式
     */
    public static void demonstrate() {
        System.out.println("========== TCC模式演示 ==========");
        System.out.println();
        System.out.println("【场景】电商下单流程");
        System.out.println("1. Try: 冻结余额 + 预扣库存");
        System.out.println("2. Confirm: 扣减余额 + 确认库存");
        System.out.println("3. Cancel: 解冻余额 + 释放库存");
        System.out.println();

        // 创建TCC Action
        AccountTCCAction accountAction = new AccountTCCAction();
        StockTCCAction stockAction = new StockTCCAction();

        // 创建参数
        AccountTCCAction.AccountParams accountParams =
                new AccountTCCAction.AccountParams(1001L, new java.math.BigDecimal("100"));
        StockTCCAction.StockParams stockParams =
                new StockTCCAction.StockParams(2001L, 1);

        // 执行TCC事务
        System.out.println("【成功场景】");
        boolean result = TCCCoordinator.execute(
                "TX001",
                accountAction, accountParams,
                accountAction, accountParams,
                accountAction, accountParams
        );
        System.out.println("事务结果: " + (result ? "成功" : "失败"));
    }

    /**
     * 演示TCC模式的注意事项
     */
    public static void demonstrateConsiderations() {
        System.out.println("========== TCC模式注意事项 ==========");
        System.out.println();
        System.out.println("【空回滚】");
        System.out.println("问题：Try未执行，直接收到Cancel请求");
        System.out.println("原因：Try超时，协调者认为失败，发送Cancel");
        System.out.println("解决：Cancel时检查Try是否执行过，未执行则直接返回成功");
        System.out.println();
        System.out.println("【悬挂】");
        System.out.println("问题：Cancel执行后，Try才到达");
        System.out.println("原因：Try超时重试，在Cancel之后到达");
        System.out.println("解决：Try时检查Cancel是否执行过，已执行则直接返回失败");
        System.out.println();
        System.out.println("【幂等】");
        System.out.println("问题：Confirm或Cancel重复执行");
        System.out.println("原因：网络超时重试");
        System.out.println("解决：使用事务ID保证幂等性");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("@Override");
        System.out.println("public boolean cancelAction(params) {");
        System.out.println("    // 1. 检查是否空回滚");
        System.out.println("    if (!tryExecuted(params.getTransactionId())) {");
        System.out.println("        // 记录空回滚，直接返回成功");
        System.out.println("        return true;");
        System.out.println("    }");
        System.out.println("    ");
        System.out.println("    // 2. 检查是否幂等");
        System.out.println("    if (cancelExecuted(params.getTransactionId())) {");
        System.out.println("        return true; // 已执行过，直接返回");
        System.out.println("    }");
        System.out.println("    ");
        System.out.println("    // 3. 执行Cancel逻辑");
        System.out.println("    // ...");
        System.out.println("}");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrate();
        System.out.println("\n");
        demonstrateConsiderations();
    }
}
