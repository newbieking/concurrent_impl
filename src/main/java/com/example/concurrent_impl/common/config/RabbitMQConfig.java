package com.example.concurrent_impl.common.config;

import com.example.concurrent_impl.common.constant.BusinessConstant;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 【RabbitMQ配置】消息队列配置
 * 
 * 【业务背景】
 * RabbitMQ是企业级项目中常用的消息队列，用于异步处理、
 * 系统解耦、流量削峰等场景。
 * 
 * 【为什么这样写】
 * 1. 配置消息转换器：使用JSON序列化，可读性好
 * 2. 配置确认机制：确保消息发送成功
 * 3. 配置重试机制：处理消费失败的情况
 * 4. 声明队列和交换机：确保队列存在
 * 
 * 【不遵守的后果】
 * 1. 不配置确认机制：消息可能丢失
 * 2. 不配置重试机制：消费失败后消息被丢弃
 * 3. 不声明队列：队列不存在时会报错
 * 4. 使用默认序列化：可读性差，难以调试
 * 
 * 【消息可靠性保证】
 * 1. 发送确认：确保消息到达交换机
 * 2. 队列持久化：确保消息不会因重启丢失
 * 3. 消费确认：确保消息被正确处理
 * 4. 死信队列：处理消费失败的消息
 * 
 * 【实际案例】
 * 1. 订单创建后发送消息通知库存服务扣减库存
 * 2. 支付成功后发送消息通知订单服务更新状态
 * 3. 订单超时未支付自动取消
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 声明订单队列
     * 
     * 【为什么使用持久化队列】
     * 1. 队列信息会持久化到磁盘
     * 2. RabbitMQ重启后队列不会丢失
     * 
     * 【为什么不使用自动删除队列】
     * 自动删除队列在没有消费者时会被删除，可能导致消息丢失
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(BusinessConstant.ORDER_QUEUE_NAME)
                // 【关键点1】配置死信交换机
                // 消费失败的消息会被发送到死信队列
                .withArgument("x-dead-letter-exchange", "order.dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "order.dlx.routingKey")
                // 【关键点2】配置队列过期时间（可选）
                // .withArgument("x-message-ttl", 60000)
                .build();
    }

    /**
     * 声明订单交换机
     * 
     * 【为什么使用直连交换机】
     * 1. 根据routing key精确匹配队列
     * 2. 适用于点对点的消息传递
     * 
     * 【其他交换机类型】
     * 1. TopicExchange：支持通配符匹配
     * 2. FanoutExchange：广播到所有队列
     * 3. HeadersExchange：根据消息头匹配
     */
    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange(BusinessConstant.ORDER_EXCHANGE_NAME)
                .durable(true) // 持久化
                .build();
    }

    /**
     * 绑定订单队列到订单交换机
     */
    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue)
                .to(orderExchange)
                .with(BusinessConstant.ORDER_ROUTING_KEY);
    }

    /**
     * 声明死信队列
     * 
     * 【什么是死信队列】
     * 当消息消费失败、被拒绝或过期时，会被发送到死信队列
     * 
     * 【为什么需要死信队列】
     * 1. 避免消费失败的消息丢失
     * 2. 可以对失败消息进行重试或人工处理
     * 3. 便于监控和告警
     */
    @Bean
    public Queue orderDlxQueue() {
        return QueueBuilder.durable("order.dlx.queue").build();
    }

    /**
     * 声明死信交换机
     */
    @Bean
    public DirectExchange orderDlxExchange() {
        return ExchangeBuilder.directExchange("order.dlx.exchange")
                .durable(true)
                .build();
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding orderDlxBinding(Queue orderDlxQueue, DirectExchange orderDlxExchange) {
        return BindingBuilder.bind(orderDlxQueue)
                .to(orderDlxExchange)
                .with("order.dlx.routingKey");
    }

    /**
     * 配置RabbitTemplate
     * 
     * 【为什么需要自定义RabbitTemplate】
     * 1. 配置消息转换器，使用JSON序列化
     * 2. 配置发送确认，确保消息到达交换机
     * 3. 配置返回确认，确保消息到达队列
     * 
     * @param connectionFactory 连接工厂
     * @return RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // 【关键点1】配置消息转换器
        // 使用JSON序列化，可读性好
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        
        // 【关键点2】配置发送确认
        // 确保消息到达交换机
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 消息发送失败，记录日志或重试
                System.err.println("消息发送失败: " + cause);
            }
        });
        
        // 【关键点3】配置返回确认
        // 确保消息到达队列
        template.setReturnsCallback(returned -> {
            System.err.println("消息被退回: " + returned.getMessage());
        });
        
        // 【关键点4】开启mandatory模式
        // 配合returnCallback使用
        template.setMandatory(true);
        
        return template;
    }

    /**
     * 配置监听器容器工厂
     * 
     * 【为什么需要配置监听器容器工厂】
     * 1. 配置消息转换器，支持JSON反序列化
     * 2. 配置确认模式，确保消息被正确处理
     * 3. 配置并发消费者数量，提高消费速度
     * 
     * @param connectionFactory 连接工厂
     * @return 监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        
        // 【关键点1】配置消息转换器
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        
        // 【关键点2】配置确认模式
        // MANUAL：手动确认，需要在代码中调用ack或nack
        // AUTO：自动确认，消息被消费后自动确认
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        
        // 【关键点3】配置并发消费者数量
        // 根据业务需求和服务器性能调整
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        
        // 【关键点4】配置预取数量
        // 每次从队列中获取的消息数量
        factory.setPrefetchCount(10);
        
        return factory;
    }
}
