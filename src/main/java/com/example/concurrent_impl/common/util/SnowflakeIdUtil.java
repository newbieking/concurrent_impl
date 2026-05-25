package com.example.concurrent_impl.common.util;

/**
 * 【雪花算法ID生成器】生成分布式唯一ID
 * 
 * 【业务背景】
 * 在分布式系统中，需要生成全局唯一的ID，用于：
 * 1. 订单号
 * 2. 用户ID
 * 3. 消息ID
 * 4. 日志追踪ID
 * 
 * 【雪花算法原理】
 * 雪花算法生成64位的Long型ID，结构如下：
 * 0 | 0000000000 0000000000 0000000000 0000000000 0 | 00000 | 00000 | 000000000000
 * 
 * 1位符号位（固定为0） | 41位时间戳 | 5位数据中心ID | 5位工作机器ID | 12位序列号
 * 
 * 【为什么这样写】
 * 1. 64位Long型：占用空间小，数据库索引效率高
 * 2. 时间戳在高位：ID整体递增，便于排序
 * 3. 数据中心ID和机器ID：保证不同节点生成的ID不冲突
 * 4. 序列号：同一毫秒内可以生成多个ID
 * 
 * 【不遵守的后果】
 * 1. 使用UUID：无序，索引效率低，占用空间大
 * 2. 使用数据库自增ID：分布式环境下可能冲突
 * 3. 使用时间戳：高并发时可能重复
 * 
 * 【ID容量】
 * 1. 时间戳：41位，可以使用约69年
 * 2. 数据中心ID：5位，最多32个数据中心
 * 3. 工作机器ID：5位，每个数据中心最多32台机器
 * 4. 序列号：12位，同一毫秒最多4096个ID
 * 
 * 【实际案例】
 * 1. 订单号生成：全局唯一，可排序
 * 2. 用户ID生成：分布式环境下不冲突
 * 3. 消息ID生成：保证消息的唯一性
 */
public class SnowflakeIdUtil {

    /**
     * 起始时间戳（2020-01-01 00:00:00）
     * 
     * 【为什么设置起始时间戳】
     * 1. 延长ID的使用时间
     * 2. 避免时间戳过大
     */
    private static final long START_TIMESTAMP = 1577808000000L;

    /**
     * 数据中心ID占用的位数
     */
    private static final long DATA_CENTER_ID_BITS = 5L;

    /**
     * 工作机器ID占用的位数
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 序列号占用的位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 数据中心ID的最大值
     */
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    /**
     * 工作机器ID的最大值
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 序列号的最大值
     */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /**
     * 工作机器ID的左移位数
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心ID的左移位数
     */
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳的左移位数
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    /**
     * 数据中心ID
     */
    private final long dataCenterId;

    /**
     * 工作机器ID
     */
    private final long workerId;

    /**
     * 序列号
     */
    private long sequence = 0L;

    /**
     * 上一次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 构造方法
     * 
     * @param dataCenterId 数据中心ID（0-31）
     * @param workerId 工作机器ID（0-31）
     */
    public SnowflakeIdUtil(long dataCenterId, long workerId) {
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("数据中心ID不能大于" + MAX_DATA_CENTER_ID + "或小于0");
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("工作机器ID不能大于" + MAX_WORKER_ID + "或小于0");
        }
        this.dataCenterId = dataCenterId;
        this.workerId = workerId;
    }

    /**
     * 生成下一个ID
     * 
     * @return 唯一ID
     */
    public synchronized long nextId() {
        long currentTimestamp = getTimestamp();

        // 【关键点1】检查时钟回拨
        // 如果当前时间小于上一次生成ID的时间戳，说明时钟回拨了
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }

        // 【关键点2】同一毫秒内生成多个ID
        if (currentTimestamp == lastTimestamp) {
            // 序列号+1，并取模
            // [0, ~(-1L << BITS)]
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 序列号溢出（超过4096）
            if (sequence == 0) {
                // 等待下一毫秒
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置为0
            sequence = 0L;
        }

        // 【关键点3】更新上一次生成ID的时间戳
        lastTimestamp = currentTimestamp;

        // 【关键点4】生成ID
        // 时间戳 | 数据中心ID | 工作机器ID | 序列号
        return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待下一毫秒
     * 
     * @param lastTimestamp 上一次的时间戳
     * @return 下一毫秒的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = getTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getTimestamp();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     * 
     * @return 当前时间戳（毫秒）
     */
    private long getTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 解析ID
     * 
     * 【使用场景】
     * 从ID中提取时间戳等信息
     * 
     * @param id 雪花ID
     * @return 解析结果
     */
    public static IdInfo parseId(long id) {
        long sequence = id & MAX_SEQUENCE;
        long workerId = (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
        long dataCenterId = (id >> DATA_CENTER_ID_SHIFT) & MAX_DATA_CENTER_ID;
        long timestamp = (id >> TIMESTAMP_SHIFT) + START_TIMESTAMP;
        
        return new IdInfo(timestamp, dataCenterId, workerId, sequence);
    }

    /**
     * ID信息
     */
    public static class IdInfo {
        private final long timestamp;
        private final long dataCenterId;
        private final long workerId;
        private final long sequence;

        public IdInfo(long timestamp, long dataCenterId, long workerId, long sequence) {
            this.timestamp = timestamp;
            this.dataCenterId = dataCenterId;
            this.workerId = workerId;
            this.sequence = sequence;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getDataCenterId() {
            return dataCenterId;
        }

        public long getWorkerId() {
            return workerId;
        }

        public long getSequence() {
            return sequence;
        }

        @Override
        public String toString() {
            return "IdInfo{" +
                    "timestamp=" + timestamp +
                    ", dataCenterId=" + dataCenterId +
                    ", workerId=" + workerId +
                    ", sequence=" + sequence +
                    '}';
        }
    }
}
