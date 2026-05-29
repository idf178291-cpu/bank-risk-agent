<template>
  <div class="select-options">
    <button
      v-for="opt in options"
      :key="opt"
      :class="{ selected: selectedValues.has(opt) }"
      @click="toggle(opt)"
    >{{ opt }}</button>
  </div>
  <button class="interact-submit" @click="submit"
    :disabled="selectedValues.size === 0">提交</button>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  options: { type: Array, default: () => [] }
})
const emit = defineEmits(['submit'])

const selectedValues = ref(new Set())

function toggle(opt) {
  const next = new Set(selectedValues.value)
  if (next.has(opt)) next.delete(opt)
  else next.add(opt)
  selectedValues.value = next
}

function submit() {
  if (selectedValues.value.size > 0) {
    emit('submit', Array.from(selectedValues.value))
  }
}
</script>

<style scoped>
.select-options { display: flex; flex-wrap: wrap; gap: 6px; }
.select-options button {
  padding: 8px 14px; border-radius: 6px; border: 1px solid var(--border);
  background: transparent; color: var(--text-secondary); cursor: pointer;
  font-family: inherit; font-size: 11px; transition: all 0.15s;
}
.select-options button:hover { border-color: var(--amber); color: var(--amber); }
.select-options button.selected {
  background: rgba(251,191,36,0.15); border-color: var(--amber); color: var(--amber);
}
.interact-submit {
  margin-top: 10px; padding: 8px 20px; border-radius: 6px; border: none;
  background: var(--amber); color: #020617; font-weight: 600;
  font-family: inherit; font-size: 11px; cursor: pointer;
}
.interact-submit:disabled { opacity: 0.4; cursor: not-allowed; }
.interact-submit:hover:not(:disabled) { background: #f59e0b; }
</style>
