# 景区导览服务AI数字人系统 - 项目规格说明书

## 1. 项目概述与愿景

### 1.1 项目名称
**景区导览AI数字人服务系统** (Scenic Guide AI Digital Person System)

### 1.2 核心价值
为景区游客提供智能、实时、沉浸式的导览服务体验，同时为景区管理人员提供数据监控、紧急通知和知识库管理的统一平台。

### 1.3 目标用户
- **游客端**: 景区游客，通过Android手机应用与AI数字人进行交互
- **管理员端**: 景区管理人员，通过Web后台进行数据监控、紧急通知发送、知识库管理

### 1.4 技术架构概览
```
┌─────────────────────────────────────────────────────────────────────┐
│                        统一后端服务 (Spring Boot)                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │  WebSocket  │  │   REST API  │  │  数据存储   │  │  业务逻辑  │ │
│  │    服务     │  │   服务      │  │  (MySQL)   │  │   服务      │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
         ▲                              ▲
         │                              │
    ┌────┴────┐                    ┌────┴────┐
    │ Android │                    │  Web    │
    │ 游客端  │                    │ 管理员端 │
    └─────────┘                    └─────────┘
```

---

## 2. 统一后端服务规格

### 2.1 技术栈
- **框架**: Spring Boot 2.7.x
- **语言**: Java 17
- **WebSocket**: Spring WebSocket + STOMP
- **数据库**: MySQL 8.0
- **ORM**: MyBatis-Plus
- **构建工具**: Maven
- **实时通信**: WebSocket + JSON

### 2.2 数据库设计

#### 2.2.1 游客交互记录表 (visitor_interactions)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| visitor_id | VARCHAR(64) | 游客唯一标识 (设备ID) |
| question | TEXT | 游客提问内容 |
| answer | TEXT | AI数字人回答内容 |
| interaction_type | VARCHAR(32) | 交互类型: QA/GREETING/GUIDANCE |
| scenic_spot | VARCHAR(128) | 当前景点名称 |
| interaction_time | DATETIME | 交互时间 |
| session_id | VARCHAR(64) | 会话ID |

#### 2.2.2 知识库表 (knowledge_base)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| question_pattern | TEXT | 问题模式（支持模糊匹配） |
| answer | TEXT | 标准回答 |
| keywords | VARCHAR(512) | 关键词，逗号分隔 |
| category | VARCHAR(64) | 分类：景点介绍/路线规划/餐饮服务/紧急求助 |
| priority | INT | 优先级（数字越大优先级越高） |
| status | TINYINT | 状态：0-禁用 1-启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 2.2.3 紧急通知表 (emergency_notifications)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| title | VARCHAR(256) | 通知标题 |
| content | TEXT | 通知内容 |
| notification_type | VARCHAR(32) | 通知类型：EMERGENCY/INFO/UPDATE |
| target_scope | VARCHAR(32) | 推送范围：ALL/SCENIC_AREA/SPOT |
| target_spot | VARCHAR(128) | 指定景点（可选） |
| push_time | DATETIME | 推送时间 |
| expiry_time | DATETIME | 过期时间 |
| status | TINYINT | 状态：0-待推送 1-已推送 2-已过期 |
| created_by | VARCHAR(64) | 创建人ID |
| created_at | DATETIME | 创建时间 |

#### 2.2.4 统计数据表 (daily_statistics)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| stat_date | DATE | 统计日期 |
| total_interactions | INT | 今日总交互次数 |
| total_visitors | INT | 今日独立访客数 |
| peak_hour | INT | 高峰时段（小时） |
| popular_qa | JSON | 热门问答TOP10 JSON |
| hotspot_spots | JSON | 热门景点JSON |
| created_at | DATETIME | 创建时间 |

#### 2.2.5 管理员表 (admins)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(64) | 用户名 |
| password | VARCHAR(256) | 密码（BCrypt加密） |
| real_name | VARCHAR(128) | 真实姓名 |
| role | VARCHAR(32) | 角色：SUPER_ADMIN/ADMIN |
| status | TINYINT | 状态：0-禁用 1-启用 |
| created_at | DATETIME | 创建时间 |
| last_login | DATETIME | 最后登录时间 |

