<template>
  <div class="sidebar">
    <h2>企业风险管理</h2>
    <div class="subtitle">AgentScope AG-UI | Vue 3</div>
    <button
      v-for="q in quickQueries"
      :key="q"
      class="quick-btn"
      @click="$emit('quick-query', q)"
    >查询{{ q }}行业</button>
    <div class="risk-legend">
      <h4>风险等级图例</h4>
      <div class="risk-dot low">低风险 — 指标均达标</div>
      <div class="risk-dot watch">关注 — 接近红线</div>
      <div class="risk-dot high">高风险 — 部分超标</div>
      <div class="risk-dot critical">严重 — 多项严重超标</div>
    </div>
  </div>
</template>

<script setup>
defineEmits(['quick-query'])
const quickQueries = ['新能源', '通信', '汽车', '房地产', '航空']
</script>

<style scoped>
.sidebar {
  width: 260px; background: var(--surface); border-right: 1px solid var(--border);
  padding: 20px; display: flex; flex-direction: column; gap: 16px; flex-shrink: 0;
}
.sidebar h2 { font-size: 14px; font-weight: 600; white-space: nowrap; }
.subtitle { font-size: 10px; color: var(--text-muted); margin-top: -8px; }
.quick-btn {
  padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border);
  background: transparent; color: var(--text-secondary); cursor: pointer;
  font-family: inherit; font-size: 11px; text-align: left; transition: all 0.2s;
}
.quick-btn:hover { border-color: var(--blue); color: var(--blue); }
.risk-legend { margin-top: auto; }
.risk-legend h4 { font-size: 10px; color: var(--text-muted); margin-bottom: 8px; }
.risk-dot {
  display: flex; align-items: center; gap: 8px; font-size: 10px;
  color: var(--text-secondary); padding: 3px 0;
}
.risk-dot::before { content: ''; width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.risk-dot.low::before { background: var(--risk-low); }
.risk-dot.watch::before { background: var(--risk-watch); }
.risk-dot.high::before { background: var(--risk-high); }
.risk-dot.critical::before { background: var(--risk-critical); }
</style>
