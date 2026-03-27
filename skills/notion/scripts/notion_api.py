#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Notion API 技能
通过 Notion REST API 管理页面、数据库、块。
"""

import sys
import os
import json
import subprocess

def install_package(pkg):
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", pkg, "--break-system-packages"],
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return True
    except Exception:
        return False

def get_api_key():
    key = os.environ.get("NOTION_API_KEY", "")
    if key:
        return key
    config_path = os.path.expanduser("~/.config/notion/api_key")
    if os.path.isfile(config_path):
        with open(config_path) as f:
            return f.read().strip()
    return None

def api_call(method, endpoint, data=None):
    import requests
    key = get_api_key()
    if not key:
        return "Error: 未配置 NOTION_API_KEY"
    url = "https://api.notion.com/v1" + endpoint
    headers = {
        "Authorization": "Bearer " + key,
        "Notion-Version": "2022-06-28",
        "Content-Type": "application/json"
    }
    resp = requests.request(method, url, headers=headers, json=data, timeout=15)
    if resp.status_code >= 400:
        return "Error: " + str(resp.status_code) + " " + resp.text[:200]
    return json.dumps(resp.json(), ensure_ascii=False, indent=2)

def main():
    args = sys.argv[1:]
    if not args or args[0] == "test":
        print("test ok")
        return
    try:
        import requests
    except ImportError:
        install_package("requests")

    cmd = args[0]
    if cmd == "search":
        query = " ".join(args[1:]) if len(args) > 1 else ""
        print(api_call("POST", "/search", {"query": query}))
    elif cmd == "page" and len(args) >= 2:
        print(api_call("GET", "/pages/" + args[1]))
    elif cmd == "blocks" and len(args) >= 2:
        print(api_call("GET", "/blocks/" + args[1] + "/children"))
    elif cmd == "create-page" and len(args) >= 2:
        print(api_call("POST", "/pages", json.loads(args[1])))
    elif cmd == "query-db" and len(args) >= 2:
        print(api_call("POST", "/databases/" + args[1] + "/query",
                        json.loads(args[2]) if len(args) > 2 else {}))
    elif cmd == "--help":
        print("Notion API 技能")
        print("  search [query]           搜索页面")
        print("  page <page_id>           获取页面")
        print("  blocks <page_id>         获取页面内容块")
        print("  create-page <json>       创建页面")
        print("  query-db <db_id> [json]  查询数据库")
    else:
        print("Error: 未知命令，使用 --help 查看帮助")

if __name__ == "__main__":
    main()
