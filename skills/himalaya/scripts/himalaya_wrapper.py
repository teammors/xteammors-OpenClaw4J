#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
邮件管理技能（Himalaya）
通过 himalaya CLI 管理 IMAP/SMTP 邮件。
"""

import sys
import subprocess

def check_himalaya():
    try:
        subprocess.run(["himalaya", "--version"], capture_output=True, check=True)
        return True
    except (FileNotFoundError, subprocess.CalledProcessError):
        print("Error: 未安装 himalaya CLI")
        print("安装: brew install himalaya")
        return False

def run_himalaya(args):
    result = subprocess.run(["himalaya"] + args, capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        return "Error: " + (result.stderr.strip() or result.stdout.strip())
    return result.stdout.strip()

def main():
    args = sys.argv[1:]
    if not args or args[0] == "test":
        print("test ok")
        return
    if not check_himalaya():
        return
    print(run_himalaya(args))

if __name__ == "__main__":
    main()
