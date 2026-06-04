<template>
  <RiskSidebar :pipeline-step="pipelineStep" :pipeline-status="pipelineStatus" @quick="onQuick" />
  <div class="main-area">
    <ChatHeader :status="agentStatus" />
    <ChatArea :messages="messages" @submit-interaction="onSubmitInteraction" />
    <ChatInput :is-running="agentStatus === 'running'" @send="onSend" @stop="onStop" @toggle-upload="showUpload = !showUpload" />
  </div>
  <FileUploader :visible="showUpload" :session-id="currentSessionId"
    @close="showUpload = false" @uploaded="onFilesUploaded" />
</template>

<script setup>
import { ref, reactive } from 'vue'
import RiskSidebar from './components/RiskSidebar.vue'
import ChatHeader from './components/ChatHeader.vue'
import ChatArea from './components/ChatArea.vue'
import ChatInput from './components/ChatInput.vue'
import FileUploader from './components/FileUploader.vue'
import { useAguiClient } from './composables/useAguiClient.js'

const { run, abort } = useAguiClient('/agui/run')

const messages = ref([])
const agentStatus = ref('idle')
const showUpload = ref(false)
const pipelineStep = ref(0)
const pipelineStatus = ref('')
const threadId = ref('diag-' + Date.now())
const currentSessionId = ref('')

let messageHistory = []
let pendingInteraction = null
let pendingAssistantMsg = null  // deferred assistant msg + tool results when ask_user is pending
let currentToolCalls = []
let currentToolResults = []
let currentRunTexts = ''
let finishedOnce = false

function onSend(text) {
  const msgId = 'msg-' + Date.now()
  const userMsg = { id: msgId, role: 'user', content: text }
  messageHistory.push(userMsg)
  messages.value.push({ id: msgId, role: 'user', content: text })
  runAgent()
}

function onQuick(text) { onSend(text) }
function onStop() { abort(); agentStatus.value = 'idle' }

function onFilesUploaded(data) {
  showUpload.value = false
  if (data.sessionId) currentSessionId.value = data.sessionId
  const isSupplement = currentSessionId.value && messageHistory.length > 0
  const msg = isSupplement
    ? `补充上传了 ${data.filesProcessed} 个文件，已追加到当前会话（会话ID: ${data.sessionId}）。请用 parse_documents 重新获取全部文档。`
    : `已上传 ${data.filesProcessed} 个文件。会话ID: ${data.sessionId}，银行: ${data.bankName}。请开始风险诊断。`
  const msgId = 'msg-' + Date.now()
  const userMsg = { id: msgId, role: 'user', content: msg }
  messageHistory.push(userMsg)
  messages.value.push({ id: msgId, role: 'user', content: msg })
  runAgent()
}

