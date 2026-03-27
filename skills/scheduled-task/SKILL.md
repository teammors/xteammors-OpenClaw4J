# Scheduled Task Skill

## Description
Create and manage scheduled tasks that execute specified skills at regular intervals. This skill allows users to set up recurring tasks like checking crypto prices, sending reminders, or performing other automated actions.

## Invocation

### Command
```
scheduled-task
```

### Natural Language Examples
- "帮我每间隔2分钟收集一次BTC，ETH，SOL的实时价格"
- "每小时提醒我检查邮件"
- "每天早上8点发送天气报告"
- "每5秒检查一次系统状态"

## Parameters
- Time interval (e.g., 2分钟, 1小时, 5秒, 1天)
- Task description (what to do)
- Target skill to execute
- Additional parameters for the target skill
- chatId (automatically passed from context)

## Configuration

```yaml
scheduled-task:
  max_tasks_per_user: 10
  default_interval: "60s"
  min_interval: "1s"
  max_interval: "30d"
  task_storage: "memory"  # memory, file, or database
```

## Task Format

```json
{
  "taskId": "unique-task-id",
  "chatId": "user-chat-id",
  "interval": "30s",  // 时间间隔 (s, m, h, d)
  "skill": "crypto-price",  // 要执行的技能
  "params": "BTC,ETH,SOL",  // 技能参数
  "description": "每30秒检查加密货币价格",
  "createdAt": "2026-03-25T12:00:00Z",
  "nextRun": "2026-03-25T12:00:30Z"
}
```

## Output Format

### Task Creation
```
定时任务已创建成功！

任务ID: task-12345
描述: 每2分钟收集一次BTC，ETH，SOL的实时价格
间隔: 2分钟
下次执行: 2026-03-25 14:32:00
```

### Task List
```
您当前的定时任务：
1. 任务ID: task-12345
   描述: 每2分钟收集一次BTC，ETH，SOL的实时价格
   间隔: 2分钟
   状态: 运行中
   下次执行: 2026-03-25 14:32:00

2. 任务ID: task-67890
   描述: 每小时提醒检查邮件
   间隔: 1小时
   状态: 运行中
   下次执行: 2026-03-25 15:00:00
```

## Implementation

The skill works in the following way:
1. **Task Creation**: Parse user input to extract time interval and task details
2. **Task Scheduling**: Use Java's scheduling mechanism to execute tasks at specified intervals
3. **Skill Execution**: When a task runs, execute the specified skill with the provided parameters
4. **Result Delivery**: Send the skill execution results back to the specified chatId

## Scripts

- `manage_tasks.py`: Manages task creation, listing, and deletion (for future extension)

## Dependencies
- Python 3.x
- PyYAML

## Notes
- Time intervals are specified using units: s (seconds), m (minutes), h (hours), d (days)
- The minimum interval is 1 second, maximum is 30 days
- Each user can have up to 10 active tasks
- Tasks are stored in memory by default, but can be configured to use file or database storage
