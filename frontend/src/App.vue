<script setup lang="ts">
import type { GlobalThemeOverrides } from 'naive-ui';
import AppNotify from '@/components/AppNotify.vue';
import '@/assets/styles/theme.scss';

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#0058be',
    primaryColorHover: '#004aa0',
    primaryColorPressed: '#003f87',
    primaryColorSuppl: '#0058be',
    infoColor: '#0058be',
    successColor: '#10b981',
    warningColor: '#f59e0b',
    errorColor: '#dc2626',
    bodyColor: '#f8f9ff',
    cardColor: '#ffffff',
    modalColor: '#ffffff',
    popoverColor: '#ffffff',
    tableColor: '#ffffff',
    borderColor: '#d9e2f2',
    dividerColor: '#e5eeff',
    textColor1: '#0b1c30',
    textColor2: '#425466',
    textColor3: '#6b7a90',
    placeholderColor: '#8ca0bd',
    inputColor: '#ffffff',
    codeColor: '#eff4ff',
    actionColor: '#eff4ff',
  },
  Card: {
    color: '#ffffff',
    colorModal: '#ffffff',
    borderColor: '#d9e2f2',
    borderRadius: '20px',
  },
  Button: {
    borderRadiusMedium: '12px',
    borderRadiusSmall: '10px',
    borderRadiusLarge: '14px',
  },
  Input: {
    borderRadius: '12px',
    color: '#ffffff',
    colorFocus: '#ffffff',
    colorFocusError: '#ffffff',
  },
  Layout: {
    color: '#f8f9ff',
    siderColor: '#ffffff',
    headerColor: 'rgba(248, 249, 255, 0.92)',
  },
  Tabs: {
    tabColorSegment: '#eff4ff',
  },
};
</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <NMessageProvider>
      <NNotificationProvider>
        <AppNotify />
        <router-view />
      </NNotificationProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>
