# CampusForum 前端页面-接口映射表

## 1. 文档说明

本文从“页面调用了哪些接口、在什么时机调用、用于什么交互”三个维度，对当前前端页面进行接口映射整理，便于：

- 前后端联调
- 接口缺口排查
- 页面重构时的依赖梳理
- 测试用例编写

说明：

- 这里只统计当前前端代码中已经直接调用的接口。
- 若页面明显应当调用某接口但当前尚未接入，会在“待接入/说明”中注明。
- 接口路径以 `src/api/*.ts` 中封装语义为准，不重复展开全部底层 axios 细节。

---

## 2. 全局层接口映射

## 2.1 `App.vue`

### 直接接口

无。

### 依赖的全局能力

- 通过 `AppNotify.vue` 间接依赖 WebSocket 通知能力

---

## 2.2 `AppNotify.vue`

### 直接能力

- WebSocket：`/ws/notify?token=...`

### 调用时机

- 应用启动后常驻挂载

### 用途

- 接收实时通知事件并弹出全局通知

### 输出影响

- 不改页面数据
- 只做全局提醒

---

## 2.3 `MainLayout.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getMe()` | `GET /auth/me` | 页面挂载时，且已登录但 store 无 user 时 | 补齐当前用户信息 |
| `logout()` | `POST /auth/logout` | 用户下拉点击退出登录时 | 结束登录态 |

### 依赖状态

- `useAuthStore()`
- `localStorage.token`
- `localStorage.role`

### 交互说明

- 搜索跳转不直接请求接口，而是跳到 `/search`
- 消息、通知也只是导航，不在布局层直接拉数据

---

## 2.4 `AdminLayout.vue`

### 直接接口

无。

### 说明

- 仅作为后台导航框架，不直接处理数据请求

---

## 2.5 全局请求层 `src/api/request.ts`

### 全局行为

- 自动注入 `Authorization`
- 自动注入 `X-Tenant-Id`
- 非 `code === 0` 时统一抛错
- `401` 时清空 token 并跳 `/login`

### 影响范围

- 影响所有业务页面

---

## 3. 账号与访客页面接口映射

## 3.1 `Home.vue`

### 直接接口

无。

### 说明

- 当前首页为展示型页面，统计数据与热门部落均为静态 mock

### 待接入建议

- 首页统计概览接口
- 首页热门空间/热门帖子接口

---

## 3.2 `Login.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `login(data)` | `POST /auth/login` | 点击登录时 | 获取 token 与当前用户信息 |

### 入参

- `email`
- `password`

### 成功后动作

- `authStore.setToken(res.token)`
- `authStore.setUser(res.user)`
- 跳转 `/square`

---

## 3.3 `Register.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `register(data)` | `POST /auth/register` | 点击注册时 | 创建账号 |

### 入参

- `email`
- `password`
- `studentNo?`
- `nickname`

### 成功后动作

- 提示成功
- 跳转 `/login`

---

## 3.4 `ForgotPassword.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `forgotPassword(email)` | `POST /auth/forgot-password` | 第一步点击获取重置令牌时 | 生成重置 token |
| `resetPassword(email, token, newPassword)` | `POST /auth/reset-password` | 第二步点击重置密码时 | 完成密码重置 |

### 说明

- 当前前端直接消费返回 token，用于第二步提交

---

## 4. 广场与帖子模块接口映射

## 4.1 `Square.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getPosts(params)` | `GET /posts` | 页面初始化、切换排序、滚动触底 | 获取广场帖子流 |

### 关键参数

- `scope: 'SQUARE'`
- `sort: latest / trending / essence / follow`
- `cursor`
- `cursorId`
- `limit: 10`

### 说明

- 不同排序模式下，游标语义略有差异
- 依赖后端支持 `follow`、`trending` 等排序策略

---

## 4.2 `PostCreate.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getPostById(id)` | `GET /posts/:id` | 带 `quote` 参数进入页面时 | 获取被引用帖子 |
| `createPost(data)` | `POST /posts` | 点击发布时 | 创建帖子 |

### 提交参数

可能包含：

- `title`
- `content`
- `topics`
- `scope: 'SQUARE'`
- `type: NORMAL / QA`
- `bountyPoints`
- `quotePostId`

---

