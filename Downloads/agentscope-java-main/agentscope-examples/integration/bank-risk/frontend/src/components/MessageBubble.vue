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
.msg.user .avatar { background: rgba(129,140,248,0.15); }
.msg.agent .avatar { background: rgba(56,189,248,0.1); }
.msg.tool .avatar { background: rgba(245,158,11,0.1); }
.msg .bubble {
  padding: 10px 14px; border-radius: 10px; font-size: 12px; line-height: 1.7;
}
.msg.user .bubble {
  background: rgba(129,140,248,0.12); border: 1px solid rgba(129,140,248,0.25);
  border-bottom-right-radius: 4px;
}
.msg.agent .bubble {
  background: rgba(56,189,248,0.06); border: 1px solid rgba(56,189,248,0.15);
  border-bottom-left-radius: 4px;
}
.msg.tool .bubble {
  background: rgba(245,158,11,0.06); border: 1px solid rgba(245,158,11,0.15);
  font-size: 10px; color: var(--text-secondary);
}
</style>
