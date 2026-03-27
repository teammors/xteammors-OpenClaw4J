#!/usr/bin/env python3
import sys, subprocess
def main():
    args = sys.argv[1:]
    if not args or args[0] == "test":
        print("test ok"); return
    try:
        subprocess.run(["oracle"] + args, timeout=300)
    except FileNotFoundError:
        print("Error: 未安装 oracle CLI。安装: npm install -g @steipete/oracle")
if __name__ == "__main__": main()