### 2.3 WebSocket接口设计

#### 2.3.1 端点配置
- WebSocket握手端点: `/ws`
- STOMP消息代理: `/topic`, `/queue`
- 应用目标: `/app`

#### 2.3.2 消息类型定义

**客户端→服务端:**
| 消息类型 | 目标地址 | 载荷 |
|---------|---------|------|
| 游客发送消息 | /app/visitor/message | {visitorId, sessionId, message, scenicSpot, timestamp} |
| 管理员发送通知 | /app/admin/notification | {title, content, type, scope, spot, adminId} |
| 管理员更新知识库 | /app/admin/knowledge | {action, data} |
| 游客端订阅 | /app/visitor/subscribe | {visitorId} |

**服务端→客户端:**
| 消息类型 | 广播地址 | 载荷 |
|---------|---------|------|
| AI回复消息 | /topic/visitor/{visitorId} | {answer, sessionId, timestamp, type} |
| 紧急通知 | /topic/all-visitors | {notificationId, title, content, type, timestamp} |
| 知识库更新 | /topic/knowledge-update | {action, updatedData, timestamp} |
| 管理员大屏更新 | /topic/admin/dashboard | {totalInteractions, onlineVisitors, popularQA, hotspotSpots} |

### 2.4 REST API接口

#### 2.4.1 管理员认证
- `POST /api/admin/login` - 管理员登录
- `POST /api/admin/logout` - 管理员登出
- `GET /api/admin/profile` - 获取管理员信息

#### 2.4.2 知识库管理
- `GET /api/knowledge` - 获取知识库列表
- `POST /api/knowledge` - 添加知识库条目
- `PUT /api/knowledge/{id}` - 更新知识库条目
- `DELETE /api/knowledge/{id}` - 删除知识库条目
- `POST /api/knowledge/sync` - 同步知识库到游客端

#### 2.4.3 紧急通知
- `GET /api/notifications` - 获取通知列表
- `POST /api/notifications` - 创建新通知（同时推送给游客）
- `PUT /api/notifications/{id}` - 更新通知
- `DELETE /api/notifications/{id}` - 删除通知

#### 2.4.4 数据统计
- `GET /api/statistics/today` - 获取今日统计数据
- `GET /api/statistics/history` - 获取历史统计数据
- `GET /api/statistics/realtime` - 获取实时数据（大屏用）

#### 2.4.5 游客交互（内部接口）
- `GET /api/interactions` - 获取交互记录列表
- `GET /api/interactions/export` - 导出交互记录

---

## 3. Android游客端规格

### 3.1 技术栈
- **语言**: Kotlin
- **最低SDK**: Android 7.0 (API 24)
- **目标SDK**: Android 14 (API 34)
- **UI框架**: Jetpack Compose + Material Design 3
- **架构**: MVVM + Clean Architecture
- **网络**: OkHttp + Retrofit
- **WebSocket**: OkHttp WebSocket
- **JSON解析**: Gson
- **协程**: Kotlin Coroutines + Flow
- **依赖注入**: Hilt
- **本地存储**: DataStore Preferences

### 3.2 界面设计

#### 3.2.1 主界面布局
```
┌─────────────────────────────────────┐
│  状态栏 (网络状态、通知指示)          │
├─────────────────────────────────────┤
│                                     │
│         AI数字人形象展示区             │
│     (动态表情、口型同步、动作)          │
│                                     │
├─────────────────────────────────────┤
│                                     │
│         聊天消息展示区                │
│    (气泡样式展示对话历史)              │
│                                     │
├─────────────────────────────────────┤
│  快捷问题卡片 (景点/路线/餐饮/帮助)    │
├─────────────────────────────────────┤
│  [        输入框        ] [发送]    │
└─────────────────────────────────────┘
```

