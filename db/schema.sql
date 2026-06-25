-- ============================================================
-- CampusForum Database Schema
-- 版本: v1.0
-- 字符集: utf8mb4 + utf8mb4_0900_ai_ci
-- ============================================================

CREATE DATABASE IF NOT EXISTS campus_forum
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE campus_forum;

-- ============================================================
-- 1. tenants 租户/学校
-- ============================================================
CREATE TABLE tenants (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code         VARCHAR(32)  NOT NULL COMMENT '租户编码（用于子域名）',
  name         VARCHAR(128) NOT NULL COMMENT '学校全称',
  logo_url     VARCHAR(255) DEFAULT NULL,
  domain       VARCHAR(128) DEFAULT NULL,
  status       TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  ai_config    JSON DEFAULT NULL COMMENT 'AI 配置',
  announcement VARCHAR(500) DEFAULT NULL COMMENT '租户公告',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB COMMENT='租户/学校';

-- ============================================================
-- 2. users 用户
-- ============================================================
CREATE TABLE users (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       BIGINT UNSIGNED NOT NULL,
  student_no      VARCHAR(32)  DEFAULT NULL COMMENT '学号',
  email           VARCHAR(128) NOT NULL,
  password_hash   VARCHAR(128) NOT NULL,
  nickname        VARCHAR(64)  NOT NULL,
  avatar_url      VARCHAR(255) DEFAULT NULL,
  wechat_openid   VARCHAR(64)  DEFAULT NULL COMMENT '微信小程序 openid',
  wechat_unionid  VARCHAR(64)  DEFAULT NULL COMMENT '微信开放平台 unionid',
  qq_openid       VARCHAR(64)  DEFAULT NULL COMMENT 'QQ openid',
  github_id       VARCHAR(64)  DEFAULT NULL COMMENT 'GitHub 用户 id',
  profile_cover_url VARCHAR(255) DEFAULT NULL COMMENT '个人主页封面图',
  bio             VARCHAR(255) DEFAULT NULL,
  college         VARCHAR(64)  DEFAULT NULL COMMENT '学院',
  major           VARCHAR(64)  DEFAULT NULL COMMENT '专业',
  grade           VARCHAR(8)   DEFAULT NULL COMMENT '年级',
  role            VARCHAR(32)  NOT NULL DEFAULT 'USER' COMMENT 'USER/TENANT_ADMIN/SUPER_ADMIN',
  status          TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0封禁',
  last_login_at   DATETIME DEFAULT NULL,
  reset_token     VARCHAR(64)  DEFAULT NULL COMMENT '密码重置令牌 SHA-256 哈希（hex）',
  reset_token_expires DATETIME DEFAULT NULL COMMENT '密码重置令牌过期时间',
  mute_settings   JSON DEFAULT NULL COMMENT '消息免打扰设置',
  tag_subscriptions JSON DEFAULT NULL COMMENT '问答标签订阅',
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_email (tenant_id, email),
  UNIQUE KEY uk_tenant_student (tenant_id, student_no),
  UNIQUE KEY uk_tenant_wechat_openid (tenant_id, wechat_openid),
  UNIQUE KEY uk_tenant_qq_openid (tenant_id, qq_openid),
  UNIQUE KEY uk_tenant_github_id (tenant_id, github_id),
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB COMMENT='用户';

-- ============================================================
-- 3. spaces 学习空间
-- ============================================================
CREATE TABLE spaces (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  owner_id      BIGINT UNSIGNED NOT NULL,
  name          VARCHAR(64)  NOT NULL,
  description   VARCHAR(255) DEFAULT NULL,
  category      VARCHAR(16)  NOT NULL COMMENT 'MAJOR/CLASS/CLUB/INTEREST',
  visibility    VARCHAR(16)  NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/REVIEW/INVITE',
  cover_url     VARCHAR(255) DEFAULT NULL,
  sensitive_words TEXT DEFAULT NULL COMMENT '空间自定义敏感词',
  post_notice   VARCHAR(500) DEFAULT NULL COMMENT '发帖须知',
  member_count  INT NOT NULL DEFAULT 0,
  post_count    INT NOT NULL DEFAULT 0,
  status        TINYINT NOT NULL DEFAULT 1,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT NOT NULL DEFAULT 0,
  KEY idx_tenant_category (tenant_id, category),
  KEY idx_owner (owner_id)
) ENGINE=InnoDB COMMENT='学习空间';

-- ============================================================
-- 4. space_members 空间成员
-- ============================================================
CREATE TABLE space_members (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id   BIGINT UNSIGNED NOT NULL,
  space_id    BIGINT UNSIGNED NOT NULL,
  user_id     BIGINT UNSIGNED NOT NULL,
  role        VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT 'OWNER/ADMIN/MEMBER',
  status      TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1已加入 2已退出 3已拒绝',
  joined_at   DATETIME DEFAULT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_space_user (space_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB COMMENT='空间成员';

-- ============================================================
-- 5. posts 帖子（统一表）
-- ============================================================
CREATE TABLE posts (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  author_id     BIGINT UNSIGNED NOT NULL,
  scope         VARCHAR(8)  NOT NULL COMMENT 'SQUARE/SPACE',
  space_id      BIGINT UNSIGNED DEFAULT NULL,
  type          VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/QA/CHECKIN/RESOURCE',
  title         VARCHAR(255) DEFAULT NULL,
  content       MEDIUMTEXT NOT NULL,
  attachments   JSON DEFAULT NULL,
  topics        JSON DEFAULT NULL COMMENT '话题',
  tags          JSON DEFAULT NULL,
  ai_summary    TEXT DEFAULT NULL COMMENT 'AI 生成摘要',
  ai_risk_level TINYINT DEFAULT 0 COMMENT '0正常 1中风险 2高风险',
  view_count    INT NOT NULL DEFAULT 0,
  like_count    INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  is_pinned     TINYINT NOT NULL DEFAULT 0,
  is_essence    TINYINT NOT NULL DEFAULT 0,
  status        TINYINT NOT NULL DEFAULT 1 COMMENT '0待审 1正常 2隐藏',
  pinned_at     DATETIME DEFAULT NULL COMMENT '置顶时间',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT NOT NULL DEFAULT 0,
  KEY idx_tenant_scope_time (tenant_id, scope, created_at),
  KEY idx_space_time (space_id, created_at),
  KEY idx_author (author_id),
  FULLTEXT KEY ft_title_content (title, content) /*!50700 WITH PARSER ngram */
) ENGINE=InnoDB COMMENT='帖子';

-- ============================================================
-- 6. comments 评论
-- ============================================================
CREATE TABLE comments (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id   BIGINT UNSIGNED NOT NULL,
  post_id     BIGINT UNSIGNED NOT NULL,
  parent_id   BIGINT UNSIGNED DEFAULT NULL,
  reply_to_id BIGINT UNSIGNED DEFAULT NULL,
  author_id   BIGINT UNSIGNED NOT NULL,
  content     TEXT NOT NULL,
  like_count  INT NOT NULL DEFAULT 0,
  status      TINYINT NOT NULL DEFAULT 1,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME DEFAULT NULL COMMENT '最后编辑时间',
  deleted     TINYINT NOT NULL DEFAULT 0,
  KEY idx_post (post_id, created_at),
  KEY idx_author (author_id)
) ENGINE=InnoDB COMMENT='评论';

-- ============================================================
-- 7. reactions 点赞/收藏
-- ============================================================
CREATE TABLE reactions (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id   BIGINT UNSIGNED NOT NULL,
  user_id     BIGINT UNSIGNED NOT NULL,
  target_type VARCHAR(16) NOT NULL COMMENT 'POST/COMMENT/RESOURCE',
  target_id   BIGINT UNSIGNED NOT NULL,
  type        VARCHAR(16) NOT NULL COMMENT 'LIKE/COLLECT',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_target (user_id, target_type, target_id, type),
  KEY idx_target (target_type, target_id)
) ENGINE=InnoDB COMMENT='点赞收藏';

-- ============================================================
-- 8. qa_questions 问答扩展
-- ============================================================
CREATE TABLE qa_questions (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id       BIGINT UNSIGNED NOT NULL,
  post_id         BIGINT UNSIGNED NOT NULL,
  is_solved       TINYINT NOT NULL DEFAULT 0,
  accepted_comment_id BIGINT UNSIGNED DEFAULT NULL,
  solved_at       DATETIME DEFAULT NULL,
  UNIQUE KEY uk_post (post_id)
) ENGINE=InnoDB COMMENT='问答扩展';

-- ============================================================
-- 9. checkin_challenges 打卡挑战
-- ============================================================
CREATE TABLE checkin_challenges (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  space_id      BIGINT UNSIGNED DEFAULT NULL,
  creator_id    BIGINT UNSIGNED NOT NULL,
  name          VARCHAR(64) NOT NULL,
  description   VARCHAR(500) DEFAULT NULL,
  start_date    DATE NOT NULL,
  end_date      DATE NOT NULL,
  rule          JSON DEFAULT NULL,
  member_count  INT NOT NULL DEFAULT 0,
  status        TINYINT NOT NULL DEFAULT 1,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='打卡挑战';

-- ============================================================
-- 10. checkin_records 打卡记录
-- ============================================================
CREATE TABLE checkin_records (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  challenge_id  BIGINT UNSIGNED NOT NULL,
  user_id       BIGINT UNSIGNED NOT NULL,
  checkin_date  DATE NOT NULL,
  content       TEXT DEFAULT NULL,
  image_urls    JSON DEFAULT NULL,
  ai_check      TINYINT DEFAULT 0 COMMENT 'AI 内容合规校验',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (challenge_id, user_id, checkin_date),
  KEY idx_user (user_id, checkin_date)
) ENGINE=InnoDB COMMENT='打卡记录';

-- ============================================================
-- 11. resources 资源
-- ============================================================
CREATE TABLE resources (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  uploader_id   BIGINT UNSIGNED NOT NULL,
  space_id      BIGINT UNSIGNED DEFAULT NULL,
  file_name     VARCHAR(255) NOT NULL,
  file_size     BIGINT UNSIGNED NOT NULL,
  file_type     VARCHAR(32)  NOT NULL,
  file_md5      VARCHAR(64)  DEFAULT NULL COMMENT '@Deprecated - 保留至历史数据 100% 迁移到 file_sha256（spec T8.10）',
  file_sha256   VARCHAR(64)  DEFAULT NULL COMMENT 'SHA-256 hex 指纹',
  storage_key   VARCHAR(255) NOT NULL,
  visibility    VARCHAR(16)  NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/SPACE/PRIVATE',
  college       VARCHAR(64)  DEFAULT NULL,
  major         VARCHAR(64)  DEFAULT NULL,
  course        VARCHAR(128) DEFAULT NULL,
  semester      VARCHAR(16)  DEFAULT NULL,
  tags          JSON DEFAULT NULL,
  download_count INT NOT NULL DEFAULT 0,
  collect_count  INT NOT NULL DEFAULT 0,
  version       VARCHAR(32)  DEFAULT NULL,
  description   TEXT DEFAULT NULL,
  status        TINYINT NOT NULL DEFAULT 1,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT NOT NULL DEFAULT 0,
  KEY idx_tenant (tenant_id),
  KEY idx_uploader (uploader_id),
  KEY idx_space (space_id),
  KEY idx_md5 (file_md5),
  KEY idx_resources_file_sha256 (file_sha256)
) ENGINE=InnoDB COMMENT='资源';

-- ============================================================
-- 12. notifications 通知
-- ============================================================
CREATE TABLE notifications (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id    BIGINT UNSIGNED NOT NULL,
  receiver_id  BIGINT UNSIGNED NOT NULL,
  sender_id    BIGINT UNSIGNED DEFAULT NULL,
  type         VARCHAR(32) NOT NULL COMMENT 'COMMENT/LIKE/REPLY/MENTION/ACCEPT/JOIN/SYSTEM',
  title        VARCHAR(128) NOT NULL,
  content      TEXT DEFAULT NULL,
  redirect_url VARCHAR(255) DEFAULT NULL,
  is_read      TINYINT NOT NULL DEFAULT 0,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_receiver_read_time (receiver_id, is_read, created_at)
) ENGINE=InnoDB COMMENT='通知';

-- ============================================================
-- 13. messages 私信
-- ============================================================
CREATE TABLE messages (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id   BIGINT UNSIGNED NOT NULL,
  sender_id   BIGINT UNSIGNED NOT NULL,
  receiver_id BIGINT UNSIGNED NOT NULL,
  content     TEXT DEFAULT NULL,
  image_url   VARCHAR(255) DEFAULT NULL,
  ai_risk_level TINYINT NOT NULL DEFAULT 0 COMMENT '0=安全 1=疑似 2=违规（来自 SensitiveWordService.getRiskLevel，spec T8.10）',
  is_read     TINYINT NOT NULL DEFAULT 0,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_conversation (sender_id, receiver_id, created_at),
  KEY idx_receiver_read (receiver_id, is_read, created_at)
) ENGINE=InnoDB COMMENT='私信';

-- ============================================================
-- 14. audit_logs 审计日志
-- ============================================================
CREATE TABLE audit_logs (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id    BIGINT UNSIGNED NOT NULL,
  operator_id  BIGINT UNSIGNED DEFAULT NULL,
  action       VARCHAR(64)  NOT NULL COMMENT '操作类型',
  target_type  VARCHAR(32)  DEFAULT NULL,
  target_id    BIGINT UNSIGNED DEFAULT NULL,
  detail       JSON DEFAULT NULL COMMENT '操作详情',
  ip_address   VARCHAR(64)  DEFAULT NULL,
  user_agent   VARCHAR(255) DEFAULT NULL COMMENT '客户端 UA（含异步线程上下文，T9.2）',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tenant_time (tenant_id, created_at),
  KEY idx_operator (operator_id),
  KEY idx_audit_log_action_created (action, created_at)
) ENGINE=InnoDB COMMENT='审计日志';

-- ============================================================
-- 15. reports 举报
-- ============================================================
CREATE TABLE reports (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id    BIGINT UNSIGNED NOT NULL,
  reporter_id  BIGINT UNSIGNED NOT NULL,
  target_type  VARCHAR(16) NOT NULL COMMENT 'POST/COMMENT/RESOURCE/USER',
  target_id    BIGINT UNSIGNED NOT NULL,
  reason       VARCHAR(32)  NOT NULL,
  description  TEXT DEFAULT NULL,
  status       TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2已驳回',
  handler_id   BIGINT UNSIGNED DEFAULT NULL,
  handle_note  TEXT DEFAULT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handled_at   DATETIME DEFAULT NULL,
  KEY idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB COMMENT='举报';

-- ============================================================
-- 16. sensitive_words 敏感词
-- ============================================================
CREATE TABLE sensitive_words (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id   BIGINT UNSIGNED NOT NULL,
  word        VARCHAR(64) NOT NULL,
  level       TINYINT NOT NULL DEFAULT 1 COMMENT '1低 2中 3高',
  is_regex    TINYINT NOT NULL DEFAULT 0 COMMENT '0=普通词 1=正则表达式（漏洞 27 / T8.5）',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tenant_word (tenant_id, word)
) ENGINE=InnoDB COMMENT='敏感词';

-- ============================================================
-- 17. achievements 成就
-- ============================================================
CREATE TABLE achievements (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code        VARCHAR(32)  NOT NULL COMMENT '成就编码',
  name        VARCHAR(64)  NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  icon_url    VARCHAR(255) DEFAULT NULL,
  rule        JSON DEFAULT NULL COMMENT '触发规则',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB COMMENT='成就定义';

CREATE TABLE user_achievements (
  id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id      BIGINT UNSIGNED NOT NULL,
  user_id        BIGINT UNSIGNED NOT NULL,
  achievement_id BIGINT UNSIGNED NOT NULL,
  awarded_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_achieve (user_id, achievement_id)
) ENGINE=InnoDB COMMENT='用户成就';


-- ============================================================
-- 19. follows 用户关注
-- ============================================================
CREATE TABLE follows (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id   BIGINT UNSIGNED NOT NULL,
  follower_id BIGINT UNSIGNED NOT NULL COMMENT '关注者',
  followee_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_follow (follower_id, followee_id),
  KEY idx_followee (followee_id)
) ENGINE=InnoDB COMMENT='用户关注';

-- ============================================================
-- 20. AI 工作台与知识库 RAG
-- ============================================================
CREATE TABLE ai_agents (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  owner_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500) DEFAULT NULL,
  category VARCHAR(64) NOT NULL DEFAULT 'General',
  model VARCHAR(128) DEFAULT NULL,
  prompt TEXT DEFAULT NULL,
  abilities JSON DEFAULT NULL,
  knowledge_base_ids JSON DEFAULT NULL,
  plugin_ids JSON DEFAULT NULL,
  tags JSON DEFAULT NULL,
  avatar VARCHAR(255) DEFAULT NULL,
  color VARCHAR(32) DEFAULT NULL,
  user_count BIGINT NOT NULL DEFAULT 0,
  rating DECIMAL(3,2) NOT NULL DEFAULT 5.00,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_agents_tenant_owner (tenant_id, owner_id),
  KEY idx_ai_agents_category (tenant_id, category)
) ENGINE=InnoDB COMMENT='AI 工作台智能体';

CREATE TABLE ai_plugins (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  owner_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500) DEFAULT NULL,
  category VARCHAR(64) NOT NULL DEFAULT 'Productivity',
  icon VARCHAR(255) DEFAULT NULL,
  color VARCHAR(32) DEFAULT NULL,
  usage_count BIGINT NOT NULL DEFAULT 0,
  install_count BIGINT NOT NULL DEFAULT 0,
  rating DECIMAL(3,2) NOT NULL DEFAULT 5.00,
  is_official TINYINT NOT NULL DEFAULT 0,
  is_featured TINYINT NOT NULL DEFAULT 0,
  permissions JSON DEFAULT NULL,
  input_schema JSON DEFAULT NULL,
  output_schema JSON DEFAULT NULL,
  endpoint VARCHAR(500) DEFAULT NULL,
  review_status VARCHAR(32) NOT NULL DEFAULT 'pending',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_plugins_tenant_owner (tenant_id, owner_id),
  KEY idx_ai_plugins_review (tenant_id, review_status)
) ENGINE=InnoDB COMMENT='AI 工作台插件';

CREATE TABLE ai_knowledge_bases (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  owner_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500) DEFAULT NULL,
  category VARCHAR(64) NOT NULL DEFAULT 'General',
  type VARCHAR(64) NOT NULL DEFAULT 'General',
  visibility VARCHAR(16) NOT NULL DEFAULT 'private',
  document_count BIGINT NOT NULL DEFAULT 0,
  vector_count BIGINT NOT NULL DEFAULT 0,
  storage_bytes BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_kb_tenant_owner (tenant_id, owner_id),
  KEY idx_ai_kb_visibility (tenant_id, visibility)
) ENGINE=InnoDB COMMENT='AI 工作台知识库';

CREATE TABLE ai_knowledge_documents (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  knowledge_base_id VARCHAR(64) NOT NULL,
  owner_id BIGINT UNSIGNED NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_size BIGINT UNSIGNED NOT NULL DEFAULT 0,
  file_type VARCHAR(32) DEFAULT NULL,
  storage_key VARCHAR(255) DEFAULT NULL,
  tags JSON DEFAULT NULL,
  parse_mode VARCHAR(32) NOT NULL DEFAULT 'auto',
  status VARCHAR(32) NOT NULL DEFAULT 'processing',
  chunk_count BIGINT NOT NULL DEFAULT 0,
  storage_bytes BIGINT NOT NULL DEFAULT 0,
  indexed_at DATETIME DEFAULT NULL,
  error_message VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_docs_kb (tenant_id, knowledge_base_id),
  KEY idx_ai_docs_status (tenant_id, status)
) ENGINE=InnoDB COMMENT='AI 知识库文档';

CREATE TABLE ai_knowledge_chunks (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  knowledge_base_id VARCHAR(64) NOT NULL,
  document_id VARCHAR(64) NOT NULL,
  owner_id BIGINT UNSIGNED NOT NULL,
  chunk_index INT NOT NULL,
  title VARCHAR(255) DEFAULT NULL,
  content MEDIUMTEXT NOT NULL,
  content_hash CHAR(64) DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ready',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ai_chunk_doc_index (document_id, chunk_index),
  KEY idx_ai_chunks_kb (tenant_id, knowledge_base_id),
  KEY idx_ai_chunks_doc (tenant_id, document_id)
) ENGINE=InnoDB COMMENT='AI 知识库文本分块';

CREATE TABLE ai_ingest_tasks (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  knowledge_base_id VARCHAR(64) NOT NULL,
  owner_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'processing',
  progress INT NOT NULL DEFAULT 0,
  uploaded INT NOT NULL DEFAULT 0,
  failed INT NOT NULL DEFAULT 0,
  document_ids JSON DEFAULT NULL,
  message VARCHAR(500) DEFAULT NULL,
  error_message VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_tasks_kb (tenant_id, knowledge_base_id),
  KEY idx_ai_tasks_owner (tenant_id, owner_id)
) ENGINE=InnoDB COMMENT='AI 知识库导入任务';

CREATE TABLE ai_conversations (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  owner_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(255) NOT NULL,
  agent_id VARCHAR(64) DEFAULT NULL,
  plugin_ids JSON DEFAULT NULL,
  knowledge_base_ids JSON DEFAULT NULL,
  model VARCHAR(128) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_conv_owner_time (tenant_id, owner_id, updated_at)
) ENGINE=InnoDB COMMENT='AI 工作台对话';

CREATE TABLE ai_conversation_messages (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  conversation_id VARCHAR(64) NOT NULL,
  owner_id BIGINT UNSIGNED NOT NULL,
  role VARCHAR(16) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  model VARCHAR(128) DEFAULT NULL,
  agent_id VARCHAR(64) DEFAULT NULL,
  plugin_ids JSON DEFAULT NULL,
  knowledge_base_ids JSON DEFAULT NULL,
  citations JSON DEFAULT NULL,
  feedback_helpful TINYINT DEFAULT NULL,
  feedback_comment VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_msg_conv_time (tenant_id, conversation_id, created_at),
  KEY idx_ai_msg_owner (tenant_id, owner_id)
) ENGINE=InnoDB COMMENT='AI 工作台对话消息';

CREATE TABLE ai_agent_favorites (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  agent_id VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ai_agent_favorite (tenant_id, user_id, agent_id)
) ENGINE=InnoDB COMMENT='AI 智能体收藏';

CREATE TABLE ai_plugin_installs (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  plugin_id VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ai_plugin_install (tenant_id, user_id, plugin_id)
) ENGINE=InnoDB COMMENT='AI 插件安装';

CREATE TABLE ai_kb_favorites (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  knowledge_base_id VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ai_kb_favorite (tenant_id, user_id, knowledge_base_id)
) ENGINE=InnoDB COMMENT='AI 知识库收藏';

CREATE TABLE ai_kb_qa_pairs (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  knowledge_base_id VARCHAR(64) NOT NULL,
  owner_id BIGINT UNSIGNED NOT NULL,
  question TEXT NOT NULL,
  answer MEDIUMTEXT NOT NULL,
  tags JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_kb_qa (tenant_id, knowledge_base_id)
) ENGINE=InnoDB COMMENT='AI 知识库 QA 对';

-- ============================================================
-- 初始数据：默认租户（standalone 模式必需）
-- ============================================================
INSERT INTO tenants (id, code, name, status, created_at, updated_at)
VALUES (1, 'default', '默认租户', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 1;
