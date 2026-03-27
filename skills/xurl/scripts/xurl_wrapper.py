#!/usr/bin/env python3
import sys, subprocess
def main():
    args = sys.argv[1:]
    if not args or args[0] == "test":
        print("test ok"); return
    try:
        subprocess.run(["xurl"] + args, timeout=30)
    except FileNotFoundError:
        print("Error: 未安装 xurl CLI。安装: brew install xdevplatform/tap/xurl")
if __name__ == "__main__": main()
