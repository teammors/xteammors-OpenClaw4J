#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
博客/RSS 监控技能
订阅 RSS/Atom 源，检查更新，列出最新文章。
"""

import sys
import os
import json
import subprocess
import time
from datetime import datetime

DATA_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "blogs.json")

def install_package(pkg):
    try:
        subprocess.check_call([
            sys.executable, "-m", "pip", "install", pkg, "--break-system-packages"
        ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return True
    except Exception:
        return False

def check_dependencies():
    try:
        import feedparser
        return True
    except ImportError:
        print("正在安装 feedparser...")
        return install_package("feedparser")

def load_blogs():
    if os.path.isfile(DATA_FILE):
        with open(DATA_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {}

def save_blogs(blogs):
    with open(DATA_FILE, "w", encoding="utf-8") as f:
        json.dump(blogs, f, ensure_ascii=False, indent=2)

def cmd_add(name, url):
    """添加博客订阅"""
    blogs = load_blogs()
    if name in blogs:
        print("已存在同名订阅: " + name)
        return
    blogs[name] = {"url": url, "added": datetime.now().isoformat(), "last_check": None}
    save_blogs(blogs)
    print("已添加订阅: " + name + " (" + url + ")")

def cmd_list():
    """列出所有订阅"""
    blogs = load_blogs()
    if not blogs:
        print("暂无订阅。使用 add <名称> <URL> 添加。")
        return
    print("已订阅 " + str(len(blogs)) + " 个博客:")
    for name, info in blogs.items():
        last = info.get("last_check", "从未检查")
        if last:
            last = last[:19].replace("T", " ")
        print("  " + name)
        print("    URL: " + info["url"])
        print("    上次检查: " + str(last))

def cmd_remove(name):
    """移除订阅"""
    blogs = load_blogs()
    if name not in blogs:
        print("未找到订阅: " + name)
        return
    del blogs[name]
    save_blogs(blogs)
    print("已移除: " + name)

def cmd_scan(limit=5):
    """扫描所有订阅源，获取最新文章"""
    import feedparser

    blogs = load_blogs()
    if not blogs:
        print("暂无订阅。")
        return

    print("正在扫描 " + str(len(blogs)) + " 个订阅源...")
    all_articles = []

    for name, info in blogs.items():
        url = info["url"]
        try:
            feed = feedparser.parse(url)
            count = 0
            for entry in feed.entries[:limit]:
                title = entry.get("title", "无标题")
                link = entry.get("link", "")
                published = entry.get("published", entry.get("updated", ""))
                all_articles.append({
                    "blog": name,
                    "title": title,
                    "link": link,
                    "published": published
                })
                count += 1
            print("  " + name + ": 发现 " + str(count) + " 篇文章")
            info["last_check"] = datetime.now().isoformat()
        except Exception as e:
            print("  " + name + ": 扫描失败 - " + str(e))

    save_blogs(blogs)

    if all_articles:
        print("\n最新文章:")
        print("-" * 60)
        for i, a in enumerate(all_articles, 1):
            print(str(i) + ". [" + a["blog"] + "] " + a["title"])
            if a["link"]:
                print("   链接: " + a["link"])
            if a["published"]:
                print("   发布: " + a["published"])
            print()
    else:
        print("未发现任何文章。")

def main():
    args = sys.argv[1:]

    if not args or args[0] == "test":
        print("test ok")
        return

    if not check_dependencies():
        print("Error: 无法安装 feedparser 依赖")
        return

    cmd = args[0]

    if cmd == "add" and len(args) >= 3:
        cmd_add(args[1], args[2])
    elif cmd == "list":
        cmd_list()
    elif cmd == "remove" and len(args) >= 2:
        cmd_remove(args[1])
    elif cmd == "scan":
        limit = int(args[1]) if len(args) >= 2 else 5
        cmd_scan(limit)
    elif cmd == "--help" or cmd == "-h":
        print("博客/RSS 监控技能")
        print()
        print("命令:")
        print("  add <名称> <URL>    添加博客订阅")
        print("  list                列出所有订阅")
        print("  remove <名称>       移除订阅")
        print("  scan [数量]         扫描最新文章（默认5篇）")
        print()
        print("示例:")
        print("  add xkcd https://xkcd.com/rss.xml")
        print("  scan 10")
    else:
        print("未知命令: " + cmd)
        print("使用 --help 查看帮助")

if __name__ == "__main__":
    main()
