package com.example.concurrent_impl.transactionsafety.distributed.service;

import com.example.concurrent_impl.entity.LocalMessage;
import com.example.concurrent_impl.entity.enums.MessageStatus;
import com.example.concurrent_impl.mapper.LocalMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 【企业级】本地消息表服务 - 保证MQ消息可靠性
 *
 * 【业务背景】
 * 在分布式系统中，需要保证本地事务和消息发送的一致性，
 * 本地消息表是一种可靠的解决方案。
 *
 * 【实现原理】
 * 1. 本地事务：业务操作 + 插入消息表
 * 2. 定时任务：扫描消息表，发送消息
 * 3. 消息确认：更新消息状态
 * 4. 补偿机制：失败重试
 *
 * 【为什么这样写】
 * 1. 保证本地事务和消息发送的一致性
 * 2. 支持消息重试
 * 3. 实现最终一致性
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalMessageService {

    private final LocalMessageMapper localMessageMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 保存本地消息（在同一事务中）
     *
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param content 消息内容
     * @return 消息ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String saveMessage(String exchange, String routingKey, String content) {
        String messageId = UUID.randomUUID().toString();

        LocalMessage message = new LocalMessage();
        message.setMessageId(messageId);
        message.setExchange(exchange);
        message.setRoutingKey(routingKey);
        message.setContent(content);
        message.setStatus(MessageStatus.PENDING);
        message.setRetryCount(0);
        message.setMaxRetryCount(MAX_RETRY_COUNT);
        message.setNextRetryTime(LocalDateTime.now());

        localMessageMapper.save(message);
        log.info("保存本地消息: messageId={}, exchange={}, routingKey={}", messageId, exchange, routingKey);

        return messageId;
    }

    /**
     * 发送消息到MQ
     *
     * @param messageId 消息ID
     */
    public void sendMessage(String messageId) {
        LocalMessage message = localMessageMapper.findByMessageId(messageId);
        if (message == null) {
            log.warn("消息不存在: messageId={}", messageId);
            return;
        }

        try {
            // 发送消息
            CorrelationData correlationData = new CorrelationData(messageId);
            rabbitTemplate.convertAndSend(
                    message.getExchange(),
                    message.getRoutingKey(),
                    message.getContent(),
                    correlationData
            );

            // 更新状态为已发送
            message.setStatus(MessageStatus.SENT);
            localMessageMapper.save(message);
            log.info("消息发送成功: messageId={}", messageId);

        } catch (Exception e) {
            log.error("消息发送失败: messageId={}", messageId, e);
            handleSendFailure(message);
        }
    }

    /**
     * 处理发送失败
     */
    private void handleSendFailure(LocalMessage message) {
        int retryCount = message.getRetryCount() + 1;

        if (retryCount >= message.getMaxRetryCount()) {
            // 超过最大重试次数，标记为失败
            message.setStatus(MessageStatus.FAILED);
            message.setErrorMessage("超过最大重试次数");
            log.error("消息发送失败（超过最大重试次数）: messageId={}", message.getMessageId());
        } else {
            // 更新重试次数和下次重试时间（指数退避）
            message.setRetryCount(retryCount);
            long nextRetryDelay = (long) Math.pow(2, retryCount) * 1000; // 2^n 秒
            message.setNextRetryTime(LocalDateTime.now().plusSeconds(nextRetryDelay / 1000));
            log.info("消息重试: messageId={}, retryCount={}, nextRetryTime={}",
                    message.getMessageId(), retryCount, message.getNextRetryTime());
        }

        localMessageMapper.save(message);
    }

    /**
     * 消息发送确认
     *
     * @param messageId 消息ID
     * @param success 是否成功
     */
    public void confirmMessage(String messageId, boolean success) {
        LocalMessage message = localMessageMapper.findByMessageId(messageId);
        if (message == null) {
            return;
        }

        if (success) {
            message.setStatus(MessageStatus.CONFIRMED);
            log.info("消息确认成功: messageId={}", messageId);
        } else {
            handleSendFailure(message);
        }

        localMessageMapper.save(message);
    }

    /**
     * 定时任务：扫描待发送消息
     *
     * 【执行频率】每5秒执行一次
     */
    @Scheduled(fixedDelay = 5000)
    public void scanPendingMessages() {
        List<LocalMessage> messages = localMessageMapper.findByStatusAndNextRetryTimeBefore(
                MessageStatus.PENDING, LocalDateTime.now());

        if (messages.isEmpty()) {
            return;
        }

        log.info("扫描到待发送消息: count={}", messages.size());

        for (LocalMessage message : messages) {
            sendMessage(message.getMessageId());
        }
    }

    /**
     * 定时任务：清理已确认消息
     *
     * 【执行频率】每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanConfirmedMessages() {
        LocalDateTime before = LocalDateTime.now().minusDays(7); // 保留7天
        int count = localMessageMapper.deleteByStatusAndCreatedAtBefore(MessageStatus.CONFIRMED, before);
        log.info("清理已确认消息: count={}", count);
    }

    /**
     * 获取消息统计
     */
    public MessageStatistics getStatistics() {
        MessageStatistics stats = new MessageStatistics();
        stats.setPendingCount(localMessageMapper.countByStatus(MessageStatus.PENDING));
        stats.setSentCount(localMessageMapper.countByStatus(MessageStatus.SENT));
        stats.setConfirmedCount(localMessageMapper.countByStatus(MessageStatus.CONFIRMED));
        stats.setFailedCount(localMessageMapper.countByStatus(MessageStatus.FAILED));
        return stats;
    }

    /**
     * 消息统计
     */
    @lombok.Data
    public static class MessageStatistics {
        private long pendingCount;
        private long sentCount;
        private long confirmedCount;
        private long failedCount;
    }
}
