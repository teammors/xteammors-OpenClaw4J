---
name: "system-status"
description: "Retrieves the current system status, including CPU usage, memory usage, disk usage, and the count of running user processes. 获取系统状态，包括 CPU 使用率、内存使用情况、磁盘使用情况和运行中的用户进程数量。"
---

# System Status Skill / 系统状态技能

This skill enables the agent to fetch real-time system performance metrics.
It uses a Python script to gather information about CPU, Memory, Disk, and Process count.
这个技能可以让代理获取实时系统性能指标。
它使用 Python 脚本来收集 CPU、内存、磁盘和进程数量的信息。

## Capabilities / 功能
- Get CPU usage percentage / 获取 CPU 使用率百分比
- Get Memory usage (Total, Used, Free, Percent) / 获取内存使用情况 (总计、已用、空闲、百分比)
- Get Disk usage (Total, Used, Free, Percent) / 获取磁盘使用情况 (总计、已用、空闲、百分比)
- Get count of non-system processes (running user processes) / 获取非系统进程数量 (运行中的用户进程)

## Usage / 使用场景
When the user asks "How is the system doing?", "Check server status", or "Show CPU and memory usage".
当用户询问"系统状态如何？"、"查看服务器状态"、"显示 CPU 和内存使用情况"、"查看系统状态"时使用此技能。

## Configuration
No specific configuration required for this skill.

## Dependencies
This skill requires the `psutil` python library.
The script handles dependency installation automatically. If `psutil` is missing, it will attempt to install it via pip (using `--break-system-packages` where necessary).
