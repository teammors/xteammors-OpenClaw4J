package com.xteammors.openclaw.utils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间工具类
 */
public class TimeUtils {

    /*
     * 将时间转换为时间戳
     */
    public static String dateToStamp(String s) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String res = "";
        if (!"".equals(s)) {
            try {
                res = String.valueOf(sdf.parse(s).getTime() / 1000);
            } catch (Exception e) {
                //System.out.println("传入了null值");
            }
        } else {
            long time = System.currentTimeMillis();
            res = String.valueOf(time / 1000);
        }

        return res;
    }

    /*
     * 将时间戳转换为时间
     */
    public static String stampToDate(int time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String times = format.format(new Date(time * 1000L));
        return times;
    }

    /*
     * 将时间戳转换为时间
     */
    public static String stampToDateLong(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String times = format.format(new Date(time * 1000L));
        return times;
    }

    /*
     * 获得当前时间
     */
    public static String getDateTime() {
        Date date = new Date();
        String strDateFormat = "yyyy-MM-dd HH:mm:ss";//yyyy-MM-dd HH:mm:ss
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        return sdf.format(date);
    }

    /*
     * 获得当前时间 --毫秒
     */
    public static String getDateTimeHao() {
        Date date = new Date();
        String strDateFormat = "yyyy-MM-dd HH:mm:ss.SSSSSS";//yyyy-MM-dd HH:mm:ss
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        return sdf.format(date);
    }

    /*
     * 获得当前日期
     */
    public static String getDate() {
        Date date = new Date();
        String strDateFormat = "yyyy-MM-dd";//yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        return sdf.format(date);
    }

    /*
     * 获得当前日期
     */
    public static String getDateF() {
        Date date = new Date();
        String strDateFormat = "yyyyMMdd";//yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        return sdf.format(date);
    }

    /**
     * Params :
     * dateFormat : 日期格式 如 yyyy-MM-dd
     * date:要修改的日期的String类型
     * type:要增加的日期，年或者月或者日
     * num:要加的位数
     */
    public static String addDateNum(String dateFormat, String date, String type, int num) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        Calendar c = Calendar.getInstance();
        c.setTime(format.parse(date));
        if (type.equals("MONTH")) {
            c.add(Calendar.MONTH, +num);
            return format.format(c.getTime());
        } else if (type.equals("YEAR")) {
            c.add(Calendar.YEAR, +num);
            return format.format(c.getTime());
        } else if (type.equals("DAY")) {
            c.add(Calendar.DATE, +num);
            return format.format(c.getTime());
        } else if (type.equals("SECOND")) {
            c.add(Calendar.SECOND, +num);
            return format.format(c.getTime());
        } else {
            return date;
        }
    }

    /**
     * 获取纳秒时间搓
     *
     * @return
     */
    public static long getNanoTime() {
        return System.currentTimeMillis() * 1000000L + System.nanoTime() % 1000000L;
    }

    public static String getTimeSt() {
        //当前时间十位时间戳
        long nowTimestamp = System.currentTimeMillis() / 1000;
        return nowTimestamp + "";
    }

    public static long getTimeSt10() {
        //当前时间十位时间戳
        return System.currentTimeMillis() / 1000;
    }

    public static String getTimeSt13() {
        //当前时间十位时间戳
        long nowTimestamp = System.currentTimeMillis();
        return nowTimestamp + "";
    }

    /**
     * 获取明天的日期字符串
     *
     * @return
     */
    public static String tomorrowDateStr() {
        Date date = new Date();//取时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        //把日期往后增加一天.整数往后推,负数往前移动(1:表示明天、-1：表示昨天，0：表示今天)
        calendar.add(Calendar.DATE, 1);

        //这个时间就是日期往后推一天的结果
        date = calendar.getTime();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String tomorrowStr = formatter.format(date);
        return tomorrowStr;
    }

    public static boolean checkDate() {
        boolean rs = false;
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String nowDate = getDate() + " 21:30:00";
            String nowDate2 = getDateTime();
            Date sd1 = df.parse(nowDate);
            Date sd2 = df.parse(nowDate2);
            rs = sd1.before(sd2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rs;
    }

    public static boolean isZhouyi() {
        boolean iszhouyi = false;
        try {
            DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String Date = getDate();  //定义初始是周一
            Date testdate = sdf.parse(Date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(testdate);
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                //System.out.println(sdf.format(cal.getTime()) + "=========" + "是周一=========");
                iszhouyi = true;
            }
        } catch (Exception e) {

        }
        return iszhouyi;
    }

    /**
     * 判断时间是否在时间段内
     *
     * @param nowTime
     * @param beginTime
     * @param endTime
     * @return
     */
    public static boolean belongCalendar(Date nowTime, Date beginTime, Date endTime) {
        Calendar date = Calendar.getInstance();
        date.setTime(nowTime);

        Calendar begin = Calendar.getInstance();
        begin.setTime(beginTime);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);

        if (date.after(begin) && date.before(end)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isBelong() {

        SimpleDateFormat df = new SimpleDateFormat("HH:mm");//设置日期格式
        Date now = null;
        Date beginTime = null;
        Date endTime = null;
        boolean zhouyi = isZhouyi();
        try {
            now = df.parse(df.format(new Date()));
            if (zhouyi) {
                beginTime = df.parse("19:00");
                endTime = df.parse("20:00");
            } else {
                beginTime = df.parse("19:00");
                endTime = df.parse("20:00");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Boolean flag = belongCalendar(now, beginTime, endTime);
        return flag;
    }


    public static boolean isBetTime(String endTime, int bfLong) {

        boolean cFlag = false;

        try {
            String canTime = addDateNum("yyyy-MM-dd HH:mm:ss", stampToDate(Integer.parseInt(endTime)), "SECOND", -(bfLong + 5));
            String nowTime = getDateTime();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date sd1 = df.parse(canTime);
            Date sd2 = df.parse(nowTime);
            cFlag = sd1.before(sd2);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return cFlag;
    }

    public static String changeFormat(String originalDateStr){
        // 解析原始格式
        DateTimeFormatter originalFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(originalDateStr, originalFormatter);

        // 格式化为新格式
        DateTimeFormatter newFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String newDateStr = dateTime.format(newFormatter);
        return newDateStr;
    }


    public static SecretKey generate() {
        try {
            // 使用AES算法生成256位的SecretKey
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void main(String[] args) {
        SecretKey secretKey = generate();
        if (secretKey != null) {
            System.out.println(Base64.getEncoder().encodeToString(secretKey.getEncoded()));
        }

    }


}