function runAgent() {
  finishedOnce = false
  agentStatus.value = 'running'
  pipelineStep.value = 0
  pipelineStatus.value = 'RUNNING'
  currentToolCalls = []
  currentToolResults = []
  currentRunTexts = ''

  run(
    { threadId: threadId.value, runId: 'run-' + Date.now(), messages: messageHistory.slice() },
    {
      onRunStarted() { agentStatus.value = 'running' },
      onTextMessageStart(event) {
        const msgId = event.messageId || 'agent-' + Date.now()
        const msg = reactive({ id: msgId, role: 'assistant', content: '' })
        messages.value.push(msg)
        // Track text but don't push to history yet
        const idx = currentToolCalls.length // text segment before tool calls
        currentRunTexts += (event.delta || '')
      },
      onTextContent(event) {
        currentRunTexts += (event.delta || '')
        // Also update the visible message
        const lastMsg = messages.value[messages.value.length - 1]
        if (lastMsg && lastMsg.role === 'assistant') {
          lastMsg.content += (event.delta || '')
        }
      },
      onTextMessageEnd() {},
      onToolCallStart(event) {
        const toolName = event.toolCallName || event.toolName || 'unknown'
        const tc = { id: event.toolCallId || '', name: toolName, arguments: '' }
        if (toolName === 'ask_user') {
          pendingInteraction = { toolCallId: event.toolCallId }
        }
        currentToolCalls.push(tc)
        messages.value.push({
          id: 'tool-start-' + (event.toolCallId || Date.now()), role: 'tool',
          content: '🔧 调用工具: ' + toolName
        })
      },
      onToolCallArgs(event) {
        const tc = currentToolCalls.find(t => t.id === event.toolCallId)
        if (tc) tc.arguments += (event.delta || '')
      },
      onToolCallResult(event) {
        // Skip ask_user results (they never complete normally due to suspend)
        if (!pendingInteraction || pendingInteraction.toolCallId !== event.toolCallId) {
          currentToolResults.push({
            toolCallId: event.toolCallId || '',
            content: event.content || ''
          })
        }
      },
      onToolCallEnd(event) {
        if (pendingInteraction && pendingInteraction.toolCallId === event.toolCallId) {
          // ask_user completed — parse args for UI
          try {
            const tc = currentToolCalls.find(t => t.id === event.toolCallId)
            const args = tc ? JSON.parse(tc.arguments || '{}') : {}
            pendingInteraction = {
              ...pendingInteraction,
              uiType: args.ui_type || 'text',
              question: args.question || '请提供信息',
              options: args.options || [],
              fields: args.fields || []
            }
            messages.value.push({
              type: 'interaction', id: 'int-' + Date.now(),
              interaction: { ...pendingInteraction }
            })
          } catch (e) { console.warn('parse ask_user:', e) }
        }
      },
      onRunFinished() {
        if (finishedOnce) return
        finishedOnce = true

        // DEFER push to messageHistory if ask_user is pending
        // (tool results must follow tool calls atomically)
        if (pendingInteraction) {
          pendingAssistantMsg = {
            id: 'assistant-' + Date.now(),
            role: 'assistant',
            content: currentRunTexts,
            toolCalls: currentToolCalls,
            toolResults: currentToolResults
          }
        } else {
          // No ask_user — push immediately
          flushToHistory(currentRunTexts, currentToolCalls, currentToolResults)
        }

        agentStatus.value = 'idle'
        pipelineStatus.value = 'COMPLETED'
        pipelineStep.value = 8
      },
      onError(msg) {
        agentStatus.value = 'error'
        pipelineStatus.value = 'FAILED'
        messages.value.push({ id: 'err-' + Date.now(), role: 'tool', content: '❌ 错误: ' + String(msg) })
        flushToHistory(currentRunTexts, currentToolCalls, currentToolResults)
      }
    }
  )
}

function flushToHistory(text, toolCalls, toolResults) {
  if (!text && toolCalls.length === 0) return
  const msg = { id: 'assistant-' + Date.now(), role: 'assistant', content: text }
  if (toolCalls.length > 0) {
    msg.toolCalls = toolCalls.map(tc => ({
      id: tc.id, type: 'function',
      function: { name: tc.name, arguments: tc.arguments }
    }))
  }
  messageHistory.push(msg)
  for (const tr of toolResults) {
    messageHistory.push({ id: 'tr-' + Date.now(), role: 'tool', toolCallId: tr.toolCallId, content: tr.content })
  }
}

function onSubmitInteraction(interaction, payload) {
  pendingInteraction = null
  // Remove interaction card from display
  const idx = messages.value.findIndex(m => m.type === 'interaction')
  if (idx >= 0) messages.value.splice(idx, 1)

  const respText = Array.isArray(payload) ? payload.join(', ') : String(payload)
  messages.value.push({ id: 'resp-' + Date.now(), role: 'user', content: '📤 ' + respText })

  // Flush DEFERRED assistant message + tool results FIRST
  if (pendingAssistantMsg) {
    const pam = pendingAssistantMsg
    const msg = { id: pam.id, role: 'assistant', content: pam.content }
    if (pam.toolCalls && pam.toolCalls.length > 0) {
      msg.toolCalls = pam.toolCalls.map(tc => ({
        id: tc.id, type: 'function',
        function: { name: tc.name, arguments: tc.arguments }
      }))
    }
    messageHistory.push(msg)
    for (const tr of (pam.toolResults || [])) {
      messageHistory.push({ id: 'tr-' + Date.now(), role: 'tool', toolCallId: tr.toolCallId, content: tr.content })
    }
    pendingAssistantMsg = null
  }

  // NOW push ask_user tool result
  const toolMsg = { id: 'tr-ask-' + Date.now(), role: 'tool', toolCallId: interaction.toolCallId, content: respText }
  messageHistory.push(toolMsg)

  // Continue the agent run
  runAgent()
}
</script>
