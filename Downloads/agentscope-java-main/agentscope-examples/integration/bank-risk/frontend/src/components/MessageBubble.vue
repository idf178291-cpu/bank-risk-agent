<template>
  <div :class="['msg', message.role]" :style="{ animation: 'fadeIn 0.3s ease' }">
    <div class="avatar">{{ avatar }}</div>
    <div :class="['bubble', message.role]">
      <MarkdownRenderer v-if="message.role === 'agent'" :content="message.content" />
      <span v-else>{{ message.content }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownRenderer from './MarkdownRenderer.vue'

const props = defineProps({
  message: { type: Object, required: true }
})

const avatar = computed(() => {
  return { user: '👤', agent: '🤖', tool: '🔧' }[props.message.role] || '💬'
})
</script>

<style scoped>
.msg { display: flex; gap: 10px; max-width: 85%; }
.msg.user { flex-direction: row-reverse; align-self: flex-end; }
.msg .avatar {
  width: 32px; height: 32px; border-radius: 8px; display: flex;
  align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0;
}
.msg.user .avatar { background: var(--blue-glass); }
.msg.agent .avatar { background: rgba(16,185,129,0.1); }
.msg.tool .avatar { background: rgba(99,102,241,0.08); }
.msg .bubble {
  padding: 10px 14px; border-radius: 10px; font-size: 12px; line-height: 1.7;
}
.msg.user .bubble {
  background: var(--blue-glass); border: 1px solid rgba(37,99,235,0.15);
  border-bottom-right-radius: 4px;
}
.msg.agent .bubble {
  background: #f8fafc; border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
}
.msg.tool .bubble {
  background: #f8fafc; border: 1px solid var(--border);
  font-size: 10px; color: var(--text-secondary);
}
</style>
