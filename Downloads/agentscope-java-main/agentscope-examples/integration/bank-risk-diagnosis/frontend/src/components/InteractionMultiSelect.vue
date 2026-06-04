<template>
  <div>
    <div class="options">
      <button v-for="opt in options" :key="opt.value"
        :class="['opt-btn', { active: selected.includes(opt.value) }]"
        @click="toggle(opt.value)">{{ opt.label }}</button>
    </div>
    <button class="submit" @click="emit('submit', selected)" :disabled="selected.length === 0">提交 ({{ selected.length }})</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const props = defineProps({ options: Array })
const emit = defineEmits(['submit'])
const selected = ref([])
function toggle(v) {
  const idx = selected.value.indexOf(v)
  if (idx >= 0) selected.value.splice(idx, 1)
  else selected.value.push(v)
}
</script>

<style scoped>
.options { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.opt-btn { background: var(--bg-input); color: var(--text-primary); padding: 8px 16px; border-radius: 20px; }
.opt-btn.active { background: var(--accent); color: #000; font-weight: 600; }
.submit { background: var(--accent); color: #000; font-weight: 600; }
</style>
