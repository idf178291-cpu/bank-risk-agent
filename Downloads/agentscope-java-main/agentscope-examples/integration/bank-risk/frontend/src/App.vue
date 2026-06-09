<template>
  <div class="app">
    <RiskSidebar @upload-doc="showUploader = true" />
    <div class="main">
      <ChatHeader :status="agentStatus" />
      <ChatArea
        :messages="displayMessages"
        @submit-interaction="onSubmitInteraction" />
      <ChatInput
        ref="chatInputRef"
        :disabled="isRunning"
        @send="sendMessage"
        @stop="stopRun"
        @upload-doc="showUploader = true" />
    </div>
    <FileUploader
      :visible="showUploader"
      :session-id="docSessionId"
      @close="showUploader = false"
      @uploaded="onFilesUploaded" />
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useAguiClient } from './composables/useAguiClient.js'
import RiskSidebar from './components/RiskSidebar.vue'
import ChatHeader from './components/ChatHeader.vue'
import ChatArea from './components/ChatArea.vue'
import ChatInput from './components/ChatInput.vue'
import FileUploader from './components/FileUploader.vue'

const { isRunning, run, abort } = useAguiClient('/agui/run')
const chatInputRef = ref(null)
const agentStatus = ref('idle')
const messages = reactive([])
const pendingInteraction = ref(null)
const currentMessageId = ref(null)
const currentAssistantContent = ref('')
const tempArgs = ref('')

let threadId = 'bankrisk-' + Date.now()
let messageHistory = []
let assistedMsgIndex = -1
let currentToolCalls = []
let currentRunTexts = []
let currentToolResults = []
let pendingAssistantMsg = null
let finishedOnce = false

const showUploader = ref(false)
const docSessionId = ref(null)

const displayMessages = computed(() => messages)

function onFilesUploaded(data) {
  showUploader.value = false
  docSessionId.value = data.sessionId
  const names = data.files.map(f => f.fileName).join('、')
  sendMessage('已上传 ' + data.totalFiles + ' 个文件: ' + names + ', sessionId: ' + data.sessionId)
}

async function sendMessage(text) {
  if (isRunning.value) return

  const msgId = 'msg-' + Date.now()
  const userMsg = { id: msgId, role: 'user', content: text }
  messageHistory.push(userMsg)
  messages.push({ id: msgId, role: 'user', content: text })

  await runAgent()
}