## 4.3 `PostDetail.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getPostById(id)` | `GET /posts/:id` | 页面初始化 | 获取帖子详情 |
| `getComments(postId, ...)` | `GET /posts/:id/comments` | 页面初始化、评论后、删评后 | 获取评论列表 |
| `getQaInfo(postId)` | `GET /qa/:postId` | QA 帖初始化时 | 获取问答信息 |
| `toggleReaction(id, 'LIKE')` | `POST /posts/:id/reactions` | 点赞帖子时 | 点赞/取消点赞 |
| `deletePost(id)` | `DELETE /posts/:id` | 删除帖子时 | 删除帖子 |
| `createComment(data)` | `POST /posts/:postId/comments` | 发表评论/回复时 | 新建评论 |
| `toggleCommentReaction(id, 'LIKE')` | `POST /comments/:id/reactions` | 点赞评论时 | 点赞/取消点赞评论 |
| `deleteComment(id)` | `DELETE /comments/:id` | 删除评论时 | 删除评论 |
| `acceptAnswer(postId, commentId)` | `POST /qa/:postId/accept/:commentId` | 帖子作者采纳回答时 | 标记最佳答案 |
| `createReport(data)` | `POST /reports` | 举报帖子/评论时 | 发起举报 |

### 参数说明

#### `createComment()`

- `postId`
- `parentId?`
- `replyToId?`
- `content`

#### `createReport()`

- `targetType: POST / COMMENT`
- `targetId`
- `reason`
- `description?`

---

## 5. 用户与关注关系模块接口映射

## 5.1 `Profile.vue`

### 直接接口

无。

### 说明

- 当前页面为视觉稿/静态数据为主
- 尚未接入这些已存在的能力：
  - `getMyProfile()`
  - `updateProfile()`
  - `getMuteSettings()`
  - `updateMuteSettings()`
  - `getTagSubscriptions()`
  - `updateTagSubscriptions()`

---

## 5.2 `UserPage.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getUserById(id)` | `GET /users/:id` | 页面初始化、切换用户时 | 获取用户资料 |
| `getFollowCounts(id)` | `GET /follows/:id/counts` | 页面初始化 | 获取关注/粉丝数 |
| `getPosts(params)` | `GET /posts` | 页面初始化 | 获取该用户最近帖子 |
| `getUserAchievements(id)` | `GET /achievements?userId=...` | 页面初始化 | 获取用户成就 |
| `isFollowing(id)` | `GET /follows/check/:id` | 页面初始化、用户切换、登录态变化时 | 判断当前登录用户是否已关注该用户 |
| `follow(id)` | `POST /follows/:id` | 点击关注时 | 关注用户 |
| `unfollow(id)` | `DELETE /follows/:id` | 点击取消关注时 | 取消关注 |

### 关键参数

`getPosts()` 当前使用：

- `scope: 'SQUARE'`
- `authorId: 用户ID`
- `sort: 'latest'`
- `limit: 6`

### 说明

- `authorId` 参数在 `posts.ts` 类型中未显式声明，但页面代码已在使用，说明存在类型与实际接口能力不完全同步的情况

---

## 5.3 `FollowsList.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getUserFollowers(userId)` | `GET /follows/:id/followers` | 页面初始化 | 拉取粉丝列表 |
| `getUserFollowing(userId)` | `GET /follows/:id/following` | 页面初始化 | 拉取关注列表 |
| `getFollowCounts(userId)` | `GET /follows/:id/counts` | 页面初始化 | 获取关注/粉丝总数 |
| `getUserById(userId)` | `GET /users/:id` | 页面初始化 | 获取用户昵称作为标题 |

---

## 6. 学习空间模块接口映射

## 6.1 `Spaces.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getSpaces(params)` | `GET /spaces` | 页面初始化、切换分类时 | 获取空间列表 |

### 参数

- `category?`
- `limit: 20`

---

## 6.2 `SpaceCreate.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `createSpace(data)` | `POST /spaces` | 点击创建时 | 创建学习空间 |

### 参数

- `name`
- `description?`
- `category`
- `visibility`

---

## 6.3 `SpaceDetail.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getSpaceById(id)` | `GET /spaces/:id` | 页面初始化 | 获取空间详情 |
| `getSpaceMembers(id)` | `GET /spaces/:id/members` | 页面初始化 | 获取空间成员 |
| `getPosts(params)` | `GET /posts` | 页面初始化 | 获取空间帖子 |

### 当前问题说明

