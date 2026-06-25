-- AI workspace production storage and knowledge-base RAG metadata.

CREATE TABLE IF NOT EXISTS ai_agents (
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

CREATE TABLE IF NOT EXISTS ai_plugins (
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

CREATE TABLE IF NOT EXISTS ai_knowledge_bases (
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

CREATE TABLE IF NOT EXISTS ai_knowledge_documents (
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

CREATE TABLE IF NOT EXISTS ai_knowledge_chunks (
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

CREATE TABLE IF NOT EXISTS ai_ingest_tasks (
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

CREATE TABLE IF NOT EXISTS ai_conversations (
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

CREATE TABLE IF NOT EXISTS ai_conversation_messages (
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

CREATE TABLE IF NOT EXISTS ai_agent_favorites (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  agent_id VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ai_agent_favorite (tenant_id, user_id, agent_id)
) ENGINE=InnoDB COMMENT='AI 智能体收藏';

CREATE TABLE IF NOT EXISTS ai_plugin_installs (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  plugin_id VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ai_plugin_install (tenant_id, user_id, plugin_id)
) ENGINE=InnoDB COMMENT='AI 插件安装';

CREATE TABLE IF NOT EXISTS ai_kb_favorites (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  knowledge_base_id VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ai_kb_favorite (tenant_id, user_id, knowledge_base_id)
) ENGINE=InnoDB COMMENT='AI 知识库收藏';

CREATE TABLE IF NOT EXISTS ai_kb_qa_pairs (
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