async function runAgent() {
  isRunning.value = true
  agentStatus.value = 'running'
  if (chatInputRef.value) chatInputRef.value.setRunning(true)
  currentMessageId.value = null
  currentAssistantContent.value = ''
  tempArgs.value = ''
  pendingInteraction.value = null
  assistedMsgIndex = -1
  currentToolCalls.length = 0
  finishedOnce = false

  try {
    await run(
      {
        threadId: threadId,
        runId: 'run-' + Date.now(),
        messages: messageHistory
      },
      {
        onRunStarted: () => {
          currentAssistantContent.value = ''
          assistedMsgIndex = -1
          currentToolCalls.length = 0
          currentRunTexts.length = 0
          currentToolResults.length = 0
        },
        onTextMessageStart: (messageId) => {
          currentMessageId.value = messageId
          currentAssistantContent.value = ''
          assistedMsgIndex = -1
        },
        onTextContent: (delta) => {
          if (!delta) return
          if (assistedMsgIndex === -1) {
            const msgId = currentMessageId.value || 'agent-' + Date.now()
            messages.push({ id: msgId, role: 'agent', content: '' })
            assistedMsgIndex = messages.length - 1
          }
          messages[assistedMsgIndex].content += delta
          currentAssistantContent.value += delta
        },
        onTextMessageEnd: (messageId) => {
          // Accumulate text segments for this run
          const content = currentAssistantContent.value
          if (content) {
            currentRunTexts.push(content)
          }
          assistedMsgIndex = -1
        },
        onToolCallStart: (toolCallId, toolName) => {
          assistedMsgIndex = -1
          currentToolCalls.push({ id: toolCallId, name: toolName, arguments: '' })
          if (toolName === 'ask_user') {
            pendingInteraction.value = {
              toolCallId, uiType: null, question: '',
              options: [], fields: [], allowOther: false, defaultValue: null
            }
            tempArgs.value = ''
            messages.push(
              { id: 'tool-' + toolCallId, role: 'tool', content: '🔧 等待用户输入...' })
          } else {
            messages.push(
              { id: 'tool-' + toolCallId, role: 'tool', content: '🔧 调用工具: ' + toolName })
          }
        },
        onToolCallArgs: (toolCallId, delta) => {
          // Track args for tool call history
          const tc = currentToolCalls.find(t => t.id === toolCallId)
          if (tc) tc.arguments += (delta || '')
          // Track args for ask_user interaction
          if (pendingInteraction.value
              && pendingInteraction.value.toolCallId === toolCallId) {
            tempArgs.value += (delta || '')
          }
        },
        onToolCallResult: (toolCallId, content) => {
          // Capture tool result for non-ask_user tools
          if (!pendingInteraction.value
              || pendingInteraction.value.toolCallId !== toolCallId) {
            currentToolResults.push({ toolCallId, content })
          }
        },
        onToolCallEnd: (toolCallId) => {
          if (pendingInteraction.value
              && pendingInteraction.value.toolCallId === toolCallId) {
            try {
              const args = JSON.parse(tempArgs.value)
              pendingInteraction.value = {
                ...pendingInteraction.value,
                uiType: args.ui_type || 'text',
                question: args.question || '请提供信息',
                options: args.options || [],
                fields: args.fields || [],
                allowOther: args.allow_other || false,
                defaultValue: args.default_value || null
              }
              messages.push({
                id: 'interact-' + toolCallId,
                type: 'interaction',
                interaction: { ...pendingInteraction.value }
              })
              // Don't remove from messageHistory — wait for tool result
            } catch (e) {
              pendingInteraction.value = {
                ...pendingInteraction.value,
                uiType: 'text', question: '请提供信息'
              }
              messages.push({
                id: 'interact-' + toolCallId,
                type: 'interaction',
                interaction: { ...pendingInteraction.value }
              })
            }
          }
        },
        onError: (error) => {
          messages.push(
            { id: 'err-' + Date.now(), role: 'tool', content: '❌ 错误: ' + String(error) })
        },
        onRunFinished: () => {
          // Guard against double-call (SSE RUN_FINISHED + finally block)
          // Both carry the same result; ignore if already processed
          if (finishedOnce) return
          finishedOnce = true
          const fullText = currentRunTexts.join('')
          // Defer pushing to messageHistory if ask_user is pending:
          // tool results must always follow tool calls in the same batch
          if (pendingInteraction.value) {
            pendingAssistantMsg = {
              id: currentMessageId.value || 'agent-' + Date.now(),
              role: 'assistant',
              content: fullText,
              toolCalls: currentToolCalls.slice(),
              toolResults: currentToolResults.slice()
            }
          } else {
            if (fullText || currentToolCalls.length > 0) {
              const msg = {
                id: currentMessageId.value || 'agent-' + Date.now(),
                role: 'assistant',
                content: fullText
              }
              if (currentToolCalls.length > 0) {
                msg.toolCalls = currentToolCalls.map(tc => ({
                  id: tc.id,
                  function: { name: tc.name, arguments: tc.arguments }
                }))
              }
              messageHistory.push(msg)
            }
            for (const tr of currentToolResults) {
              messageHistory.push({
                id: 'tr-' + Date.now(),
                role: 'tool',
                toolCallId: tr.toolCallId,
                content: tr.content
              })
            }
          }
          currentRunTexts.length = 0
          currentToolCalls.length = 0
          currentToolResults.length = 0
          if (!pendingInteraction.value) finishRun()
        }
      }
    )
  } catch (error) {
    if (error.name !== 'AbortError') {
      messages.push(
        { id: 'err-' + Date.now(), role: 'tool', content: '❌ 请求失败: ' + error.message })
    }
    finishRun()
  }
}

function finishRun() {
  isRunning.value = false
  agentStatus.value = 'idle'
  if (chatInputRef.value) chatInputRef.value.setRunning(false)
}

function onSubmitInteraction(payload) {
  if (!pendingInteraction.value) return

  const { toolCallId, response } = payload
  const respText = Array.isArray(response) ? response.join(', ') : String(response)

  // Flush pending assistant message + tool results deferred from onRunFinished
  if (pendingAssistantMsg) {
    messageHistory.push({
      id: pendingAssistantMsg.id,
      role: 'assistant',
      content: pendingAssistantMsg.content,
      toolCalls: pendingAssistantMsg.toolCalls.length > 0
        ? pendingAssistantMsg.toolCalls.map(tc => ({
          id: tc.id,
          function: { name: tc.name, arguments: tc.arguments }
        }))
        : undefined
    })
    for (const tr of pendingAssistantMsg.toolResults) {
      messageHistory.push({
        id: 'tr-' + Date.now(),
        role: 'tool',
        toolCallId: tr.toolCallId,
        content: tr.content
      })
    }
    pendingAssistantMsg = null
  }

  const toolMsg = {
    id: 'tool-' + Date.now(),
    role: 'tool',
    toolCallId: toolCallId,
    content: respText
  }
  messageHistory.push(toolMsg)
  messages.push({ id: toolMsg.id, role: 'user', content: '📤 ' + respText })

  const idx = messages.findIndex(m => m.id === 'interact-' + toolCallId)
  if (idx !== -1) messages.splice(idx, 1)

  pendingInteraction.value = null
  tempArgs.value = ''

  runAgent()
}

function stopRun() {
  abort()
  pendingInteraction.value = null
  tempArgs.value = ''
  finishRun()
}
</script>

<style>
.main {
  flex: 1; display: flex; flex-direction: column; min-width: 0;
}
</style>
