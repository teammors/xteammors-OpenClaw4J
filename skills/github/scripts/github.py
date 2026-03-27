#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GitHub 操作技能
通过 gh CLI 操作 GitHub：issues、PRs、CI、API 查询。
"""

import sys
import os
import subprocess
import json

def check_gh():
    """检查 gh CLI 是否已安装且已认证"""
    try:
        result = subprocess.run(["gh", "auth", "status"], capture_output=True, text=True)
        if result.returncode == 0 or "Logged in" in result.stderr:
            return True
        print("gh CLI 已安装但未认证，请先运行: gh auth login")
        return False
    except FileNotFoundError:
        print("Error: 未安装 gh CLI")
        print("安装方法: brew install gh  或  apt install gh")
        return False

def run_gh(args):
    """执行 gh 命令并返回输出"""
    cmd = ["gh"] + args
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        error = result.stderr.strip() if result.stderr.strip() else result.stdout.strip()
        return "Error: " + error
    return result.stdout.strip()

def main():
    args = sys.argv[1:]

    if not args or args[0] == "test":
        print("test ok")
        return

    if args[0] == "--help" or args[0] == "-h":
        print("GitHub 操作技能 — 通过 gh CLI 操作 GitHub")
        print()
        print("命令:")
        print("  issue list <repo> [--state open|closed]    列出 issues")
        print("  issue create <repo> --title <标题> --body <内容>  创建 issue")
        print("  issue close <repo> <number>               关闭 issue")
        print("  pr list <repo> [--state open|merged|closed] 列出 PRs")
        print("  pr view <repo> <number>                   查看 PR 详情")
        print("  pr checks <repo> <number>                 查看 PR CI 状态")
        print("  pr create <repo> --title <标题> --body <内容>  创建 PR")
        print("  pr merge <repo> <number> [--squash]        合并 PR")
        print("  run list <repo> [--limit N]                列出 CI 运行")
        print("  run view <repo> <run-id> [--log-failed]    查看 CI 运行详情")
        print("  api <endpoint> [--jq <表达式>]             调用 GitHub API")
        print()
        print("示例:")
        print("  issue list owner/repo --state open")
        print("  pr checks owner/repo 55")
        print("  api repos/owner/repo --jq '.stargazers_count'")
        return

    if not check_gh():
        return

    cmd = args[0]
    rest = args[1:]

    if cmd == "issue":
        sub = rest[0] if rest else "list"
        if sub == "list":
            print(run_gh(["issue", "list"] + rest[1:]))
        elif sub == "create":
            print(run_gh(["issue", "create"] + rest[1:]))
        elif sub == "close":
            print(run_gh(["issue", "close"] + rest[1:]))
        else:
            print(run_gh(["issue", sub] + rest[1:]))

    elif cmd == "pr":
        sub = rest[0] if rest else "list"
        if sub == "list":
            print(run_gh(["pr", "list"] + rest[1:]))
        elif sub == "view":
            print(run_gh(["pr", "view"] + rest[1:]))
        elif sub == "checks":
            print(run_gh(["pr", "checks"] + rest[1:]))
        elif sub == "create":
            print(run_gh(["pr", "create"] + rest[1:]))
        elif sub == "merge":
            print(run_gh(["pr", "merge"] + rest[1:]))
        else:
            print(run_gh(["pr", sub] + rest[1:]))

    elif cmd == "run":
        sub = rest[0] if rest else "list"
        if sub == "list":
            print(run_gh(["run", "list"] + rest[1:]))
        elif sub == "view":
            print(run_gh(["run", "view"] + rest[1:]))
        else:
            print(run_gh(["run", sub] + rest[1:]))

    elif cmd == "api":
        print(run_gh(["api"] + rest))

    else:
        # 直接透传给 gh
        print(run_gh(args))

if __name__ == "__main__":
    main()
