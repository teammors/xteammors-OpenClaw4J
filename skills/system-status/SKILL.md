---
name: "system-status"
description: "Retrieves the current system status, including CPU usage, memory usage, disk usage, and the count of running user processes."
---

# System Status Skill

This skill enables the agent to fetch real-time system performance metrics.
It uses a Python script to gather information about CPU, Memory, Disk, and Process count.

## Capabilities
- Get CPU usage percentage
- Get Memory usage (Total, Used, Free, Percent)
- Get Disk usage (Total, Used, Free, Percent)
- Get count of non-system processes (running user processes)

## Usage
When the user asks "How is the system doing?", "Check server status", or "Show CPU and memory usage", this skill should be invoked.

## Configuration
No specific configuration required for this skill.

## Dependencies
This skill requires the `psutil` python library.
The script handles dependency installation automatically. If `psutil` is missing, it will attempt to install it via pip (using `--break-system-packages` where necessary).
