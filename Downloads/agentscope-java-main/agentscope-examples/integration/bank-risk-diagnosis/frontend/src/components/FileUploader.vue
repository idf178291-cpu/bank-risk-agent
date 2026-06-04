<template>
  <div v-if="visible" class="upload-overlay" @click.self="$emit('close')">
    <div class="upload-panel">
      <h3>📎 上传文档</h3>
      <p>支持 OFD、Word (.doc/.docx)、Excel (.xls/.xlsx)</p>

      <div class="drop-zone"
        @dragover.prevent
        @drop.prevent="onDrop"
        @click="triggerInput">
        <span v-if="selected.length === 0">拖拽文件到此处或点击选择</span>
        <div v-else class="file-list">
          <div v-for="f in selected" :key="f.name" class="file-item">
            <span>{{ icon(f.name) }}</span>
            <span class="fname">{{ f.name }}</span>
            <span class="fsize">{{ formatSize(f.size) }}</span>
          </div>
        </div>
      </div>

      <input ref="fileInput" type="file" multiple hidden
        accept=".ofd,.doc,.docx,.xls,.xlsx"
        @change="onFileChange" />

      <div class="upload-actions">
        <input v-model="bankName" placeholder="输入银行名称（可选）" class="bank-input" />
        <button class="cancel-btn" @click="$emit('close')">取消</button>
        <button class="confirm-btn" @click="upload" :disabled="selected.length === 0">
          上传并解析 ({{ selected.length }} 个文件)
        </button>
      </div>

      <div v-if="uploadResult" class="result">
        <p :class="uploadResult.error ? 'err' : 'ok'">{{ uploadResult.message }}</p>
        <p v-if="uploadResult.sessionId" class="session">会话 ID: {{ uploadResult.sessionId }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({ visible: Boolean, sessionId: String })
const emit = defineEmits(['close', 'uploaded'])

const selected = ref([])
const bankName = ref('')
const fileInput = ref(null)
const uploadResult = ref(null)

function icon(name) {
  if (name.endsWith('.ofd')) return '📄'
  if (name.endsWith('.doc') || name.endsWith('.docx')) return '📝'
  if (name.endsWith('.xls') || name.endsWith('.xlsx')) return '📊'
  return '📎'
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function triggerInput() { fileInput.value?.click() }

function onFileChange(e) {
  selected.value = Array.from(e.target.files || [])
}

function onDrop(e) {
  selected.value = Array.from(e.dataTransfer?.files || [])
}

async function upload() {
  if (selected.value.length === 0) return
  const form = new FormData()
  selected.value.forEach(f => form.append('files', f))
  if (bankName.value) form.append('bankName', bankName.value)
  if (props.sessionId) form.append('sessionId', props.sessionId)

  try {
    const res = await fetch('/api/files/upload', { method: 'POST', body: form })
    const data = await res.json()
    uploadResult.value = { message: `成功解析 ${data.filesProcessed} 个文件`, sessionId: data.sessionId }
    setTimeout(() => emit('uploaded', data), 1500)
  } catch (e) {
    uploadResult.value = { error: true, message: '上传失败: ' + e.message }
  }
}
</script>

<style scoped>
.upload-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 100; }
.upload-panel { background: var(--bg-card); border: 1px solid var(--border); border-radius: 16px; padding: 32px; width: 520px; max-height: 80vh; overflow-y: auto; }
.upload-panel h3 { margin-bottom: 6px; }
.upload-panel > p { color: var(--text-secondary); font-size: 13px; margin-bottom: 20px; }
.drop-zone { border: 2px dashed var(--border); border-radius: 12px; padding: 40px; text-align: center; cursor: pointer; color: var(--text-secondary); transition: border-color 0.2s; }
.drop-zone:hover { border-color: var(--accent); }
.file-list { display: flex; flex-direction: column; gap: 8px; }
.file-item { display: flex; align-items: center; gap: 10px; padding: 8px; background: var(--bg-input); border-radius: 8px; }
.fname { flex: 1; text-align: left; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fsize { font-size: 12px; color: var(--text-secondary); }
.upload-actions { display: flex; gap: 10px; margin-top: 20px; align-items: center; }
.bank-input { flex: 1; }
.cancel-btn { background: var(--bg-input); color: var(--text-secondary); }
.confirm-btn { background: var(--accent); color: #000; font-weight: 600; padding: 10px 20px; }
.result { margin-top: 16px; padding: 12px; border-radius: 8px; background: var(--bg-input); font-size: 13px; }
.result .ok { color: var(--success); }
.result .err { color: var(--critical); }
.session { color: var(--text-secondary); margin-top: 4px; font-size: 12px; }
</style>
