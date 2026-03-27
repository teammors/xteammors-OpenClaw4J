# Chat Summary Skill

## Description
Summarize chat messages from the database based on user requirements. This skill queries the chat_messages table and uses AI to generate a summary of recent conversations.

## Invocation

### Command
```
chat-summary
```

### Natural Language Examples
- "把刚才聊天的内容总结一下"
- "帮我把下午聊天内容总结一下"
- "总结一下最近的聊天记录"
- "总结一下今天和xxx的聊天"

## Parameters
- User input describing what chat content to summarize (time range, specific chat, etc.)
- chatId is passed from the execute method context

## Configuration

```yaml
database:
  host: "db_ip"
  port: db_port
  database: "db_name"
  username: "user"
  password: "password"
  jdbc_url: "jdbc:mysql://db_ip:3306/db_name?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B0&autoReconnect=true"
```

## Database Schema

Table: `chat_messages`

Key columns:
- `id`: Message ID (varchar)
- `text`: Message content text (text)
- `timestamp`: Entry time format: 2026-03-25 00:59:56 (varchar)
- `mediaUrls`: Actual media URLs (text)
- `chatId`: Chat ID (varchar)

## Output Format

时间段: [开始时间] - [结束时间]

聊天内容要点:
1. 要点1
2. 要点2
3. 要点3
...

## Implementation

The skill works in three steps (all AI calls are handled by Java side):
1. **First AI call (Java)**: Build SQL query based on user requirements and table structure
2. **Database query (Python)**: Execute the SQL query and return raw data as JSON
3. **Second AI call (Java)**: Summarize the chat content retrieved from database

## Scripts

- `query_chat.py`: Queries the database and returns results as JSON. No AI calls in this script.

## Dependencies
- Python 3.x
- PyMySQL
- PyYAML

Note: AI processing is done by the Java application using the configured ChatClient. No OpenAI API key is needed in the Python script.
