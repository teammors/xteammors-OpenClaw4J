---
name: "trello"
description: "通过 Trello REST API 管理看板、列表和卡片。需配置 TRELLO_API_KEY 和 TRELLO_TOKEN。"
---

# Trello API

## 使用方式

```
python3 trello_api.py boards
python3 trello_api.py lists <board_id>
python3 trello_api.py cards <list_id>
python3 trello_api.py create-card <list_id> "卡片标题" "描述"
python3 trello_api.py comment <card_id> "评论内容"
python3 trello_api.py archive <card_id>
```

## 配置

设置环境变量：
- `TRELLO_API_KEY` — 从 https://trello.com/app-key 获取
- `TRELLO_TOKEN` — 在 app-key 页面点击 "Token" 链接生成
