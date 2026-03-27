#!/usr/bin/env python3
import sys, subprocess
def main():
    args = sys.argv[1:]
    if not args or args[0] == "test":
        print("test ok"); return
    try:
        subprocess.run(["gemini"] + args, timeout=60)
    except FileNotFoundError:
        print("Error: 未安装 gemini CLI。安装: brew install gemini-cli")
if __name__ == "__main__": main()
