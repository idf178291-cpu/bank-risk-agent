<template>
  <div v-if="visible" class="upload-overlay" @click.self="$emit('close')">
    <div class="upload-panel">
      <h3>上传机构文档</h3>
      <p class="hint">支持 OFD (.ofd)、Word (.doc/.docx)、Excel (.xls/.xlsx)</p>

      <div class="drop-zone" @dragover.prevent @drop.prevent="onDrop" @click="triggerInput">
        <span v-if="selected.length === 0">📂 拖拽文件到此处或点击选择</span>
        <div v-else class="file-list">
          <div v-for="f in selected" :key="f.name" class="file-item">
            <span class="ficon">{{ icon(f.name) }}</span>
            <span class="fname">{{ f.name }}</span>
            <span class="fsize">{{ formatSize(f.size) }}</span>
          </div>
        </div>
      </div>

      <input ref="fileInput" type="file" multiple hidden
        accept=".ofd,.doc,.docx,.xls,.xlsx" @change="onFileChange" />

      <div class="upload-actions">
        <button class="cancel-btn" @click="$emit('close')">取消</button>
        <button class="upload-btn" @click="upload" :disabled="selected.length === 0 || uploading">
          {{ uploading ? '解析中...' : `上传并解析 (${selected.length} 个文件)` }}
        </button>
      </div>

      <div v-if="uploadResult" class="result" :class="uploadResult.error ? 'err' : 'ok'">
        {{ uploadResult.message }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineEmits } from 'vue'

const props = defineProps({
  visible: Boolean,
  sessionId: String
})

const emit = defineEmits(['close', 'uploaded'])

const fileInput = ref(null)
const selected = ref([])
const uploading = ref(false)
const uploadResult = ref(null)

function icon(name) {
  const n = name.toLowerCase()
  if (n.endsWith('.ofd')) return '📄'
  if (n.endsWith('.doc') || n.endsWith('.docx')) return '📝'
  if (n.endsWith('.xls') || n.endsWith('.xlsx')) return '📊'
  return '📎'
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function triggerInput() {
  fileInput.value?.click()
}

function onFileChange(e) {
  selected.value = Array.from(e.target.files || [])
  uploadResult.value = null
}

function onDrop(e) {
  selected.value = Array.from(e.dataTransfer.files || [])
  uploadResult.value = null
}

async function upload() {
  if (selected.value.length === 0) return
  uploading.value = true
  uploadResult.value = null

  const form = new FormData()
  for (const f of selected.value) {
    form.append('files', f)
  }
  if (props.sessionId) {
    form.append('sessionId', props.sessionId)
  }

  try {
    const r = await fetch('/api/files/upload', { method: 'POST', body: form })
    const data = await r.json()
    emit('uploaded', {
      sessionId: data.sessionId,
      totalFiles: data.totalFiles,
      totalCharCount: data.totalCharCount,
      files: data.files
    })
    selected.value = []
    uploading.value = false
  } catch (e) {
    uploadResult.value = { error: true, message: '上传失败: ' + e.message }
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.upload-panel {
  background: #1e2130; border-radius: 12px; padding: 28px; width: 480px; max-width: 90vw;
}
h3 { margin: 0 0 6px; color: #e0e0e0; }
.hint { margin: 0 0 16px; color: #888; font-size: 13px; }
.drop-zone {
  border: 2px dashed #444; border-radius: 8px; padding: 32px;
  text-align: center; cursor: pointer; color: #888; transition: border-color .2s;
}
.drop-zone:hover { border-color: #6c8; }
.file-list { max-height: 160px; overflow-y: auto; }
.file-item {
  display: flex; align-items: center; gap: 8px; padding: 8px;
  border-bottom: 1px solid #333;
}
.ficon { font-size: 20px; }
.fname { flex: 1; color: #ccc; text-align: left; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fsize { color: #666; font-size: 12px; }
.upload-actions { display: flex; gap: 10px; margin-top: 16px; }
.cancel-btn {
  flex: 1; padding: 10px; border: 1px solid #444; background: transparent;
  color: #aaa; border-radius: 6px; cursor: pointer; font-size: 14px;
}
.upload-btn {
  flex: 2; padding: 10px; border: none; background: #4a6cf7; color: white;
  border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 600;
}
.upload-btn:disabled { background: #555; color: #888; cursor: not-allowed; }
.result { margin-top: 12px; padding: 8px 12px; border-radius: 6px; font-size: 13px; }
.result.ok { background: #1a3a1a; color: #6c8; }
.result.err { background: #3a1a1a; color: #e66; }
</style>