- 页面请求了 `getPosts({ scope: 'SPACE', limit: 10 })`，但没有把当前 `spaceId` 传进去
- 页面主内容仍大量使用 mock 数据，没有真正绑定请求结果

### 待接入建议

- `joinSpace(id)`
- `leaveSpace(id)`
- `handleMember(spaceId, userId, action)`
- `dismissSpace(id)`
- 空间内发帖/文件/公告能力

---

## 7. 打卡挑战模块接口映射

## 7.1 `CheckinChallenges.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getChallenges(params)` | `GET /checkin/challenges` | 页面初始化 | 获取挑战列表 |

### 参数

- `limit: 30`

---

## 7.2 `CheckinChallengeCreate.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `createChallenge(data)` | `POST /checkin/challenges` | 点击创建时 | 创建打卡挑战 |

### 参数

- `name`
- `description?`
- `startDate`
- `endDate`

---

## 7.3 `CheckinChallengeDetail.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getChallengeById(id)` | `GET /checkin/challenges/:id` | 页面初始化、打卡后刷新 | 获取挑战详情 |
| `getRecords(id, ...)` | `GET /checkin/challenges/:id/records` | 页面初始化、打卡后刷新 | 获取打卡记录 |
| `getLeaderboard(id)` | `GET /checkin/challenges/:id/leaderboard` | 页面初始化、打卡后刷新 | 获取排行榜 |
| `checkin(id, data)` | `POST /checkin/challenges/:id/checkin` | 点击打卡时 | 提交打卡 |
| `deleteChallenge(id)` | `DELETE /checkin/challenges/:id` | 创建者删除挑战时 | 删除挑战 |
| `shareCheckinRecord(recordId)` | `POST /checkin/records/:id/share` | 分享记录到广场时 | 生成分享帖子 |

### 参数

`checkin()`：

- `content?`

---

## 8. 资源分享模块接口映射

## 8.1 `Resources.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getResources(params)` | `GET /resources` | 页面初始化 | 获取资源列表 |

### 参数

- `college?`
- `limit: 30`

### 说明

- 页面中虽然有 `collegeFilter` 状态，但当前没有筛选 UI 触发它

---

## 8.2 `ResourceUpload.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `uploadResource(file, meta)` | `POST /resources` | 点击上传时 | 上传资源文件 |

### 参数

元数据可能包含：

- `visibility`
- `college?`
- `major?`
- `course?`
- `description?`

---

## 8.3 `ResourceDetail.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getResourceById(id)` | `GET /resources/:id` | 页面初始化、下载后刷新 | 获取资源详情 |
| `getDownloadUrl(id)` | 下载链接生成 | 点击下载时 | 获取带 token 的下载地址 |
| `deleteResource(id)` | `DELETE /resources/:id` | 删除资源时 | 删除资源 |

### 说明

- 下载不走 axios，而是浏览器打开下载 URL
- 下载后延迟刷新详情，用于更新下载次数

---

## 9. 搜索模块接口映射

## 9.1 `Search.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `search(params)` | `GET /search` | 搜索提交、切换类型后二次搜索 | 获取全站搜索结果 |

### 参数

- `keyword`
- `type?`

### 当前问题说明

- 页面没有读取路由 query 中的 `q`
- 所以从全局搜索框或 `@提及` 跳来后，不会自动搜索

---

## 10. 积分模块接口映射

## 10.1 `Points.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getBalance()` | `GET /points/balance` | 页面初始化 | 获取当前积分余额 |
| `getPointsLogs()` | `GET /points/logs` | 页面初始化 | 获取积分流水 |

---

## 11. AI 助手模块接口映射

## 11.1 `AiAssistant.vue`

