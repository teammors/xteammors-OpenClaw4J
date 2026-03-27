package com.xteammors.openclaw.skills.task;

import java.time.LocalDateTime;
import java.util.UUID;

public class ScheduledTask {
    private String taskId;
    private String chatId;
    private String interval; // 时间间隔 (e.g., "30s", "5m", "1h", "1d")
    private String skill;
    private String params;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime nextRun;
    private boolean active;
    private String scheduledTaskId; // 用于取消定时任务的ID

    // 默认构造方法，用于反序列化
    public ScheduledTask() {
    }

    // 构造方法
    public ScheduledTask(String chatId, String interval, String skill, String params, String description) {
        this.taskId = "task-" + UUID.randomUUID().toString().substring(0, 8);
        this.chatId = chatId;
        this.interval = interval;
        this.skill = skill;
        this.params = params;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.nextRun = calculateNextRun();
        this.active = true;
    }

    /**
     * 计算下次执行时间
     */
    public LocalDateTime calculateNextRun() {
        if (interval == null || interval.isEmpty()) {
            return LocalDateTime.now();
        }

        int value = Integer.parseInt(interval.replaceAll("\\D", ""));
        String unit = interval.replaceAll("\\d", "");

        switch (unit) {
            case "s":
                return LocalDateTime.now().plusSeconds(value);
            case "m":
                return LocalDateTime.now().plusMinutes(value);
            case "h":
                return LocalDateTime.now().plusHours(value);
            case "d":
                return LocalDateTime.now().plusDays(value);
            default:
                return LocalDateTime.now();
        }
    }

    /**
     * 计算时间间隔（毫秒）
     */
    public long getIntervalMillis() {
        if (interval == null || interval.isEmpty()) {
            return 60000; // 默认1分钟
        }

        int value = Integer.parseInt(interval.replaceAll("\\D", ""));
        String unit = interval.replaceAll("\\d", "");

        switch (unit) {
            case "s":
                return value * 1000L;
            case "m":
                return value * 60 * 1000L;
            case "h":
                return value * 60 * 60 * 1000L;
            case "d":
                return value * 24 * 60 * 60 * 1000L;
            default:
                return 60000; // 默认1分钟
        }
    }

    /**
     * 重置下次执行时间
     */
    public void resetNextRun() {
        this.nextRun = calculateNextRun();
    }

    /**
     * 暂停任务
     */
    public void pause() {
        this.active = false;
    }

    /**
     * 恢复任务
     */
    public void resume() {
        this.active = true;
        this.nextRun = calculateNextRun();
    }

    /**
     * 停止任务
     */
    public void stop() {
        this.active = false;
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getNextRun() {
        return nextRun;
    }

    public void setNextRun(LocalDateTime nextRun) {
        this.nextRun = nextRun;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getScheduledTaskId() {
        return scheduledTaskId;
    }

    public void setScheduledTaskId(String scheduledTaskId) {
        this.scheduledTaskId = scheduledTaskId;
    }

    @Override
    public String toString() {
        return "ScheduledTask{" +
                "taskId='" + taskId + '\'' +
                ", chatId='" + chatId + '\'' +
                ", interval='" + interval + '\'' +
                ", skill='" + skill + '\'' +
                ", params='" + params + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", nextRun=" + nextRun +
                ", active=" + active +
                ", scheduledTaskId='" + scheduledTaskId + '\'' +
                '}';
    }
}
