package com.xteammors.openclaw.utils;

public class PositiveIntegerValidator {

    // 只允许纯数字，不能以0开头（除非就是0本身）
    public static boolean isPositiveInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("(0|[1-9]\\d*)");
    }

    // 允许以0开头的数字（如0123）
    public static boolean isPositiveIntegerAllowLeadingZero(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("\\d+");
    }

    public static void main(String[] args) {
        String[] testCases = {"0", "123", "0123", "00123", "12345", "abc", "12a3", "-123", "12.34"};

        System.out.println("严格模式（不允许前导0）：");
        for (String test : testCases) {
            System.out.println("'" + test + "' is positive integer: " + isPositiveInteger(test));
        }

        System.out.println("\n宽松模式（允许前导0）：");
        for (String test : testCases) {
            System.out.println("'" + test + "' is positive integer: " + isPositiveIntegerAllowLeadingZero(test));
        }
    }
}