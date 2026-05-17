<script setup lang="ts">
import { h, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { getMe, logout as apiLogout } from '@/api/auth';
import {
  PlanetOutline,
  CheckmarkCircleOutline,
  StarOutline,
  SparklesOutline,
  SearchOutline,
  NotificationsOutline,
  ChatbubblesOutline,
  PersonOutline,
  LogOutOutline,
  BonfireOutline
} from '@vicons/ionicons5';
import { NIcon, NAvatar, NDropdown, NInput } from 'naive-ui';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const navLinks = [
  { name: '广场', path: '/square', icon: PlanetOutline },
  { name: '星际部落', path: '/spaces', icon: BonfireOutline },
  { name: '打卡', path: '/checkin', icon: CheckmarkCircleOutline },
  { name: '排行榜', path: '/points', icon: StarOutline },
  { name: 'AI 助手', path: '/ai', icon: SparklesOutline },
];

onMounted(async () => {
  if (authStore.isLoggedIn && !authStore.user) {
    try {
      const user = await getMe();
      authStore.setUser(user);
    } catch (e) {
      authStore.logout();
      router.push('/login');
    }
  }
});

const userDropdownOptions = [
  {
    label: '个人中心',
    key: 'profile',
    icon: () => h(NIcon, null, { default: () => h(PersonOutline) })
  },
  {
    label: '退出登录',
    key: 'logout',
    icon: () => h(NIcon, null, { default: () => h(LogOutOutline) })
  }
];

async function handleDropdownSelect(key: string | number) {
  if (key === 'profile') {
    router.push('/profile');
  } else if (key === 'logout') {
    await apiLogout();
    authStore.logout();
    router.push('/login');
  }
}

function handleSearch(query: string) {
  if (query.trim()) {
    router.push({ path: '/search', query: { q: query } });
  }
}
</script>

<template>
  <div class="main-layout">
    <!-- Sidebar -->
    <aside class="sidebar glass-panel">
      <div
        class="logo-area"
        @click="router.push('/')"
      >
        <n-icon
          size="28"
          color="#6366f1"
        >
          <PlanetOutline />
        </n-icon>
        <span class="logo-text">CampusForum</span>
      </div>

      <nav class="nav-menu">
        <a 
          v-for="link in navLinks" 
          :key="link.path"
          class="nav-item"
          :class="{ active: route.path.startsWith(link.path) }"
          @click="router.push(link.path)"
        >
          <n-icon size="22"><component :is="link.icon" /></n-icon>
          <span>{{ link.name }}</span>
        </a>
      </nav>
    </aside>

    <!-- Main Content Area -->
    <div class="content-wrapper">
      <!-- Top Header -->
      <header class="top-header glass-panel">
        <div class="header-left">
          <!-- Optional: page specific titles could go here -->
        </div>
        <div class="header-right">
          <n-input 
            round 
            placeholder="搜索帖子、用户或空间" 
            class="search-input"
            @keyup.enter="(e) => handleSearch((e.target as HTMLInputElement).value)"
          >
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
          
          <button
            class="icon-btn"
            title="私信"
            @click="router.push('/messages')"
          >
            <n-icon size="20">
              <ChatbubblesOutline />
            </n-icon>
          </button>
          
          <button
            class="icon-btn"
            title="通知"
            @click="router.push('/notifications')"
          >
            <n-icon size="20">
              <NotificationsOutline />
            </n-icon>
          </button>

          <n-dropdown 
            v-if="authStore.user"
            :options="userDropdownOptions" 
            @select="handleDropdownSelect"
          >
            <div class="user-profile">
              <n-avatar 
                round 
                size="small" 
                :src="authStore.user.avatarUrl"
                :fallback-src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${authStore.user.nickname}`"
              />
              <span class="user-name">{{ authStore.user.nickname }}</span>
            </div>
          </n-dropdown>
        </div>
      </header>

      <!-- Page Content -->
      <main class="page-content">
        <router-view v-slot="{ Component }">
          <transition
            name="fade"
            mode="out-in"
          >
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.main-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: var(--cf-bg-base);
  background-image: radial-gradient(circle at 15% 50%, rgba(99, 102, 241, 0.08), transparent 30%),
                    radial-gradient(circle at 85% 30%, rgba(139, 92, 246, 0.08), transparent 30%);
  overflow: hidden;
}

.glass-panel {
  background: rgba(13, 17, 23, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.sidebar {
  width: 240px;
  display: flex;
  flex-direction: column;
  border-right-width: 1px;
  z-index: 100;
  
  .logo-area {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 24px;
    cursor: pointer;
    
    .logo-text {
      font-size: 20px;
      font-weight: 800;
      color: var(--cf-text-primary);
      letter-spacing: -0.5px;
    }
  }

  .nav-menu {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 16px;
    
    .nav-item {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 12px 16px;
      border-radius: 12px;
      color: var(--cf-text-secondary);
      font-size: 15px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      
      &:hover {
        background: rgba(255, 255, 255, 0.05);
        color: var(--cf-text-primary);
      }
      
      &.active {
        background: rgba(99, 102, 241, 0.15);
        color: var(--cf-primary);
        box-shadow: inset 3px 0 0 var(--cf-primary);
      }
    }
  }
}

.content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  border-bottom-width: 1px;
  z-index: 90;
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-left: auto;
    
    .search-input {
      width: 240px;
      background: rgba(255, 255, 255, 0.03);
    }
    
    .icon-btn {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: transparent;
      border: 1px solid transparent;
      color: var(--cf-text-secondary);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s;
      
      &:hover {
        background: rgba(255, 255, 255, 0.05);
        color: var(--cf-text-primary);
        border-color: rgba(255, 255, 255, 0.1);
      }
    }
    
    .user-profile {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 4px 12px 4px 4px;
      border-radius: 20px;
      cursor: pointer;
      transition: background 0.3s;
      
      &:hover {
        background: rgba(255, 255, 255, 0.05);
      }
      
      .user-name {
        font-size: 14px;
        font-weight: 500;
        color: var(--cf-text-primary);
      }
    }
  }
}

.page-content {
  flex: 1;
  overflow-y: auto;
  position: relative;
  
  /* Custom scrollbar for main content */
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 3px;
  }
  &::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.2);
  }
}

/* Page Transition Animations */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
