---
name: "unread-mail"
description: "Retrieves unread emails from the mailbox. Invoke this skill when the user asks to check for unread emails or what's new in their inbox. 当用户请求查看未读邮件时使用此技能。"
---

# Unread Mail Skill / 未读邮件技能

This skill enables the agent to fetch unread emails from the configured mailbox.
It connects via IMAP and retrieves the subject and sender of unread messages.
这个技能可以让代理从配置的邮箱中获取未读邮件。
它通过 IMAP 连接并检索未读邮件的主题和发件人。

## Capabilities / 功能
- List unread emails / 列出未读邮件
- Show sender and subject / 显示发件人和主题
- Limit the number of emails retrieved / 限制检索的邮件数量

## Usage / 使用场景
When the user says "Check my unread emails" or "Do I have any new mail?", this skill should be invoked.
当用户说"帮我看看是否有未读邮件"、"检查我的未读邮件"、"有什么新邮件"时使用此技能。

## Configuration
```yaml
mail:
  imap_host: imap server address
  imap_port: 993
  username: your mail address
  password: your mail client password (It's not a login password, but a client password)
```
