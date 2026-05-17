<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import { parseMentions } from '@/utils/mention';

const props = defineProps<{
  text?: string;
}>();

const segments = computed(() => parseMentions(props.text ?? ''));
</script>

<template>
  <template
    v-for="(segment, index) in segments"
    :key="`${index}-${segment.text}`"
  >
    <RouterLink
      v-if="segment.mention"
      class="mention-link"
      :to="{ path: '/search', query: { q: `@${segment.mention}` } }"
    >
      {{ segment.text }}
    </RouterLink>
    <template v-else>
      {{ segment.text }}
    </template>
  </template>
</template>
