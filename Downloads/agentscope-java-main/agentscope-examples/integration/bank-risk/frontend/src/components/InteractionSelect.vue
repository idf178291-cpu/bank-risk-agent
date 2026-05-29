<template>
  <div class="select-options">
    <button
      v-for="opt in options"
      :key="opt"
      :class="{ selected: selectedValue === opt }"
      @click="selectedValue = opt"
    >{{ opt }}</button>
    <button v-if="allowOther" class="other-btn" @click="showOther = true">✏️ 其他...</button>
  </div>
  <div v-if="showOther" class="other-input">
    <input v-model="otherValue" type="text" placeholder="输入自定义值..."
      @keydown.enter="submit" />
  </div>
  <button class="interact-submit" @click="submit">提交</button>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  options: { type: Array, default: () => [] },
  allowOther: { type: Boolean, default: false }
})
const emit = defineEmits(['submit'])

const selectedValue = ref(null)
const showOther = ref(false)
const otherValue = ref('')

function submit() {
  const result = showOther.value && otherValue.value
    ? otherValue.value : selectedValue.value
  if (result) emit('submit', result)
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
.other-btn { border-style: dashed !important; }
.other-input { margin-top: 8px; }
.other-input input {
  width: 100%; padding: 10px 14px; background: rgba(2,6,23,0.6); border: 1px solid var(--border);
  border-radius: 6px; color: var(--text); font-family: inherit; font-size: 12px;
}
.other-input input:focus { outline: none; border-color: var(--amber); }
.interact-submit {
  margin-top: 10px; padding: 8px 20px; border-radius: 6px; border: none;
  background: var(--amber); color: #020617; font-weight: 600;
  font-family: inherit; font-size: 11px; cursor: pointer;
}
.interact-submit:hover { background: #f59e0b; }
</style>
