---
name: "summarize"
description: "总结 URL 网页、PDF 文件、文本文件的内容。提取正文后调用火山引擎大模型生成中文摘要。"
---

# Summarize / 内容总结

从 URL、PDF、文本文件中提取正文，调用火山引擎大模型生成摘要。

## 使用方式

```
用户: "帮我总结一下这篇文章 https://example.com/article"
→ python3 summarize.py "https://example.com/article"

用户: "把这个PDF总结一下，要简短"
→ python3 summarize.py report.pdf --length short

用户: "详细总结一下这些笔记"
→ python3 summarize.py notes.txt --length long
```

## 参数

| 参数 | 说明 | 必填 |
|---|---|---|
| `<URL或文件>` | 网页URL或本地文件路径 | 是 |
| `--length` | 摘要长度: short(100字)/medium(200字)/long(500字) | 否（默认medium） |

## 支持的输入

- **URL**：自动提取网页正文（HTML解析，去除导航/广告等噪音）
- **PDF**：提取 PDF 文本内容（需要 PyPDF2）
- **文本文件**：直接读取（.txt / .md 等）

## 依赖

- `requests`、`beautifulsoup4`（自动安装）
- `PyPDF2`（仅 PDF 时需要，自动安装）
- 火山引擎大模型（通过 `VOLCENGINE_*` 环境变量，Java端自动注入）
