<script setup lang="ts">
import { ref } from 'vue';
import { useMessage } from 'naive-ui';
import { 
  DocumentTextOutline,
  ShieldCheckmarkOutline,
  AlertCircleOutline,
  ChatbubbleEllipsesOutline,
  CopyOutline,
  RefreshOutline,
  LinkOutline
} from '@vicons/ionicons5';
import aiRobotImg from '@/assets/images/ai_robot.png';
import { getPostById } from '@/api/posts';
import type { PostVO } from '@/types/post';

const message = useMessage();

const menus = ref([
  { id: 'summary', label: '智能摘要', icon: DocumentTextOutline },
  { id: 'content', label: '内容检测', icon: ShieldCheckmarkOutline },
  { id: 'sensitive', label: '敏感词汇检测', icon: AlertCircleOutline },
  { id: 'qa', label: 'AI 问答', icon: ChatbubbleEllipsesOutline },
]);

const activeMenu = ref('summary');

const inputLink = ref('');
const summaryResult = ref('');
const summaryLoading = ref(false);
const targetPost = ref<PostVO | null>(null);

async function generateSummary() {
  const link = inputLink.value.trim();
  if (!link) {
    message.warning('请输入帖子链接或ID');
    return;
  }
  
  let postId: number | null = null;
  
  // parse ID from link (e.g. http://localhost:3000/posts/12)
  const match = link.match(/\/posts\/(\d+)/);
  if (match && match[1]) {
    postId = parseInt(match[1]);
  } else if (/^\d+$/.test(link)) {
    postId = parseInt(link);
  } else {
    message.error('无法识别的帖子链接，请确保链接中包含 /posts/{id}');
    return;
  }

  summaryLoading.value = true;
  summaryResult.value = '';
  targetPost.value = null;

  try {
    const post = await getPostById(postId);
    targetPost.value = post;
    
    // mock AI summary generation
    setTimeout(() => {
      summaryResult.value = `本文主要讨论了关于“${post.title || '无标题'}”的相关话题。作者分享了相关的经验和见解，引发了社区讨论。核心观点包括：\n1. 内容的主要价值体现和个人见解。\n2. 相关的技术细节或生活思考。\n3. 对未来的展望或给读者的建议。\n（此摘要为 AI 自动阅读原帖后生成）`;
      summaryLoading.value = false;
      message.success('摘要生成成功');
    }, 1500);

  } catch (err) {
    message.error('获取帖子失败，请检查链接是否正确或您是否有权限访问');
    summaryLoading.value = false;
  }
}

function copySummary() {
  if (summaryResult.value) {
    navigator.clipboard.writeText(summaryResult.value);
    message.success('已复制到剪贴板');
  }
}
</script>

<template>
  <div class="ai-layout">
    <header class="top-header">
      <h2>AI 助手</h2>
    </header>

    <div class="main-container">
      <aside class="sidebar glass-card">
        <div 
          v-for="m in menus" 
          :key="m.id" 
          class="menu-item" 
          :class="{ active: activeMenu === m.id }"
          @click="activeMenu = m.id"
        >
          <n-icon size="18" style="margin-right: 8px;"><component :is="m.icon" /></n-icon>
          {{ m.label }}
        </div>
      </aside>

      <main class="content-area">
        <div v-if="activeMenu === 'summary'">
          <div class="content-header">
            <h3>智能摘要</h3>
            <p>输入系统内的帖子链接，AI 将自动读取并提炼核心内容，免去长文阅读烦恼</p>
          </div>

          <div class="interaction-area">
            <div class="input-section glass-card">
              <div class="link-input-wrapper">
                <n-icon size="20" class="link-icon"><LinkOutline /></n-icon>
                <input 
                  v-model="inputLink" 
                  type="text" 
                  placeholder="在此粘贴帖子链接 (如 http://.../posts/123) 或输入帖子 ID"
                  @keyup.enter="generateSummary"
                />
              </div>
              <div class="input-footer">
                <p class="hint-text">支持粘贴完整链接或直接输入帖子 ID</p>
                <button class="neon-btn" :disabled="summaryLoading" @click="generateSummary">
                  {{ summaryLoading ? '分析中...' : '生成摘要' }}
                </button>
              </div>
            </div>
            <div class="robot-illustration">
              <img :src="aiRobotImg" alt="AI Robot" class="floating-robot" />
            </div>
          </div>

          <div class="result-section" v-if="summaryResult || summaryLoading">
            <h4>分析结果</h4>
            <div class="example-card glass-card">
              <div class="ex-content" v-if="summaryLoading">
                <div class="loading-state">
                  <div class="ai-pulse"></div>
                  <p>AI 正在阅读帖子内容并提取关键信息...</p>
                </div>
              </div>
              <div class="ex-content" v-else>
                <h5>原文标题：{{ targetPost?.title || '无标题' }}</h5>
                <div class="summary-box">
                  <span class="label">摘要：</span>
                  <p>{{ summaryResult }}</p>
                </div>
                <div class="tags" v-if="targetPost?.topics && targetPost.topics.length > 0">
                  <span class="tag" v-for="t in targetPost.topics" :key="t">{{ t }}</span>
                </div>
              </div>
              <div class="ex-actions" v-if="!summaryLoading">
                <span class="action" @click="copySummary"><n-icon><CopyOutline/></n-icon> 复制</span>
                <span class="action" @click="generateSummary"><n-icon><RefreshOutline/></n-icon> 重新生成</span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="coming-soon">
          <n-icon size="64" color="#30363d"><component :is="menus.find(m => m.id === activeMenu)?.icon" /></n-icon>
          <h3>{{ menus.find(m => m.id === activeMenu)?.label }}</h3>
          <p>工程师们正在日夜兼程为您开发该功能，敬请期待...</p>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.ai-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--cf-bg-base);
  color: var(--cf-text-primary);
  overflow: hidden;
  background-image: radial-gradient(circle at 80% 20%, rgba(139, 92, 246, 0.08), transparent 40%);
}

