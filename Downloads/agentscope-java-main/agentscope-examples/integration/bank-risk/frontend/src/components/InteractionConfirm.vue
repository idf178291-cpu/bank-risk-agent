<template>
  <div class="confirm-buttons">
    <button class="btn-yes" @click="$emit('submit', 'yes')">✓ 确认</button>
    <button class="btn-no" @click="$emit('submit', 'no')">✗ 取消</button>
  </div>
</template>

<script setup>
defineEmits(['submit'])
</script>

<style scoped>
.confirm-buttons { display: flex; gap: 10px; }
.btn-yes {
  padding: 8px 20px; border-radius: 6px; border: 1px solid var(--emerald);
  background: transparent; color: var(--emerald); cursor: pointer;
  font-family: inherit; font-size: 11px; font-weight: 600;
}
.btn-yes:hover { background: rgba(52,211,153,0.1); }
.btn-no {
  padding: 8px 20px; border-radius: 6px; border: 1px solid var(--rose);
  background: transparent; color: var(--rose); cursor: pointer;
  font-family: inherit; font-size: 11px; font-weight: 600;
}
.btn-no:hover { background: rgba(251,113,133,0.1); }
</style>
