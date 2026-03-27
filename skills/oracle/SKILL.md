---
name: "oracle"
description: "将 prompt + 代码文件打包成一次性请求，发送给其他模型分析。需安装 oracle CLI。"
---

# Oracle / Prompt 打包工具

```
python3 oracle_wrapper.py "帮我审查这段代码" --files src/main.py
python3 oracle_wrapper.py --engine browser --model gpt-5.2-pro "分析这个repo"
```

## 安装

`npm install -g @steipete/oracle`
