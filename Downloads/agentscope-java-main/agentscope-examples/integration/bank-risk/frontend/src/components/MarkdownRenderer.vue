<template>
  <div class="markdown-body" v-html="renderedHtml"></div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  content: { type: String, required: true }
})

function escapeHtml(text) {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

function renderMarkdown(text) {
  if (!text) return ''
  let html = escapeHtml(text)

  // Risk level badges
  html = html.replace(/\*\*(低风险|关注|高风险|严重)\*\*/g, (_, level) => {
    const colors = {
      '低风险': 'var(--risk-low)',
      '关注': 'var(--risk-watch)',
      '高风险': 'var(--risk-high)',
      '严重': 'var(--risk-critical)'
    }
    return '<span style="display:inline-block;padding:2px 8px;border-radius:4px;font-size:11px;'
      + 'font-weight:600;color:' + colors[level] + ';background:' + colors[level]
      + '15;border:1px solid ' + colors[level] + '40;">' + level + '</span>'
  })

  // Status badges
  html = html.replace(/\*\*(✓ 达标|✗ 不达标|✗ 超标|✗ 不足)\*\*/g, (_, status) => {
    const isPass = status.includes('✓')
    return '<strong style="color:' + (isPass ? 'var(--emerald)' : 'var(--rose)') + ';">'
      + status + '</strong>'
  })

  // Headers
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1 style="font-size:18px;margin:16px 0 8px;">$1</h1>')

  // Bold and italic
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  // Code
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')

  // Horizontal rules
  html = html.replace(/^---$/gm, '<hr>')

  // Blockquotes
  html = html.replace(/^&gt; (.+)$/gm, '<blockquote>$1</blockquote>')

  // Unordered lists
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/((?:<li>.*<\/li>\n?)+)/g, '<ul>$1</ul>')

  // Tables
  const lines = html.split('\n')
  let inTable = false; let tableHtml = ''; let headerHtml = ''; let afterHeader = false
  const result = []
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim()
    if (line.startsWith('|') && line.endsWith('|')) {
      if (!inTable) {
        inTable = true
        headerHtml = '<tr>' + line.split('|').filter(c => c.trim())
            .map(c => '<th>' + c.trim() + '</th>').join('') + '</tr>'
      } else if (line.match(/^\|[-:\s|]+\|$/)) {
        afterHeader = true
      } else {
        if (afterHeader && headerHtml) {
          result.push('<table>' + headerHtml)
          headerHtml = ''
          afterHeader = false
        }
        result.push('<tr>' + line.split('|').filter(c => c.trim())
            .map(c => '<td>' + c.trim() + '</td>').join('') + '</tr>')
      }
    } else {
      if (inTable) {
        result.push('</table>')
        inTable = false; headerHtml = ''; afterHeader = false
      }
      result.push(line)
    }
  }
  if (inTable) result.push('</table>')
  html = result.join('\n')

  html = html.replace(/\n\n/g, '<br><br>')

  return html
}

const renderedHtml = computed(() => renderMarkdown(props.content))
</script>

<style scoped>
.markdown-body :deep(table) {
  width: 100%; border-collapse: collapse; margin: 8px 0; font-size: 11px;
}
.markdown-body :deep(th) {
  background: var(--input-bg); padding: 7px 10px; text-align: left;
  color: var(--text-secondary); font-weight: 600; border-bottom: 1px solid var(--border);
}
.markdown-body :deep(td) {
  padding: 7px 10px; border-bottom: 1px solid var(--border);
}
.markdown-body :deep(h2) { font-size: 16px; margin: 12px 0 8px; }
.markdown-body :deep(h3) { font-size: 14px; margin: 10px 0 6px; }
.markdown-body :deep(hr) { border: none; border-top: 1px solid var(--border); margin: 12px 0; }
.markdown-body :deep(ul, ol) { padding-left: 20px; margin: 6px 0; }
.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--blue); padding-left: 12px;
  color: var(--text-secondary); margin: 8px 0;
}
.markdown-body :deep(code) {
  background: var(--input-bg); padding: 2px 6px; border-radius: 4px; font-size: 10px;
}
</style>
