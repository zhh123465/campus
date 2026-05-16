<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { NInput, NSpin, NEmpty, NIcon, NTag } from 'naive-ui';
import { search } from '@/api/search';
import type { SearchResult } from '@/types/search';
import { 
  SearchOutline, 
  ChatbubblesOutline, 
  PersonOutline, 
  DocumentTextOutline, 
  PlanetOutline,
  GridOutline,
  ArrowBackOutline,
  EyeOutline,
  HeartOutline,
  CloudDownloadOutline
} from '@vicons/ionicons5';

const router = useRouter();
const keyword = ref('');
const searchType = ref('');
const results = ref<SearchResult[]>([]);
const loading = ref(false);
const searched = ref(false);

const types = [
  { key: '', label: '全部', icon: GridOutline },
  { key: 'POST', label: '帖子', icon: ChatbubblesOutline },
  { key: 'USER', label: '用户', icon: PersonOutline },
  { key: 'RESOURCE', label: '资源', icon: DocumentTextOutline },
  { key: 'SPACE', label: '空间', icon: PlanetOutline },
] as const;

async function doSearch() {
  if (!keyword.value.trim()) return;
  loading.value = true;
  searched.value = true;
  try {
    results.value = await search({ keyword: keyword.value.trim(), type: searchType.value || undefined });
  } catch {
    results.value = [];
  }
  loading.value = false;
}

function switchType(t: string) {
  searchType.value = t;
  if (searched.value) doSearch();
}

function goTo(result: SearchResult) {
  switch (result.type) {
    case 'POST': router.push(`/posts/${result.id}`); break;
    case 'USER': router.push(`/users/${result.id}`); break;
    case 'RESOURCE': router.push(`/resources/${result.id}`); break;
    case 'SPACE': router.push(`/spaces/${result.id}`); break;
  }
}

