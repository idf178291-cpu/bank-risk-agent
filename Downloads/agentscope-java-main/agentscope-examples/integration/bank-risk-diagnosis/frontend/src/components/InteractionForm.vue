<template>
  <div>
    <div v-for="field in fields" :key="field.name" class="field">
      <label>{{ field.label || field.name }}</label>
      <input v-if="field.type === 'text' || !field.type" v-model="formData[field.name]" :placeholder="field.placeholder" />
      <select v-else-if="field.type === 'select'" v-model="formData[field.name]">
        <option v-for="o in field.options" :key="o" :value="o">{{ o }}</option>
      </select>
      <textarea v-else-if="field.type === 'textarea'" v-model="formData[field.name]" :placeholder="field.placeholder" rows="3"></textarea>
    </div>
    <button class="submit" @click="emit('submit', JSON.stringify(formData))">提交</button>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
const props = defineProps({ fields: Array })
const emit = defineEmits(['submit'])
const formData = reactive({})
</script>

<style scoped>
.field { margin-bottom: 12px; }
.field label { display: block; font-size: 13px; margin-bottom: 4px; color: var(--text-secondary); }
.field input, .field select, .field textarea { width: 100%; }
.submit { background: var(--accent); color: #000; font-weight: 600; }
</style>