.top-header {
  height: 60px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--cf-border);
  background: rgba(13, 17, 23, 0.5);
  backdrop-filter: blur(10px);
  h2 { margin: 0; font-size: 18px; font-weight: 600; }
}

.main-container {
  flex: 1;
  display: flex;
  padding: 32px;
  gap: 32px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.sidebar {
  width: 220px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: fit-content;

  .menu-item {
    display: flex;
    align-items: center;
    padding: 14px 16px;
    border-radius: 12px;
    cursor: pointer;
    color: var(--cf-text-secondary);
    font-size: 15px;
    transition: all 0.2s;
    font-weight: 500;

    &:hover { background: rgba(255,255,255,0.05); color: white; }
    &.active {
      background: rgba(99,102,241,0.15);
      color: var(--cf-primary);
      border-left: 3px solid var(--cf-primary);
      border-radius: 4px 12px 12px 4px;
    }
  }
}

.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 32px;
  overflow-y: auto;
  padding-right: 16px;

  /* hide scrollbar for cleaner look */
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 3px;
  }

  .content-header {
    margin-bottom: 24px;
    h3 { margin: 0 0 8px; font-size: 28px; color: white; font-weight: 800; }
    p { margin: 0; color: var(--cf-text-secondary); font-size: 15px; }
  }

  .interaction-area {
    display: flex;
    gap: 32px;
    align-items: stretch;

    .input-section {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 32px;
      justify-content: center;

      .link-input-wrapper {
        display: flex;
        align-items: center;
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid var(--cf-border);
        border-radius: 12px;
        padding: 0 16px;
        transition: all 0.3s;
        
        &:focus-within {
          border-color: var(--cf-primary);
          box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
        }

        .link-icon {
          color: var(--cf-text-secondary);
          margin-right: 12px;
        }

        input {
          flex: 1;
          height: 54px;
          background: transparent;
          border: none;
          color: white;
          font-size: 16px;
          outline: none;
          &::placeholder { color: var(--cf-text-muted); }
        }
      }

      .input-footer {
        display: flex; 
        justify-content: space-between; 
        align-items: center;
        margin-top: 24px;
        
        .hint-text { margin: 0; color: var(--cf-text-muted); font-size: 13px; }
        
        .neon-btn {
          height: 40px;
          padding: 0 24px;
          border-radius: 20px;
          
          &:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            animation: pulse 2s infinite;
          }
        }
      }
    }

    .robot-illustration {
      width: 220px;
      display: flex; align-items: center; justify-content: center;
      .floating-robot {
        width: 100%;
        animation: float 4s ease-in-out infinite;
        filter: drop-shadow(0 0 20px rgba(56,189,248,0.3));
      }
    }
  }

  .result-section {
    margin-top: 16px;
    h4 { margin: 0 0 16px; font-size: 18px; color: white; }
    
    .example-card {
      padding: 24px;
      display: flex; flex-direction: column; gap: 20px;

      .ex-content {
        h5 { margin: 0 0 16px; font-size: 18px; color: white; }
        .summary-box {
          display: flex; gap: 12px;
          background: rgba(99, 102, 241, 0.05);
          padding: 16px;
          border-radius: 12px;
          border-left: 4px solid var(--cf-primary);
          
          .label { color: var(--cf-primary); font-weight: bold; flex-shrink: 0; }
          p { margin: 0; color: var(--cf-text-primary); font-size: 15px; line-height: 1.6; white-space: pre-wrap; }
        }
        .tags {
          display: flex; gap: 12px; margin-top: 20px;
          .tag { font-size: 13px; padding: 4px 12px; border-radius: 16px; border: 1px solid rgba(99,102,241,0.3); color: var(--cf-primary); background: rgba(99,102,241,0.1); }
        }
        
        .loading-state {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 40px 0;
          color: var(--cf-primary);
          
          .ai-pulse {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background: var(--cf-gradient-primary);
            margin-bottom: 16px;
            animation: pulse 1.5s infinite;
          }
        }
      }

      .ex-actions {
        display: flex; justify-content: flex-end; gap: 24px;
        padding-top: 16px;
        border-top: 1px solid var(--cf-border);
        .action { 
          display: flex; align-items: center; gap: 6px; 
          color: var(--cf-text-secondary); font-size: 14px; 
          cursor: pointer; transition: color 0.3s;
          &:hover { color: var(--cf-primary); } 
        }
      }
    }
  }

  .coming-soon {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 400px;
    color: var(--cf-text-secondary);
    
    h3 {
      margin: 24px 0 8px;
      color: white;
      font-size: 24px;
    }
    
    p { margin: 0; font-size: 15px; }
  }
}

@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-15px); }
  100% { transform: translateY(0px); }
}

@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 15px rgba(99, 102, 241, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(99, 102, 241, 0); }
}
</style>
