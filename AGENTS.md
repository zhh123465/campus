# CampusForum 全局 Agent 上下文

本文件给后续 AI/Agent 对话快速理解当前仓库使用。结论以 2026-06-24 对 `D:\develop\campus` 的源码、配置、测试输出为准；如果本文与源码冲突，优先读源码中的 `backend/src/main/resources/*.yml`、Controller、Service、前端 `src/api/*` 与 `src/router/index.ts`。

## 项目定位

- CampusForum 是前后端分离的高校学习社群平台，中文品牌名在前端 PWA 中为“青云阁”。
- 核心产品形态是“全校广场 + 学习空间 + 资源 + 打卡 + AI 助手 + 管理后台”。
- 后端是 Spring Boot 单体，入口为 `backend/src/main/java/com/campusforum/CampusForumApplication.java`。
- 前端是 Vue 3 + Vite 应用，入口为 `frontend/src/main.ts`，路由在 `frontend/src/router/index.ts`。
- API 前缀统一是 `/api/v1`；前端 Axios 封装在 `frontend/src/api/request.ts`，`baseURL` 固定为 `/api/v1`。
- WebSocket 通知路径是 `/ws/notify`；生产建议先通过 `POST /api/v1/auth/ws-ticket` 获取短期票据。

## 技术栈

- 后端：Java 17、Spring Boot 3.3.0、MyBatis-Plus 3.5.7、Sa-Token、Redis、MySQL 8、Caffeine、MeiliSearch/MySQL 搜索兜底、阿里云 OSS/Local 存储、LangChain4j/OpenAI 兼容协议。
- 前端：Vue 3、Vite 5、TypeScript、Naive UI、Pinia、Vue Router、vue-i18n、vite-plugin-pwa、Tailwind/PostCSS、Vitest。
- 部署：`deploy/docker-compose.yml` 包含 nginx、app、mysql、redis、meilisearch；Dockerfile 运行镜像是 `eclipse-temurin:21-jre-alpine`，源码编译目标仍是 Java 17。
- 数据库：主 schema 在 `db/schema.sql`；迁移脚本在 `db/migrations/`。

## 主要目录

- `backend/src/main/java/com/campusforum`：后端业务源码，按模块分包。
- `backend/src/main/resources`：运行配置，`application.yml` 是通用配置，`application-dev.yml` 和 `application-prod.yml` 分环境覆盖。
- `backend/src/test/java`：后端测试；多数 SpringBootTest 依赖 MySQL/Redis。
- `frontend/src/api`：前端 API 封装。
- `frontend/src/pages`：页面实现，`pages/admin` 是管理后台。
- `frontend/src/layout/MainLayout.vue`：主布局和导航。
- `deploy/`：生产部署脚本、nginx、compose、环境变量模板。
- `docs/`：已有后端 API、实现说明、开发指南和安全加固报告。

## 后端模块地图

- `user`：注册、登录、邮箱验证码、密码重置、个人资料、头像/封面资产上传、微信/QQ/GitHub 登录接入。
- `security`、`infra/security`：Sa-Token 配置、启动安全校验、CORS、SSRF 防护、安全 HTTP 客户端、签名 URL、AES-GCM 加密、旧 ECB 兼容、WS ticket。
- `tenant`：standalone/multi 多租户上下文、解析器、过滤器、租户管理、租户缓存和租户审计。
- `post`、`qa`：帖子、评论、点赞/收藏、引用、问答采纳、浏览去重、置顶清理。
- `space`：学习空间创建、加入、成员、可见性、空间配置和敏感词设置。
- `resource`：资源上传、真实 MIME 校验、SHA-256 去重、权限过滤、下载/预览/文本预览。
- `ai`：普通 AI 接口、RAG、帖子智能卡片、租户 AI 配置代理、OpenAI 兼容调用。
- `ai.workspace`：AI 工作台原型，当前用 `backend/data/ai-workspace.json` 轻量持久化，不是最终生产数据库模型。
- `checkin`：打卡挑战、打卡记录、排行榜、打卡分享。
- `message`、`notify`：私信、通知、WebSocket 推送。
- `admin`、`report`、`sensitive`、`search`、`achievement`、`follow`：后台、举报、敏感词、搜索、成就和关注关系。

## 认证和权限约定

- 当前认证是 Sa-Token + Redis 持久化随机 tik token，不是 JWT；不要新增无效的 JWT secret 配置。
- 前端请求头是 `Authorization: <token>`，不带 `Bearer`。
- 登录成功后服务端 session 写入 `userId`、`role`、`tenantId`、`tenantCode`。
- 前端不再主动注入 `X-Tenant-Id`；已登录用户的租户上下文由服务端 session 和解析器权威决定。
- 管理后台权限来自 `AdminStpInterface`：`TENANT_ADMIN` 有租户后台权限，`SUPER_ADMIN` 额外有 `super:tenant:manage`。
- 普通 JSON API 返回 `R<T>`：`{ code, message, data, traceId }`，`code=0` 成功；文件下载/预览/导出可能直接写流。

