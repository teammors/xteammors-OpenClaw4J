---
name: "blogwatcher"
description: "订阅 RSS/Atom 博客源，扫描更新，查看最新文章。无需安装 Go CLI，纯 Python 实现。"
---

# Blogwatcher / 博客监控

订阅和监控 RSS/Atom 博客源，随时查看最新文章。

## 命令

| 命令 | 说明 | 示例 |
|---|---|---|
| `add <名称> <URL>` | 添加订阅 | `add xkcd https://xkcd.com/rss.xml` |
| `list` | 列出所有订阅 | `list` |
| `remove <名称>` | 移除订阅 | `remove xkcd` |
| `scan [数量]` | 扫描最新文章 | `scan 10` |

## 使用方式

```
用户: "帮我订阅阮一峰的博客"
→ add ruanyifeng https://www.ruanyifeng.com/blog/atom.xml

用户: "看看有什么新文章"
→ scan

用户: "我订阅了哪些博客？"
→ list
```

## 实现

- 使用 `feedparser` 解析 RSS/Atom（缺失时自动安装）
- 订阅数据存储在同目录 `blogs.json` 中
- 纯 Python 实现，无需安装 Go CLI
