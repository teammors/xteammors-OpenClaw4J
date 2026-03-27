---
name: "nano-pdf"
description: "通过自然语言指令编辑 PDF 文件的指定页面。需要 nano-pdf CLI（自动安装）。"
---

# nano-pdf / PDF 自然语言编辑

使用自然语言指令编辑 PDF 的指定页面。

## 使用方式

```
用户: "帮我把报告第1页的标题改成Q3报告"
→ python3 nano_pdf.py report.pdf 1 "把标题改为Q3报告"

用户: "修复 slides.pdf 第3页第2段的错别字"
→ python3 nano_pdf.py slides.pdf 3 "修复第2段的错别字" --output slides_fixed.pdf
```

## 参数

| 参数 | 说明 | 必填 |
|---|---|---|
| `<PDF文件>` | 要编辑的 PDF 文件路径 | 是 |
| `<页码>` | 要编辑的页码（0-based 或 1-based） | 是 |
| `<编辑指令>` | 自然语言描述的编辑操作 | 是 |
| `--output <路径>` | 输出文件路径 | 否 |

## 依赖

- `nano-pdf` Python 包（缺失时自动安装）
- 注意：页码可能是 0-based 或 1-based，如果结果偏移一页，请换用另一种。
