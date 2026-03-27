#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""本地离线文字转语音（sherpa-onnx）"""
import sys, os, subprocess

def main():
    args = sys.argv[1:]
    if not args or args[0] == "test":
        print("test ok"); return
    runtime_dir = os.environ.get("SHERPA_ONNX_RUNTIME_DIR", "")
    model_dir = os.environ.get("SHERPA_ONNX_MODEL_DIR", "")
    if not runtime_dir or not model_dir:
        print("Error: 未配置 SHERPA_ONNX_RUNTIME_DIR / SHERPA_ONNX_MODEL_DIR"); return

    tts_bin = os.path.join(runtime_dir, "sherpa-onnx-offline-tts")
    if not os.path.isfile(tts_bin):
        print("Error: 未找到 sherpa-onnx-offline-tts，在 " + runtime_dir); return

    text = args[0]
    output = args[1] if len(args) > 1 else "output.wav"
    model_path = os.path.join(model_dir, "model.onnx")

    cmd = [tts_bin, "--tts-model", model_path, "--output-filename", output, text]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
    if result.returncode == 0:
        print("语音生成成功: " + output)
    else:
        print("Error: " + result.stderr.strip())

if __name__ == "__main__": main()
