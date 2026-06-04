<template>
  <div class="chat-area" ref="scrollRef">
    <div v-if="messages.length === 0" class="welcome">
      <h2>🏦 银行机构风险诊断智能体</h2>
      <p>上传监管检查、审计报告、整改台账等文档，自动完成风险诊断分析。</p>
      <div class="features">
        <span>📄 多格式文档解析</span>
        <span>🔍 问题自动提取</span>
        <span>📊 六大风险分类</span>
        <span>📈 趋势分析</span>
        <span>📝 报告自动生成</span>
      </div>
    </div>
    <div v-for="(msg, i) in messages" :key="i">
      <template v-if="msg.type === 'interaction'">
        <InteractionCard :interaction="msg.interaction" :message-id="msg.id"
          @submit="payload => $emit('submit-interaction', msg.interaction, payload)" />
      </template>
      <template v-else>
        <MessageBubble :message="msg" />
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import MessageBubble from './MessageBubble.vue'
import InteractionCard from './InteractionCard.vue'

const props = defineProps({ messages: Array })
defineEmits(['submit-interaction'])

const scrollRef = ref(null)

watch(() => props.messages?.length, () => {
  nextTick(() => {
    const el = scrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
})
</script>

<style scoped>
.chat-area { flex: 1; overflow-y: auto; padding: 20px 24px; }
.welcome { text-align: center; padding: 60px 20px; }
.welcome h2 { font-size: 22px; margin-bottom: 12px; }
.welcome p { color: var(--text-secondary); margin-bottom: 20px; }
.features { display: flex; justify-content: center; flex-wrap: wrap; gap: 10px; }
.features span { background: var(--bg-card); border: 1px solid var(--border); padding: 8px 16px; border-radius: 20px; font-size: 13px; color: var(--accent); }
</style>
