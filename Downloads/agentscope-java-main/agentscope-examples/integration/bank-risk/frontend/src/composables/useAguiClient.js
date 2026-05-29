import { ref } from 'vue'

export function useAguiClient(endpoint) {
  const isRunning = ref(false)
  let abortController = null

  async function run(input, callbacks = {}) {
    isRunning.value = true
    abortController = new AbortController()

    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify(input),
        signal: abortController.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status} ${response.statusText}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        let delim = '\n\n'
        let idx = buffer.indexOf(delim)
        if (idx === -1) {
          delim = '\r\n\r\n'
          idx = buffer.indexOf(delim)
        }

        while (idx !== -1) {
          const message = buffer.substring(0, idx)
          buffer = buffer.substring(idx + delim.length)

          const lines = message.split(/\r?\n/)
          for (const line of lines) {
            if (line.startsWith('data:')) {
              try {
                const jsonStr =
                    line.startsWith('data: ') ? line.substring(6) : line.substring(5)
                const event = JSON.parse(jsonStr)
                dispatchEvent(event, callbacks)
              } catch (e) {
                console.warn('Failed to parse SSE event:', line, e)
              }
            }
          }

          idx = buffer.indexOf('\n\n')
          if (idx === -1) {
            idx = buffer.indexOf('\r\n\r\n')
            delim = '\r\n\r\n'
          } else {
            delim = '\n\n'
          }
        }
      }

      if (buffer.trim()) {
        const lines = buffer.split(/\r?\n/)
        for (const line of lines) {
          if (line.startsWith('data:')) {
            try {
              const jsonStr =
                  line.startsWith('data: ') ? line.substring(6) : line.substring(5)
              const event = JSON.parse(jsonStr)
              dispatchEvent(event, callbacks)
            } catch (e) {
              console.warn('Failed to parse remaining event:', line, e)
            }
          }
        }
      }
    } catch (error) {
      if (error.name !== 'AbortError') {
        callbacks.onError?.(error)
      }
    } finally {
      reader?.releaseLock()
      abortController = null
      isRunning.value = false
      callbacks.onRunFinished?.()
    }
  }

  function dispatchEvent(event, callbacks) {
    if (!event || !event.type) return

    switch (event.type) {
      case 'RUN_STARTED':
        callbacks.onRunStarted?.(event.threadId, event.runId)
        break
      case 'RUN_FINISHED':
        callbacks.onRunFinished?.(event.threadId, event.runId)
        break
      case 'TEXT_MESSAGE_START':
        callbacks.onTextMessageStart?.(event.messageId, event.role)
        break
      case 'TEXT_MESSAGE_CONTENT':
        callbacks.onTextContent?.(event.delta || '', event.messageId)
        break
      case 'TEXT_MESSAGE_END':
        callbacks.onTextMessageEnd?.(event.messageId)
        break
      case 'TOOL_CALL_START':
        callbacks.onToolCallStart?.(event.toolCallId, event.toolCallName)
        break
      case 'TOOL_CALL_ARGS':
        callbacks.onToolCallArgs?.(event.toolCallId, event.delta)
        break
      case 'TOOL_CALL_END':
        callbacks.onToolCallEnd?.(event.toolCallId)
        break
      case 'STATE_SNAPSHOT':
        callbacks.onStateSnapshot?.(event.snapshot)
        break
      case 'STATE_DELTA':
        callbacks.onStateDelta?.(event.delta)
        break
      case 'RAW':
        if (event.rawEvent?.error) {
          callbacks.onError?.(event.rawEvent.error)
        } else {
          callbacks.onRawEvent?.(event.rawEvent)
        }
        break
    }
  }

  function abort() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    isRunning.value = false
  }

  return { isRunning, run, abort }
}
