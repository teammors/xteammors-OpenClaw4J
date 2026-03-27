#!/usr/bin/env python3
import sys, subprocess, os
def main():
    args = sys.argv[1:]
    if not args or args[0] == "test":
        print("test ok"); return
    if not os.environ.get("GOOGLE_PLACES_API_KEY"):
        print("Error: 未配置 GOOGLE_PLACES_API_KEY"); return
    try:
        subprocess.run(["goplaces"] + args, timeout=30)
    except FileNotFoundError:
        print("Error: 未安装 goplaces CLI。安装: brew install steipete/tap/goplaces")
if __name__ == "__main__": main()
