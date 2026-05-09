<script setup lang="ts">
import { NConfigProvider, NMessageProvider, NNotificationProvider, darkTheme } from 'naive-ui';
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