function formatSize(bytes: number): string {
  if (!bytes) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

const typeLabels: Record<string, string> = { POST: '帖子', USER: '用户', RESOURCE: '资源', SPACE: '空间' };
const typeColors: Record<string, string> = { 
  POST: 'rgba(99, 102, 241, 0.2)', 
  USER: 'rgba(16, 185, 129, 0.2)', 
  RESOURCE: 'rgba(245, 158, 11, 0.2)', 
  SPACE: 'rgba(139, 92, 246, 0.2)' 
};
const typeTextColors: Record<string, string> = { 
  POST: '#818cf8', 
  USER: '#34d399', 
  RESOURCE: '#fbbf24', 
  SPACE: '#a78bfa' 
};
const typeIcons: Record<string, any> = {
  POST: ChatbubblesOutline,
  USER: PersonOutline,
  RESOURCE: DocumentTextOutline,
  SPACE: PlanetOutline
};
</script>

<template>
  <div class="search-layout">
    <div class="header-banner">
      <div class="banner-content">
        <button class="action-btn back-btn" @click="router.back()" title="返回">
          <n-icon><ArrowBackOutline /></n-icon>
        </button>
        <h1 class="page-title gradient-text">
          全站搜索
        </h1>
      </div>
    </div>

    <div class="main-container">
      <div class="search-box glass-card">
        <n-input
          v-model:value="keyword"
          size="large"
          placeholder="探索你感兴趣的内容..."
          clearable
          class="custom-input"
          @keyup.enter="doSearch"
        >
          <template #prefix>
            <n-icon size="20" color="#8b949e"><SearchOutline /></n-icon>
          </template>
          <template #suffix>
            <button class="neon-btn search-btn" @click="doSearch">搜索</button>
          </template>
        </n-input>
      </div>

      <div class="type-bar glass-card">
        <div 
          v-for="t in types" 
          :key="t.key"
          class="type-item"
          :class="{ active: searchType === t.key }"
          @click="switchType(t.key)"
        >
          <n-icon size="18"><component :is="t.icon" /></n-icon>
          <span>{{ t.label }}</span>
        </div>
      </div>

      <div class="results-wrapper">
        <div v-if="loading" class="loading-state">
          <n-spin size="large" />
          <p>正在努力搜索中...</p>
        </div>

        <div v-else-if="searched && results.length === 0" class="empty-state glass-card">
          <n-icon size="64" color="#30363d"><SearchOutline /></n-icon>
          <h3>未找到相关内容</h3>
          <p>换个关键词试试吧</p>
        </div>

        <div v-else class="results-list">
          <div 
            v-for="r in results" 
            :key="`${r.type}-${r.id}`" 
            class="result-card glass-card" 
            @click="goTo(r)"
          >
            <div class="card-header">
              <div class="title-wrap">
                <span 
                  class="type-badge" 
                  :style="{ backgroundColor: typeColors[r.type] || 'rgba(255,255,255,0.1)', color: typeTextColors[r.type] || '#fff' }"
                >
                  <n-icon size="14" style="margin-right: 4px"><component :is="typeIcons[r.type] || GridOutline" /></n-icon>
                  {{ typeLabels[r.type] || r.type }}
                </span>
                <h3 class="result-title">{{ r.title }}</h3>
              </div>
            </div>

            <p v-if="r.description" class="result-desc">{{ r.description }}</p>
            
            <div class="card-footer">
              <div class="meta-list">
                <span v-if="r.author" class="meta-item author">
                  <div class="mini-avatar">{{ r.author.nickname?.charAt(0) || '匿' }}</div>
                  {{ r.author.nickname }}
                </span>
                <span v-if="r.createdAt" class="meta-item date">{{ new Date(r.createdAt).toLocaleDateString() }}</span>
                <span v-if="r.likeCount !== undefined" class="meta-item"><n-icon><HeartOutline/></n-icon> {{ r.likeCount }}</span>
                <span v-if="r.commentCount !== undefined" class="meta-item"><n-icon><ChatbubblesOutline/></n-icon> {{ r.commentCount }}</span>
                <span v-if="r.viewCount !== undefined" class="meta-item"><n-icon><EyeOutline/></n-icon> {{ r.viewCount }}</span>
                <span v-if="r.downloadCount !== undefined" class="meta-item"><n-icon><CloudDownloadOutline/></n-icon> {{ r.downloadCount }}</span>
                <span v-if="r.memberCount !== undefined" class="meta-item"><n-icon><PersonOutline/></n-icon> {{ r.memberCount }}</span>
                <span v-if="r.postCount !== undefined" class="meta-item"><n-icon><DocumentTextOutline/></n-icon> {{ r.postCount }}</span>
                <span v-if="r.fileSize" class="meta-item size">{{ formatSize(r.fileSize) }}</span>
              </div>
              <div class="tags" v-if="r.category || r.fileType">
                <span v-if="r.category" class="info-tag">{{ r.category }}</span>
                <span v-if="r.fileType" class="info-tag">{{ r.fileType }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.search-layout {
  height: 100vh;
  overflow-y: auto;
  background: var(--cf-bg-base);
  background-image: radial-gradient(circle at 80% 20%, rgba(139, 92, 246, 0.08), transparent 40%);
}

.header-banner {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 24px 24px;

  .banner-content {
    display: flex;
    align-items: center;
    gap: 16px;

    .back-btn {
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
      font-size: 20px;
      transition: all 0.3s;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        transform: translateX(-2px);
      }
    }

    .page-title {
      font-size: 32px;
      font-weight: 800;
      margin: 0;
    }
  }
}

.main-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px 40px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.search-box {
  padding: 8px;
  border-radius: 20px;
  display: flex;
  align-items: center;

  .custom-input {
    flex: 1;
    background: transparent;
    --n-border: none !important;
    --n-border-hover: none !important;
    --n-border-focus: none !important;
    --n-box-shadow-focus: none !important;
    --n-color: transparent !important;
    --n-color-focus: transparent !important;
    
    :deep(.n-input__input-el) {
      font-size: 16px;
    }
  }

  .search-btn {
    border-radius: 12px;
    padding: 0 24px;
    height: 38px;
  }
}

.type-bar {
  display: flex;
  padding: 6px;
  gap: 4px;
  border-radius: 16px;

  .type-item {
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

.results-wrapper {
  min-height: 400px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 16px;
  color: var(--cf-text-secondary);
}

.empty-state {
  padding: 80px 0;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  
  h3 {
    margin: 16px 0 8px;
    font-size: 18px;
    color: var(--cf-text-primary);
  }
  
  p {
    margin: 0;
    color: var(--cf-text-secondary);
    font-size: 14px;
  }
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-card {
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    border-color: var(--cf-primary);
    box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15);
  }

  .card-header {
    margin-bottom: 12px;

    .title-wrap {
      display: flex;
      align-items: flex-start;
      gap: 12px;

      .type-badge {
        display: inline-flex;
        align-items: center;
        padding: 4px 10px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 600;
        white-space: nowrap;
        margin-top: 2px;
      }

      .result-title {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: var(--cf-text-primary);
        line-height: 1.4;
      }
    }
  }

  .result-desc {
    color: var(--cf-text-secondary);
    font-size: 15px;
    line-height: 1.6;
    margin: 0 0 20px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid var(--cf-border);
    padding-top: 16px;

    .meta-list {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      
      .meta-item {
        display: flex;
        align-items: center;
        gap: 6px;
        color: var(--cf-text-secondary);
        font-size: 13px;
        
        &.author {
          color: var(--cf-text-primary);
          font-weight: 500;
          
          .mini-avatar {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background: var(--cf-gradient-primary);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
            font-weight: bold;
          }
        }
      }
    }

    .tags {
      display: flex;
      gap: 8px;
      
      .info-tag {
        font-size: 12px;
        color: var(--cf-text-secondary);
        background: rgba(255, 255, 255, 0.05);
        padding: 4px 10px;
        border-radius: 12px;
      }
    }
  }
}
</style>
