<template>
  <div class="confirm-btns">
    <button class="yes" @click="emit('submit', 'yes')">✓ 确认</button>
    <button class="no" @click="emit('submit', 'no')">✗ 取消</button>
  </div>
</template>

<script setup>
const emit = defineEmits(['submit'])
</script>

<style scoped>
.confirm-btns { display: flex; gap: 12px; }
.yes { background: var(--success); color: #000; font-weight: 600; padding: 10px 24px; }
.no { background: var(--critical); color: #fff; padding: 10px 24px; }
</style>
