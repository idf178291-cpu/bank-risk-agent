<template>
  <div class="form-fields">
    <template v-for="field in fields" :key="field.name || field.label">
      <label>{{ field.label || field.name }}{{ field.required ? ' *' : '' }}</label>
      <select
        v-if="field.type === 'select'"
        v-model="formData[field.name]"
      >
        <option v-for="o in (field.options || [])" :key="o" :value="o">{{ o }}</option>
      </select>
      <textarea
        v-else-if="field.type === 'textarea'"
        v-model="formData[field.name]"
        :placeholder="field.placeholder || ''"
        rows="3"
      ></textarea>
      <input
        v-else
        :type="field.type || 'text'"
        v-model="formData[field.name]"
        :placeholder="field.placeholder || ''"
        :min="field.min"
        :max="field.max"
        :step="field.step"
      />
    </template>
  </div>
  <button class="interact-submit" @click="submit">提交</button>
</template>

<script setup>
import { reactive } from 'vue'

const props = defineProps({
  fields: { type: Array, default: () => [] }
})
const emit = defineEmits(['submit'])

const formData = reactive({})

function submit() {
  emit('submit', JSON.stringify({ ...formData }))
}
</script>

<style scoped>
.form-fields label {
  display: block; font-size: 10px; color: var(--text-muted); margin: 6px 0 2px;
}
.form-fields input, .form-fields select, .form-fields textarea {
  width: 100%; padding: 6px 8px; background: var(--input-bg); border: 1px solid var(--border);
  border-radius: 6px; color: var(--text); font-family: inherit; font-size: 11px;
}
.form-fields input:focus, .form-fields select:focus, .form-fields textarea:focus {
  outline: none; border-color: var(--amber);
}
.interact-submit {
  margin-top: 10px; padding: 8px 20px; border-radius: 6px; border: none;
  background: var(--amber); color: #020617; font-weight: 600;
  font-family: inherit; font-size: 11px; cursor: pointer;
}
.interact-submit:hover { background: #f59e0b; }
</style>
