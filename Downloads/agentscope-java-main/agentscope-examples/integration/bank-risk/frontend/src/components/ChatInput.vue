<template>
  <div class="input-bar">
    <input
      ref="inputRef"
      type="text"
      v-model="text"
      placeholder="输入企业名称关键词开始查询..."
      :disabled="disabled"
      @keydown.enter.prevent="send"
    />
    <button v-if="!running" class="btn-send" @click="send"
      :disabled="!text.trim()">发送</button>
    <button v-else class="btn-stop" @click="$emit('stop')">⏹ 停止</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['send', 'stop'])

const text = ref('')
const running = ref(false)
const inputRef = ref(null)

function send() {
  const trimmed = text.value.trim()
  if (!trimmed || running.value) return
  emit('send', trimmed)
  text.value = ''
  running.value = true
}

defineExpose({ setRunning: (v) => { running.value = v } })
</script>

<style scoped>
.input-bar {
  padding: 14px 24px; border-top: 1px solid var(--border); display: flex; gap: 10px;
}
.input-bar input {
  flex: 1; padding: 10px 16px; background: var(--surface); border: 1px solid var(--border);
  border-radius: 8px; color: var(--text); font-family: inherit; font-size: 12px;
}
.input-bar input:focus { outline: none; border-color: var(--blue); }
.btn-send {
  padding: 10px 22px; border: none; border-radius: 8px; font-family: inherit; font-size: 12px;
  font-weight: 600; cursor: pointer; background: var(--blue); color: #fff;
}
.btn-send:hover { background: #3b82f6; }
.btn-send:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-stop {
  padding: 10px 22px; border: none; border-radius: 8px; font-family: inherit; font-size: 12px;
  font-weight: 600; cursor: pointer; background: var(--rose); color: white;
}
</style>
