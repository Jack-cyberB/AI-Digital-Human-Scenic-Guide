# AI 数字人景区导览系统

> 基于 AI 大模型的智慧景区导览服务，集成 Live2D 数字人、高德地图路线规划、RAG 知识库问答

## 项目概述

本项目为景区提供全方位的智能导览服务，采用 Spring Boot + Android + React 架构：

- **游客端（Android）**：AI 数字人智能问答、高德地图步行路线导览、景区选择、个人中心
- **管理员端（Web）**：数据大屏监控、知识库管理、紧急通知推送
- **后端服务**：REST API + WebSocket 双向通信，集成 DeepSeek AI 和 RAGFlow 知识库

## 技术架构

```
┌──────────────────────────────────────────────────────────────────┐
│                    统一后端 (Spring Boot 2.7 + HTTPS)               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │ REST API │  │WebSocket │  │ MySQL 8  │  │ DeepSeek/RAGFlow │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘  │
└──────────────────────────┬───────────────────────────────────────┘
                           │
           ┌───────────────┴───────────────┐
           │                               │
     ┌─────┴──────┐                  ┌─────┴──────┐
     │  Android   │                  │  Web 后台   │
     │  Compose   │◄─── WebSocket ──►│  React 18  │
     │  Live2D    │                  │  Antd + ECharts │
     └────────────┘                  └────────────┘
```

## 目录结构

```
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/jingqu/
│   │   ├── config/            # WebSocket、安全、跨域配置
│   │   ├── controller/        # REST API（RagFlow、Admin、Statistics）
│   │   ├── service/           # 业务层（RagFlow、DeepSeek、WebSocket）
│   │   ├── entity/            # JPA 实体
│   │   ├── dto/               # 数据传输对象
│   │   └── websocket/         # STOMP 消息处理
│   └── src/main/resources/
│       ├── application.yml    # SSL、数据库、AI 配置
│       └── schema.sql         # 数据库初始化
│
├── android/                    # Android 游客端 (Jetpack Compose)
│   └── app/src/main/java/com/jingqu/visitor/
│       ├── data/              # API、WebSocket、Models、Repository
│       ├── domain/usecase/    # ChatUseCase（路线数据流）
│       ├── ui/
│       │   ├── screens/       # HomeScreen、AIAssistantScreen、
│       │   │                   # MapRouteScreen、MainViewModel
│       │   ├── components/    # ChatBubble、Live2DModelCard
│       │   └── theme/         # Color、Typography
│       └── di/                # Hilt 依赖注入
│
├── admin-web/                  # React Web 管理后台
│   └── src/
│       ├── api/               # Axios + WebSocket 客户端
│       ├── components/        # Dashboard、Knowledge、Notification
│       ├── pages/             # Login、Interactions
│       └── store/             # Zustand 状态管理
│
└── data/                       # RAGFlow 知识库导入数据
```

## 核心功能

### 游客端（Android）

| 功能 | 说明 |
|------|------|
| AI 智能问答 | DeepSeek + RAGFlow 双引擎，景区上下文感知 |
| 数字人模式 | Live2D Cubism SDK，支持 Mao/Haru/Hiyori 模型切换 |
| 路线导览 | 高德地图 WebView + 步行 API，逐段绘制游览路线 |
| 路线离群检测 | 自动过滤高德定位偏移 >3km 的同名 POI |
| 景区选择 | 个人中心预设 5 大景区（灵山胜境/黄山/故宫/西湖/张家界） |
| 快捷服务 | 路线规划、景点讲解、餐饮推荐、交通指引等 6 项 |
| 4-Tab 导航 | 首页 / AI助手 / 路线导览 / 我的 |

### 管理员端（Web）

- JWT 登录认证
- 数据大屏实时监控（服务人次、在线游客、满意度）
- ECharts 可视化（交互趋势、热门问答 TOP10、景点分布）
- 知识库 CRUD + 一键同步
- 紧急通知即时推送
- 交互记录与历史统计

## 快速开始

### 1. 后端

```bash
cd backend
# 要求: JDK 17+, MySQL 8.0+
# 修改 application.yml 中的数据库密码和 AI API Key
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

## API 接口

### AI 对话
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/ragflow/chat | AI 对话（含路线提取） |

### WebSocket
| 端点 | 说明 |
|------|------|
| /ws | STOMP 端点 |
| /topic/visitor/{id} | 游客消息推送 |
| /topic/admin/dashboard | 大屏实时更新 |

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7, MySQL 8.0, MyBatis-Plus, JWT |
| AI | DeepSeek API, RAGFlow 知识库 |
| Android | Jetpack Compose, Hilt, OkHttp, Gson |
| 地图 | 高德 JS API v2 (WebView + Walking API) |
| 数字人 | Live2D Cubism SDK for Java |
| Web | React 18, TypeScript, Ant Design 5, ECharts, Zustand |

## 预设景区

| 景区 | 城市 | 说明 |
|------|------|------|
| 灵山胜境 | 无锡市 | 佛教圣地，灵山大佛 |
| 黄山 | 黄山市 | 奇松怪石，云海温泉 |
| 故宫 | 北京市 | 皇家宫殿，六百年辉煌 |
| 西湖 | 杭州市 | 淡妆浓抹总相宜 |
| 张家界 | 张家界市 | 峰林奇观，人间仙境 |

## License

仅供学习参考使用。
