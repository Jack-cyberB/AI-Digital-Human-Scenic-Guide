# 景区导览AI数字人 Web管理后台

## 项目简介
景区导览AI数字人管理员Web端，提供数据监控、知识库管理、紧急通知等功能。

## 技术栈
- React 18 + TypeScript
- Vite
- Ant Design 5
- ECharts
- Tailwind CSS
- Zustand (状态管理)
- Axios (HTTP客户端)
- WebSocket (实时通信)

## 功能特性
- 管理员登录认证
- 数据大屏实时监控
- 知识库增删改查
- 紧急通知推送
- 游客交互记录查看
- 历史统计数据报表

## 快速开始

### 安装依赖
```bash
npm install
```

### 开发模式
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

### 配置后端地址
编辑 `.env` 文件：
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```

## 项目结构
```
admin-web/
├── src/
│   ├── api/           # API接口、WebSocket客户端
│   ├── components/    # 组件
│   │   ├── Dashboard/
│   │   ├── Knowledge/
│   │   ├── Notification/
│   │   └── Layout/
│   ├── pages/         # 页面
│   ├── store/         # 状态管理
│   ├── types/         # TypeScript类型定义
│   ├── App.tsx
│   └── main.tsx
├── index.html
├── package.json
└── vite.config.ts
```

## 默认管理员
- 用户名：`admin`
- 密码：`admin123`

## 页面说明

### 数据大屏 (/dashboard)
- 今日服务人次、在线游客等实时数据
- 交互趋势折线图
- 热门问答柱状图
- 热门景点饼图
- 最近交互记录列表

### 知识库管理 (/knowledge)
- 知识库条目列表
- 添加/编辑/删除功能
- 分类和状态筛选
- 一键同步到游客端

### 紧急通知 (/notifications)
- 通知列表
- 创建和发送通知
- 推送历史记录

### 交互记录 (/interactions)
- 游客交互历史
- 关键词搜索
- 访客ID筛选

### 统计报表 (/statistics)
- 历史数据趋势图
- 日均统计
- 数据导出
