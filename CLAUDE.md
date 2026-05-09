# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

CampusForum — 基于多租户开源架构与 AI 增强的高校轻量化学习社群平台。MIT 协议开源。

## 技术栈

| 层 | 选型 |
|---|---|
| 前端 | Vue3 + Composition API + Vite 5 + Naive UI + Pinia + vite-plugin-pwa |
| 后端 | Java 17 + Spring Boot 3.x + MyBatis-Plus + Sa-Token |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 + Caffeine（二级缓存） |
| 搜索 | MeiliSearch（MySQL FULLTEXT 兜底） |
| 存储 | MinIO（S3 兼容，预留 OSS 适配） |
| AI | LangChain4j + OpenAI 兼容协议 |
| 部署 | Docker Compose（默认） + K8s Helm（可选） |
| CI/CD | GitHub Actions |

## 常用命令

项目代码尚未建立，以下为规划中的命令（技术设计文档约定）。

**后端**（`backend/`，Spring Boot Maven 项目）：
```bash
cd backend
./mvnw spring-boot:run          # 启动开发服务器
./mvnw test                      # 运行全部单元测试
./mvnw test -Dtest=XxxTest      # 运行单个测试类
./mvnw verify                    # 运行集成测试（需 Docker）
```

**前端**（`frontend/`）：
```bash
cd frontend
npm run dev                      # 启动 Vite 开发服务器
npm run build                    # 生产构建 → dist/
npm run lint                     # ESLint + Prettier
npm run test                     # Vitest 单元测试
```

**部署**（`deploy/`）：
```bash
cd deploy
cp .env.example .env             # 修改域名、密码、AI Key 等
bash install.sh                  # 一键部署（拉镜像 + 初始化 + 启动）
docker compose up -d             # 或直接 compose 启动
docker compose -f docker-compose.saas.yml up -d  # SaaS 多租户模式
```

## 高层架构

**设计原则**：轻量优先、前后端分离、模块化分层、多租户为一等公民。

**架构模式**：阶段一采用**模块化单体（Modular Monolith）**，后端按 package 划分业务模块，模块间通过 Spring 内部 API 调用、禁止反向依赖。后续可按需拆分为微服务。

### 应用分层

```
Controller（接入层）→ Service（业务编排）→ Domain（领域模型）→ Infra（基础设施）
横向切面：鉴权 / 租户上下文 / 日志 / 限流 / 审计
```

### 后端 Package 划分（`com.campusforum`）

| package | 职责 |
|---|---|
| `common` | 通用组件：异常、统一响应、工具类、注解 |
| `infra` | 基础设施：缓存、存储、搜索、AI、邮件 |
| `tenant` | 多租户：TenantContext（ThreadLocal）、拦截器、MyBatis 插件 |
| `security` | 鉴权与权限（Sa-Token + RBAC） |
| `user` | 用户、个人主页、关注 |
| `space` | 学习空间（专业/班级/社团）、成员管理 |
| `post` | 帖子、评论、点赞、话题 |
| `qa` | 学习互助问答 |
| `checkin` | 自律打卡 |
| `resource` | 资源共享 |
| `notify` | 通知中心 + WebSocket |
| `search` | 搜索聚合 |
| `admin` | 管理后台 |
| `audit` | 操作审计 |
| `ai` | AI 能力封装（摘要/审核/标签/RAG） |

### 多租户设计（核心研究点）

- **双模可切换**：`tenant.mode = standalone | multi`，一键切换独立部署/多租户 SaaS
- **数据隔离**：所有业务表带 `tenant_id`，由 MyBatis-Plus `TenantLineInnerInterceptor` 自动改写 SQL
- **双保险**：SQL 插件注入 + Service 层 `tenant_id` 比对断言
- **缓存隔离**：Redis Key 格式 `{app}:t{tenantId}:{module}:{key}`
- **搜索隔离**：MeiliSearch 索引按租户分片 `post-t1`、`post-t2`
- **文件隔离**：MinIO 按 `tenant-{id}/` 分目录

### 权限模型

三级 RBAC + 资源域校验：平台层（超级管理员）→ 校级（管理员/普通用户）→ 空间级（所有者/管理员/成员）。权限粒度到接口级，以 `domain:resource:action` 命名。

### AI 可插拔设计

`AiService` 接口抽象，通过 `ai.provider` 配置切换实现（`openai` → `ollama` → `mock`）。支持 OpenAI 兼容协议的任何模型（DeepSeek、智谱、通义、本地 Ollama）。

## 工程目录（规划）

```
CampusForum/
├─ backend/                  # Spring Boot 应用
├─ frontend/                 # Vue3 + Vite
├─ deploy/                   # Docker Compose / Helm / 一键脚本
├─ docs/                     # 需求/设计/用户/二次开发文档
├─ db/                       # schema.sql + migrations
├─ .github/                  # CI/CD workflows
├─ LICENSE                   # MIT
└─ README.md
```

## 开发约定

- **提交**：Conventional Commits（`feat:` / `fix:` / `docs:` / `refactor:`）
- **分支策略**：`main`（稳定）← `develop`（集成）← `feature/*` / `fix/*`
- **统一响应格式**：`{ code: 0, message: "ok", data: {...}, traceId: "..." }`
- **错误码**：`0` 成功，`1xxxx` 业务错误，`4xxxx` 客户端错误，`5xxxx` 服务端错误
- **URL 前缀**：`/api/v1/`
- **鉴权 Header**：`Authorization: Bearer <token>`
- **多租户 Header**：`X-Tenant-Id`（仅 SaaS 模式）
- **分页**：游标分页优先（`id < lastId limit n`），避免深翻页 OFFSET
- **时间格式**：ISO 8601

## 当前状态

项目处于需求与设计冻结阶段（2026.05），预计 2026.06 开始工程脚手架搭建。当前仓库仅含需求分析文档和技术设计文档。完整设计细节见 `01-需求分析文档.md` 和 `02-技术设计文档.md`。
