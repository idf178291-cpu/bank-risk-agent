<template>
  <div class="text-input">
    <input
      :type="inputType"
      v-model="value"
      :placeholder="placeholder"
      @keydown.enter="submit"
    />
  </div>
  <button class="interact-submit" @click="submit" :disabled="!value">提交</button>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  uiType: { type: String, default: 'text' },
  defaultValue: { default: null }
})
const emit = defineEmits(['submit'])

const value = ref(props.defaultValue || '')

const inputType = computed(() => {
  if (props.uiType === 'number') return 'number'
  if (props.uiType === 'date') return 'date'
  return 'text'
})

const placeholder = computed(() => {
  if (props.uiType === 'date') return '选择日期...'
  if (props.uiType === 'number') return '输入数值...'
  return '输入...'
})

function submit() {
  if (value.value) emit('submit', value.value)
}
</script>

<style scoped>
.text-input { margin-bottom: 8px; }
.text-input input {
  width: 100%; padding: 10px 14px; background: var(--input-bg); border: 1px solid var(--border);
  border-radius: 6px; color: var(--text); font-family: inherit; font-size: 12px;
}
.text-input input:focus { outline: none; border-color: var(--blue); }
.interact-submit {
  padding: 8px 20px; border-radius: 6px; border: none;
  background: var(--violet); color: #fff; font-weight: 600;
  font-family: inherit; font-size: 11px; cursor: pointer;
}
.interact-submit:disabled { opacity: 0.4; cursor: not-allowed; }
.interact-submit:hover:not(:disabled) { background: #6366f1; }
</style>
