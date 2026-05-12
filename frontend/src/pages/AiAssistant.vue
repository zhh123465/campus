<script setup lang="ts">
import { ref, nextTick } from 'vue';
import { NInput, NButton, NTag, NSpace, NSpin, NCard, NAlert, NDivider } from 'naive-ui';
import { aiChat, aiSummarize, aiModerate, aiRecommendTags } from '@/api/ai';
import type { ChatMessage } from '@/types/ai';

interface UIMessage {
  role: 'user' | 'assistant';
  content: string;
  time: string;
}

const messages = ref<UIMessage[]>([]);
const input = ref('');
const loading = ref(false);
const mode = ref<'chat' | 'summarize' | 'moderate' | 'tags'>('chat');
const toolInput = ref('');
const toolTitle = ref('');
const toolResult = ref('');
const toolResultType = ref<'success' | 'warning' | 'error' | 'info'>('info');

const apiMessages = ref<ChatMessage[]>([]);

async function send() {
  const text = input.value.trim();
  if (!text || loading.value) return;

  const now = new Date().toLocaleTimeString();
  messages.value.push({ role: 'user', content: text, time: now });
  input.value = '';
  loading.value = true;

  try {
    apiMessages.value.push({ role: 'user', content: text });
    const res = await aiChat(apiMessages.value);
    const reply = res.reply || '抱歉，我没有理解你的问题。';
    apiMessages.value.push({ role: 'assistant', content: reply });
    messages.value.push({ role: 'assistant', content: reply, time: new Date().toLocaleTimeString() });
  } catch {
    messages.value.push({ role: 'assistant', content: 'AI 服务暂时不可用，请稍后重试。', time: new Date().toLocaleTimeString() });
  }
  loading.value = false;
  await nextTick();
  scrollBottom();
}

function scrollBottom() {
  const el = document.querySelector('.chat-messages');
  if (el) el.scrollTop = el.scrollHeight;
}

async function runSummarize() {
  if (!toolInput.value.trim()) return;
  loading.value = true;
  try {
    const res = await aiSummarize(toolInput.value);
    toolResult.value = res.summary || '';
    toolResultType.value = 'info';
  } catch {
    toolResult.value = '摘要生成失败';
    toolResultType.value = 'error';
  }
  loading.value = false;
}

async function runModerate() {
  if (!toolInput.value.trim()) return;
  loading.value = true;
  try {
    const res = await aiModerate(toolInput.value);
    const level = res.riskLevel ?? 0;
    if (level === 0) {
      toolResult.value = '内容审核通过，未检测到风险。';
      toolResultType.value = 'success';
    } else if (level === 1) {
      toolResult.value = '中风险: ' + (res.riskReason || '内容需人工复核');
      toolResultType.value = 'warning';
    } else {
      toolResult.value = '高风险: ' + (res.riskReason || '内容包含违规信息');
      toolResultType.value = 'error';
    }
  } catch {
    toolResult.value = '审核失败';
    toolResultType.value = 'error';
  }
  loading.value = false;
}

async function runTags() {
  if (!toolInput.value.trim()) return;
  loading.value = true;
  try {
    const res = await aiRecommendTags(toolTitle.value, toolInput.value);
    toolResult.value = res.tags?.length ? res.tags.join('、') : '未识别到合适的标签';
    toolResultType.value = 'info';
  } catch {
    toolResult.value = '标签推荐失败';
    toolResultType.value = 'error';
  }
  loading.value = false;
}

function clearMessages() {
  messages.value = [];
  apiMessages.value = [];
}
</script>

