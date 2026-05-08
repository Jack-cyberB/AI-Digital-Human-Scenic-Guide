# 景区导览服务AI数字人系统

> 统一前后端分离架构的景区智能导览服务，包含Android游客端和Web管理后台

## 项目概述

本项目为景区提供智能导览服务，采用统一后端 + 双前端（Android + Web）的分离架构：

- **游客端（Android）**：为游客提供AI数字人智能问答、景点导览服务
- **管理员端（Web）**：为景区管理人员提供数据监控、知识库管理、紧急通知等功能
- **统一后端**：集成REST API和WebSocket服务，实现双向实时通信

## 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        统一后端服务 (Spring Boot)                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │
│  │  WebSocket  │  │   REST API  │  │  MySQL 8.0  │  │ JWT认证 │ │
│  │   服务      │  │   服务      │  │  数据存储    │  │  服务   │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────┘ │
└───────────────────────────┬─────────────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            │                               │
      ┌─────┴─────┐                 ┌──────┴──────┐
      │  Android  │                 │   Web后台    │
      │  游客端   │◄───────────────►│  管理员端   │
      │  (Kotlin) │    WebSocket    │  (React)    │
      └───────────┘                 └─────────────┘
```

## 目录结构

```
jingqu/
├── SPEC.md                    # 项目规格说明书
├── README.md                  # 项目主文档（本文件）
│
├── backend/                   # 统一后端服务
│   ├── src/main/java/com/jingqu/
│   │   ├── config/           # 配置类（WebSocket、安全、跨域）
│   │   ├── controller/       # REST API控制器
│   │   ├── service/          # 业务服务层
│   │   ├── websocket/        # WebSocket消息处理
│   │   ├── entity/           # 实体类
│   │   ├── mapper/           # MyBatis Mapper
│   │   └── dto/              # 数据传输对象
│   ├── src/main/resources/
│   │   ├── application.yml  # 应用配置
│   │   └── schema.sql        # 数据库初始化脚本
│   └── pom.xml               # Maven依赖配置
│
├── android/                   # Android游客端
│   └── app/src/main/java/com/jingqu/visitor/
│       ├── data/             # 数据层（API、WebSocket、Repository）
│       ├── domain/           # 业务用例
│       ├── ui/               # UI层（Compose组件、屏幕、主题）
│       └── di/               # 依赖注入（Hilt）
│
└── admin-web/                 # Web管理员端
    └── src/
        ├── api/              # API接口、WebSocket客户端
        ├── components/       # React组件
        │   ├── Dashboard/    # 数据大屏
        │   ├── Knowledge/    # 知识库管理
        │   └── Notification/ # 通知管理
        ├── pages/           # 页面组件
        ├── store/           # 状态管理（Zustand）
        └── types/           # TypeScript类型定义
```

## 核心功能

### 游客端功能
- AI数字人智能问答
- 快捷问题卡片（景点介绍、路线规划、餐饮服务、帮助服务）
- WebSocket实时通信
- 紧急通知弹窗接收
- 知识库更新实时同步
- 消息气泡样式展示

### 管理员端功能
- 管理员登录认证（JWT）
- 数据大屏实时监控
  - 今日服务人次
  - 当前在线游客
  - 独立访客统计
  - 满意度指标
- ECharts数据可视化
  - 实时交互趋势图
  - 热门问答TOP10
  - 热门景点分布饼图
- 知识库管理
  - 增删改查操作
  - 分类筛选
  - 一键同步到游客端
- 紧急通知推送
  - 创建通知
  - 即时推送到所有游客
  - 通知历史管理
- 交互记录查看
- 历史统计数据报表

### WebSocket双向通信
| 方向 | 消息类型 | 说明 |
|------|---------|------|
| 游客→后端→管理员 | VISITOR_MESSAGE | 游客发送消息，广播到大屏 |
| 后端→游客 | AI_RESPONSE | AI回复消息 |
| 管理员→后端→游客 | NOTIFICATION | 紧急通知推送 |
| 管理员→后端→游客 | KNOWLEDGE_UPDATE | 知识库更新通知 |
| 后端→管理员 | DASHBOARD_UPDATE | 大屏数据实时更新 |

## 快速开始

### 1. 后端服务

#### 环境要求
- JDK 17+
- MySQL 8.0+
- Maven 3.8+

#### 启动步骤
```bash
cd backend

# 创建数据库
mysql -u root -p < src/main/resources/schema.sql

# 修改数据库配置
# 编辑 src/main/resources/application.yml

# 编译打包
mvn clean package -DskipTests

# 运行服务
java -jar target/jingqu-backend-1.0.0.jar
```

服务启动后访问：`http://localhost:8080`

### 2. Android游客端

#### 环境要求
- Android Studio Hedgehog (2023.1.1)+
- Kotlin 1.9+
- Android SDK 34

#### 启动步骤
```bash
cd android

# 导入项目到Android Studio
# 等待Gradle同步完成

# 配置后端服务器地址
# 编辑 app/build.gradle.kts 中的 debug 配置

# 连接设备或模拟器
# 运行应用
```

### 3. Web管理员端

#### 环境要求
- Node.js 18+
- npm 9+

#### 启动步骤
```bash
cd admin-web

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

访问：`http://localhost:3000`

## 默认管理员

```
用户名：admin
密码：admin123
```

## API接口文档

### 管理员认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/admin/login | 管理员登录 |
| POST | /api/admin/logout | 管理员登出 |
| GET | /api/admin/profile | 获取管理员信息 |

### 知识库管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/knowledge | 获取知识库列表 |
| POST | /api/knowledge | 添加知识库条目 |
| PUT | /api/knowledge/{id} | 更新知识库条目 |
| DELETE | /api/knowledge/{id} | 删除知识库条目 |
| POST | /api/knowledge/sync | 同步知识库 |

### 紧急通知
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/notifications | 获取通知列表 |
| POST | /api/notifications | 创建通知 |
| DELETE | /api/notifications/{id} | 删除通知 |

### 统计数据
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/statistics/today | 今日统计 |
| GET | /api/statistics/realtime | 实时大屏数据 |
| GET | /api/statistics/history | 历史统计 |

### WebSocket端点
| 端点 | 说明 |
|------|------|
| /ws | STOMP WebSocket端点 |
| /topic/all-visitors | 广播到所有游客 |
| /topic/visitor/{id} | 发送到特定游客 |
| /topic/admin/dashboard | 管理员大屏更新 |

## 数据库表

| 表名 | 说明 |
|------|------|
| admins | 管理员表 |
| visitor_interactions | 游客交互记录 |
| knowledge_base | 知识库 |
| emergency_notifications | 紧急通知 |
| daily_statistics | 每日统计 |
| online_visitors | 在线访客 |

## 技术栈汇总

| 模块 | 技术 |
|------|------|
| 后端框架 | Spring Boot 2.7 |
| 数据库 | MySQL 8.0 + MyBatis-Plus |
| WebSocket | Spring WebSocket + STOMP |
| 认证 | JWT |
| Android UI | Jetpack Compose |
| Android架构 | MVVM + Hilt |
| Web框架 | React 18 + TypeScript |
| Web UI | Ant Design 5 + ECharts |
| Web状态 | Zustand |

## 开发团队

- 项目设计：AI Assistant
- 开发日期：2026-05-06

## License

本项目仅供学习参考使用。
