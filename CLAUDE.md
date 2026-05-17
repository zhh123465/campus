# CLAUDE.md

This file provides guidance to Claude (claude.ai/code) when working with code in this repository.

## 角色设定

与我交流必须使用中文。

遵守以下规则：
1. 在做任何决策之前，必须先进行充分的研究和分析。
2. 对于有疑问的地方，必须先进行确认，然后再进行操作。
3. 对于任何没有的工具和配置，优先询问是否安装，待确认后再安装，不要自作主张使用降级策略替代。

## 项目概述

CampusForum — 基于多租户开源架构与 AI 增强的高校轻量化学习社群平台。MIT 协议开源。

## 常用命令

**后端**（`backend/`，Spring Boot Maven 项目，JDK 17）：

```bash
export JAVA_HOME=/home/morose/.local/jdk-17
cd backend
/mnt/d/develop/apache-maven-3.9.4/bin/mvn spring-boot:run        # 启动开发服务器
/mnt/d/develop/apache-maven-3.9.4/bin/mvn test                   # 运行全部单元测试
/mnt/d/develop/apache-maven-3.9.4/bin/mvn test -Dtest=XxxTest    # 运行单个测试类
/mnt/d/develop/apache-maven-3.9.4/bin/mvn package -DskipTests    # 打包（跳过测试）
```

**前端**（`frontend/`）：

```bash
cd frontend
npm run dev          # 启动 Vite 开发服务器（默认 http://localhost:5173）
npm run build        # 生产构建 → dist/
npm run lint         # ESLint + Prettier 检查
npm run format       # Prettier 格式化
npm test             # vitest 单元测试（run 模式）
npm run test:watch   # vitest 监听模式
```

**部署**（`deploy/`）：

```bash
cd deploy
cp .env.example .env       # 修改域名、密码、AI Key 等
bash install.sh            # 一键部署（拉镜像 + 初始化 + 启动）
docker compose up -d       # 直接 compose 启动
```

## 技术栈

| 层 | 选型 |
|---|---|
| 前端 | Vue3 + Composition API + Vite 5 + Naive UI + Pinia + vite-plugin-pwa |
| 后端 | Java 17 + Spring Boot 3.x + MyBatis-Plus + Sa-Token |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 + Caffeine（二级缓存） |
| 搜索 | MeiliSearch（MySQL FULLTEXT 兜底） |
| 存储 | MinIO（S3 兼容）/ 阿里云 OSS（可选） |
| AI | LangChain4j + OpenAI 兼容协议 |
| 部署 | Docker Compose（默认）+ K8s Helm（可选） |

## 高层架构

**架构模式**：模块化单体（Modular Monolith）。后端按 package 划分业务模块，模块间通过 Spring 内部 API 调用，禁止反向依赖。

**应用分层**：

```
Controller（接入层）→ Service（业务编排）→ Domain（领域模型）→ Infra（基础设施）
横向切面：鉴权 / 租户上下文 / 日志 / 限流 / 审计
```

**后端 package 划分**（`com.campusforum`）：

