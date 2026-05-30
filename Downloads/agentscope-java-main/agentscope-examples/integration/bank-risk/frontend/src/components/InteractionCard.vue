<template>
  <div class="interact-card">
    <div class="ic-label">💬 {{ interaction.question }}</div>
    <InteractionSelect
      v-if="interaction.uiType === 'select'"
      :options="interaction.options"
      :allowOther="interaction.allowOther"
      @submit="onSelect"
    />
    <InteractionMultiSelect
      v-else-if="interaction.uiType === 'multi_select'"
      :options="interaction.options"
      @submit="onSelect"
    />
    <InteractionConfirm
      v-else-if="interaction.uiType === 'confirm'"
      @submit="onSelect"
    />
    <InteractionForm
      v-else-if="interaction.uiType === 'form' && interaction.fields"
      :fields="interaction.fields"
      @submit="onSelect"
    />
    <InteractionText
      v-else
      :uiType="interaction.uiType"
      :defaultValue="interaction.defaultValue"
      @submit="onSelect"
    />
  </div>
</template>

<script setup>
import InteractionSelect from './InteractionSelect.vue'
import InteractionMultiSelect from './InteractionMultiSelect.vue'
import InteractionConfirm from './InteractionConfirm.vue'
import InteractionForm from './InteractionForm.vue'
import InteractionText from './InteractionText.vue'

const props = defineProps({
  interaction: { type: Object, required: true }
})
const emit = defineEmits(['submit'])

function onSelect(value) {
  emit('submit', {
    toolCallId: props.interaction.toolCallId,
    uiType: props.interaction.uiType,
    response: value
  })
}
</script>

<style scoped>
.interact-card {
  background: var(--surface); border: 1px solid var(--border); border-radius: 10px;
  padding: 16px; margin: 8px 0; max-width: 85%;
  border-left: 3px solid var(--blue);
}
.ic-label { font-size: 12px; margin-bottom: 12px; color: var(--text); font-weight: 500; }
</style>
