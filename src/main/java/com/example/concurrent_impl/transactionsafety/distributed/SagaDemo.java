package com.example.concurrent_impl.transactionsafety.distributed;

import java.util.ArrayList;
import java.util.List;

/**
 * 【事务安全 - Saga模式】分布式事务实现
 *
 * 【业务背景】
 * 在微服务架构中，一个业务操作可能涉及多个服务，
 * 每个服务有自己的数据库，无法使用本地事务。
 *
 * 【Saga模式原理】
 * 1. 将大事务拆分为多个小事务
 * 2. 每个小事务都有对应的补偿操作
 * 3. 如果某个小事务失败，执行之前所有小事务的补偿操作
 *
 * 【Saga模式类型】
 * 1. 编排式（Choreography）：事件驱动，服务之间通过消息通信
 * 2. 编排式（Orchestration）：由一个协调者统一调度
 *
 * 【为什么这样写】
 * 1. 支持长事务
 * 2. 每个服务独立提交
 * 3. 通过补偿保证最终一致性
 *
 * 【不遵守的后果】
 * 1. 不使用分布式事务：数据不一致
 * 2. 不设计补偿操作：无法回滚
 * 3. 补偿操作失败：需要人工介入
 *
 * 【正确示例】
 * 使用Saga模式，每个操作都有补偿
 *
 * 【错误示例】
 * 跨服务调用不考虑失败情况
 *
 * 【实际案例】
 * 1. 电商下单（订单 + 库存 + 支付）
 * 2. 旅行预订（机票 + 酒店 + 租车）
 * 3. 转账（A账户 + B账户）
 *
 * @author concurrent_impl
 * @date 2024
 */
public class SagaDemo {

    /**
     * Saga步骤
     */
    public interface SagaStep {
        /**
         * 执行操作
         */
        boolean execute();

        /**
         * 补偿操作
         */
        boolean compensate();

        /**
         * 获取步骤名称
         */
        String getName();
    }

    /**
     * Saga执行结果
     */
    @lombok.Data
    public static class SagaResult {
        private boolean success;
        private String message;
        private List<String> completedSteps = new ArrayList<>();
        private List<String> failedCompensations = new ArrayList<>();
    }

    /**
     * Saga协调者
     */
    public static class SagaCoordinator {

        private final List<SagaStep> steps = new ArrayList<>();
        private final List<SagaStep> completedSteps = new ArrayList<>();

        /**
         * 添加步骤
         */
        public SagaCoordinator addStep(SagaStep step) {
            steps.add(step);
            return this;
        }

        /**
         * 执行Saga
         */
        public SagaResult execute() {
            SagaResult result = new SagaResult();

            for (SagaStep step : steps) {
                System.out.println("执行步骤: " + step.getName());
                try {
                    boolean success = step.execute();
                    if (success) {
                        completedSteps.add(step);
                        result.getCompletedSteps().add(step.getName());
                        System.out.println("步骤成功: " + step.getName());
                    } else {
                        // 执行失败，开始补偿
                        System.out.println("步骤失败: " + step.getName() + "，开始补偿...");
                        result.setMessage("步骤失败: " + step.getName());
                        compensate(result);
                        return result;
                    }
                } catch (Exception e) {
                    // 执行异常，开始补偿
                    System.out.println("步骤异常: " + step.getName() + "，开始补偿...");
                    result.setMessage("步骤异常: " + step.getName() + ", " + e.getMessage());
                    compensate(result);
                    return result;
                }
            }

            result.setSuccess(true);
            result.setMessage("所有步骤执行成功");
            return result;
        }