#### 3.2.2 页面清单
1. **SplashActivity**: 启动页，包含Logo和加载动画
2. **MainActivity**: 主界面，包含AI数字人展示、聊天区、快捷入口
3. **ScenicSpotActivity**: 景点详情页，展示景点介绍和导览
4. **SettingsActivity**: 设置页，音量、通知开关、关于信息
5. **EmergencyNotificationDialog**: 紧急通知弹窗

#### 3.2.3 视觉风格
- **主色调**: #2E7D32 (景区绿色)
- **辅助色**: #1565C0 (天空蓝)
- **强调色**: #FF6F00 (活力橙)
- **背景色**: #F5F5F5 (浅灰白)
- **字体**: 系统默认 + 思源黑体
- **圆角**: 16dp (卡片)、24dp (按钮)
- **阴影**: Material Design 3 elevation

### 3.3 功能模块

#### 3.3.1 AI数字人交互模块
- 文字输入发送消息
- 快捷问题卡片快速提问
- 语音识别输入（可选）
- AI数字人动画表情反馈
- 消息发送状态指示

#### 3.3.2 景点导览模块
- 景点列表展示
- 景点详情查看
- 地图导航入口
- 语音讲解（预留）

#### 3.3.3 实时通知接收模块
- WebSocket长连接维护
- 紧急通知弹窗展示
- 知识库更新自动刷新
- 后台消息推送（FCM预留）

#### 3.3.4 设置模块
- 通知开关
- 音量调节
- 清空聊天记录
- 关于我们

### 3.4 WebSocket通信流程

#### 3.4.1 连接建立流程
```
1. App启动 → 获取设备ID
2. 连接WebSocket服务器 /ws
3. 认证身份 → 发送 {type: "VISITOR_CONNECT", visitorId: "xxx"}
4. 订阅个人消息通道 /topic/visitor/{visitorId}
5. 订阅公共通知通道 /topic/all-visitors
6. 订阅知识库更新通道 /topic/knowledge-update
```

#### 3.4.2 消息发送流程
```
1. 用户输入问题 → 点击发送
2. 本地显示消息气泡
3. 发送消息到 /app/visitor/message
4. 等待服务器响应 /topic/visitor/{visitorId}
5. 显示AI回复
6. 同时触发统计更新到 /topic/admin/dashboard
```

#### 3.4.3 通知接收流程
```
1. 管理员发送通知 → /app/admin/notification
2. 服务器广播到 /topic/all-visitors
3. App接收消息 → 解析通知内容
4. 显示通知弹窗 → 用户确认
5. 记录通知已读状态
```

---

## 4. Web管理员端规格

### 4.1 技术栈
- **前端框架**: React 18 + TypeScript
- **UI库**: Ant Design 5.x
- **状态管理**: Zustand
- **图表**: ECharts
- **WebSocket**: 原生WebSocket API
- **构建工具**: Vite
- **HTTP客户端**: Axios
- **样式**: Tailwind CSS

### 4.2 页面结构

#### 4.2.1 页面清单
1. **登录页** (`/login`): 管理员登录
2. **仪表盘页** (`/dashboard`): 数据大屏和实时监控
3. **知识库管理页** (`/knowledge`): 知识库增删改查
4. **紧急通知页** (`/notifications`): 创建和发送紧急通知
5. **交互记录页** (`/interactions`): 查看游客交互记录
6. **统计报表页** (`/statistics`): 历史数据统计

