<template>
  <div :class="['bubble', role]">
    <div class="avatar">{{ avatar }}</div>
    <div :class="['content', role]">
      <div v-if="role === 'agent'" v-html="rendered"></div>
      <div v-else class="plain">{{ message.content }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '../utils/markdown.js'

const props = defineProps({ message: Object })

const role = computed(() => props.message.role)

const avatar = computed(() => {
  switch (role.value) {
    case 'user': return '👤'
    case 'agent': return '🤖'
    case 'tool': return '🔧'
    default: return '📋'
  }
})

const rendered = computed(() => {
  if (role.value === 'agent') {
    return renderMarkdown(props.message.content || '')
  }
  return ''
})
</script>

<style scoped>
.bubble { display: flex; gap: 10px; padding: 8px 0; animation: fadeIn 0.3s ease; }
.bubble.user { flex-direction: row-reverse; }
.avatar { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }
.content { max-width: 75%; padding: 12px 16px; border-radius: 12px; font-size: 14px; line-height: 1.6; }
.content.user { background: var(--user-bubble); border-bottom-right-radius: 4px; }
.content.agent { background: var(--agent-bubble); border: 1px solid var(--border); border-bottom-left-radius: 4px; }
.content.tool { background: var(--tool-bubble); border-bottom-left-radius: 4px; font-size: 12px; max-width: 85%; }
.plain { white-space: pre-wrap; word-break: break-word; }
</style>
