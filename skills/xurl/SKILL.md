---
name: "xurl"
description: "通过 xurl CLI 操作 X/Twitter API：发推、回复、搜索、点赞、关注、DM。需先安装 xurl 并认证。"
---

# X/Twitter 操作

通过 `xurl` CLI 操作 X (Twitter) API。

## 常用命令

```
python3 xurl_wrapper.py post "Hello!"
python3 xurl_wrapper.py search "query" -n 10
python3 xurl_wrapper.py reply POST_ID "回复内容"
python3 xurl_wrapper.py like POST_ID
python3 xurl_wrapper.py dm @user "私信内容"
python3 xurl_wrapper.py whoami
```

## 前置条件

1. 安装：`brew install xdevplatform/tap/xurl`
2. 认证：`xurl auth oauth2`
