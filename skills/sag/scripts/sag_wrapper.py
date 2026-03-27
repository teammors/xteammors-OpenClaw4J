#!/usr/bin/env python3
import sys, subprocess, os
def main():
    args = sys.argv[1:]
    if not args or args[0] == "test":
        print("test ok"); return
    if not os.environ.get("ELEVENLABS_API_KEY"):
        print("Error: 未配置 ELEVENLABS_API_KEY"); return
    try:
        subprocess.run(["sag"] + args, timeout=60)
    except FileNotFoundError:
        print("Error: 未安装 sag CLI。安装: brew install steipete/tap/sag")
if __name__ == "__main__": main()
