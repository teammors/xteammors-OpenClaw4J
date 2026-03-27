---
name: "video-frames"
description: "使用 ffmpeg 从视频中提取单帧图片或创建缩略图。支持按时间戳或帧号提取。"
---

# Video Frames / 视频帧提取

从视频文件中提取单帧图片，支持按时间戳或帧索引定位。

## 功能

- 提取视频第一帧（默认）
- 按时间戳提取（`--time HH:MM:SS`）
- 按帧号提取（`--index N`）
- 自定义输出路径（`--out /path/to/output.jpg`）

## 使用方式

```
用户: "帮我从这个视频第10秒截一帧图"
→ python3 video_frames.py /path/to/video.mp4 --time 00:00:10 --out /tmp/frame.jpg

用户: "提取视频第一帧"
→ python3 video_frames.py /path/to/video.mp4 --out /tmp/first_frame.png
```

## 依赖

- `ffmpeg` / `ffprobe`（需在系统 PATH 中）

## 参数说明

| 参数 | 说明 | 必填 |
|---|---|---|
| `<视频文件>` | 视频文件路径 | 是 |
| `--time HH:MM:SS` | 提取指定时间戳的帧 | 否 |
| `--index N` | 提取第 N 帧 | 否 |
| `--out <路径>` | 输出图片路径 | 否（默认当前目录） |

输出格式由文件扩展名决定（.jpg / .png）。
