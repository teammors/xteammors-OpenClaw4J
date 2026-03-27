#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Trello API 技能
通过 Trello REST API 管理看板、列表、卡片。
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

def get_auth():
    key = os.environ.get("TRELLO_API_KEY", "")
    token = os.environ.get("TRELLO_TOKEN", "")
    if not key or not token:
        return None
    return "key=" + key + "&token=" + token

def api_call(method, path, data=None):
    import requests
    auth = get_auth()
    if not auth:
        return "Error: 未配置 TRELLO_API_KEY / TRELLO_TOKEN"
    sep = "&" if "?" in path else "?"
    url = "https://api.trello.com/1" + path + sep + auth
    resp = requests.request(method, url, data=data, timeout=15)
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
    if cmd == "boards":
        print(api_call("GET", "/members/me/boards?fields=name,id"))
    elif cmd == "lists" and len(args) >= 2:
        print(api_call("GET", "/boards/" + args[1] + "/lists?fields=name,id"))
    elif cmd == "cards" and len(args) >= 2:
        print(api_call("GET", "/lists/" + args[1] + "/cards?fields=name,id,desc"))
    elif cmd == "create-card" and len(args) >= 3:
        print(api_call("POST", "/cards?idList=" + args[1] + "&name=" + args[2] +
                        ("&desc=" + args[3] if len(args) > 3 else "")))
    elif cmd == "comment" and len(args) >= 3:
        print(api_call("POST", "/cards/" + args[1] + "/actions/comments?text=" + args[2]))
    elif cmd == "archive" and len(args) >= 2:
        print(api_call("PUT", "/cards/" + args[1] + "?closed=true"))
    elif cmd == "--help":
        print("Trello API 技能")
        print("  boards              列出看板")
        print("  lists <board_id>    列出列表")
        print("  cards <list_id>     列出卡片")
        print("  create-card <list_id> <title> [desc]  创建卡片")
        print("  comment <card_id> <text>  添加评论")
        print("  archive <card_id>   归档卡片")
    else:
        print("Error: 未知命令，使用 --help 查看帮助")

if __name__ == "__main__":
    main()
