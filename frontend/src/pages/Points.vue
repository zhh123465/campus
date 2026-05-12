<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { NSpace, NTag, NEmpty, NSpin } from 'naive-ui';
import { getBalance, getPointsLogs } from '@/api/points';
import type { PointsLogVO } from '@/types/points';

const balance = ref(0);
const logs = ref<PointsLogVO[]>([]);
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    const [bal, logList] = await Promise.all([getBalance(), getPointsLogs()]);
    balance.value = bal;
    logs.value = logList;
  } catch { /* ignore */ }
  loading.value = false;
});

const typeLabels: Record<string, string> = {
  LOGIN: '每日登录',
  POST: '发表帖子',
  LIKED: '被点赞',
  ACCEPTED: '回答采纳',
  CHECKIN: '打卡',
  BOUNTY: '悬赏支出',
};
const typeColors: Record<string, string> = {
  LOGIN: '#52c41a',
  POST: '#1890ff',
  LIKED: '#fa8c16',
  ACCEPTED: '#722ed1',
  CHECKIN: '#13c2c2',
  BOUNTY: '#f5222d',
};
</script>

<template>
  <div class="points-page">
    <div class="points-header">
      <h2>我的积分</h2>
      <div class="balance-card">
        <div class="balance-num">{{ balance }}</div>
        <div class="balance-label">当前积分</div>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <NSpin />
    </div>

    <div v-else-if="logs.length === 0" class="empty">
      <NEmpty description="暂无积分记录" />
    </div>

    <div v-else class="logs">
      <div v-for="entry in logs" :key="entry.id" class="log-item">
        <div class="log-left">
          <NTag
            :color="{ color: typeColors[entry.type] || '#999', textColor: '#fff' }"
            size="small"
          >
            {{ typeLabels[entry.type] || entry.type }}
          </NTag>
          <span class="log-ref">{{ entry.reference || '-' }}</span>
        </div>
        <div class="log-right">
          <span class="log-amount" :class="entry.amount > 0 ? 'positive' : 'negative'">
            {{ entry.amount > 0 ? '+' : '' }}{{ entry.amount }}
          </span>
          <span class="log-balance">余额: {{ entry.balance }}</span>
          <span class="log-time">{{ new Date(entry.createdAt).toLocaleDateString() }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.points-page {
  height: 100vh;
  overflow-y: auto;
  padding: 24px 16px;
  max-width: 680px;
  margin: 0 auto;
}
.points-header {
  text-align: center;
  margin-bottom: 24px;
}
.points-header h2 { margin-bottom: 16px; }
.balance-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 12px;
  padding: 24px;
  display: inline-block;
}
.balance-num {
  font-size: 36px;
  font-weight: bold;
}
.balance-label {
  font-size: 14px;
  opacity: 0.8;
  margin-top: 4px;
}
.loading {
  text-align: center;
  padding: 48px;
}
.empty {
  padding: 48px;
}
.logs {
  margin-top: 16px;
}
.log-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.log-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.log-ref {
  color: #666;
  font-size: 13px;
}
.log-right {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.log-amount {
  font-weight: 600;
  font-size: 16px;
}
.log-amount.positive { color: #52c41a; }
.log-amount.negative { color: #f5222d; }
.log-balance {
  font-size: 12px;
  color: #999;
}
.log-time {
  font-size: 12px;
  color: #bbb;
}
</style>
