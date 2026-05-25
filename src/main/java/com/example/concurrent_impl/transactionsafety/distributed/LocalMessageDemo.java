package com.example.concurrent_impl.transactionsafety.distributed;

import java.util.UUID;

/**
 * 【事务安全 - 本地消息表】分布式事务最终一致性实现
 *
 * 【业务背景】
 * 在微服务架构中，需要保证本地事务和消息发送的一致性，
 * 本地消息表是一种可靠的解决方案。
 *
 * 【实现原理】
 * 1. 本地事务：业务操作 + 插入消息表
 * 2. 定时任务：扫描消息表，发送消息
 * 3. 消息消费：处理消息，更新消息状态
 * 4. 补偿机制：失败重试
 *
 * 【为什么这样写】
 * 1. 保证本地事务和消息发送的一致性
 * 2. 支持消息重试
 * 3. 实现最终一致性
 *
 * 【不遵守的后果】
 * 1. 不使用消息表：消息可能丢失
 * 2. 不做重试：消息发送失败无法恢复
 * 3. 不做幂等：消息重复消费
 *
 * 【正确示例】
 * 使用本地消息表 + 定时任务
 *
 * 【错误示例】
 * 先执行业务，再发送消息（可能消息发送失败）
 *
 * 【实际案例】
 * 1. 订单创建后通知库存服务
 * 2. 支付成功后通知订单服务
 * 3. 用户注册后发送欢迎邮件
 *
 * @author concurrent_impl
 * @date 2024
 */
public class LocalMessageDemo {

    /**
     * 消息状态
     */
    public enum MessageStatus {
        PENDING(0, "待发送"),
        SENT(1, "已发送"),
        CONFIRMED(2, "已确认"),
        FAILED(3, "发送失败");

        private final int code;
        private final String description;

        MessageStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }
    }

    /**
     * 本地消息
     */
    @lombok.Data
    public static class LocalMessage {
        private Long id;
        private String messageId;
        private String exchange;
        private String routingKey;
        private String content;
        private MessageStatus status;
        private int retryCount;
        private int maxRetryCount;
        private long nextRetryTime;
        private String errorMessage;
    }

    /**
     * 演示本地消息表流程
     */
    public static void demonstrate() {
        System.out.println("========== 本地消息表演示 ==========");
        System.out.println();
        System.out.println("【场景】订单创建后通知库存服务");
        System.out.println();

        // 步骤1：执行本地事务 + 插入消息表
        System.out.println("步骤1：执行本地事务 + 插入消息表");
        System.out.println("```sql");
        System.out.println("BEGIN;");
        System.out.println("  -- 创建订单");
        System.out.println("  INSERT INTO orders (order_no, user_id, amount) VALUES ('ORD001', 1001, 100);");
        System.out.println("  ");
        System.out.println("  -- 插入消息表");
        System.out.println("  INSERT INTO message (message_id, exchange, routing_key, content, status)");
        System.out.println("  VALUES ('MSG001', 'order.exchange', 'order.create', '{\"orderId\":1}', 0);");
        System.out.println("COMMIT;");
        System.out.println("```");
        System.out.println();

        // 步骤2：定时任务扫描消息表
        System.out.println("步骤2：定时任务扫描消息表");
        System.out.println("```sql");
        System.out.println("SELECT * FROM message");
        System.out.println("WHERE status = 0 AND next_retry_time <= NOW()");
        System.out.println("LIMIT 100;");
        System.out.println("```");
        System.out.println();

        // 步骤3：发送消息
        System.out.println("步骤3：发送消息到MQ");
        System.out.println("```java");
        System.out.println("for (LocalMessage message : messages) {");
        System.out.println("    try {");
        System.out.println("        rabbitTemplate.convertAndSend(");
        System.out.println("            message.getExchange(),");
        System.out.println("            message.getRoutingKey(),");
        System.out.println("            message.getContent()");
        System.out.println("        );");
        System.out.println("        // 更新状态为已发送");
        System.out.println("        updateMessageStatus(message.getMessageId(), SENT);");
        System.out.println("    } catch (Exception e) {");
        System.out.println("        // 发送失败，更新重试次数和下次重试时间");
        System.out.println("        updateRetryInfo(message.getMessageId());");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```");
        System.out.println();

        // 步骤4：消费消息
        System.out.println("步骤4：消费消息");
        System.out.println("```java");
        System.out.println("@RabbitListener(queues = \"order.queue\")");
        System.out.println("public void handleMessage(String message, Channel channel, ");
        System.out.println("                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {");
        System.out.println("    try {");
        System.out.println("        // 处理消息");
        System.out.println("        processMessage(message);");
        System.out.println("        ");
        System.out.println("        // 确认消息");
        System.out.println("        channel.basicAck(deliveryTag, false);");
        System.out.println("        ");
        System.out.println("        // 更新消息状态");
        System.out.println("        updateMessageStatus(messageId, CONFIRMED);");
        System.out.println("    } catch (Exception e) {");
        System.out.println("        // 拒绝消息，重新入队");
        System.out.println("        channel.basicNack(deliveryTag, false, true);");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```");
    }

    /**
     * 演示消息重试机制
     */
    public static void demonstrateRetryMechanism() {
        System.out.println("========== 消息重试机制演示 ==========");
        System.out.println();
        System.out.println("【重试策略】");
        System.out.println("1. 最大重试次数：3次");
        System.out.println("2. 重试间隔：指数退避（1s, 2s, 4s）");
        System.out.println("3. 超过最大重试次数：人工介入");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("private void updateRetryInfo(String messageId) {");
        System.out.println("    LocalMessage message = getMessage(messageId);");
        System.out.println("    ");
        System.out.println("    if (message.getRetryCount() >= message.getMaxRetryCount()) {");
        System.out.println("        // 超过最大重试次数，标记为失败");
        System.out.println("        updateMessageStatus(messageId, FAILED);");
        System.out.println("        // 发送告警");
        System.out.println("        sendAlert(message);");
        System.out.println("    } else {");
        System.out.println("        // 更新重试次数和下次重试时间");
        System.out.println("        int retryCount = message.getRetryCount() + 1;");
        System.out.println("        long nextRetryTime = System.currentTimeMillis() + (1000L << retryCount);");
        System.out.println("        ");
        System.out.println("        updateMessage(messageId, retryCount, nextRetryTime);");
        System.out.println("    }");
        System.out.println("}");
    }

    /**
     * 演示消息幂等消费
     */
    public static void demonstrateIdempotentConsumption() {
        System.out.println("========== 消息幂等消费演示 ==========");
        System.out.println();
        System.out.println("【为什么需要幂等】");
        System.out.println("1. 网络超时重试");
        System.out.println("2. 消费者处理成功但ACK失败");
        System.out.println("3. MQ重复投递");
        System.out.println();
        System.out.println("【幂等方案】");
        System.out.println("1. 数据库唯一索引");
        System.out.println("2. Redis SET NX");
        System.out.println("3. 状态机判断");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("public void processMessage(String messageId, OrderMessage msg) {");
        System.out.println("    // 方案1：使用Redis判断");
        System.out.println("    Boolean processed = redisTemplate.opsForValue()");
        System.out.println("        .setIfAbsent(\"msg:\" + messageId, \"1\", 24, TimeUnit.HOURS);");
        System.out.println("    if (!Boolean.TRUE.equals(processed)) {");
        System.out.println("        log.info(\"消息已处理: {}\", messageId);");
        System.out.println("        return;");
        System.out.println("    }");
        System.out.println("    ");
        System.out.println("    // 方案2：使用数据库唯一索引");
        System.out.println("    try {");
        System.out.println("        messageLogMapper.insert(messageId);");
        System.out.println("    } catch (DuplicateKeyException e) {");
        System.out.println("        log.info(\"消息已处理: {}\", messageId);");
        System.out.println("        return;");
        System.out.println("    }");
        System.out.println("    ");
        System.out.println("    // 处理业务逻辑");
        System.out.println("    doBusiness(msg);");
        System.out.println("}");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrate();
        System.out.println("\n");
        demonstrateRetryMechanism();
        System.out.println("\n");
        demonstrateIdempotentConsumption();
    }
}
