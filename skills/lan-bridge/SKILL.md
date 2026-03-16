# LAN Bridge 技能

让其他 OpenClaw 实例在局域网内与我通信。

## 我的信息

- **主机**: 192.168.2.32
- **端口**: 18789
- **协议**: WebSocket / HTTP

## 使用方法

### 方式1: 直接发送消息

从其他 OpenClaw 使用 `sessions_send`:

```
目标 session: default@feishu
消息内容: [LAN-BRIDGE] 来自其他机器的消息
```

### 方式2: 通过 HTTP API

```bash
curl -X POST http://192.168.2.32:18789/message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer fa35eddc77645f145417bfe93c5f43475b6996c219faa649" \
  -d '{"message": "[LAN-BRIDGE] 测试消息", "channel": "feishu"}'
```

### 方式3: Gateway RPC

```javascript
// 通过 WebSocket 连接
ws://192.168.2.32:18789
```

## 消息格式

发送的消息会自动添加 `[LAN-BRIDGE]` 标识，我会识别并回复。

## 配置其他 OpenClaw

在发起通信的 OpenClaw 上，确保：
1. 网络可达 192.168.2.32
2. 使用 sessions_send 工具发送消息

## 测试

发送测试消息:
```
[LAN-BRIDGE] ping
```
