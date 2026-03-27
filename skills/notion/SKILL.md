---
name: "notion"
description: "通过 Notion REST API 管理页面、数据库和内容块。需配置 NOTION_API_KEY。"
---

# Notion API

## 使用方式

```
python3 notion_api.py search "项目计划"
python3 notion_api.py page <page_id>
python3 notion_api.py blocks <page_id>
python3 notion_api.py query-db <db_id> '{"filter": {...}}'
```

## 配置

设置环境变量 `NOTION_API_KEY` 或创建 `~/.config/notion/api_key` 文件。
