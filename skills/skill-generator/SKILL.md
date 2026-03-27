---
name: "skill-generator"
description: "技能生成与升级器。自动创建新技能，或升级迭代现有技能。均通过AI大模型循环生成Python脚本并自测，直至通过。"
---

# Skill Generator & Upgrader / 技能生成与升级器

自动检测用户需求：若需新技能则创建，若需改进现有技能则升级迭代。

## 核心流程

```
用户需求 → AI意图识别 → CREATE / UPGRADE → 确认 → AI循环生成+自测 → 注册/更新
```

## 功能

### 1. 创建新技能
用户描述一个全新的功能需求时，自动生成 Python 脚本。

```
用户: "帮我创建一个查询股票价格的技能"
→ AI意图识别: CREATE
→ 用户确认
→ AI生成Python脚本 + 自测循环(最多5次)
→ 写入 skills/stock-price/scripts/stock_price.py
→ 生成 SKILL.md
→ 注册到RAG
```

### 2. 升级现有技能
用户反馈某个技能有问题或需要改进时，自动升级。

```
用户: "pdf创建成功了，但是中文部分变成黑色看不见，能改一下吗？"
→ AI意图识别: UPGRADE, skill_name=pdf-generator
→ 读取现有脚本
→ 用户确认
→ AI基于现有代码 + 改进需求 重新生成 + 自测循环
→ 覆盖写入原脚本
→ 重新注册到RAG
```

## 升级时的 AI Prompt 要点

升级提示词中必须包含：
1. **现有代码完整内容** — 让 AI 知道改哪里
2. **改进需求描述** — 用户的具体诉求
3. **错误反馈**（迭代时）— 上一次自测的 stderr 输出

## 自测机制

- 将生成的 Python 脚本写入临时目录
- 以 `python3 <script> test` 执行
- 捕获 stdout 和 stderr
- exitCode != 0 则记录 stderr 进入下一轮迭代
- 超时 30 秒自动终止

## 已有技能列表（自动扫描）

AI 会自动扫描 `skills/` 目录下所有包含 `scripts/*.py` 的子目录，作为升级目标候选。

## 输出格式

### 创建成功
```
技能创建成功！
技能名称: pdf-generator
描述: 将文本内容转换为PDF文件
```

### 升级成功
```
技能升级成功！
技能名称: pdf-generator
改进内容: 支持中文显示，解决中文变黑块的问题
```

### 升级失败
```
技能升级失败: 经过 5 次尝试仍无法生成可用的脚本。
原始脚本未被修改。
```

## 依赖
- Python 3.x
- Spring AI ChatClient（已配置的AI大模型）