### 当前实际调用接口

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getPostById(id)` | `GET /posts/:id` | 输入帖子链接/ID后生成摘要时 | 获取帖子内容 |

### 当前未接入但已存在的 AI 接口

| 接口封装 | 后端语义 | 适合接入页面位置 | 用途 |
|---|---|---|---|
| `aiSummarize(content)` | `POST /ai/summarize` | 智能摘要 | 对帖子正文做真实摘要 |
| `aiModerate(content)` | `POST /ai/moderate` | 内容检测 | 检测违规/风险内容 |
| `aiRecommendTags(title, content)` | `POST /ai/tags` | 发帖页 / AI 助手 | 自动推荐标签 |
| `aiChat(messages)` | `POST /ai/chat` | AI 问答 | 多轮问答 |

### 当前实现说明

- 摘要结果使用 `setTimeout` 模拟生成
- 还不是正式 AI 联调版本

---

## 12. 通知模块接口映射

## 12.1 `Notifications.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getNotifications(cursor, limit)` | `GET /notifications` | 初始化、滚动加载更多 | 获取通知列表 |
| `getUnreadCount()` | `GET /notifications/unread-count` | 页面初始化 | 获取未读数 |
| `markRead(id)` | `PUT /notifications/:id/read` | 点击单条通知时 | 标记单条已读 |
| `markAllRead()` | `PUT /notifications/read-all` | 点击全部已读时 | 标记全部已读 |

### 说明

- 点击通知后若 `redirectUrl` 存在，会进行业务跳转

---

## 13. 消息模块接口映射

## 13.1 `Messages.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `listConversations()` | `GET /messages/conversations` | 页面初始化、每 5 秒轮询 | 获取会话列表 |
| `getConversation(peerId)` | `GET /messages/conversations/:peerId` | 打开会话时 | 获取聊天记录 |
| `markRead(peerId)` | `PUT /messages/conversations/:peerId/read` | 打开会话时 | 标记该会话已读 |
| `sendMessage(peerId, content, imageUrl?)` | `POST /messages` | 点击发送时 | 发送消息 |

### 当前状态说明

- 图片按钮仅预留 UI
- 尚未接图片上传/发送能力
- 聊天消息没有 WebSocket 实时收新，只在切换会话时拉取一次

---

## 14. 后台管理模块接口映射

## 14.1 `AdminDashboard.vue`

### 当前实际接口

无。

### 待接入但已存在接口

| 接口封装 | 后端语义 | 用途 |
|---|---|---|
| `getDashboard()` | `GET /admin/dashboard` | 拉取后台仪表盘真实统计数据 |

---

## 14.2 `AdminUsers.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getAdminUsers(params)` | `GET /admin/users` | 初始化、搜索、加载更多 | 获取用户列表 |
| `banUser(id)` | `PUT /admin/users/:id/ban` | 单个封禁 | 封禁用户 |
| `unbanUser(id)` | `PUT /admin/users/:id/unban` | 单个解禁 | 解禁用户 |
| `changeUserRole(id, role)` | `PUT /admin/users/:id/role` | 修改角色 | 更新用户角色 |
| `batchSetUserStatus(ids, status)` | `PUT /admin/users/batch-status` | 批量封禁/解禁 | 批量更新用户状态 |

---

## 14.3 `AdminPosts.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getAdminPosts(params)` | `GET /admin/posts` | 初始化、搜索、加载更多 | 获取帖子列表 |
| `togglePin(id)` | `PUT /admin/posts/:id/pin` | 点击置顶/取消置顶 | 调整帖子置顶状态 |
| `toggleEssence(id)` | `PUT /admin/posts/:id/essence` | 点击加精/取消精华 | 调整帖子精华状态 |
| `setPostStatus(id, status)` | `PUT /admin/posts/:id/status` | 隐藏/恢复帖子 | 更新帖子状态 |

---

## 14.4 `AdminSpaces.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getAdminSpaces(params)` | `GET /admin/spaces` | 初始化、搜索、加载更多 | 获取空间列表 |
| `setSpaceStatus(id, status)` | `PUT /admin/spaces/:id/status` | 启用/禁用空间 | 更新空间状态 |
| `adminDeleteSpace(id)` | `DELETE /admin/spaces/:id` | 解散空间 | 强制删除空间 |

---

## 14.5 `AdminAuditLog.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getAuditLogs(params)` | `GET /admin/audit-logs` | 初始化、搜索、加载更多 | 获取审计日志 |

---

## 14.6 `AdminReports.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getReports(params)` | `GET /admin/reports` | 初始化、筛选、滚动加载 | 获取举报列表 |
| `handleReport(id, data)` | `PUT /admin/reports/:id/handle` | 单条处理/驳回 | 处理举报 |
| `batchHandleReports(ids, status, note?)` | `PUT /admin/reports/batch-handle` | 批量处理/驳回 | 批量更新举报状态 |

---

## 14.7 `AdminSensitiveWords.vue`

### 接口列表

