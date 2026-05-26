package com.example.concurrent_impl.transactionsafety.distributed.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 【企业级】Saga事务服务
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
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaTransactionService {

    /**
     * Saga步骤接口
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
     * 执行Saga事务
     *
     * @param transactionId 事务ID
     * @param steps 步骤列表
     * @return 执行结果
     */
    public SagaResult execute(String transactionId, List<SagaStep> steps) {
        log.info("开始Saga事务: transactionId={}, steps={}", transactionId, steps.size());

        SagaResult result = new SagaResult();
        List<SagaStep> completedSteps = new ArrayList<>();

        for (SagaStep step : steps) {
            log.info("执行步骤: transactionId={}, step={}", transactionId, step.getName());

            try {
                boolean success = step.execute();
                if (success) {
                    completedSteps.add(step);
                    result.getCompletedSteps().add(step.getName());
                    log.info("步骤成功: transactionId={}, step={}", transactionId, step.getName());
                } else {
                    // 执行失败，开始补偿
                    log.warn("步骤失败，开始补偿: transactionId={}, step={}", transactionId, step.getName());
                    result.setMessage("步骤失败: " + step.getName());
                    compensate(transactionId, completedSteps, result);
                    return result;
                }
            } catch (Exception e) {
                // 执行异常，开始补偿
                log.error("步骤异常，开始补偿: transactionId={}, step={}", transactionId, step.getName(), e);
                result.setMessage("步骤异常: " + step.getName() + ", " + e.getMessage());
                compensate(transactionId, completedSteps, result);
                return result;
            }
        }

        result.setSuccess(true);
        result.setMessage("所有步骤执行成功");
        log.info("Saga事务成功: transactionId={}", transactionId);
        return result;
    }

    /**
     * 执行补偿操作
     */
    private void compensate(String transactionId, List<SagaStep> completedSteps, SagaResult result) {
        // 逆序补偿
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep step = completedSteps.get(i);
            log.info("补偿步骤: transactionId={}, step={}", transactionId, step.getName());

            try {
                boolean success = step.compensate();
                if (success) {
                    log.info("补偿成功: transactionId={}, step={}", transactionId, step.getName());
                } else {
                    log.error("补偿失败: transactionId={}, step={}", transactionId, step.getName());
                    result.getFailedCompensations().add(step.getName());
                }
            } catch (Exception e) {
                log.error("补偿异常: transactionId={}, step={}", transactionId, step.getName(), e);
                result.getFailedCompensations().add(step.getName());
            }
        }
    }
}
