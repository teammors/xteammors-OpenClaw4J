#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""OpenAI Whisper API 语音转文字"""
import sys, os, subprocess

def install_package(pkg):
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", pkg, "--break-system-packages"],
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except Exception: pass

def main():
    args = sys.argv[1:]
    if not args or args[0] == "test":
        print("test ok"); return
    api_key = os.environ.get("OPENAI_API_KEY", "")
    if not api_key:
        print("Error: 未配置 OPENAI_API_KEY"); return
    try:
        import requests
    except ImportError:
        install_package("requests"); import requests

    audio_path = args[0]
    if not os.path.isfile(audio_path):
        print("Error: 文件不存在: " + audio_path); return

    with open(audio_path, "rb") as f:
        resp = requests.post(
            "https://api.openai.com/v1/audio/transcriptions",
            headers={"Authorization": "Bearer " + api_key},
            data={"model": "whisper-1"},
            files={"file": f},
            timeout=120
        )
    if resp.status_code == 200:
        print(resp.json().get("text", ""))
    else:
        print("Error: " + str(resp.status_code) + " " + resp.text[:200])

if __name__ == "__main__": main()