#### 4.2.2 仪表盘布局
```
┌────────────────────────────────────────────────────────────────────┐
│  LOGO   景区导览AI数字人管理系统    [通知] [管理员: xxx] [退出]    │
├────────────────────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                │
│ │ 今日服务 │ │ 在线游客 │ │ 今日问题 │ │ 满意度   │                │
│ │  1,234   │ │   567    │ │  3,456   │ │  98.5%   │                │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘                │
├────────────────────────────────────────────────────────────────────┤
│ ┌────────────────────────────┐ ┌────────────────────────────┐       │
│ │    实时交互曲线图            │ │    热门问答TOP10           │       │
│ │    (ECharts折线图)          │ │    (词云/柱状图)            │       │
│ │                            │ │                            │       │
│ └────────────────────────────┘ └────────────────────────────┘       │
├────────────────────────────────────────────────────────────────────┤
│ ┌────────────────────────────┐ ┌────────────────────────────┐       │
│ │    热门景点分布              │ │    最近交互记录              │       │
│ │    (ECharts饼图)           │ │    (实时滚动列表)            │       │
│ └────────────────────────────┘ └────────────────────────────┘       │
├────────────────────────────────────────────────────────────────────┤
│ [紧急通知] 按钮                     [同步知识库] 按钮                │
└────────────────────────────────────────────────────────────────────┘
```

### 4.3 功能模块

#### 4.3.1 管理员认证模块
- 登录表单验证
- Token令牌管理
- 登录状态保持
- 权限控制

#### 4.3.2 数据大屏模块
- 实时数据卡片（服务人次、在线游客等）
- ECharts图表展示（折线图、饼图、柱状图）
- 实时交互记录滚动列表
- WebSocket实时更新

#### 4.3.3 知识库管理模块
- 知识库列表展示（分页）
- 添加/编辑/删除知识条目
- 关键词高亮匹配
- 知识库同步推送到游客端

#### 4.3.4 紧急通知模块
- 通知表单（标题、内容、类型、范围）
- 定时发送设置
- 通知历史记录
- 实时推送到所有游客端

#### 4.3.5 交互记录模块
- 交互记录列表（分页、筛选）
- 关键词搜索
- 数据导出功能

---

## 5. 项目目录结构

```
jingqu/
├── backend/                          # 统一后端服务
│   ├── src/
│   │   └── main/
│   │       ├── java/com/jingqu/
│   │       │   ├── JingQuApplication.java
│   │       │   ├── config/
│   │       │   │   ├── WebSocketConfig.java
│   │       │   │   └── CorsConfig.java
│   │       │   ├── controller/
│   │       │   │   ├── AdminController.java
│   │       │   │   ├── KnowledgeController.java
│   │       │   │   ├── NotificationController.java
│   │       │   │   └── StatisticsController.java
│   │       │   ├── service/
│   │       │   │   ├── AdminService.java
│   │       │   │   ├── KnowledgeService.java
│   │       │   │   ├── NotificationService.java
│   │       │   │   ├── StatisticsService.java
│   │       │   │   └── WebSocketService.java
│   │       │   ├── websocket/
│   │       │   │   ├── WebSocketHandler.java
│   │       │   │   └── WebSocketMessage.java
│   │       │   ├── entity/
│   │       │   │   ├── VisitorInteraction.java
│   │       │   │   ├── KnowledgeBase.java
│   │       │   │   ├── EmergencyNotification.java
│   │       │   │   ├── DailyStatistics.java
│   │       │   │   └── Admin.java
│   │       │   ├── mapper/
│   │       │   │   └── *Mapper.java
│   │       │   └── dto/
│   │       │       ├── MessageDTO.java
│   │       │       └── ResponseDTO.java
│   │       └── resources/
│   │           ├── application.yml
│   │           └── mapper/
│   ├── pom.xml
│   └── README.md
│
├── android/                         # Android游客端
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/jingqu/visitor/
│   │       │   ├── JingQuApp.java
│   │       │   ├── MainActivity.kt
│   │       │   ├── ui/
│   │       │   │   ├── theme/
│   │       │   │   ├── components/
│   │       │   │   └── screens/
│   │       │   ├── data/
│   │       │   │   ├── api/
│   │       │   │   ├── model/
│   │       │   │   └── repository/
│   │       │   ├── domain/
│   │       │   │   └── usecase/
│   │       │   └── di/
│   │       │       └── AppModule.kt
│   │       └── res/
│   │           ├── drawable/
│   │           ├── values/
│   │           └── layout/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── README.md
│
└── admin-web/                       # Web管理员端
    ├── public/
    ├── src/
    │   ├── api/
    │   │   ├── index.ts
    │   │   └── websocket.ts
    │   ├── components/
    │   │   ├── Layout/
    │   │   ├── Dashboard/
    │   │   ├── Knowledge/
    │   │   └── Notification/
    │   ├── pages/
    │   │   ├── Login.tsx
    │   │   ├── Dashboard.tsx
    │   │   ├── Knowledge.tsx
    │   │   ├── Notification.tsx
    │   │   └── Statistics.tsx
    │   ├── store/
    │   │   └── index.ts
    │   ├── types/
    │   │   └── index.ts
    │   ├── App.tsx
    │   ├── main.tsx
    │   └── index.css
    ├── index.html
    ├── package.json
    ├── tsconfig.json
    ├── vite.config.ts
    └── README.md
```

