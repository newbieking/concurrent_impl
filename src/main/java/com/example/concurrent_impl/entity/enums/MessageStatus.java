package com.example.concurrent_impl.entity.enums;

/**
 * 【枚举】消息状态
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

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MessageStatus fromCode(int code) {
        for (MessageStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
