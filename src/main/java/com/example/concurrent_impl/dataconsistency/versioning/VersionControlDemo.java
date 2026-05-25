package com.example.concurrent_impl.dataconsistency.versioning;

/**
 * 【数据一致性 - 版本控制】乐观锁版本控制实现
 *
 * 【业务背景】
 * 在并发更新场景中，需要使用版本控制来保证数据一致性，
 * 防止数据被覆盖。
 *
 * 【实现原理】
 * 1. 每条数据有一个版本号
 * 2. 更新时带上版本号
 * 3. 如果版本号不匹配，更新失败
 * 4. 更新成功后版本号+1
 *
 * 【为什么这样写】
 * 1. 无锁实现，性能好
 * 2. 简单可靠
 * 3. 支持高并发
 *
 * 【不遵守的后果】
 * 1. 不使用版本控制：数据被覆盖
 * 2. 不重试：用户体验差
 * 3. 不限制重试次数：死循环
 *
 * 【正确示例】
 * 使用版本号 + 重试机制
 *
 * 【错误示例】
 * 直接UPDATE，不检查版本号
 *
 * 【实际案例】
 * 1. 商品库存扣减
 * 2. 账户余额更新
 * 3. 订单状态更新
 *
 * @author concurrent_impl
 * @date 2024
 */
public class VersionControlDemo {

    /**
     * 模拟数据库记录
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class Record {
        private Long id;
        private String data;
        private int version;
    }

    /**
     * 模拟数据库
     */
    private static Record record = new Record(1L, "初始数据", 0);

    /**
     * 使用乐观锁更新
     *
     * @param id 记录ID
     * @param newData 新数据
     * @param expectedVersion 期望版本号
     * @param maxRetries 最大重试次数
     * @return 是否更新成功
     */
    public static boolean updateWithVersion(Long id, String newData, int expectedVersion, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            // 【关键点1】检查版本号
            if (record.getVersion() != expectedVersion) {
                System.out.println("版本不匹配，当前版本: " + record.getVersion() +
                        ", 期望版本: " + expectedVersion);
                // 重新获取最新数据
                expectedVersion = record.getVersion();
                continue;
            }

            // 【关键点2】更新数据和版本号
            record.setData(newData);
            record.setVersion(expectedVersion + 1);

            System.out.println("更新成功，新版本: " + record.getVersion());
            return true;
        }

        System.out.println("更新失败，超过最大重试次数");
        return false;
    }

    /**
     * 演示版本控制
     */
    public static void demonstrate() {
        System.out.println("========== 版本控制演示 ==========");
        System.out.println();
        System.out.println("初始数据: " + record);
        System.out.println();

        // 场景1：正常更新
        System.out.println("【场景1】正常更新");
        boolean result = updateWithVersion(1L, "更新后的数据", 0, 3);
        System.out.println("更新结果: " + result);
        System.out.println("当前数据: " + record);
        System.out.println();

        // 场景2：版本冲突
        System.out.println("【场景2】版本冲突（使用旧版本号）");
        result = updateWithVersion(1L, "再次更新", 0, 3);
        System.out.println("更新结果: " + result);
        System.out.println("当前数据: " + record);
    }

    /**
     * 演示版本控制最佳实践
     */
    public static void demonstrateBestPractices() {
        System.out.println("========== 版本控制最佳实践 ==========");
        System.out.println();
        System.out.println("【SQL示例】");
        System.out.println("-- 查询时获取版本号");
        System.out.println("SELECT id, data, version FROM record WHERE id = 1;");
        System.out.println();
        System.out.println("-- 更新时带上版本号");
        System.out.println("UPDATE record SET data = 'new_data', version = version + 1");
        System.out.println("WHERE id = 1 AND version = 0;");
        System.out.println();
        System.out.println("-- 如果更新行数为0，说明版本冲突");
        System.out.println();
        System.out.println("【MyBatis-Plus示例】");
        System.out.println("@Data");
        System.out.println("public class Record {");
        System.out.println("    @TableId(type = IdType.AUTO)");
        System.out.println("    private Long id;");
        System.out.println("    private String data;");
        System.out.println("    @Version // 乐观锁注解");
        System.out.println("    private Integer version;");
        System.out.println("}");
        System.out.println();
        System.out.println("【重试策略】");
        System.out.println("1. 最大重试次数：3次");
        System.out.println("2. 重试间隔：指数退避");
        System.out.println("3. 超过重试次数：返回失败");
        System.out.println();
        System.out.println("【注意事项】");
        System.out.println("1. 版本号必须是数值类型");
        System.out.println("2. 更新时必须带上版本号");
        System.out.println("3. 重试时要重新获取最新版本号");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrate();
        System.out.println("\n");
        demonstrateBestPractices();
    }
}