| 调用方式 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `request({ method: 'GET', url: '/admin/sensitive-words' })` | `GET /admin/sensitive-words` | 页面初始化、增删后刷新 | 获取敏感词列表 |
| `request({ method: 'POST', url: '/admin/sensitive-words' })` | `POST /admin/sensitive-words` | 添加敏感词时 | 新增敏感词 |
| `request({ method: 'DELETE', url: '/admin/sensitive-words/:id' })` | `DELETE /admin/sensitive-words/:id` | 删除时 | 删除敏感词 |

### 说明

- 该页面没有单独的 `api/sensitive-words.ts` 封装，直接使用了通用 `request`

---

## 14.8 `AdminTenants.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getTenants()` | `GET /admin/tenants` | 初始化、增删改后刷新 | 获取租户列表 |
| `createTenant(data)` | `POST /admin/tenants` | 新建租户时 | 创建租户 |
| `updateTenant(id, data)` | `PUT /admin/tenants/:id` | 编辑租户时 | 更新租户信息 |
| `toggleTenantStatus(id)` | `PUT /admin/tenants/:id/status` | 启用/停用时 | 切换租户状态 |

---

## 14.9 `AdminAiConfig.vue`

### 接口列表

| 接口封装 | 后端语义 | 调用时机 | 用途 |
|---|---|---|---|
| `getTenants()` | `GET /admin/tenants` | 页面初始化 | 获取租户下拉列表 |
| `getTenantAiConfig(id)` | `GET /admin/tenants/:id/ai-config` | 选择租户时 | 获取 AI 配置 |
| `updateTenantAiConfig(id, data)` | `PUT /admin/tenants/:id/ai-config` | 点击保存时 | 更新 AI 配置 |

---

## 15. 页面与接口依赖总览摘要

## 15.1 调用接口最多的核心页面

### 第一梯队

- `PostDetail.vue`
- `UserPage.vue`
- `CheckinChallengeDetail.vue`
- `Messages.vue`
- `AdminUsers.vue`

这些页面具有明显的“主业务枢纽”属性。

## 15.2 以列表查询为主的页面

- `Square.vue`
- `Spaces.vue`
- `CheckinChallenges.vue`
- `Resources.vue`
- `Notifications.vue`
- `AdminPosts.vue`
- `AdminSpaces.vue`
- `AdminAuditLog.vue`
- `AdminReports.vue`

## 15.3 以提交动作为主的页面

- `Login.vue`
- `Register.vue`
- `ForgotPassword.vue`
- `PostCreate.vue`
- `SpaceCreate.vue`
- `CheckinChallengeCreate.vue`
- `ResourceUpload.vue`
- `AdminAiConfig.vue`

## 15.4 当前明显存在“页面已设计、接口未真正接入”的页面

- `Home.vue`
- `Profile.vue`
- `SpaceDetail.vue`
- `AiAssistant.vue`
- `AdminDashboard.vue`

---

## 16. 当前接口接入层面的主要问题

## 16.1 页面与 query 参数联动不足

- `Search.vue` 未消费 `/search?q=...`

## 16.2 类型定义与实际用法不完全一致

- `getPosts()` 的页面实际传参中包含 `authorId`、`sort` 等更丰富能力
- 但 API 类型定义未完全覆盖所有用法

## 16.3 页面存在直接 `request()` 调用

- `AdminSensitiveWords.vue`
- 这说明 API 封装层还不完全统一

## 16.4 页面 UI 已经存在，但真实接口没接上

- `Profile.vue`
- `AiAssistant.vue`
- `AdminDashboard.vue`
- `SpaceDetail.vue`

---

## 17. 建议的后续整理方向

建议后续再做两步工程化增强：

1. **补一个 `docs/frontend-api-gap-list.md`**
   - 专门列“已有 API 但页面未接入”的差距表

2. **补一个 `src/api` 统一收口整理**
   - 把页面直调 `request()` 的地方收敛成独立 API 模块
   - 顺便统一参数类型定义

---

## 18. 总结

当前前端接口接入情况呈现出一个比较清晰的结构：

- **账号、帖子、打卡、资源、通知、消息、后台 CRUD** 已经有较完整接口闭环
- **用户个人中心、空间详情、AI 助手、后台仪表盘** 仍有明显的“页面先行、接口后补”特征
- **搜索联动、消息实时性、API 封装统一性** 是现阶段最值得优先补强的三个方向