---

## 6. 业务流程图

### 6.1 游客咨询流程
```
游客打开App
    ↓
连接WebSocket服务器
    ↓
显示AI数字人欢迎语
    ↓
游客输入问题/点击快捷卡片
    ↓
发送消息到后端
    ↓
后端查询知识库匹配答案
    ↓
返回AI回答给游客
    ↓
后端更新统计数据
    ↓
广播给管理员大屏
```

### 6.2 紧急通知流程
```
管理员登录Web后台
    ↓
点击"发送紧急通知"
    ↓
填写通知内容
    ↓
点击发送
    ↓
后端通过WebSocket广播
    ↓
所有在线游客端收到通知
    ↓
游客端弹出通知弹窗
    ↓
游客确认阅读
```

### 6.3 知识库更新流程
```
管理员在Web后台编辑知识库
    ↓
添加/修改/删除知识条目
    ↓
点击"同步知识库"
    ↓
后端更新数据库
    ↓
通过WebSocket推送更新
    ↓
游客端接收并更新本地缓存
```

---

## 7. 开发计划

### 阶段一：后端服务开发 (1-2周)
1. 项目初始化，搭建Spring Boot框架
2. 数据库表设计和创建
3. MyBatis-Plus配置和Mapper开发
4. REST API接口开发
5. WebSocket配置和消息处理
6. 基础功能测试

### 阶段二：Android游客端开发 (2-3周)
1. 项目初始化，配置依赖
2. 页面框架搭建（Compose）
3. WebSocket客户端实现
4. AI聊天功能开发
5. 景点导览功能开发
6. 通知接收功能开发
7. UI细节优化

### 阶段三：Web管理员端开发 (1-2周)
1. 项目初始化（Vite + React）
2. 页面路由配置
3. 登录认证功能
4. 数据大屏开发（ECharts）
5. 知识库管理功能
6. 紧急通知功能
7. WebSocket实时通信

### 阶段四：联调测试 (1周)
1. 后端接口测试
2. Android端与后端联调
3. Web端与后端联调
4. WebSocket双向通信测试
5. 性能优化
6. Bug修复

---

## 8. 验收标准

### 8.1 功能验收
- [ ] 后端WebSocket服务稳定运行
- [ ] 管理员端可以发送紧急通知
- [ ] 游客端可以实时接收通知
- [ ] 游客端与AI数字人正常对话
- [ ] 管理员大屏实时更新统计数据
- [ ] 知识库更新实时同步到游客端

### 8.2 性能验收
- [ ] WebSocket连接延迟 < 100ms
- [ ] 消息送达率 > 99.9%
- [ ] Android端启动时间 < 3秒
- [ ] Web端首屏加载 < 2秒
- [ ] 支持1000+并发连接

### 8.3 安全验收
- [ ] 管理员密码加密存储
- [ ] WebSocket连接认证
- [ ] API接口权限控制
- [ ] 输入内容安全过滤

---

*文档版本: 1.0*
*创建日期: 2026-05-06*
*作者: AI Assistant*