        /**
         * 补偿操作
         */
        private void compensate(SagaResult result) {
            // 逆序补偿
            for (int i = completedSteps.size() - 1; i >= 0; i--) {
                SagaStep step = completedSteps.get(i);
                System.out.println("补偿步骤: " + step.getName());
                try {
                    boolean success = step.compensate();
                    if (success) {
                        System.out.println("补偿成功: " + step.getName());
                    } else {
                        System.out.println("补偿失败: " + step.getName());
                        result.getFailedCompensations().add(step.getName());
                    }
                } catch (Exception e) {
                    System.out.println("补偿异常: " + step.getName());
                    result.getFailedCompensations().add(step.getName());
                }
            }
        }
    }

    /**
     * 演示Saga模式
     */
    public static void demonstrate() {
        System.out.println("========== Saga模式演示 ==========");
        System.out.println();
        System.out.println("【场景】电商下单流程");
        System.out.println("1. 创建订单");
        System.out.println("2. 扣减库存");
        System.out.println("3. 扣减余额");
        System.out.println();

        // 创建Saga步骤
        SagaCoordinator coordinator = new SagaCoordinator();

        // 步骤1：创建订单
        coordinator.addStep(new SagaStep() {
            private boolean executed = false;

            @Override
            public boolean execute() {
                System.out.println("  创建订单: ORD" + System.currentTimeMillis());
                executed = true;
                return true;
            }

            @Override
            public boolean compensate() {
                if (executed) {
                    System.out.println("  取消订单");
                    executed = false;
                }
                return true;
            }

            @Override
            public String getName() {
                return "创建订单";
            }
        });

        // 步骤2：扣减库存
        coordinator.addStep(new SagaStep() {
            private boolean executed = false;

            @Override
            public boolean execute() {
                System.out.println("  扣减库存: 商品ID=1001, 数量=1");
                executed = true;
                return true;
            }

            @Override
            public boolean compensate() {
                if (executed) {
                    System.out.println("  恢复库存: 商品ID=1001, 数量=1");
                    executed = false;
                }
                return true;
            }

            @Override
            public String getName() {
                return "扣减库存";
            }
        });

        // 步骤3：扣减余额（模拟失败）
        coordinator.addStep(new SagaStep() {
            @Override
            public boolean execute() {
                System.out.println("  扣减余额: 用户ID=1001, 金额=100");
                // 模拟余额不足
                return false;
            }

            @Override
            public boolean compensate() {
                System.out.println("  恢复余额");
                return true;
            }

            @Override
            public String getName() {
                return "扣减余额";
            }
        });

        // 执行Saga
        SagaResult result = coordinator.execute();

        System.out.println();
        System.out.println("执行结果: " + (result.isSuccess() ? "成功" : "失败"));
        System.out.println("完成步骤: " + result.getCompletedSteps());
        if (!result.isSuccess()) {
            System.out.println("失败原因: " + result.getMessage());
            System.out.println("失败补偿: " + result.getFailedCompensations());
        }
    }

    /**
     * 演示Saga模式的优缺点
     */
    public static void demonstrateProsAndCons() {
        System.out.println("========== Saga模式优缺点 ==========");
        System.out.println();
        System.out.println("【优点】");
        System.out.println("1. 支持长事务");
        System.out.println("2. 每个服务独立提交，性能好");
        System.out.println("3. 通过补偿保证最终一致性");
        System.out.println("4. 不会长时间锁定资源");
        System.out.println();
        System.out.println("【缺点】");
        System.out.println("1. 补偿逻辑复杂");
        System.out.println("2. 只能保证最终一致性");
        System.out.println("3. 补偿操作可能失败");
        System.out.println("4. 隔离性差，可能读到中间状态");
        System.out.println();
        System.out.println("【适用场景】");
        System.out.println("1. 跨服务的业务操作");
        System.out.println("2. 长时间运行的事务");
        System.out.println("3. 对一致性要求不高的场景");
        System.out.println();
        System.out.println("【不适用场景】");
        System.out.println("1. 需要强一致性的场景");
        System.out.println("2. 补偿操作不可逆的场景");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrate();
        System.out.println("\n");
        demonstrateProsAndCons();
    }
}
