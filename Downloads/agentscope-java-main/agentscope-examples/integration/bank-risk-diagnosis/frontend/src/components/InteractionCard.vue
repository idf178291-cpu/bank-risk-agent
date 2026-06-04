<template>
  <div class="interaction-card fade-in">
    <div class="iq-header">
      <span class="iq-icon">💬</span>
      <span class="iq-question">{{ interaction.question }}</span>
    </div>
    <InteractionSelect v-if="interaction.uiType === 'select'"
      :options="interaction.options" @submit="v => $emit('submit', v)" />
    <InteractionMultiSelect v-else-if="interaction.uiType === 'multi_select'"
      :options="interaction.options" @submit="v => $emit('submit', v)" />
    <InteractionConfirm v-else-if="interaction.uiType === 'confirm'"
      @submit="v => $emit('submit', v)" />
    <InteractionForm v-else-if="interaction.uiType === 'form'"
      :fields="interaction.fields" @submit="v => $emit('submit', v)" />
    <InteractionText v-else
      :field-type="interaction.uiType" @submit="v => $emit('submit', v)" />
  </div>
</template>

<script setup>
import InteractionSelect from './InteractionSelect.vue'
import InteractionMultiSelect from './InteractionMultiSelect.vue'
import InteractionConfirm from './InteractionConfirm.vue'
import InteractionForm from './InteractionForm.vue'
import InteractionText from './InteractionText.vue'

defineProps({ interaction: Object, messageId: String })
defineEmits(['submit'])
</script>

<style scoped>
.interaction-card {
  border-left: 3px solid var(--accent);
  background: var(--bg-card);
  border-radius: 0 12px 12px 0;
  padding: 16px;
  margin: 12px 0;
}
.iq-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.iq-icon { font-size: 18px; }
.iq-question { font-size: 14px; font-weight: 500; }
</style>
