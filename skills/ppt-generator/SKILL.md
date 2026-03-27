---
name: "ppt-generator"
description: "根据用户输入的文字内容，通过两阶段AI调用（结构设计+内容丰富）生成高质量PPT"
---

# PPT 生成器

## 两阶段生成流程

```
用户文本内容
    ↓
[Stage 1] 结构设计 — 豆包模型分析内容，设计PPT整体结构
    输出：封面 → 议程 → 内容页(n) → 总结 → 行动计划 → 致谢
    每页：标题(结论性) + 要点(动词开头，含数据)
    ↓
[Stage 2] 内容丰富 — 逐页调用豆包模型丰富内容
    每页增加：更多要点 / 关键数据 / 演讲备注
    ↓
[Stage 3] python-pptx 渲染
    深色封面 + 绿色强调色条 + 圆点列表 + 数据高亮
```

## Prompt 设计要点

### Stage 1 — 结构 prompt
- **角色**：资深PPT架构师
- **原则**：逻辑为王、每页聚焦、数据驱动、标题要结论性
- **页数自动决定**：按内容长度 5~18 页
- **类型**：cover / agenda / content / data / summary / action / thanks

### Stage 2 — 丰富 prompt
- **逐页丰富**：4-6 个要点、关键数据、演讲备注
- **上下文感知**：传入前后页标题，保持逻辑连贯

## API 配置
- 模型：火山引擎 doubao-seed-2-0-pro-260215
- 环境变量由 Java 端自动注入：`VOLCENGINE_*`

## 使用
```
用户: "帮我把这些内容做成PPT..."
→ [1/3] 生成结构大纲
→ [2/3] 逐页丰富内容
→ [3/3] 构建PPT文件
→ 输出: generated_presentation.pptx
```

## 依赖
- `requests`（自动安装）
- `python-pptx`（自动安装）
