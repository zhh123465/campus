<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NInput, NCard, NSpace, NTag, useMessage } from 'naive-ui';
import { createPost } from '@/api/posts';

const router = useRouter();
const message = useMessage();

const title = ref('');
const content = ref('');
const topicInput = ref('');
const topics = ref<string[]>([]);
const loading = ref(false);

function addTopic() {
  const t = topicInput.value.trim();
  if (t && !topics.value.includes(t)) {
    topics.value.push(t);
    topicInput.value = '';
  }
}

function removeTopic(t: string) {
  topics.value = topics.value.filter(x => x !== t);
}

async function submit() {
  if (!content.value.trim()) {
    message.warning('请输入内容');
    return;
  }
  loading.value = true;
  try {
    const post = await createPost({
      title: title.value.trim() || undefined,
      content: content.value,
      topics: topics.value.length > 0 ? topics.value : undefined,
      scope: 'SQUARE',
      type: 'NORMAL',
    });
    message.success('发布成功');
    router.push(`/posts/${post.id}`);
  } catch {
    message.error('发布失败');
  }
  loading.value = false;
}

function cancel() {
  router.back();
}
</script>

<template>
  <div class="create-page">
    <NCard title="发帖">
      <div class="form">
        <NInput v-model:value="title" placeholder="标题（可选）" class="field" maxlength="255" />

        <NInput
          v-model:value="content"
          type="textarea"
          placeholder="想说些什么..."
          class="field"
          :autosize="{ minRows: 6, maxRows: 16 }"
          maxlength="20000"
        />

        <div class="topic-row">
          <NInput
            v-model:value="topicInput"
            placeholder="添加话题（回车确认）"
            class="topic-input"
            @keyup.enter="addTopic"
          />
          <NButton size="small" @click="addTopic">添加</NButton>
        </div>
        <NSpace v-if="topics.length > 0" class="topic-list">
          <NTag v-for="t in topics" :key="t" closable @close="removeTopic(t)">{{ t }}</NTag>
        </NSpace>

        <NSpace class="actions">
          <NButton type="primary" :loading="loading" @click="submit">发布</NButton>
          <NButton @click="cancel">取消</NButton>
        </NSpace>
      </div>
    </NCard>
  </div>
</template>

<style scoped>
.create-page {
  max-width: 680px;
  margin: 40px auto;
  padding: 0 16px;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.topic-row {
  display: flex;
  gap: 8px;
}
.topic-input {
  flex: 1;
}
.topic-list {
  flex-wrap: wrap;
}
.actions {
  margin-top: 8px;
}
</style>