## 多租户要点

- `tenant.mode=standalone|multi`。
- standalone 默认使用 `tenant.standalone-tenant-id=1`，必须对应 `tenants` 表中启用的记录。
- multi 模式通过根域名子域或受控 `X-Tenant-Id` fallback 解析租户。
- 后续做 SQL、缓存、搜索、通知、AI 配置时必须确认租户上下文，避免跨租户泄漏。

## AI 和外部服务

- 全局 fallback AI 配置在 `ai.*`，租户级配置优先存于 `tenants.ai_config`，由 `TenantAwareAiService` 每次请求解析。
- `OpenAiCompatService` 会将 base URL 规范到 `/v1` 风格。
- 截至 2026-06-24 联网核验：DeepSeek 官方 OpenAI 兼容 base URL 可用 `https://api.deepseek.com`，配置中的 `deepseek-v4-flash` 属于官方可用模型；小米 MiMo OpenAI 兼容 base URL 是 `https://api.xiaomimimo.com/v1`。
- 任何后续外部 AI 服务、模型名、base URL、SDK 版本、官方限制都要联网核验后再改配置。

## 前端运行逻辑

- `frontend/src/api/request.ts` 统一处理 token、401 清会话跳登录、429/413/415 错误文案。
- `frontend/src/stores/auth.ts` 用 localStorage 保存 `token`、`role`、`tenantId`、`tenantCode`；存在 `GUEST_TOKEN` 游客态。
- `frontend/src/router/index.ts` 对 requiresAuth、guest、requiresAdmin 做路由守卫；目前存在一个重复的 `/spaces/:id` 路由定义，需要后续清理。
- 管理后台路由挂在 `/admin`，页面位于 `frontend/src/pages/admin`。
- 前端部分页面仍包含演示/mock UI，如打卡页 fallback challenges、空间详情活动图表、后台 dashboard mock chart。

## 资源和上传

- 后端默认允许 `pdf/doc/docx/ppt/pptx/xls/xlsx/jpg/jpeg/png/gif/webp/md/markdown`。
- 后端默认不允许 `zip/rar/7z`；但 `SpaceDetail.vue` 的 `uploadAccept` 仍包含压缩包扩展名，前后端存在联调不一致。
- 上传会校验扩展名和真实 MIME，流式计算 SHA-256，命中重复文件后复用已有资源并删除新上传对象。
- 资源可见性：`PUBLIC` 登录/公开场景可见，`SPACE` 空间成员可见，`PRIVATE` 仅上传者和管理员可见；无权访问按资源不存在处理。

## 部署和安全

- 生产配置必须用环境变量注入关键密钥：`SIGNED_URL_SECRET`、`CRYPTO_MASTER_KEY`、Redis 密码、MeiliSearch key、OSS 凭证、CORS/WS 来源等。
- `application-prod.yml` 不应添加字面密钥默认值。
- `SecurityStartupValidator` 会阻止生产使用弱默认值、缺少关键密钥、过期 legacy ECB 或 WS ticket 切换配置。
- `deploy/install.sh` 会生成关键随机密钥并检查 `ChangeMe` 占位值。
- `deploy/nginx/nginx.conf` 负责代理 `/api/`、`/ws/`，并屏蔽 actuator、Swagger/Knife4j 路径。

## 常用命令

后端：

```bash
cd backend
mvn test -Dspring.profiles.active=ci
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm ci
npm run lint
npm run test
npm run build
```

部署：

```bash
cd deploy
cp .env.example .env
bash install.sh
```

## 当前环境和已知坑

- 当前工作区根目录是 `D:\develop\campus`。
- 2026-06-24 检查时 `rg.exe` 存在但执行返回 Access denied，可改用 `git ls-files`、PowerShell `Get-ChildItem`、`Select-String`。
- 当前本机未检测到 Docker；本地 `127.0.0.1:3306` 可连接，`127.0.0.1:6379` 不可连接。
- 默认 `mvn test` 会激活 `dev` profile 并尝试连接 `192.168.150.130:3306`、`192.168.150.130:6379`；没有这台开发虚拟机时会出现大量 ApplicationContext 错误。更可靠的本地/CI 测试需要显式提供 MySQL/Redis 并使用 `-Dspring.profiles.active=ci`。
- 根目录现有 `agent.md` 是未提交修改状态且内容显示为乱码；README 又引用了不存在的 `AGENT.md`。后续处理文档时不要直接覆盖 `agent.md`，先确认用户意图。
- 不要删除工作区外文件；若需要删除任何工作区外文件，必须先问用户。
- 如果任务需要某个工具而本机没有安装，应说明并自行安装，不要因为工具缺失而降级成明显更差的策略。
- 对具备时效性的信息必须联网核验，包括 AI 服务 base URL、模型名、依赖最新版本、第三方 API 规则等。

