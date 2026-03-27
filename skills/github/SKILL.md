---
name: "github"
description: "通过 gh CLI 操作 GitHub：管理 issues、PRs、CI 运行、API 查询。需先安装 gh CLI 并认证。"
---

# GitHub 操作

通过 `gh` CLI 操作 GitHub 仓库。

## 常用命令

```
# 列出 issues
python3 github.py issue list owner/repo --state open

# 查看 PR 详情
python3 github.py pr view owner/repo 55

# 检查 PR CI 状态
python3 github.py pr checks owner/repo 55

# 列出 CI 运行
python3 github.py run list owner/repo --limit 10

# 调用 API
python3 github.py api repos/owner/repo --jq '.stargazers_count'
```

## 使用方式

```
用户: "看看 openclaw4j 项目的 open issues"
→ python3 github.py issue list xteammors/OpenClaw4J --state open

用户: "PR #12 的 CI 状态"
→ python3 github.py pr checks xteammors/OpenClaw4J 12
```

## 前置条件

1. 安装 gh CLI：`brew install gh` 或 `apt install gh`
2. 认证：`gh auth login`
