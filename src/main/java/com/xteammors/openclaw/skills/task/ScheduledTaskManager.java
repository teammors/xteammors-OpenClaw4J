package com.xteammors.openclaw.skills.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.xteammors.openclaw.adapter.TeammorsMessageAdapter;
import com.xteammors.openclaw.rag.service.RAGService;
import com.xteammors.openclaw.skills.GenericSkill;
import com.xteammors.openclaw.skills.SkillGeneratorSkill;
import com.xteammors.openclaw.utils.ThreadUtils;
import com.xteammors.openclaw.wssdk.XMessageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Component
public class ScheduledTaskManager {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskManager.class);

    @Autowired
    private ApplicationContext applicationContext;

    private TaskScheduler taskScheduler;
    private Map<String, ScheduledTask> tasks;
    private Map<String, ScheduledFuture<?>> scheduledFutures;
    private final String TASKS_FILE = "scheduled_tasks.json";

    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        ((ThreadPoolTaskScheduler) taskScheduler).setPoolSize(10);
        ((ThreadPoolTaskScheduler) taskScheduler).setThreadNamePrefix("scheduled-task-");
        ((ThreadPoolTaskScheduler) taskScheduler).initialize();
        
        tasks = new HashMap<>();
        scheduledFutures = new HashMap<>();
        
        // 从文件加载任务
        loadTasksFromFile();
        
        log.info("ScheduledTaskManager initialized");
    }

    @PreDestroy
    public void destroy() {
        // 保存任务到文件
        saveTasksToFile();
        
        // 取消所有定时任务
        for (String taskId : scheduledFutures.keySet()) {
            ScheduledFuture<?> future = scheduledFutures.get(taskId);
            if (future != null && !future.isCancelled()) {
                future.cancel(true);
            }
        }
        
        // 关闭线程池
        if (taskScheduler instanceof ThreadPoolTaskScheduler) {
            ((ThreadPoolTaskScheduler) taskScheduler).shutdown();
        }
        
        log.info("ScheduledTaskManager destroyed");
    }

    /**
     * 添加定时任务
     */
    public ScheduledTask addTask(ScheduledTask task) {
        if (task == null || task.getChatId() == null || task.getInterval() == null || task.getSkill() == null) {
            throw new IllegalArgumentException("Task parameters cannot be null");
        }

        // 检查用户任务数量限制
        int userTaskCount = getTasksByChatId(task.getChatId()).size();
        if (userTaskCount >= 10) { // 每用户最多10个任务
            throw new IllegalStateException("Maximum 10 tasks per user");
        }

        // 检查时间间隔范围
        long intervalMillis = task.getIntervalMillis();
        if (intervalMillis < 1000) { // 最小1秒
            throw new IllegalArgumentException("Minimum interval is 1 second");
        }
        if (intervalMillis > 30L * 24 * 60 * 60 * 1000) { // 最大30天
            throw new IllegalArgumentException("Maximum interval is 30 days");
        }

        // 取消之前的同名任务（如果存在）
        cancelTaskByDescription(task.getChatId(), task.getDescription());

        // 调度任务
        scheduleTask(task);

        // 保存任务
        tasks.put(task.getTaskId(), task);
        
        // 保存任务到文件
        saveTasksToFile();
        
        log.info("Added scheduled task: {}", task);
        return task;
    }

    /**
     * 调度任务
     */
    private void scheduleTask(ScheduledTask task) {
        // 计算首次执行延迟
        long initialDelay = task.getIntervalMillis();

        // 调度任务
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
            () -> executeTask(task),
            initialDelay
        );

        // 保存调度结果
        task.setScheduledTaskId(UUID.randomUUID().toString());
        scheduledFutures.put(task.getTaskId(), future);
    }

    /**
     * 执行任务
     */
    private void executeTask(ScheduledTask task) {
        if (!task.isActive()) {
            return;
        }

        try {
            log.info("Executing scheduled task: {}", task.getTaskId());

            // 获取核心任务语句
            String coreTaskText = task.getParams() != null && !task.getParams().isEmpty() ? task.getParams() : task.getSkill();
            log.info("Core task text: {} for chatId: {}", coreTaskText, task.getChatId());

            // 获取必要的服务实例
            RAGService ragService = applicationContext.getBean(RAGService.class);
            GenericSkill genericSkill = applicationContext.getBean(GenericSkill.class);
            SkillGeneratorSkill skillGeneratorSkill = applicationContext.getBean(SkillGeneratorSkill.class);
            
            // 创建消息适配器并执行
            TeammorsMessageAdapter teammorsMessageAdapter = new TeammorsMessageAdapter(task.getChatId(), coreTaskText, ragService, genericSkill, skillGeneratorSkill);
            ThreadUtils.instance().getExecutor().execute(teammorsMessageAdapter);

            // 重置下次执行时间
            task.resetNextRun();

        } catch (Exception e) {
            log.error("Error executing scheduled task: {}", task.getTaskId(), e);
            // 发送错误信息给用户
            if(task.getChatId().contains("-")){
                XMessageClient.instance().sendToGroupTxtMessage("定时任务执行失败: " + e.getMessage(), task.getChatId(), 1);
            }else {
                XMessageClient.instance().sendSingleUserTxtMessage("定时任务执行失败: " + e.getMessage(), task.getChatId(), 1);
            }

        }
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        ScheduledTask task = tasks.get(taskId);
        if (task == null) {
            return false;
        }

        // 取消调度
        ScheduledFuture<?> future = scheduledFutures.remove(taskId);
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }

        // 移除任务
        tasks.remove(taskId);
        task.stop();

        // 保存任务到文件
        saveTasksToFile();

        log.info("Cancelled scheduled task: {}", taskId);
        return true;
    }

    /**
     * 按描述取消任务
     */
    public boolean cancelTaskByDescription(String chatId, String description) {
        for (ScheduledTask task : tasks.values()) {
            if (task.getChatId().equals(chatId)) {
                // 支持模糊匹配
                if (task.getDescription().equals(description) || 
                    task.getDescription().contains(description) || 
                    description.contains(task.getDescription())) {
                    return cancelTask(task.getTaskId());
                }
            }
        }
        return false;
    }

    /**
     * 取消用户的所有定时任务
     */
    public int cancelAllTasks(String chatId) {
        List<String> taskIdsToCancel = new ArrayList<>();
        
        // 收集用户的所有任务ID
        for (ScheduledTask task : tasks.values()) {
            if (task.getChatId().equals(chatId)) {
                taskIdsToCancel.add(task.getTaskId());
            }
        }
        
        // 取消所有任务
        int cancelledCount = 0;
        for (String taskId : taskIdsToCancel) {
            if (cancelTask(taskId)) {
                cancelledCount++;
            }
        }
        
        log.info("Cancelled {} tasks for chatId: {}", cancelledCount, chatId);
        return cancelledCount;
    }

    /**
     * 获取所有任务
     */
    public List<ScheduledTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * 获取用户的任务
     */
    public List<ScheduledTask> getTasksByChatId(String chatId) {
        List<ScheduledTask> userTasks = new ArrayList<>();
        for (ScheduledTask task : tasks.values()) {
            if (task.getChatId().equals(chatId)) {
                userTasks.add(task);
            }
        }
        return userTasks;
    }

    /**
     * 获取任务
     */
    public ScheduledTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * 暂停任务
     */
    public boolean pauseTask(String taskId) {
        ScheduledTask task = tasks.get(taskId);
        if (task == null) {
            return false;
        }

        task.pause();
        log.info("Paused scheduled task: {}", taskId);
        return true;
    }

    /**
     * 恢复任务
     */
    public boolean resumeTask(String taskId) {
        ScheduledTask task = tasks.get(taskId);
        if (task == null) {
            return false;
        }

        task.resume();
        log.info("Resumed scheduled task: {}", taskId);
        return true;
    }

    /**
     * 清理过期任务
     */
    public void cleanupTasks() {
        List<String> tasksToRemove = new ArrayList<>();
        for (Map.Entry<String, ScheduledTask> entry : tasks.entrySet()) {
            ScheduledTask task = entry.getValue();
            if (!task.isActive() && task.getNextRun().plusDays(7).isBefore(LocalDateTime.now())) {
                tasksToRemove.add(entry.getKey());
            }
        }

        for (String taskId : tasksToRemove) {
            cancelTask(taskId);
        }

        if (!tasksToRemove.isEmpty()) {
            log.info("Cleaned up {} expired tasks", tasksToRemove.size());
        }
    }

    /**
     * 保存任务到文件
     */
    private void saveTasksToFile() {
        try {
            File file = new File(TASKS_FILE);
            List<ScheduledTask> activeTasks = new ArrayList<>();
            
            // 只保存活跃的任务
            for (ScheduledTask task : tasks.values()) {
                if (task.isActive()) {
                    activeTasks.add(task);
                }
            }
            
            // 序列化任务
            String json = JSON.toJSONString(activeTasks, true);
            
            // 写入文件
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
            }
            
            log.info("Saved {} active tasks to file: {}", activeTasks.size(), TASKS_FILE);
        } catch (Exception e) {
            log.error("Error saving tasks to file", e);
        }
    }

    /**
     * 从文件加载任务
     */
    private void loadTasksFromFile() {
        try {
            File file = new File(TASKS_FILE);
            if (!file.exists()) {
                log.info("Tasks file not found, skipping load");
                return;
            }
            
            // 读取文件
            try (FileReader reader = new FileReader(file);
                 BufferedReader bufferedReader = new BufferedReader(reader)) {
                // 读取文件内容为字符串
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                String jsonContent = sb.toString();
                
                // 反序列化任务
                List<ScheduledTask> loadedTasks = JSON.parseObject(
                    jsonContent, 
                    new TypeReference<List<ScheduledTask>>() {}
                );
                
                // 加载任务
                for (ScheduledTask task : loadedTasks) {
                    // 检查任务是否仍然有效
                    if (task.isActive() && task.getNextRun().isAfter(LocalDateTime.now().minusDays(1))) {
                        // 重新调度任务
                        scheduleTask(task);
                        tasks.put(task.getTaskId(), task);
                        log.info("Loaded task: {}", task.getTaskId());
                    } else {
                        log.info("Skipping inactive task: {}", task.getTaskId());
                    }
                }
                
                log.info("Loaded {} tasks from file", loadedTasks.size());
            }
        } catch (Exception e) {
            log.error("Error loading tasks from file", e);
        }
    }
}
