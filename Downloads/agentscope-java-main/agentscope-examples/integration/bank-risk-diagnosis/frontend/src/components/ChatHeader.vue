<template>
  <header class="header">
    <div class="header-left">
      <span class="logo">🏦</span>
      <h1>银行机构风险诊断</h1>
      <span class="badge">AgentScope AG-UI</span>
    </div>
    <div class="header-right">
      <span :class="['status-dot', status]"></span>
      <span class="status-text">{{ statusText }}</span>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ status: { type: String, default: 'idle' } })

const statusText = computed(() => {
  switch (props.status) {
    case 'running': return '诊断中...'
    case 'error': return '异常'
    default: return '就绪'
  }
})
</script>

<style scoped>
.header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 24px; border-bottom: 1px solid var(--border);
  background: var(--bg-sidebar);
}
.header-left { display: flex; align-items: center; gap: 12px; }
.logo { font-size: 22px; }
h1 { font-size: 16px; font-weight: 600; }
.badge { font-size: 11px; color: var(--text-secondary); background: var(--bg-input); padding: 2px 8px; border-radius: 12px; }
.header-right { display: flex; align-items: center; gap: 8px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--success); }
.status-dot.running { background: var(--accent); animation: pulse 1.5s infinite; }
.status-dot.error { background: var(--critical); }
.status-text { font-size: 13px; color: var(--text-secondary); }
</style>
