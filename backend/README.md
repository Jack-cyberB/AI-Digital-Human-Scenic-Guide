# 后端服务自述文件

## 项目简介
景区导览AI数字人统一后端服务，提供REST API和WebSocket实时通信能力。

## 技术栈
- Spring Boot 2.7.18
- Java 17
- MySQL 8.0
- MyBatis-Plus 3.5.3
- Spring WebSocket + STOMP
- JWT认证

## 快速开始

### 1. 环境要求
- JDK 17+
- MySQL 8.0+
- Maven 3.8+

### 2. 数据库配置
1. 创建数据库：`jingqu_db`
2. 执行初始化脚本：`src/main/resources/schema.sql`

### 3. 修改配置
编辑 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/jingqu_db
    username: your_username
    password: your_password
```

### 4. 编译运行
```bash
mvn clean package
java -jar target/jingqu-backend-1.0.0.jar
```

服务启动后访问：`http://localhost:8080`

## 主要接口

### WebSocket端点
- STOMP端点：`/ws`
- 消息前缀：`/app`
- 订阅前缀：`/topic`

### REST API
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/admin/login | 管理员登录 |
| GET | /api/knowledge | 获取知识库列表 |
| POST | /api/notifications | 创建紧急通知 |
| GET | /api/statistics/realtime | 获取实时数据 |

## 默认管理员
- 用户名：`admin`
- 密码：`admin123`

## 项目结构
```
backend/
├── src/main/java/com/jingqu/
│   ├── config/        # 配置类
│   ├── controller/   # 控制器
│   ├── service/       # 业务服务
│   ├── mapper/       # 数据访问层
│   ├── entity/       # 实体类
│   ├── dto/          # 数据传输对象
│   └── websocket/    # WebSocket处理
└── src/main/resources/
    ├── application.yml
    └── schema.sql
```
