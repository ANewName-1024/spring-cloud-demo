# 文件上传技能

允许其他 OpenClaw 向我发送文件。

## 我的信息

- **IP**: 192.168.2.32
- **端口**: 18789

## 发送文件的方法

### 方法1: 在 WSL 启动 HTTP 服务器 (推荐)

**WSL上执行:**
```bash
# 进入文件目录，启动HTTP服务
cd /path/to/夏云凤.rar所在目录
python3 -m http.server 8080
```

**我这边执行:**
```powershell
Invoke-WebRequest -Uri "http://[WSL的IP]:8080/夏云凤.rar" -OutFile "C:\Users\Administrator\.openclaw\media\inbound\夏云凤.rar"
```

### 方法2: 通过飞书发送文件

直接在飞书中把文件发给我，我会自动接收。

### 方法3: 使用飞书云文档

把文件上传到飞书云文档，分享链接给我。

## 提醒 WSL

请告诉我 WSL 的 IP 地址:
```bash
hostname -I
```
