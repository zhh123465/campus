<script setup lang="ts">
import { useWebSocket } from '@/composables/useWebSocket';
import { useNotification } from 'naive-ui';

const notif = useNotification();

useWebSocket((event) => {
  notif.create({
    title: event.title || '新通知',
    content: event.content || '',
    duration: 5000,
  });
});
</script>

<template>
  <NConfigProvider>
    <NMessageProvider>
      <NNotificationProvider>
        <router-view />
      </NNotificationProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>
