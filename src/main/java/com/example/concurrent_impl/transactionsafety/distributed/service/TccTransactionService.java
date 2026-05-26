package com.example.concurrent_impl.transactionsafety.distributed.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 【企业级】TCC事务服务
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
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TccTransactionService {

    /**
     * TCC操作接口
     */
    public interface TccAction<T> {
        /**
         * Try阶段：预留资源
         */
        boolean tryAction(T params);

        /**
         * Confirm阶段：确认提交
         */
        boolean confirmAction(T params);

        /**
         * Cancel阶段：取消操作
         */
        boolean cancelAction(T params);
    }

    /**
     * TCC执行结果
     */
    @lombok.Data
    public static class TccResult {
        private boolean success;
        private String message;
        private String phase; // try, confirm, cancel
    }

    /**
     * 执行TCC事务
     *
     * @param transactionId 事务ID
     * @param action TCC操作
     * @param params 参数
     * @param <T> 参数类型
     * @return 执行结果
     */
    public <T> TccResult execute(String transactionId, TccAction<T> action, T params) {
        log.info("开始TCC事务: transactionId={}", transactionId);

        TccResult result = new TccResult();

        try {
            // Try阶段
            log.info("TCC Try阶段: transactionId={}", transactionId);
            result.setPhase("try");

            boolean tryResult = action.tryAction(params);
            if (!tryResult) {
                log.warn("TCC Try失败: transactionId={}", transactionId);
                result.setMessage("Try失败");
                return result;
            }

            // Confirm阶段
            log.info("TCC Confirm阶段: transactionId={}", transactionId);
            result.setPhase("confirm");

            boolean confirmResult = action.confirmAction(params);
            if (!confirmResult) {
                log.warn("TCC Confirm失败，执行Cancel: transactionId={}", transactionId);
                action.cancelAction(params);
                result.setMessage("Confirm失败");
                return result;
            }

            result.setSuccess(true);
            result.setMessage("TCC事务成功");
            log.info("TCC事务成功: transactionId={}", transactionId);

        } catch (Exception e) {
            log.error("TCC事务异常，执行Cancel: transactionId={}", transactionId, e);
            result.setPhase("cancel");
            result.setMessage("事务异常: " + e.getMessage());

            try {
                action.cancelAction(params);
            } catch (Exception cancelException) {
                log.error("TCC Cancel异常: transactionId={}", transactionId, cancelException);
            }
        }

        return result;
    }
}
