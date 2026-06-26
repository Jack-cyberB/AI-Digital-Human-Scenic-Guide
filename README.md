# AI 数字人景区导览系统

> 基于 AI 大模型的智慧景区导览服务，集成 Live2D 数字人 + 豆包 TTS 语音、高德地图 POI 路线规划、DeepSeek 流式对话

## 项目概述

本项目为景区提供全方位的智能导览服务，采用 Spring Boot + Android + React 架构：

- **游客端（Android）**：Live2D 数字人语音对话、高德地图 POI 标记与路线导览、地点详情（AI 介绍 + 小红书风格评价）、景区选择
- **管理员端（Web）**：数据大屏监控、知识库管理、紧急通知推送
- **后端服务**：REST API + SSE 流式 + WebSocket 双向通信，集成 DeepSeek AI、豆包语音合成、高德 POI 搜索

## 技术架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                    统一后端 (Spring Boot 2.7 + HTTPS)                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │ REST API │  │ SSE 流式 │  │ WebSocket│  │ 豆包 TTS / DeepSeek  │  │
│  └──────────┘  └──────────┘  └──────────┘  │ 高德 POI Web API     │  │
│                                              └──────────────────────┘  │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               │                               │
         ┌─────┴──────┐                  ┌─────┴──────┐
         │  Android   │                  │  Web 后台   │
         │  Compose   │◄─── SSE/WS ────►│  React 18  │
         │  Live2D    │                  │  Antd + ECharts │
         │  TTS 语音  │                  │             │
         └────────────┘                  └────────────┘
```

## 目录结构

```
├── backend/                     # Spring Boot 后端
│   ├── cosyvoice_server.py     # 豆包 TTS Python 微服务（备选）
│   └── src/main/java/com/jingqu/
│       ├── config/             # WebSocket、安全、跨域配置
│       ├── controller/         # REST API（RagFlow、Place、TTS、Admin）
│       ├── service/            # 业务层（RagFlow、DeepSeek、Place、CosyVoice）
│       ├── dto/                # 数据传输对象
│       └── websocket/          # STOMP 消息处理
│
├── android/                     # Android 游客端 (Jetpack Compose)
│   └── app/src/main/java/com/jingqu/visitor/
│       ├── data/api/           # ApiService、StreamingChatClient(SSE)、WebSocketClient
│       ├── data/model/         # Models（含 PoiDetailData、ReviewCard、PoiCategory）
│       ├── domain/usecase/     # ChatUseCase（流式消息 + 路线数据 + POI 增强）
│       ├── ui/
│       │   ├── screens/        # HomeScreen、AIAssistantScreen、MapRouteScreen、
│       │   │                    # PlaceDetailScreen、MainViewModel
│       │   ├── components/     # ChatBubble、Live2DModelCard、MapJsBridge、
│       │   │                    # PlaceDetailSheet、ImageCarousel、ReviewCardList、
│       │   │                    # TagChipRow、CategoryToggleBar、PlaceCard
│       │   └── theme/          # Color、Typography
│       └── di/                 # Hilt 依赖注入
│
├── admin-web/                   # React Web 管理后台
│
└── Pic/                         # 测试截图
```

## 核心功能

### 游客端（Android）

| 功能 | 说明 | 状态 |
|------|------|------|
| AI 智能问答 | DeepSeek 流式 SSE 对话，景区上下文感知 | ✅ |
| 数字人语音 | Live2D Mao/Haru/Hiyori 模型 + 豆包 TTS 语音合成 + 嘴部动画 | ✅ |
| 流式输出 | AI 回复边生成边显示，50ms UI 节流 | ✅ |
| 路线导览 | 高德地图 WebView + 步行/驾车 API，逐段绘制多日路线 | ✅ |
| POI 标记 | 路线点编号标记 + 分类标记（🏔景点 🍜美食 🥤饮品 🛍购物 🏨住宿）| ✅ |
| 地点详情 | 高德 POI 图片/评分/地址 + DeepSeek AI 介绍 + 小红书风格评价 | ✅ |
| 离群过滤 | 自动过滤高德定位偏移 >3km 的错误 POI | ✅ |
| 景区选择 | 个人中心预设 5 大景区（灵山胜境/黄山/故宫/西湖/张家界） | ✅ |
| 快捷服务 | 路线规划、景点讲解、餐饮推荐等 6 项，动态 prompt 拼接 | ✅ |
| 4-Tab 导航 | 首页 / AI助手 / 路线导览 / 我的 | ✅ |
| 一键导航 | 详情页 → 唤起高德地图 App 导航 | ✅ |

### 管理员端（Web）

- JWT 登录认证
- 数据大屏实时监控（服务人次、在线游客、满意度）
- ECharts 可视化（交互趋势、热门问答 TOP10、景点分布）
- 知识库 CRUD + 一键同步
- 紧急通知即时推送

## 快速开始

### 1. 后端

```bash
cd backend
# 要求: JDK 17+, MySQL 8.0+
# 设置环境变量: DEEPSEEK_API_KEY=sk-xxx
# 修改 application.yml 中的数据库密码
mvn clean package -DskipTests
java -jar target/jingqu-backend-1.0.0.jar
# HTTPS 运行在 8443 端口
```

### 2. Android

```bash
cd android
# 要求: Android Studio, SDK 34, Kotlin 1.9+
# 用 Android Studio 打开项目，Gradle 同步后运行
# 手机需通过 adb reverse tcp:8443 tcp:8443 连接后端
```

### 3. Web 后台

```bash
cd admin-web
npm install
npm run dev
# 访问 http://localhost:3000
# 默认账号: admin / admin123
```

### 4. 豆包 TTS（可选）

同步语音需要火山引擎豆包语音合成 API Key，配置在 `application.yml` 的 `doubao.api-key` 字段。无需额外部署。

如需本地 CosyVoice 2 替代，可安装 Python 依赖后启动 `backend/cosyvoice_server.py`。

## API 接口

### AI 对话

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ragflow/chat` | 非流式 AI 对话 |
| POST | `/api/ragflow/chat/stream` | **SSE 流式** AI 对话（推荐） |

