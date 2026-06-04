<template>
  <div>
    <div class="options">
      <button v-for="opt in options" :key="opt.value"
        :class="['opt-btn', { active: selected === opt.value }]"
        @click="select(opt.value)">{{ opt.label }}</button>
    </div>
    <button class="submit" @click="emit('submit', selected)" :disabled="!selected">确认选择</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const props = defineProps({ options: Array })
const emit = defineEmits(['submit'])
const selected = ref(null)
function select(v) { selected.value = v }
</script>

<style scoped>
.options { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.opt-btn { background: var(--bg-input); color: var(--text-primary); padding: 8px 16px; border-radius: 20px; }
.opt-btn.active { background: var(--accent); color: #000; font-weight: 600; }
.submit { background: var(--accent); color: #000; font-weight: 600; }
</style>
