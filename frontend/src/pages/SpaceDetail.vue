<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NCard, NButton, NTag, NSpace, NSpin, useMessage } from 'naive-ui';
import { getSpaceById, joinSpace, leaveSpace, getSpaceMembers, dismissSpace } from '@/api/spaces';
import { getPosts } from '@/api/posts';
import { useAuthStore } from '@/stores/auth';
import type { SpaceVO, SpaceMemberVO } from '@/types/space';
import type { PostVO } from '@/types/post';

const route = useRoute();
const router = useRouter();
const message = useMessage();
const authStore = useAuthStore();

const space = ref<SpaceVO | null>(null);
const members = ref<SpaceMemberVO[]>([]);
const posts = ref<PostVO[]>([]);
const loading = ref(true);
const currentUserId = authStore.user?.id;

async function loadSpace() {
  loading.value = true;
  try {
    const id = Number(route.params.id);
    space.value = await getSpaceById(id);
    members.value = await getSpaceMembers(id);
    posts.value = await getPosts({ scope: 'SPACE', limit: 10 });
  } catch {
    space.value = null;
  }
  loading.value = false;
}

async function handleJoin() {
  if (!space.value) return;
  try {
    space.value = await joinSpace(space.value.id);
    message.success('已加入空间');
  } catch {
    message.error('加入失败');
  }
}

async function handleLeave() {
  if (!space.value) return;
  try {
    await leaveSpace(space.value.id);
    message.success('已退出空间');
    loadSpace();
  } catch (e: any) {
    message.error(e.message || '退出失败');
  }
}

async function handleDismiss() {
  if (!space.value) return;
  try {
    await dismissSpace(space.value.id);
    message.success('空间已解散');
    router.replace('/spaces');
  } catch {
    message.error('操作失败');
  }
}

function goPost(id: number) {
  router.push(`/posts/${id}`);
}

onMounted(loadSpace);
</script>

<template>
  <div class="detail-page">
    <template v-if="loading">
      <div class="loading"><NSpin /></div>
    </template>

    <template v-else-if="space">
      <NCard class="space-info">
        <div class="info-header">
          <div>
            <h2>{{ space.name }}</h2>
            <NTag size="small">{{ space.category === 'MAJOR' ? '专业' : space.category === 'CLASS' ? '班级' : space.category === 'CLUB' ? '社团' : '兴趣' }}</NTag>
          </div>
          <NSpace>
            <NButton v-if="!space.isMember" type="primary" @click="handleJoin">
              {{ space.visibility === 'PUBLIC' ? '加入' : '申请加入' }}
            </NButton>
            <NButton v-else-if="space.ownerId !== currentUserId" @click="handleLeave">退出</NButton>
            <NButton v-if="space.ownerId === currentUserId" type="error" @click="handleDismiss">解散</NButton>
          </NSpace>
        </div>
        <p v-if="space.description" class="space-desc">{{ space.description }}</p>
        <div class="stats">
          <span>{{ space.memberCount }} 成员</span>
          <span>{{ space.postCount }} 帖子</span>
          <span>创建者: {{ space.owner?.nickname || '未知' }}</span>
        </div>
      </NCard>

      <!-- 成员列表 -->
      <NCard title="成员" class="members-section">
        <div v-for="m in members" :key="m.id" class="member-item">
          <div class="member-avatar">{{ m.user?.nickname?.charAt(0) || '?' }}</div>
          <span class="member-name">{{ m.user?.nickname || '未知' }}</span>
          <NTag v-if="m.role === 'OWNER'" type="error" size="tiny">创建者</NTag>
          <NTag v-else-if="m.role === 'ADMIN'" type="info" size="tiny">管理</NTag>
        </div>
      </NCard>

      <!-- 空间帖子 -->
      <NCard title="帖子" class="posts-section">
        <div v-for="post in posts" :key="post.id" class="post-item" @click="goPost(post.id)">
          <h4>{{ post.title || '无标题' }}</h4>
          <p class="post-preview">{{ post.content.slice(0, 150) }}</p>
          <div class="post-meta">
            <span>{{ post.author?.nickname }}</span>
            <span>{{ post.likeCount }} 赞</span>
            <span>{{ post.commentCount }} 评论</span>
          </div>
        </div>
        <div v-if="posts.length === 0" class="no-posts">暂无帖子</div>
      </NCard>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 720px;
  margin: 40px auto;
  padding: 0 16px;
}
.loading { text-align: center; padding: 80px; }
.info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.info-header h2 { margin: 0 0 4px; }
.space-desc { color: #666; margin-bottom: 12px; }
.stats { display: flex; gap: 16px; font-size: 14px; color: #999; }
.members-section { margin-top: 20px; }
.member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.member-avatar {
  width: 36px; height: 36px;
  border-radius: 50%;
  background: #18a058;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
.member-name { flex: 1; font-size: 14px; }
.posts-section { margin-top: 20px; }
.post-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}
.post-item h4 { margin: 0 0 6px; }
.post-preview {
  color: #666;
  font-size: 14px;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.post-meta { display: flex; gap: 12px; font-size: 12px; color: #999; }
.no-posts { text-align: center; color: #999; padding: 24px; }
</style>
