<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { NCard, NButton, NTag, NSpace, NSpin, NEmpty } from 'naive-ui';
import { getSpaces } from '@/api/spaces';
import type { SpaceVO } from '@/types/space';

const router = useRouter();
const spaces = ref<SpaceVO[]>([]);
const loading = ref(false);
const category = ref<string | undefined>(undefined);

const categories = [
  { key: undefined, label: '全部' },
  { key: 'MAJOR', label: '专业' },
  { key: 'CLASS', label: '班级' },
  { key: 'CLUB', label: '社团' },
  { key: 'INTEREST', label: '兴趣' },
] as const;

async function loadSpaces() {
  loading.value = true;
  try {
    spaces.value = await getSpaces({ category: category.value, limit: 20 });
  } catch {
    spaces.value = [];
  }
  loading.value = false;
}

function switchCategory(cat: string | undefined) {
  category.value = cat;
  loadSpaces();
}

function goDetail(id: number) {
  router.push(`/spaces/${id}`);
}

function goCreate() {
  router.push('/spaces/new');
}

onMounted(loadSpaces);
</script>

<template>
  <div class="spaces-page">
    <div class="page-header">
      <h2>学习空间</h2>
      <NButton type="primary" @click="goCreate">创建空间</NButton>
    </div>

    <NSpace class="category-bar">
      <NButton
        v-for="c in categories"
        :key="c.key"
        :type="category === c.key ? 'primary' : 'default'"
        size="small"
        @click="switchCategory(c.key)"
      >
        {{ c.label }}
      </NButton>
    </NSpace>

    <div v-if="spaces.length === 0 && !loading" class="empty">
      <NEmpty description="暂无空间" />
    </div>

    <div v-for="space in spaces" :key="space.id" class="space-card" @click="goDetail(space.id)">
      <NCard>
        <div class="card-header">
          <div class="space-name">
            {{ space.name }}
            <NTag size="small">{{ space.category === 'MAJOR' ? '专业' : space.category === 'CLASS' ? '班级' : space.category === 'CLUB' ? '社团' : '兴趣' }}</NTag>
          </div>
          <NTag v-if="space.visibility === 'REVIEW'" type="warning" size="small">需审核</NTag>
        </div>
        <p v-if="space.description" class="space-desc">{{ space.description }}</p>
        <div class="card-footer">
          <span>{{ space.memberCount }} 成员</span>
          <span>{{ space.postCount }} 帖子</span>
          <span class="owner-name">创建者: {{ space.owner?.nickname || '未知' }}</span>
        </div>
      </NCard>
    </div>

    <div v-if="loading" class="loading">
      <NSpin />
    </div>
  </div>
</template>

<style scoped>
.spaces-page {
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
.category-bar { margin-bottom: 20px; }
.empty { margin: 80px 0; }
.space-card {
  margin-bottom: 12px;
  cursor: pointer;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.space-name {
  font-weight: 600;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.space-desc {
  color: #666;
  font-size: 14px;
  margin: 0 0 10px;
}
.card-footer {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #999;
}
.owner-name { margin-left: auto; }
.loading {
  text-align: center;
  padding: 24px;
}
</style>
