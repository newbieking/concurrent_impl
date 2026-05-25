package com.example.concurrent_impl.dataconsistency.eventual;

/**
 * 【数据一致性 - 最终一致性】消息队列异步同步实现
 *
 * 【业务背景】
 * 在分布式系统中，多个服务之间的数据需要保持一致，
 * 使用消息队列可以实现最终一致性。
 *
 * 【实现原理】
 * 1. 主服务执行业务操作
 * 2. 发送消息到MQ
 * 3. 从服务消费消息，更新数据
 * 4. 通过重试机制保证最终一致
 *
 * 【为什么这样写】
 * 1. 异步处理，提高性能
 * 2. 解耦服务
 * 3. 支持重试，保证最终一致
 *
 * 【不遵守的后果】
 * 1. 同步调用：性能差，耦合度高
 * 2. 不使用MQ：消息可能丢失
 * 3. 不做重试：数据不一致
 *
 * 【正确示例】
 * 使用MQ + 重试机制
 *
 * 【错误示例】
 * 同步调用，不做重试
 *
 * 【实际案例】
 * 1. 订单创建后通知库存服务
 * 2. 支付成功后通知订单服务
 * 3. 用户注册后发送欢迎邮件
 *
 * @author concurrent_impl
 * @date 2024
 */
public class MessageQueueDemo {

    /**
     * 演示消息队列实现最终一致性
     */
    public static void demonstrate() {
        System.out.println("========== 消息队列实现最终一致性 ==========");
        System.out.println();
        System.out.println("【场景】订单创建后通知库存服务");
        System.out.println();
        System.out.println("【流程】");
        System.out.println("1. 订单服务：创建订单 + 发送消息（本地事务）");
        System.out.println("2. MQ：存储消息");
        System.out.println("3. 库存服务：消费消息，扣减库存");
        System.out.println("4. 如果消费失败：重试");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("// 订单服务");
        System.out.println("@Transactional");
        System.out.println("public void createOrder(OrderRequest request) {");
        System.out.println("    // 1. 创建订单");
        System.out.println("    Order order = new Order();");
        System.out.println("    orderMapper.insert(order);");
        System.out.println("    ");
        System.out.println("    // 2. 发送消息");
        System.out.println("    rabbitTemplate.convertAndSend(");
        System.out.println("        \"order.exchange\",");
        System.out.println("        \"order.create\",");
        System.out.println("        new OrderMessage(order.getId())");
        System.out.println("    );");
        System.out.println("}");
        System.out.println();
        System.out.println("// 库存服务");
        System.out.println("@RabbitListener(queues = \"stock.queue\")");
        System.out.println("public void handleMessage(OrderMessage message) {");
        System.out.println("    // 扣减库存");
        System.out.println("    stockService.deductStock(message.getProductId(), message.getQuantity());");
        System.out.println("}");
    }

    /**
     * 演示消息可靠性保证
     */
    public static void demonstrateReliability() {
        System.out.println("========== 消息可靠性保证 ==========");
        System.out.println();
        System.out.println("【消息丢失场景】");
        System.out.println("1. 生产者发送失败");
        System.out.println("2. MQ存储失败");
        System.out.println("3. 消费者处理失败");
        System.out.println();
        System.out.println("【解决方案】");
        System.out.println();
        System.out.println("1. 生产者确认");
        System.out.println("```yaml");
        System.out.println("spring:");
        System.out.println("  rabbitmq:");
        System.out.println("    publisher-confirm-type: correlated");
        System.out.println("    publisher-returns: true");
        System.out.println("```");
        System.out.println();
        System.out.println("2. 消息持久化");
        System.out.println("```java");
        System.out.println("// 队列持久化");
        System.out.println("QueueBuilder.durable(\"order.queue\").build();");
        System.out.println();
        System.out.println("// 消息持久化");
        System.out.println("MessageProperties props = new MessageProperties();");
        System.out.println("props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);");
        System.out.println("```");
        System.out.println();
        System.out.println("3. 消费者手动确认");
        System.out.println("```java");
        System.out.println("@RabbitListener(queues = \"order.queue\")");
        System.out.println("public void handleMessage(Message message, Channel channel,");
        System.out.println("                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) {");
        System.out.println("    try {");
        System.out.println("        // 处理消息");
        System.out.println("        process(message);");
        System.out.println("        // 确认");
        System.out.println("        channel.basicAck(tag, false);");
        System.out.println("    } catch (Exception e) {");
        System.out.println("        // 拒绝，重新入队");
        System.out.println("        channel.basicNack(tag, false, true);");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrate();
        System.out.println("\n");
        demonstrateReliability();
    }
}
