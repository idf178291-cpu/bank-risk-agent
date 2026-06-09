<template>
  <div class="sidebar">
    <h2>企业风险管理</h2>
    <div class="subtitle">AgentScope AG-UI | Vue 3</div>
    <button class="upload-btn" @click="$emit('upload-doc')">📂 上传文档分析</button>
  </div>
</template>

<script setup>
defineEmits(['upload-doc'])
</script>

<style scoped>
.sidebar {
  width: 260px;
  background: var(--sidebar-bg);
  border-right: 1px solid rgba(255,255,255,0.06);
  padding: 20px;
  display: flex; flex-direction: column; gap: 16px; flex-shrink: 0;
}
.sidebar h2 { font-size: 14px; font-weight: 600; white-space: nowrap; color: #e4e4ec; }
.subtitle { font-size: 10px; color: #7a7a8c; margin-top: -8px; }
.upload-btn {
  padding: 10px 14px; border-radius: 8px;
  border: 1px dashed rgba(255,255,255,0.15);
  background: transparent; color: #8be; cursor: pointer;
  font-family: inherit; font-size: 12px; text-align: center; transition: all 0.2s;
  margin-top: 16px;
}
.upload-btn:hover {
  border-color: #4a6cf7; background: rgba(74,108,247,0.1);
}
</style>
