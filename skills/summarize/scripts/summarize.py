#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
内容总结技能
从 URL/文件提取文本，调用火山引擎大模型生成摘要。
"""

import sys
import os
import subprocess

def install_package(pkg):
    try:
        subprocess.check_call([
            sys.executable, "-m", "pip", "install", pkg, "--break-system-packages"
        ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return True
    except Exception:
        return False

def check_dependencies():
    for pkg in ["requests", "beautifulsoup4"]:
        mod = pkg.replace("-", "_").replace(".", "_")
        try:
            __import__(mod)
        except ImportError:
            install_package(pkg)

def get_api_config():
    base_url = os.environ.get("VOLCENGINE_BASE_URL", "")
    api_key  = os.environ.get("VOLCENGINE_API_KEY", "")
    model    = os.environ.get("VOLCENGINE_MODEL", "")
    if not base_url or not api_key:
        return None
    return {
        "url": base_url.rstrip("/") + "/chat/completions",
        "api_key": api_key,
        "model": model
    }

def fetch_url(url):
    """获取 URL 内容，提取正文文本"""
    import requests
    from bs4 import BeautifulSoup

    resp = requests.get(url, timeout=15, headers={
        "User-Agent": "Mozilla/5.0 (compatible; OpenClaw4J/1.0)"
    })
    resp.raise_for_status()

    soup = BeautifulSoup(resp.text, "html.parser")

    # 移除脚本和样式
    for tag in soup(["script", "style", "nav", "footer", "header"]):
        tag.decompose()

    # 尝试找 article 或 main
    article = soup.find("article") or soup.find("main")
    if article:
        text = article.get_text(separator="\n", strip=True)
    else:
        text = soup.get_text(separator="\n", strip=True)

    # 清理多余空行
    lines = [line.strip() for line in text.split("\n") if line.strip()]
    return "\n".join(lines)

def read_file(path):
    """读取本地文件内容"""
    ext = os.path.splitext(path)[1].lower()

    if ext == ".pdf":
        try:
            from PyPDF2 import PdfReader
            reader = PdfReader(path)
            text = ""
            for page in reader.pages:
                text += page.extract_text() or ""
            return text
        except ImportError:
            install_package("PyPDF2")
            from PyPDF2 import PdfReader
            reader = PdfReader(path)
            text = ""
            for page in reader.pages:
                text += page.extract_text() or ""
            return text
    else:
        # 纯文本文件
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            return f.read()

def summarize_with_llm(config, content, length="medium"):
    """调用 LLM 生成摘要"""
    import requests

    length_map = {
        "short": "3-5句话，约100字",
        "medium": "一段话，约200字",
        "long": "详细摘要，约500字"
    }
    length_desc = length_map.get(length, length_map["medium"])

    # 截断过长内容
    max_chars = 12000
    if len(content) > max_chars:
        content = content[:max_chars] + "\n...(内容已截断)"

    system = "你是一个专业的文本摘要助手。根据用户提供的原文，生成简洁、准确、有条理的中文摘要。"
    user = "请为以下内容生成摘要（" + length_desc + "）：\n\n" + content

    headers = {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + config["api_key"]
    }
    body = {
        "model": config["model"],
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": user}
        ],
        "temperature": 0.3,
        "max_tokens": 2000
    }

    resp = requests.post(config["url"], headers=headers, json=body, timeout=60)
    resp.raise_for_status()
    return resp.json()["choices"][0]["message"]["content"]

def main():
    args = sys.argv[1:]

    if not args or args[0] == "test":
        print("test ok")
        return

    if args[0] == "--help" or args[0] == "-h":
        print("使用方法: summarize.py <URL或文件路径> [--length short|medium|long]")
        print()
        print("示例:")
        print('  summarize.py "https://example.com/article"')
        print('  summarize.py report.pdf --length long')
        print('  summarize.py notes.txt --length short')
        return

    check_dependencies()

    source = args[0]
    length = "medium"
    if "--length" in args:
        idx = args.index("--length")
        if idx + 1 < len(args):
            length = args[idx + 1]

    config = get_api_config()
    if config is None:
        print("Error: 未检测到火山引擎配置（VOLCENGINE_BASE_URL / VOLCENGINE_API_KEY）")
        return

    # 提取内容
    try:
        if source.startswith("http://") or source.startswith("https://"):
            print("正在获取网页内容...")
            content = fetch_url(source)
        elif os.path.isfile(source):
            print("正在读取文件...")
            content = read_file(source)
        else:
            print("Error: 无法识别的输入（非URL且非文件路径）: " + source)
            return
    except Exception as e:
        print("Error: 获取内容失败: " + str(e))
        return

    if not content or len(content.strip()) < 50:
        print("Error: 提取到的内容为空或过短")
        return

    print("内容长度: " + str(len(content)) + " 字符")
    print("正在调用大模型生成摘要...")

    try:
        summary = summarize_with_llm(config, content, length)
        print()
        print("=" * 50)
        print("摘要:")
        print(summary)
        print("=" * 50)
    except Exception as e:
        print("Error: 调用大模型失败: " + str(e))

if __name__ == "__main__":
    main()
