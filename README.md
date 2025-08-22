# McMotd-API 独立运行版本

McMotd-API 是一个功能强大的 Minecraft 服务器状态查询工具，本版本专为独立运行设计，可以轻松集成到您的项目中或作为独立服务使用。

## 功能特点

- **服务器状态查询**：获取 Minecraft 服务器的在线状态、玩家数量、版本信息等
- **玩家列表**：获取在线玩家列表信息
- **图标获取**：获取服务器图标并转换为 PNG 格式
- **RESTful API**：提供简洁易用的 HTTP API 接口
- **独立运行**：无需依赖 mirai-console，可单独部署
- **高度可配置**：支持通过配置文件自定义各种参数

## 下载与安装

1. 从 Release下载最新的独立运行版本
2. 确保您的系统已安装 Java 11 或更高版本
3. 无需额外依赖，直接运行即可

## 运行方式

### 基本运行

```bash
java -cp mcmotd-api.jar org.zrnq.mcmotd.StandaloneMainKt
```

运行后，服务器将在默认端口（8082）启动，并在当前目录生成配置文件 `mcmotd.yml` 和数据文件 `mcmotd_data.yml`。

### 自定义端口

您可以通过修改配置文件中的 `httpServerPort` 来更改服务器端口。

## 配置说明

配置文件 `mcmotd.yml` 包含以下主要配置项：

```yaml
# 服务器端口，0 表示禁用 HTTP API
httpServerPort: 8082

# 字体设置
fontName: "Microsoft YaHei"
fontPath: ""

# 显示设置
showPlayerList: true
showPeakPlayers: false
showServerVersion: false
showTrueAddress: false

# 缓存和限制
httpServerRequestCoolDown: 3000
httpServerParallelRequest: 32
httpServerAccessRecordRefresh: 0

# DNS 设置
dnsServerList:
  - "223.5.5.5"
  - "8.8.8.8"

# 历史记录
recordOnlinePlayer: []
recordInterval: 300
recordLimit: 21600

# 背景设置
background: "#000000"

# 服务器映射
httpServerMapping: {}
```

## API 文档

### 端点列表

#### 1. 获取服务器信息（原始 JSON）

```
GET /raw/{address}
```

- `address`: Minecraft 服务器地址（可带端口，如 `mc.example.com:25565`）
- 返回：包含服务器状态的 JSON 数据

示例响应：
```json
{
  "online": true,
  "motd": "{\"text\":\"A Minecraft Server\"}",
  "players": {
    "max": 20,
    "online": 2,
    "list": "player1, player2"
  },
  "version": "1.20.1",
  "favicon": "data:image/png;base64,..."
}
```

#### 2. 获取服务器图标

```
GET /icon/{address}
```

- `address`: Minecraft 服务器地址
- 返回：64x64 PNG 格式的服务器图标

#### 3. 获取服务器信息（图像）

```
GET /infos/{address}
```

- `address`: Minecraft 服务器地址
- 返回：包含服务器状态的 PNG 图像

## 使用示例

### 1. 获取服务器 JSON 信息

```bash
curl http://localhost:8082/raw/mcsy.net
```

### 2. 保存服务器图标

```bash
curl http://localhost:8082/icon/mcsy.net -o server-icon.png
```

### 3. 获取服务器状态图像

```bash
curl http://localhost:8082/infos/mcsy.net -o server-status.png
```

## 常见问题

### Q: 服务器无法启动？
A: 请检查 Java 版本是否符合要求（Java 11+），并确保端口未被占用。

### Q: 访问 API 时返回 Too Many Requests？
A: 这是因为配置了访问冷却限制，可以通过修改 `httpServerRequestCoolDown` 配置项来调整或禁用。

### Q: 如何自定义服务器响应的图像样式？
A: 可以通过修改配置文件中的 `background` 和字体相关配置来调整图像样式。

## 开发与贡献

如果您想参与开发或有任何问题，我可能也不会）

## 许可证

本项目采用 [MIT 许可证](https://opensource.org/licenses/MIT)。
