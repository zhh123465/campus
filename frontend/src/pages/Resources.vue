<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { NCard, NButton, NTag, NSpace, NSpin, NEmpty } from 'naive-ui';
import { getResources } from '@/api/resources';
import type { ResourceVO } from '@/types/resource';

const router = useRouter();
const resources = ref<ResourceVO[]>([]);
const loading = ref(false);
const collegeFilter = ref<string | undefined>(undefined);
const colleges = ref<string[]>([]);

async function load() {
  loading.value = true;
  try {
    resources.value = await getResources({
      college: collegeFilter.value || undefined,
      limit: 30,
    });
    if (!collegeFilter.value) {
      const seen = new Set<string>();
      resources.value.forEach((r) => {
        if (r.college) seen.add(r.college);
      });
      // 不覆盖已选筛选
    }
  } catch {
    resources.value = [];
  }
  loading.value = false;
}

function goDetail(id: number) {
  router.push(`/resources/${id}`);
}

function goUpload() {
  router.push('/resources/upload');
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

const typeIcons: Record<string, string> = {
  pdf: '📄',
  doc: '📝',
  docx: '📝',
  ppt: '📊',
  pptx: '📊',
  xls: '📈',
  xlsx: '📈',
  zip: '📦',
  rar: '📦',
  '7z': '📦',
  jpg: '🖼',
  jpeg: '🖼',
  png: '🖼',
  gif: '🖼',
  webp: '🖼',
};

onMounted(load);
</script>

<template>
  <div class="resources-page">
    <div class="page-header">
      <h2>资源分享</h2>
      <NButton type="primary" @click="goUpload">上传资源</NButton>
    </div>

    <div v-if="resources.length === 0 && !loading" class="empty">
      <NEmpty description="暂无资源" />
    </div>

    <div v-for="r in resources" :key="r.id" class="resource-card" @click="goDetail(r.id)">
      <NCard>
        <div class="card-row">
          <div class="file-icon">{{ typeIcons[r.fileType] || '📁' }}</div>
          <div class="card-body">
            <div class="file-name">{{ r.fileName }}</div>
            <div class="file-meta">
              <span>{{ formatSize(r.fileSize) }}</span>
              <span>{{ r.fileType.toUpperCase() }}</span>
              <span>{{ r.downloadCount }} 次下载</span>
              <span class="uploader-name">{{ r.uploader?.nickname || '未知' }}</span>
            </div>
            <div v-if="r.tags?.length || r.college || r.major || r.course" class="file-tags">
              <NTag v-if="r.college" size="tiny">{{ r.college }}</NTag>
              <NTag v-if="r.major" size="tiny">{{ r.major }}</NTag>
              <NTag v-if="r.course" size="tiny">{{ r.course }}</NTag>
            </div>
          </div>
        </div>
      </NCard>
    </div>

    <div v-if="loading" class="loading">
      <NSpin />
    </div>
  </div>
</template>

<style scoped>
.resources-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-header h2 { margin: 0; }
.empty { margin: 80px 0; }
.resource-card {
  margin-bottom: 12px;
  cursor: pointer;
}
.card-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.file-icon {
  font-size: 32px;
  flex-shrink: 0;
  width: 40px;
  text-align: center;
}
.card-body { flex: 1; min-width: 0; }
.file-name {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #999;
  margin-bottom: 6px;
}
.uploader-name { margin-left: auto; }
.file-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.loading { text-align: center; padding: 24px; }
</style>
