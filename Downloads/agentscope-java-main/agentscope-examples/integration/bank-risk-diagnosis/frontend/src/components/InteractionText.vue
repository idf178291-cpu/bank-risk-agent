<template>
  <div class="text-input">
    <input v-if="fieldType === 'number'" type="number" v-model="value" @keydown.enter="submit" :placeholder="'请输入...'" />
    <input v-else-if="fieldType === 'date'" type="date" v-model="value" />
    <textarea v-else v-model="value" @keydown.enter="submit" :placeholder="'请输入...'" rows="2"></textarea>
    <button @click="submit">提交</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const props = defineProps({ fieldType: String })
const emit = defineEmits(['submit'])
const value = ref('')
function submit() { if (value.value) emit('submit', value.value) }
</script>

<style scoped>
.text-input { display: flex; gap: 8px; }
.text-input input, .text-input textarea { flex: 1; }
.text-input button { background: var(--accent); color: #000; font-weight: 600; white-space: nowrap; }
</style>
