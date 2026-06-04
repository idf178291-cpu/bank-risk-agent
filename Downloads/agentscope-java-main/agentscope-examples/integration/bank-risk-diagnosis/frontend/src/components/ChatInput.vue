<template>
  <div class="input-area">
    <div class="input-row">
      <button class="upload-btn" @click="$emit('toggle-upload')" title="上传文档">📎</button>
      <input
        v-model="text"
        @keydown.enter="send"
        placeholder="输入消息或上传文档开始诊断..."
        :disabled="isRunning"
      />
      <button class="send-btn" @click="send" :disabled="!text.trim() || isRunning">发送</button>
      <button v-if="isRunning" class="stop-btn" @click="$emit('stop')">⏹ 停止</button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ isRunning: Boolean })
const emit = defineEmits(['send', 'stop', 'toggle-upload'])

const text = ref('')

watch(() => props.isRunning, (v) => { if (!v) text.value = '' })

function send() {
  if (!text.value.trim() || props.isRunning) return
  emit('send', text.value.trim())
  text.value = ''
}
</script>

<style scoped>
.input-area { padding: 14px 24px; border-top: 1px solid var(--border); background: var(--bg-sidebar); }
.input-row { display: flex; gap: 8px; align-items: center; }
.input-row input { flex: 1; }
.upload-btn { background: var(--bg-input); padding: 10px 14px; font-size: 18px; }
.send-btn { background: var(--accent); color: #000; padding: 10px 20px; font-weight: 600; }
.send-btn:hover { background: var(--accent-hover); }
.stop-btn { background: var(--critical); color: #fff; padding: 10px 16px; }
</style>
