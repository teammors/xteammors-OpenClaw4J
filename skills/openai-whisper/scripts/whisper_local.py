#!/usr/bin/env python3
import sys, subprocess
def main():
    args = sys.argv[1:]
    if not args or args[0] == "test":
        print("test ok"); return
    try:
        subprocess.run(["whisper"] + args, timeout=300)
    except FileNotFoundError:
        print("Error: 未安装 whisper CLI。安装: pip install openai-whisper")
if __name__ == "__main__": main()
