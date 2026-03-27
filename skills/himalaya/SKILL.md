---
name: "himalaya"
description: "通过 himalaya CLI 管理邮件（IMAP/SMTP）：列出、阅读、搜索、回复、转发、删除。需先安装 himalaya 并配置邮箱。"
---

# Himalaya 邮件管理

通过 `himalaya` CLI 管理邮件。

## 常用命令

```
python3 himalaya_wrapper.py envelope list                    # 列出收件箱
python3 himalaya_wrapper.py envelope list --folder "Sent"    # 列出已发送
python3 himalaya_wrapper.py message read 42                  # 阅读邮件
python3 himalaya_wrapper.py envelope list from john@test.com  # 搜索
python3 himalaya_wrapper.py message delete 42                # 删除
python3 himalaya_wrapper.py attachment download 42           # 下载附件
```

## 前置条件

1. 安装：`brew install himalaya`
2. 配置：`~/.config/himalaya/config.toml`（IMAP/SMTP 账号信息）
