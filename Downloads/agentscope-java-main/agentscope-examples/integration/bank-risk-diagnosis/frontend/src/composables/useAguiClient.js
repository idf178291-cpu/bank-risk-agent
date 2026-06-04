import { ref } from 'vue'

export function useAguiClient(endpoint) {
  const isRunning = ref(false)
  let abortController = null

  async function run(input, callbacks = {}) {
    isRunning.value = true
    abortController = new AbortController()
    let reader

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
        throw new Error(`HTTP ${response.status}`)
      }

      reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (line.startsWith('data:')) {
            try {
              const jsonStr = line.startsWith('data: ') ? line.substring(6) : line.substring(5)
              const event = JSON.parse(jsonStr)
              dispatchEvent(event, callbacks)
            } catch (e) { /* skip */ }
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
        callbacks.onRunStarted?.(event)
        break
      case 'RUN_FINISHED':
        callbacks.onRunFinished?.(event)
        break
      case 'TEXT_MESSAGE_START':
        callbacks.onTextMessageStart?.(event)
        break
      case 'TEXT_MESSAGE_CONTENT':
        callbacks.onTextContent?.(event)
        break
      case 'TEXT_MESSAGE_END':
        callbacks.onTextMessageEnd?.(event)
        break
      case 'TOOL_CALL_START':
        callbacks.onToolCallStart?.(event)
        break
      case 'TOOL_CALL_ARGS':
        callbacks.onToolCallArgs?.(event)
        break
      case 'TOOL_CALL_RESULT':
        callbacks.onToolCallResult?.(event)
        break
      case 'TOOL_CALL_END':
        callbacks.onToolCallEnd?.(event)
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
