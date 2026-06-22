# 后端代码检查与加固完成报告

日期：2026-06-22

## 执行流程

本轮按“检查 -> 计划 -> 修复 -> 再检查”执行：

1. 检查：确认 git 状态、后端技术栈、测试入口和既有安全规格；安装并校验本地 ripgrep 15.1.0 用于静态扫描；执行后端全量测试。
2. 计划：优先处理上线误配置风险和帖子点赞逻辑顺序风险，避免扩大改动面。
3. 修复：调整后端基础配置默认值、Maven 本地测试 profile、帖子点赞前置校验，并补充回归测试。
4. 再检查：执行定向测试和全量测试，并复扫弱默认配置。

## 已完成修复

### 1. 生产误部署风险加固

- `backend/src/main/resources/application.yml`
  - 移除默认激活 `dev` profile，改为 `SPRING_PROFILES_ACTIVE` 显式控制。
  - 移除基础配置中的 MySQL/Redis 明文默认地址和密码。
  - 移除 MeiliSearch `masterKey` 弱默认值。

影响：如果未显式指定 profile 或基础设施凭据，应用不再静默使用开发环境默认配置，降低误上线和弱凭据暴露风险。

### 2. 保持本地测试入口可用

- `backend/pom.xml`
  - 为 Maven 测试增加默认 `dev` profile，仅在未显式传入 `spring.profiles.active` 时生效。

影响：`mvn test` 仍保持当前本地开发体验；CI 的 `-Dspring.profiles.active=ci` 和 Docker 的 `SPRING_PROFILES_ACTIVE=prod` 不受影响。

### 3. 帖子点赞逻辑顺序修复

- `backend/src/main/java/com/campusforum/post/service/PostService.java`
  - 对 `POST + LIKE` 反应先校验目标帖子存在且未删除，再查询/写入 reaction 和更新计数。

影响：避免对不存在或已删除帖子执行反应写入路径，业务顺序更清晰，降低脏数据与计数异常风险。

### 4. 回归测试补充

- `backend/src/test/java/com/campusforum/post/service/PostServiceTest.java`
  - 新增 `toggleReactionShouldRejectMissingPostBeforeWritingReaction`，验证不存在帖子点赞会抛业务异常，且不留下 reaction 记录。

### 5. 接口与实现文档同步

- `docs/后端API接口文档.md`
  - 补充帖子点赞/收藏接口的路径 `id` 与 Body `targetId` 关系，以及目标帖子不存在或已删除时的 `40400` 行为。
- `docs/后端功能实现文档.md`
  - 补充帖子点赞前置校验和无副作用失败语义。

## 验证结果

- 定向测试：
  - `mvn "-Dtest=PostServiceTest,SecurityStartupValidatorProdTest,SecurityStartupValidatorDevTest" test`
  - 结果：34 tests, 0 failures, 0 errors, 0 skipped

- 全量后端测试：
  - `mvn test`
  - 结果：540 tests, 0 failures, 0 errors, 0 skipped

- 静态复扫：
  - 基础 `application.yml` 未再发现 MySQL/Redis 明文默认值、默认 `dev` profile、MeiliSearch `masterKey` 弱默认值。
  - `application-dev.yml` 仍保留开发环境默认值，仅在显式 dev profile 下生效。

## Git 记录

- 分支：`codex/backend-production-hardening`
- 本地提交：
  - `0565e7a fix: harden backend profile defaults and post reactions`

未推送远程。

## 遗留与建议

- 本轮未修改用户已有未提交文件：`agent.md`、`docs/后端API接口文档.md`、`docs/后端功能实现文档.md`。
- 建议后续继续分批处理更大范围的代码质量问题，例如 AI 工作台模块中大量 `Map<String, Object>` 临时数据结构、消息已读批量更新的逐行 update、以及测试环境依赖共享开发数据库导致的数据膨胀问题。
