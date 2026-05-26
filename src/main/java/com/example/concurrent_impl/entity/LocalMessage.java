package com.example.concurrent_impl.entity;

import com.example.concurrent_impl.entity.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 【实体类】本地消息表
 *
 * 【业务说明】
 * 用于保证本地事务和MQ消息发送的一致性
 */
@Data
@Entity
@Table(name = "local_message")
public class LocalMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String messageId;

    @Column(nullable = false, length = 100)
    private String exchange;

    @Column(nullable = false, length = 100)
    private String routingKey;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private MessageStatus status; // 0-待发送, 1-已发送, 2-已确认, 3-发送失败

    @Column
    private Integer retryCount = 0;

    @Column
    private Integer maxRetryCount = 3;

    @Column
    private LocalDateTime nextRetryTime;

    @Column(length = 500)
    private String errorMessage;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
