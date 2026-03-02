# ai-agent

Spring Boot + Spring AI 项目，包含对话、RAG、工具调用和 MCP 扩展能力。

## 上 GitHub 前建议

1. 确保不提交密钥：`application-local.yml` 已被 `.gitignore` 忽略，密钥请只走环境变量。
2. 确保不提交运行产物：`target/`、`tmp/`、`run-*.log` 已在忽略规则内。
3. 在仓库根目录准备环境变量文件：可参考 `.env.example`。

## 本地构建

```powershell
.\mvnw.cmd -DskipTests package
```

## 关键环境变量

- `SPRING_PROFILES_ACTIVE=prod`
- `SERVER_PORT=8080`
- `SERVER_CONTEXT_PATH=/api`
- `ALI_DASHSCOPE_API_KEY`（或 `DASHSCOPE_API_KEY`）
- `SEARCH_API_KEY`
- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS`（多个域名用逗号分隔）

## 阿里云 Serverless 托管建议（推荐 SAE）

### 方案 A：直接部署 JAR（推荐简单）

1. 打包：`.\mvnw.cmd -DskipTests package`
2. 在 SAE 创建应用（Java 17 运行时）
3. 上传 `target/*.jar`
4. 在 SAE 环境变量中配置上面的 key/db 参数
5. 健康检查路径可设为：`/api/actuator/health`（若启用 actuator）或业务探活接口

### 方案 B：容器部署（本仓库已提供 Dockerfile）

```powershell
.\mvnw.cmd -DskipTests package
docker build -t ai-agent:latest .
```

将镜像推到 ACR 后，SAE 选择镜像部署并注入环境变量。

## 说明

- 生产环境默认关闭高风险工具：终端执行与文件写入。
- 前端已优先请求同域 `/api`，更适合 Serverless 网关/域名转发场景。
- 如前后端分离部署，请设置 `APP_CORS_ALLOWED_ORIGIN_PATTERNS` 为前端域名。
