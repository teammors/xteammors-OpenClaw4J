#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
视频帧提取技能
使用 ffmpeg 从视频中提取单帧图片或创建缩略图。
"""

import sys
import os
import subprocess

def check_ffmpeg():
    """检查 ffmpeg 是否已安装"""
    try:
        subprocess.run(["ffmpeg", "-version"], capture_output=True, check=True)
        return True
    except (FileNotFoundError, subprocess.CalledProcessError):
        return False

def extract_frame(video_path, output_path, time=None, index=None):
    """
    从视频中提取一帧
    :param video_path: 视频文件路径
    :param output_path: 输出图片路径
    :param time: 时间戳，格式 HH:MM:SS
    :param index: 帧索引号
    """
    if not os.path.isfile(video_path):
        return "Error: 文件不存在: " + video_path

    # 确保输出目录存在
    output_dir = os.path.dirname(output_path)
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)

    if index is not None:
        cmd = [
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
            "-i", video_path,
            "-vf", "select=eq(n\\," + str(index) + ")",
            "-vframes", "1",
            output_path
        ]
    elif time:
        cmd = [
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
            "-ss", time,
            "-i", video_path,
            "-frames:v", "1",
            output_path
        ]
    else:
        # 默认提取第一帧
        cmd = [
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
            "-i", video_path,
            "-vf", "select=eq(n\\,0)",
            "-vframes", "1",
            output_path
        ]

    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        return "Error: ffmpeg 执行失败: " + result.stderr.strip()

    if os.path.isfile(output_path):
        size = os.path.getsize(output_path)
        return "帧提取成功！\n输出文件: " + output_path + "\n文件大小: " + str(size) + " 字节"
    else:
        return "Error: 输出文件未生成"

def get_video_info(video_path):
    """获取视频基本信息"""
    cmd = [
        "ffprobe", "-hide_banner", "-loglevel", "error",
        "-print_format", "json",
        "-show_format", "-show_streams",
        video_path
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode == 0:
        return result.stdout
    return None

def main():
    args = sys.argv[1:]

    if not args or args[0] == "test":
        print("test ok")
        return

    if args[0] == "--help" or args[0] == "-h":
        print("使用方法: video_frames.py <视频文件> [--time HH:MM:SS] [--index N] --out <输出路径>")
        print("示例:")
        print("  video_frames.py video.mp4 --out /tmp/frame.jpg")
        print("  video_frames.py video.mp4 --time 00:00:10 --out /tmp/frame-10s.jpg")
        print("  video_frames.py video.mp4 --index 0 --out /tmp/frame0.png")
        return

    if not check_ffmpeg():
        print("Error: 未安装 ffmpeg，请先安装 ffmpeg")
        return

    video_path = args[0]
    time_arg = None
    index_arg = None
    output_path = None

    # 解析参数
    i = 1
    while i < len(args):
        if args[i] == "--time" and i + 1 < len(args):
            time_arg = args[i + 1]
            i += 2
        elif args[i] == "--index" and i + 1 < len(args):
            index_arg = int(args[i + 1])
            i += 2
        elif args[i] == "--out" and i + 1 < len(args):
            output_path = args[i + 1]
            i += 2
        else:
            i += 1

    if not output_path:
        # 默认输出到当前目录
        base = os.path.splitext(os.path.basename(video_path))[0]
        output_path = base + "_frame.jpg"

    result = extract_frame(video_path, output_path, time=time_arg, index=index_arg)
    print(result)

if __name__ == "__main__":
    main()
