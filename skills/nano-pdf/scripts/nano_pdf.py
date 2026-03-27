#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PDF 编辑技能
使用 nano-pdf CLI 通过自然语言指令编辑 PDF。
"""

import sys
import os
import subprocess

def ensure_nano_pdf():
    """确保 nano-pdf 已安装"""
    try:
        result = subprocess.run(["nano-pdf", "--help"], capture_output=True, text=True)
        if result.returncode == 0 or "nano-pdf" in result.stderr.lower() or "usage" in result.stderr.lower():
            return True
    except FileNotFoundError:
        pass

    print("正在安装 nano-pdf...")
    try:
        subprocess.check_call([
            sys.executable, "-m", "pip", "install", "nano-pdf", "--break-system-packages"
        ], stdout=subprocess.DEVNULL)
        print("nano-pdf 安装成功")
        return True
    except Exception as e:
        print("Error: 安装 nano-pdf 失败: " + str(e))
        return False

def edit_pdf(pdf_path, page, instruction, output_path=None):
    """编辑 PDF 的指定页面"""
    if not os.path.isfile(pdf_path):
        return "Error: 文件不存在: " + pdf_path

    cmd = ["nano-pdf", "edit", pdf_path, str(page), instruction]
    if output_path:
        cmd.extend(["--output", output_path])

    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)

    if result.returncode != 0:
        error = result.stderr.strip() if result.stderr.strip() else result.stdout.strip()
        return "Error: nano-pdf 执行失败: " + error

    output = result.stdout.strip()
    if output:
        return output
    return "PDF 编辑完成。输出文件在同一目录下。"

def main():
    args = sys.argv[1:]

    if not args or args[0] == "test":
        print("test ok")
        return

    if args[0] == "--help" or args[0] == "-h":
        print("使用方法: nano_pdf.py <PDF文件> <页码> \"<编辑指令>\" [--output <输出路径>]")
        print()
        print("示例:")
        print('  nano_pdf.py report.pdf 1 "把标题改为Q3报告"')
        print('  nano_pdf.py slides.pdf 3 "修复第2段的错别字" --output slides_fixed.pdf')
        print()
        print("注意: 页码支持 0-based 或 1-based，如结果偏移一页，请换用另一种。")
        return

    if not ensure_nano_pdf():
        return

    if len(args) < 3:
        print("Error: 参数不足。使用 --help 查看帮助。")
        return

    pdf_path = args[0]
    page = args[1]
    instruction = args[2]

    output_path = None
    if "--output" in args:
        idx = args.index("--output")
        if idx + 1 < len(args):
            output_path = args[idx + 1]

    result = edit_pdf(pdf_path, page, instruction, output_path)
    print(result)

if __name__ == "__main__":
    main()
