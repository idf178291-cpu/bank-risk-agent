<template>
  <aside class="sidebar">
    <div class="brand">
      <h2>🏦 银行风险诊断</h2>
      <p>AgentScope AG-UI</p>
    </div>

    <div class="section">
      <h4>快速操作</h4>
      <button @click="$emit('quick', '开始新的风险诊断')">🆕 新诊断</button>
      <button @click="$emit('quick', '查看诊断报告')">📊 查看报告</button>
      <button @click="$emit('quick', '分析风险趋势')">📈 趋势分析</button>
    </div>

    <div class="section">
      <h4>六大风险分类</h4>
      <div class="cat-list">
        <div class="cat-item cat-credit"><span>💰</span> 信用风险</div>
        <div class="cat-item cat-operation"><span>⚙️</span> 操作风险</div>
        <div class="cat-item cat-security"><span>🔒</span> 安全风险</div>
        <div class="cat-item cat-compliance"><span>📋</span> 合规风险</div>
        <div class="cat-item cat-strategy"><span>🎯</span> 战略与经营风险</div>
        <div class="cat-item cat-reputation"><span>📢</span> 声誉风险</div>
      </div>
    </div>

    <div class="section">
      <h4>风险等级图例</h4>
      <div class="legend">
        <div><span class="dot" style="background:var(--success)"></span> 低风险 — 指标正常</div>
        <div><span class="dot" style="background:var(--warning)"></span> 关注 — 需跟踪</div>
        <div><span class="dot" style="background:var(--danger)"></span> 高风险 — 部分超标</div>
        <div><span class="dot" style="background:var(--critical)"></span> 严重 — 多项超标</div>
      </div>
    </div>

    <PipelineProgress :current-step="pipelineStep" :status="pipelineStatus" />
  </aside>
</template>

<script setup>
import PipelineProgress from './PipelineProgress.vue'

defineProps({ pipelineStep: Number, pipelineStatus: String })
defineEmits(['quick'])
</script>

<style scoped>
.brand { text-align: center; padding-bottom: 16px; border-bottom: 1px solid var(--border); }
.brand h2 { font-size: 15px; font-weight: 600; }
.brand p { font-size: 11px; color: var(--text-secondary); margin-top: 2px; }
.section { margin-top: 20px; }
.section h4 { font-size: 12px; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 10px; }
.section button { width: 100%; background: var(--bg-input); color: var(--text-primary); text-align: left; padding: 10px 14px; margin-bottom: 6px; }
.section button:hover { background: var(--border); }
.cat-list { display: flex; flex-direction: column; gap: 6px; }
.cat-item { padding: 8px 12px; border-radius: 8px; background: var(--bg-input); font-size: 13px; display: flex; align-items: center; gap: 8px; }
.legend { display: flex; flex-direction: column; gap: 8px; font-size: 13px; }
.legend > div { display: flex; align-items: center; gap: 8px; }
.dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
</style>
