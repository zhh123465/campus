<script setup lang="ts">
import AppNotify from '@/components/AppNotify.vue';
</script>

<template>
  <NConfigProvider>
    <NMessageProvider>
      <NNotificationProvider>
        <AppNotify />
        <router-view />
      </NNotificationProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>
