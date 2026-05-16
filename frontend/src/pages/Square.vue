<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { NTag, NSpin, NEmpty, NIcon } from 'naive-ui';
import { 
  SearchOutline, 
  ChatbubblesOutline, 
  NotificationsOutline, 
  StarOutline, 
  PlanetOutline,
  AddOutline,
  SettingsOutline,
  FlameOutline,
  TimeOutline,
  BookmarkOutline,
  RibbonOutline,
  EyeOutline,
  HeartOutline
} from '@vicons/ionicons5';
import { getPosts } from '@/api/posts';
import { renderMentions } from '@/utils/mention';
import type { PostVO } from '@/types/post';

const router = useRouter();
const posts = ref<PostVO[]>([]);
const loading = ref(false);
const hasMore = ref(true);
const sort = ref<'latest' | 'trending' | 'essence' | 'follow'>('latest');

const sortOptions = [
  { key: 'latest', label: '最新发布', icon: TimeOutline },
  { key: 'trending', label: '热门讨论', icon: FlameOutline },
  { key: 'essence', label: '精华推荐', icon: RibbonOutline },
  { key: 'follow', label: '我的关注', icon: BookmarkOutline },
] as const;

async function loadPosts(reset = false) {
  if (loading.value) return;
  loading.value = true;
  try {
    const cursor = reset ? undefined : posts.value[posts.value.length - 1]?.id;
    const cursorVal = sort.value === 'trending'
      ? (reset ? undefined : posts.value[posts.value.length - 1]?.likeCount)
      : cursor;
    const list = await getPosts({ scope: 'SQUARE', sort: sort.value, cursor: cursorVal, limit: 10 });
    if (reset) {
      posts.value = list;
    } else {
      posts.value.push(...list);
    }
    hasMore.value = list.length >= 10;
  } catch {
    // 忽略加载失败，保留当前列表状态
  }
  loading.value = false;
}

function switchSort(s: 'latest' | 'trending' | 'essence' | 'follow') {
  sort.value = s;
  posts.value = [];
  loadPosts(true);
}

function goDetail(id: number) {
  router.push(`/posts/${id}`);
}

const isAdmin = computed(() => {
  const role = localStorage.getItem('role');
  return role === 'TENANT_ADMIN' || role === 'SUPER_ADMIN';
});

function goCreate() {
  router.push('/posts/new');
}

function goAdmin() {
  router.push('/admin');
}

function scrollToListEnd(e: Event) {
  const target = e.target as HTMLElement;
  if (target.scrollHeight - target.scrollTop - target.clientHeight < 100) {
    loadPosts();
  }
}

onMounted(() => loadPosts(true));
</script>

<template>
  <div class="square-layout" @scroll="scrollToListEnd">
    <div class="header-banner">
      <div class="banner-content">
        <h1 class="page-title gradient-text">
          <n-icon size="32" class="title-icon"><PlanetOutline /></n-icon>
          广场
        </h1>
        <p class="page-subtitle">探索校园热点，分享你的想法</p>
      </div>
      <div class="header-actions">
        <button class="action-btn" @click="router.push('/search')" title="搜索">
          <n-icon><SearchOutline /></n-icon>
        </button>
        <button class="action-btn" @click="router.push('/messages')" title="私信">
          <n-icon><ChatbubblesOutline /></n-icon>
        </button>
        <button class="action-btn" @click="router.push('/notifications')" title="通知">
          <n-icon><NotificationsOutline /></n-icon>
        </button>
        <button class="action-btn" @click="router.push('/points')" title="积分">
          <n-icon><StarOutline /></n-icon>
        </button>
        <button class="action-btn" @click="router.push('/ai')" title="AI 助手">
          <span class="ai-text">AI</span>
        </button>
        <button v-if="isAdmin" class="action-btn admin-btn" @click="goAdmin" title="管理后台">
          <n-icon><SettingsOutline /></n-icon>
        </button>
        <button class="neon-btn create-btn" @click="goCreate">
          <n-icon><AddOutline /></n-icon> 发布
        </button>
      </div>
    </div>

    <div class="main-container">
      <div class="content-wrapper">
        <div class="sort-bar glass-card">
          <div 
            v-for="opt in sortOptions" 
            :key="opt.key"
            class="sort-item"
            :class="{ active: sort === opt.key }"
            @click="switchSort(opt.key)"
          >
            <n-icon size="18"><component :is="opt.icon" /></n-icon>
            <span>{{ opt.label }}</span>
          </div>
        </div>

        <div v-if="posts.length === 0 && !loading" class="empty-state glass-card">
          <n-empty description="暂无帖子，快去抢沙发吧" />
          <button class="neon-btn mt-4" @click="goCreate">立即发帖</button>
        </div>

        <div class="post-list">
          <div v-for="post in posts" :key="post.id" class="post-card glass-card" @click="goDetail(post.id)">
            <div class="card-header">
              <div class="author-info">
                <div class="avatar" :style="{ background: 'var(--cf-gradient-primary)' }">
                  {{ post.author?.nickname?.charAt(0) || '匿' }}
                </div>
                <div class="author-meta">
                  <span class="author-name">{{ post.author?.nickname || '匿名用户' }}</span>
                  <span class="post-time">{{ new Date(post.createdAt).toLocaleString() }}</span>
                </div>
              </div>
              <n-tag v-if="post.isEssence === 1" type="warning" size="small" round class="essence-tag">
                <template #icon><n-icon><RibbonOutline /></n-icon></template>精华
              </n-tag>
            </div>
            
            <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
            <p class="post-preview" v-html="renderMentions(post.content.slice(0, 200)) + (post.content.length > 200 ? '...' : '')" />
            
            <div class="card-footer">
              <div class="stats">
                <span class="stat-item"><n-icon><EyeOutline /></n-icon> {{ post.viewCount }}</span>
                <span class="stat-item"><n-icon><HeartOutline /></n-icon> {{ post.likeCount }}</span>
                <span class="stat-item"><n-icon><ChatbubblesOutline /></n-icon> {{ post.commentCount }}</span>
              </div>
              <div class="topics" v-if="post.topics && post.topics.length">
                <span v-for="t in post.topics" :key="t" class="topic-tag"># {{ t }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-state">
          <n-spin size="large" />
        </div>
        <div v-if="!hasMore && posts.length > 0" class="end-state">
          <div class="divider"></div>
          <span>— 没有更多了 —</span>
          <div class="divider"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.square-layout {
  height: 100vh;
  overflow-y: auto;
  background: var(--cf-bg-base);
  background-image: radial-gradient(circle at 50% 0%, rgba(99, 102, 241, 0.08), transparent 50%);
}

