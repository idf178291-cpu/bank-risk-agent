<template>
  <div class="chat-area" ref="chatRef">
    <template v-for="msg in messages" :key="msg.id">
      <template v-if="msg.type === 'interaction'">
        <InteractionCard
          v-if="msg.interaction"
          :interaction="msg.interaction"
          @submit="onInteractionSubmit"
        />
      </template>
      <template v-else>
        <MessageBubble :message="msg" />
      </template>
    </template>
    <div v-if="messages.length === 0" class="empty-hint">
      输入企业名称关键词开始风险查询...
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import MessageBubble from './MessageBubble.vue'
import InteractionCard from './InteractionCard.vue'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  pendingInteraction: { type: Object, default: null }
})
const emit = defineEmits(['submit-interaction'])

const chatRef = ref(null)

watch(() => props.messages.length, async () => {
  await nextTick()
  if (chatRef.value) {
    chatRef.value.scrollTop = chatRef.value.scrollHeight
  }
})

function onInteractionSubmit(payload) {
  emit('submit-interaction', payload)
}
</script>

<style scoped>
.chat-area {
  flex: 1; overflow-y: auto; padding: 20px 24px;
  display: flex; flex-direction: column; gap: 14px;
}
.empty-hint {
  color: var(--text-muted); font-size: 13px; text-align: center; margin: auto;
}
</style>
