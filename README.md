# 云智助手

基于 Spring Boot + Spring AI 的后端项目，提供账号体系、对话能力（普通/RAG/SSE）和 Manus 智能体能力，支持 MySQL 持久化会话历史，已适配微信云托管上线。

## 核心功能

- 账号系统：注册、登录、当前用户、登出（Token 会话）。
- 编程助手：
  - 普通对话：同步 / SSE
  - RAG 对话：同步 / SSE（ DashScope + 知识库）
- Manus 智能体：流式执行工具链，**最多执行 6 步**，达到上限后直接停在最后一步结果。
- 会话历史：
  - 编程助手历史：会话列表、消息列表、删除会话
  - Manus 历史：会话列表、消息列表、删除会话
- 内置页面与探测：
  - `/api/`（首页）
  - `/api/ping`（健康检查）

## 技术栈

- Java 17
- Spring Boot 3.4.x
- Spring AI + 阿里巴巴 DashScope
- MyBatis + MySQL
- 反应堆 / SSE

## 接口概览（默认前缀 `/api`）

- 认证
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `获取 /api/auth/me`
  - `POST /api/auth/logout`
- 对话
  - `获取 /api/ai/pgapp/chat/sync`
  - `GET /api/ai/pgapp/chat/sse`
  - `GET /api/ai/pgapp/chat/rag/sync`
  - `GET /api/ai/pgapp/chat/rag/sse`
  - `GET /api/ai/manus/chat`（SSE）
- 历史
  - `GET /api/ai/pgapp/history/sessions`
  - `GET /api/ai/pgapp/history/messages`
  - `DELETE /api/ai/pgapp/history/session`
  - `GET /api/ai/manus/history/sessions`
  - `GET /api/ai/manus/history/messages`
  - `DELETE /api/ai/manus/history/session`

说明：`/ai/**` 接口受鉴权拦截，需带 `Authorization: Bearer <token>`。

## 本地启动

### 1) 初始化数据库

先创建库（库名必须和配置一致）：

```sql
CREATE DATABASE IF NOT EXISTS ai_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

再执行 `src/main/resources/sql/mysql_user.sql` 建表。

### 2) 配置环境变量

可参考 `.env.example`，核心项如下：

- `SPRING_PROFILES_ACTIVE=prod`
- `SERVER_PORT=8080`
- `SERVER_CONTEXT_PATH=/api`
- `ALI_DASHSCOPE_API_KEY`（或 `DASHSCOPE_API_KEY` / `SPRING_AI_DASHSCOPE_API_KEY`）
- `SEARCH_API_KEY`
- `MYSQL_URL=jdbc:mysql://<host>:3306/ai_agent?...`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://your-frontend-domain.com`
- `APP_TOOLS_TERMINAL_ENABLED=false`
- `APP_TOOLS_FILE_ENABLED=false`

### 3) 运行

```powershell
.\mvnw.cmd -DskipTests spring-boot:run
```

## 微信云托管部署（当前推荐）

### 1) 构建 JAR

```powershell
.\mvnw.cmd -DskipTests clean package
```

### 2) 组装上传包（根目录仅 `app.jar` + `Dockerfile`）

```powershell
$outDir = "fc-upload-package"
$zip = "ai-agent-fc-upload.zip"

if (Test-Path $outDir) { Remove-Item $outDir -Recurse -Force }
if (Test-Path $zip) { Remove-Item $zip -Force }

New-Item -ItemType Directory -Path $outDir | Out-Null
Copy-Item "target\ai-agent-0.0.1-SNAPSHOT.jar" "$outDir\app.jar" -Force

@'
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY app.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar --server.port=${SERVER_PORT}"]
'@ | Set-Content -Encoding ascii "$outDir\Dockerfile"

Compress-Archive -Path "$outDir\*" -DestinationPath $zip -Force
```

### 3) 云托管发布配置

- 上传：`ai-agent-fc-upload.zip`
- 端口：`8080`
- Dockerfile 路径：`Dockerfile`
- 健康检查路径：`/api/ping`

### 4) 线上必须配置的环境变量

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_AI_DASHSCOPE_API_KEY=<your-key>`（建议直接配 Spring 原生变量）
- `SEARCH_API_KEY=<your-key>`
- `MYSQL_URL=jdbc:mysql://<内网IP>:3306/ai_agent?...`
- `MYSQL_USERNAME=root`
- `MYSQL_PASSWORD=<your-password>`
- `SERVER_PORT=8080`
- `SERVER_CONTEXT_PATH=/api`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS=<你的前端域名>`

## 常见问题排查

- 访问根域名 404：
  - 项目默认 context-path 为 `/api`，请访问 `/api/`。
- `DashScope API key must be set`：
  - 检查 `SPRING_AI_DASHSCOPE_API_KEY` 或 `ALI_DASHSCOPE_API_KEY`。
- `Could not resolve placeholder 'SEARCH_API_KEY'`：
  - 补充 `SEARCH_API_KEY` 环境变量。
- `Unknown database 'ai_agent'`：
  - 数据库中先创建 `ai_agent` 库，再执行建表 SQL。
- `Invalid CORS request`：
  - 将前端 `Origin` 加入 `APP_CORS_ALLOWED_ORIGIN_PATTERNS`（逗号分隔多个域名）。

## 安全建议

- 不要提交真实密钥和密码；`application-local.yml` 不入库。
- 密钥只通过环境变量注入。
- 若密钥曾在截图/日志中暴露，请立即轮换。