.header-banner {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 24px 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;

  .banner-content {
    .page-title {
      font-size: 36px;
      font-weight: 800;
      margin: 0 0 8px;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .page-subtitle {
      color: var(--cf-text-secondary);
      font-size: 16px;
      margin: 0;
    }
  }

  .header-actions {
    display: flex;
    gap: 12px;
    align-items: center;

    .action-btn {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--cf-border);
      color: var(--cf-text-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      font-size: 18px;
      transition: all 0.3s;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        transform: translateY(-2px);
        color: var(--cf-primary);
        border-color: var(--cf-primary);
      }
      
      .ai-text {
        font-size: 14px;
        font-weight: bold;
        background: var(--cf-gradient-primary);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
    }
    
    .admin-btn {
      color: var(--cf-warning);
      &:hover {
        color: var(--cf-warning);
        border-color: var(--cf-warning);
      }
    }

    .create-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      height: 40px;
      padding: 0 20px;
      border-radius: 20px;
    }
  }
}

.main-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 24px 40px;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sort-bar {
  display: flex;
  padding: 6px;
  gap: 4px;
  background: rgba(22, 27, 34, 0.5);
  border-radius: 16px;

  .sort-item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px;
    border-radius: 12px;
    color: var(--cf-text-secondary);
    cursor: pointer;
    font-weight: 500;
    transition: all 0.3s;

    &:hover {
      background: rgba(255, 255, 255, 0.05);
      color: var(--cf-text-primary);
    }

    &.active {
      background: rgba(99, 102, 241, 0.15);
      color: var(--cf-primary);
    }
  }
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  padding: 24px;
  cursor: pointer;
  
  &:hover {
    transform: translateY(-2px);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16px;

    .author-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-weight: bold;
        font-size: 16px;
      }

      .author-meta {
        display: flex;
        flex-direction: column;
        
        .author-name {
          font-weight: 600;
          color: var(--cf-text-primary);
          font-size: 15px;
        }
        .post-time {
          color: var(--cf-text-secondary);
          font-size: 12px;
          margin-top: 2px;
        }
      }
    }
    
    .essence-tag {
      font-weight: bold;
      background: rgba(210, 153, 34, 0.1);
      border: 1px solid rgba(210, 153, 34, 0.3);
    }
  }

  .post-title {
    margin: 0 0 12px;
    font-size: 18px;
    font-weight: 600;
    color: var(--cf-text-primary);
    line-height: 1.4;
  }

  .post-preview {
    color: var(--cf-text-secondary);
    font-size: 15px;
    line-height: 1.6;
    margin: 0 0 20px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;

    :deep(.mention-link) {
      color: var(--cf-primary);
      text-decoration: none;
      background: rgba(99, 102, 241, 0.1);
      padding: 0 4px;
      border-radius: 4px;
      &:hover { text-decoration: underline; }
    }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid var(--cf-border);
    padding-top: 16px;

    .stats {
      display: flex;
      gap: 20px;
      
      .stat-item {
        display: flex;
        align-items: center;
        gap: 6px;
        color: var(--cf-text-secondary);
        font-size: 13px;
        transition: color 0.3s;
        
        &:hover { color: var(--cf-primary); }
      }
    }

    .topics {
      display: flex;
      gap: 8px;
      
      .topic-tag {
        font-size: 12px;
        color: var(--cf-accent);
        background: rgba(139, 92, 246, 0.1);
        padding: 4px 10px;
        border-radius: 12px;
        transition: all 0.3s;
        
        &:hover {
          background: rgba(139, 92, 246, 0.2);
        }
      }
    }
  }
}

.empty-state {
  padding: 60px 0;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  
  .mt-4 { margin-top: 24px; }
}

.loading-state {
  padding: 40px;
  display: flex;
  justify-content: center;
}

.end-state {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 0;
  color: var(--cf-text-secondary);
  font-size: 14px;
  
  .divider {
    flex: 1;
    height: 1px;
    background: var(--cf-border);
  }
}
</style>