<template>
  <div class="ai-page">
    <div class="ai-header">
      <h2>AI 助手</h2>
      <NSpace>
        <NButton size="small" @click="clearMessages">清空对话</NButton>
      </NSpace>
    </div>

    <NSpace class="mode-bar">
      <NButton :type="mode === 'chat' ? 'primary' : 'default'" size="small" @click="mode = 'chat'">智能问答</NButton>
      <NButton :type="mode === 'summarize' ? 'primary' : 'default'" size="small" @click="mode = 'summarize'">内容摘要</NButton>
      <NButton :type="mode === 'moderate' ? 'primary' : 'default'" size="small" @click="mode = 'moderate'">内容审核</NButton>
      <NButton :type="mode === 'tags' ? 'primary' : 'default'" size="small" @click="mode = 'tags'">标签推荐</NButton>
    </NSpace>

    <!-- Chat Mode -->
    <div v-if="mode === 'chat'" class="chat-container">
      <div class="chat-messages">
        <div v-if="messages.length === 0" class="chat-empty">
          <p>你好！我是 CampusForum AI 助手，可以回答你关于平台使用的问题。</p>
          <p style="color: #999; font-size: 13px;">试试问我："怎么发帖？" / "如何获得积分？" / "怎么加入学习空间？"</p>
        </div>
        <div v-for="(msg, i) in messages" :key="i" class="chat-message" :class="msg.role">
          <div class="msg-bubble">
            <div class="msg-text">{{ msg.content }}</div>
            <div class="msg-time">{{ msg.time }}</div>
          </div>
        </div>
        <div v-if="loading" class="chat-loading">
          <NSpin size="small" />
        </div>
      </div>
      <div class="chat-input">
        <NInput
          v-model:value="input"
          placeholder="输入你的问题..."
          @keyup.enter="send"
          :disabled="loading"
        >
          <template #suffix>
            <NButton type="primary" size="small" @click="send" :disabled="loading">发送</NButton>
          </template>
        </NInput>
      </div>
    </div>

    <!-- Tool Mode -->
    <div v-else class="tool-container">
      <NCard>
        <div v-if="mode === 'tags'" class="tool-field">
          <label>标题（可选）</label>
          <NInput v-model:value="toolTitle" placeholder="输入标题..." />
        </div>
        <div class="tool-field">
          <label>{{ mode === 'summarize' ? '待摘要内容' : mode === 'moderate' ? '待审核内容' : '内容' }}</label>
          <NInput
            v-model:value="toolInput"
            type="textarea"
            :rows="6"
            :placeholder="mode === 'summarize' ? '输入需要生成摘要的文本...' : mode === 'moderate' ? '输入需要审核的内容...' : '输入需要推荐标签的内容...'"
          />
        </div>
        <div class="tool-action">
          <NButton
            type="primary"
            @click="mode === 'summarize' ? runSummarize() : mode === 'moderate' ? runModerate() : runTags()"
            :disabled="loading"
          >
            {{ mode === 'summarize' ? '生成摘要' : mode === 'moderate' ? '开始审核' : '推荐标签' }}
          </NButton>
        </div>
        <div v-if="loading" style="text-align: center; padding: 16px;">
          <NSpin size="small" />
        </div>
        <div v-if="toolResult && !loading" class="tool-result">
          <NDivider />
          <NAlert :type="toolResultType" :title="toolResultType === 'success' ? '审核通过' : toolResultType === 'warning' ? '注意' : toolResultType === 'error' ? '警告' : '结果'">
            {{ toolResult }}
          </NAlert>
        </div>
      </NCard>
    </div>
  </div>
</template>

<style scoped>
.ai-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  max-width: 680px;
  margin: 0 auto;
}
.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.ai-header h2 { margin: 0; }
.mode-bar {
  margin-bottom: 16px;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
  min-height: 300px;
}
.chat-empty {
  text-align: center;
  padding: 48px 16px;
  color: #666;
}
.chat-message { margin-bottom: 12px; }
.chat-message.user .msg-bubble {
  background: #1890ff;
  color: #fff;
  margin-left: auto;
  max-width: 80%;
}
.chat-message.assistant .msg-bubble {
  background: #f5f5f5;
  max-width: 80%;
}
.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  display: inline-block;
}
.msg-text { font-size: 14px; line-height: 1.5; white-space: pre-wrap; }
.msg-time { font-size: 11px; margin-top: 4px; opacity: 0.7; }
.chat-message.user { display: flex; justify-content: flex-end; }
.chat-loading { text-align: center; padding: 8px; }
.chat-input { margin-top: 8px; }

.tool-container { flex: 1; }
.tool-field { margin-bottom: 12px; }
.tool-field label { display: block; font-size: 14px; font-weight: 500; margin-bottom: 4px; color: #333; }
.tool-action { margin-top: 8px; }
.tool-result { margin-top: 8px; }
</style>