### POI 地点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/place/detail` | POI 详情（高德 + AI 介绍 + 评价） |
| GET  | `/api/place/nearby` | 周边 POI 搜索 |
| POST | `/api/place/enrich` | AI 增强（简介 + 评价） |

### TTS 语音

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/tts/speak` | 文本转语音（豆包 TTS v3），返回 MP3 |
| GET  | `/api/tts/health` | TTS 服务健康检查 |

### WebSocket

| 端点 | 说明 |
|------|------|
| `/ws` | STOMP 端点 |
| `/topic/visitor/{id}` | 游客消息推送 |
| `/topic/admin/dashboard` | 大屏实时更新 |

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7, MySQL 8.0, MyBatis-Plus, JWT, STOMP/WebSocket |
| AI | DeepSeek API（流式 SSE）, 豆包语音合成 v3 |
| Android | Jetpack Compose, Hilt DI, OkHttp 4.12, Gson, Coil |
| 地图 | 高德 JS API v2 (WebView + Walking/Driving + PlaceSearch) |
| 数字人 | Live2D Cubism SDK for Java, GLSurfaceView, @JavascriptInterface Bridge |
| Web | React 18, TypeScript, Ant Design 5, ECharts, Zustand |

## 预设景区

| 景区 | 城市 | 说明 |
|------|------|------|
| 灵山胜境 🏔 | 无锡市 | 佛教圣地，灵山大佛 |
| 黄山 ⛰ | 黄山市 | 奇松怪石，云海温泉 |
| 故宫 🏯 | 北京市 | 皇家宫殿，六百年辉煌 |
| 西湖 🌊 | 杭州市 | 淡妆浓抹总相宜 |
| 张家界 🏞 | 张家界市 | 峰林奇观，人间仙境 |

## 数据流

```
用户发送消息
  → SSE 流式返回 delta 逐字显示
  → 首个 delta: Live2D 随机表情 + 动作
  → Done: 豆包 TTS 合成完整语音 → MediaPlayer 播放
         → Live2D 嘴部参数随播放开合 (120ms 周期)
  → 路线导览 Tab: 高德 WebView 渲染路线 + POI 标记
         → 点击地名/标记 → 全屏详情（图片轮播 + AI 讲解 + 评价卡片）
```

## 路线图

- [x] AI 流式对话 + 路线规划
- [x] Live2D 数字人渲染 + 模型切换
- [x] 高德地图 POI 标记 + 分类筛选
- [x] 地点详情（图片 + AI 介绍 + 小红书评价）
- [x] 豆包 TTS 语音合成 + 嘴部动画
- [ ] 语音输入（ASR）
- [ ] 离线地图包
- [ ] 多景区动态管理

## License

仅供学习参考使用。
