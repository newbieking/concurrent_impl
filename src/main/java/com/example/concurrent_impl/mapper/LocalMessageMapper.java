package com.example.concurrent_impl.mapper;

import com.example.concurrent_impl.entity.LocalMessage;
import com.example.concurrent_impl.entity.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 【Mapper】本地消息表数据访问层
 */
@Repository
public interface LocalMessageMapper extends JpaRepository<LocalMessage, Long> {

    /**
     * 根据消息ID查找消息
     */
    LocalMessage findByMessageId(String messageId);

    /**
     * 根据状态和下次重试时间查找消息
     */
    List<LocalMessage> findByStatusAndNextRetryTimeBefore(MessageStatus status, LocalDateTime nextRetryTime);

    /**
     * 根据状态统计消息数量
     */
    long countByStatus(MessageStatus status);

    /**
     * 删除指定状态和创建时间之前的消息
     */
    @Modifying
    @Query("DELETE FROM LocalMessage m WHERE m.status = :status AND m.createdAt < :before")
    int deleteByStatusAndCreatedAtBefore(@Param("status") MessageStatus status, @Param("before") LocalDateTime before);
}
