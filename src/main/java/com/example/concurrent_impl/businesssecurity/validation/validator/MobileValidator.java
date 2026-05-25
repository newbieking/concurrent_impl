package com.example.concurrent_impl.businesssecurity.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 【手机号校验器实现】
 *
 * 【实现原理】
 * 实现ConstraintValidator接口，在isValid方法中编写校验逻辑
 *
 * @author concurrent_impl
 * @date 2024
 */
public class MobileValidator implements ConstraintValidator<Mobile, String> {

    private String regexp;
    private boolean nullable;

    @Override
    public void initialize(Mobile constraintAnnotation) {
        this.regexp = constraintAnnotation.regexp();
        this.nullable = constraintAnnotation.nullable();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return nullable;
        }
        return value.matches(regexp);
    }

    /**
     * 中国大陆手机号正则
     *
     * 【规则】
     * - 1开头
     * - 第二位是3-9
     * - 后面9位数字
     */
    private static final Pattern CHINA_MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 中国香港手机号正则
     *
     * 【规则】
     * - 8位数字
     * - 通常以5、6、9开头
     */
    private static final Pattern HK_MOBILE_PATTERN = Pattern.compile("^[569]\\d{7}$");

    /**
     * 国际手机号正则
     *
     * 【规则】
     * - 以+开头
     * - 区号1-3位
     * - 号码4-15位
     */
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile("^\\+\\d{1,3}\\d{4,15}$");

    /**
     * 校验中国大陆手机号
     *
     * @param mobile 手机号
     * @return 是否合法
     */
    public static boolean isChinaMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return false;
        }
        return CHINA_MOBILE_PATTERN.matcher(mobile).matches();
    }

    /**
     * 校验中国香港手机号
     *
     * @param mobile 手机号
     * @return 是否合法
     */
    public static boolean isHKMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return false;
        }
        return HK_MOBILE_PATTERN.matcher(mobile).matches();
    }

    /**
     * 校验国际手机号
     *
     * @param mobile 手机号
     * @return 是否合法
     */
    public static boolean isInternationalMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return false;
        }
        return INTERNATIONAL_PATTERN.matcher(mobile).matches();
    }

    /**
     * 校验手机号（自动识别格式）
     *
     * @param mobile 手机号
     * @return 是否合法
     */
    public static boolean isValidMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return false;
        }

        // 去除空格和横线
        mobile = mobile.replaceAll("[\\s-]", "");

        // 校验中国大陆手机号
        if (isChinaMobile(mobile)) {
            return true;
        }

        // 校验国际手机号
        if (isInternationalMobile(mobile)) {
            return true;
        }

        return false;
    }

    /**
     * 手机号脱敏
     *
     * 【使用场景】
     * 日志打印、数据展示时隐藏手机号中间4位
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号
     */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return "";
        }

        if (mobile.length() < 7) {
            return mobile;
        }

        // 保留前3位和后4位，中间用****代替
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    /**
     * 演示手机号校验
     */
    public static void demonstrate() {
        System.out.println("========== 手机号校验演示 ==========");
        System.out.println();

        // 测试中国大陆手机号
        System.out.println("【中国大陆手机号】");
        String[] chinaMobiles = {"13800138000", "15012345678", "12345678901", "1380013800", "138001380001"};
        for (String mobile : chinaMobiles) {
            System.out.println(mobile + " -> " + (isChinaMobile(mobile) ? "合法" : "非法"));
        }
        System.out.println();

        // 测试脱敏
        System.out.println("【手机号脱敏】");
        String[] mobiles = {"13800138000", "15012345678", "123"};
        for (String mobile : mobiles) {
            System.out.println(mobile + " -> " + maskMobile(mobile));
        }
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrate();
    }
}
