<template>
  <div class="header">
    <span class="logo">🔍</span>
    <span class="title">企业客户风险查询</span>
    <div class="status">
      <span :class="['status-dot', status]"></span>
      <span>{{ statusText }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: 'idle' }
})

const statusText = computed(() => {
  return { idle: '就绪', running: '分析中...', error: '错误' }[props.status] || '就绪'
})
</script>

<style scoped>
.header {
  padding: 14px 24px; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; gap: 10px;
}
.logo { font-size: 18px; }
.title { font-weight: 600; font-size: 13px; }
.status {
  margin-left: auto; display: flex; align-items: center; gap: 6px;
  font-size: 10px; color: var(--text-muted);
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; }
.status-dot.idle { background: var(--text-muted); }
.status-dot.running { background: var(--emerald); animation: pulse 1.5s infinite; }
.status-dot.error { background: var(--rose); }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
</style>
