---
name: "sherpa-onnx-tts"
description: "本地离线文字转语音，无需网络和 API Key。需下载 sherpa-onnx runtime 和模型。"
---

# 本地离线 TTS

```
python3 local_tts.py "要转语音的文字" output.wav
```

## 配置

- 环境变量：`SHERPA_ONNX_RUNTIME_DIR`（runtime 目录）
- 环境变量：`SHERPA_ONNX_MODEL_DIR`（模型目录）