| package | 职责 |
|---|---|
| `common` | BaseEntity, R, BusinessException, ErrorCode, GlobalExceptionHandler, CryptoUtils |
| `infra` | MyBatisPlusConfig, StorageService (MinIO/OSS/Local), WebMvcConfig, Knife4jConfig |
| `tenant` | TenantContext（ThreadLocal）、TenantInterceptor、TenantController |
| `security` | SaToken 鉴权配置 |
| `user` | 注册/登录/个人资料/封禁/角色变更 |
| `space` | 学习空间 CRUD、成员管理、加入/审核/退出 |
| `post` | 帖子 CRUD、评论（含回复）、点赞/收藏 |
| `qa` | 问答扩展：悬赏、采纳回答 |
| `checkin` | 打卡挑战、每日打卡、连续统计、排行榜 |
| `resource` | 文件上传/下载/列表 |
| `notify` | 通知（点赞/评论/回复/采纳/申请）+ WebSocket 推送 |
| `search` | 全局搜索（帖子 FULLTEXT + 用户/资源/空间 LIKE） |
| `admin` | 仪表盘/用户/帖子/空间管理/审计日志 + StpInterface 权限映射 |
| `ai` | AiService 接口抽象 + MockAiService（摘要/审核/标签/问答） |
| `message` | 私信：一对一文本+图片、WebSocket 实时推送 |
| `follow` | 关注/取关/粉丝/关注列表/计数 |
| `achievement` | 成就徽章自动触发/种子数据/用户成就查询 |
| `points` | 积分流水、登录/发帖/打卡/采纳自动奖励 |
| `sensitive` | 敏感词过滤、管理 CRUD |
| `report` | 举报创建、管理后台处理/驳回/批量操作 |

**前端页面**（`frontend/src/pages/`）：Home、Square、Spaces、PostDetail、PostCreate、Messages、Notifications、Profile、UserPage、FollowsList、Resources、ResourceDetail、ResourceUpload、CheckinChallenges、CheckinChallengeDetail、CheckinChallengeCreate、Search、Points、AiAssistant、Login、Register、ForgotPassword、NotFound

**前端 store**：`stores/auth.ts`（用户认证状态）

**前端 composables**：`composables/useWebSocket.ts`（WebSocket 连接复用）

## 关键设计

### 多租户

- **双模切换**：`application.yml` 中 `tenant.mode: standalone | multi`
- **数据隔离**：所有业务表含 `tenant_id`，由 `TenantLineInnerInterceptor` 自动改写 SQL；不隔离的表在 `MyBatisPlusConfig.TENANT_IGNORE_TABLES` 中声明（`tenants`、`achievements`、`sensitive_words`）
- **缓存隔离**：Redis Key 格式 `{app}:t{tenantId}:{module}:{key}`
- **搜索隔离**：MeiliSearch 索引按租户分片 `post-t{id}`
- **文件隔离**：MinIO 按 `tenant-{id}/` 分目录

### 权限模型（三级 RBAC）

平台层（超级管理员）→ 校级（管理员/普通用户）→ 空间级（所有者/管理员/成员）。权限以 `domain:resource:action` 格式命名，鉴权通过 Sa-Token 实现。

### AI 可插拔

`AiService` 接口抽象，通过 `ai.provider: openai | ollama | mock` 配置切换。支持任何 OpenAI 兼容协议（DeepSeek、智谱、通义、Ollama）。AI 限流：每用户每分钟 5 次，每租户每天 1000 次。

### 存储可插拔

`StorageService` 接口，通过 `storage.type: minio | oss | local` 配置切换。

### WebSocket

通知和私信均走 WebSocket（端点 `/ws/notify`），后端由 `NotifyWebSocketHandler` + `SessionRegistry` 管理连接，前端由 `useWebSocket` composable 统一复用。

## 开发约定

- **统一响应格式**：`{ code: 0, message: "ok", data: {...}, traceId: "..." }`
- **错误码**：`0` 成功，`1xxxx` 业务错误，`4xxxx` 客户端错误，`5xxxx` 服务端错误
- **URL 前缀**：`/api/v1/`
- **鉴权 Header**：`Authorization: Bearer <token>`（Sa-Token `tik` 风格）
- **多租户 Header**：`X-Tenant-Id`（仅 SaaS 模式）
- **分页**：游标分页优先（`id < lastId limit n`），MyBatis-Plus 分页插件限制最大 100 条
- **逻辑删除**：字段 `deleted`，`1` 已删除，`0` 正常
- **时间格式**：ISO 8601
- **提交规范**：Conventional Commits（`feat:` / `fix:` / `docs:` / `refactor:`）
- **分支策略**：`main` ← `develop` ← `feature/*` / `fix/*`
