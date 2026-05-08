# 景区导览 Android 游客端

## 项目简介
景区导览AI数字人Android游客端应用，为游客提供智能问答、景点导览和实时通知服务。

## 技术栈
- Kotlin
- Jetpack Compose
- Material Design 3
- MVVM + Clean Architecture
- Hilt (依赖注入)
- Retrofit + OkHttp
- WebSocket (STOMP)
- Kotlin Coroutines + Flow
- DataStore (本地存储)

## 功能特性
- AI数字人智能问答
- 快捷问题卡片
- WebSocket实时通信
- 紧急通知弹窗接收
- 知识库更新实时同步
- 消息气泡展示

## 项目结构
```
android/
├── app/src/main/java/com/jingqu/visitor/
│   ├── data/
│   │   ├── api/          # API服务、WebSocket客户端
│   │   ├── model/         # 数据模型
│   │   └── repository/   # 数据仓库
│   ├── domain/
│   │   └── usecase/      # 用例
│   ├── ui/
│   │   ├── components/   # UI组件
│   │   ├── screens/      # 页面
│   │   └── theme/        # 主题
│   └── di/               # 依赖注入
└── app/build.gradle.kts
```

## 配置说明

### 后端服务器地址
编辑 `app/build.gradle.kts` 中的 debug 配置：
```kotlin
debug {
    buildConfigField("String", "BASE_URL", "\"http://YOUR_SERVER_IP:8080/\"")
    buildConfigField("String", "WS_URL", "\"ws://YOUR_SERVER_IP:8080/ws\"")
}
```

### 数据库要求
- MySQL 8.0+
- 详见后端 schema.sql

## 快速开始

1. 打开 Android Studio
2. 导入 android 目录作为项目
3. 等待 Gradle 同步完成
4. 连接 Android 设备或模拟器
5. 运行应用

## 最低要求
- Android 7.0 (API 24)+
- Kotlin 1.9+
- Gradle 8.0+
