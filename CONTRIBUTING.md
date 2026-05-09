# Contributing to CampusForum

感谢你对 CampusForum 的关注！我们欢迎任何形式的贡献。

## 行为准则

本项目遵循 [贡献者公约](CODE_OF_CONDUCT.md)。参与即表示你同意遵守其条款。

## 如何贡献

### 报告 Bug

1. 在 Issues 中使用 "Bug Report" 模板
2. 描述复现步骤、期望行为和实际行为
3. 提供环境信息（OS、浏览器、版本号）

### 提交功能请求

1. 在 Issues 中使用 "Feature Request" 模板
2. 描述使用场景和期望效果
3. 说明是否愿意参与实现

### 代码贡献流程

1. Fork 本仓库
2. 从 `develop` 分支创建 `feature/xxx` 或 `fix/xxx` 分支
3. 编写代码并添加测试
4. 确保 CI 全部通过（lint + test）
5. 提交 PR 到 `develop` 分支
6. 等待 Code Review

## 开发环境搭建

```bash
# 后端
cd backend
./mvnw spring-boot:run

# 前端
cd frontend
npm install
npm run dev
```

## 提交规范

使用 Conventional Commits:

- `feat:` 新功能
- `fix:` 修复
- `docs:` 文档
- `refactor:` 重构
- `test:` 测试
- `chore:` 构建/工具

## 代码规范

- Java: Google Java Style + 阿里巴巴 Java 开发手册
- TypeScript: ESLint + Prettier, strict mode
- Vue: Composition API + `<script setup>`
