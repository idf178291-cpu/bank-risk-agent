/**
 * In-house Markdown-to-HTML renderer.
 * Handles: headings, bold, italic, code, tables, lists, blockquotes, rules, risk badges.
 */
export function renderMarkdown(text) {
  if (!text) return ''
  let html = escapeHtml(text)

  // Risk level badges
  html = html.replace(/🔴\s*严重|CRITICAL/gi, '<span class="risk-critical">🔴 严重</span>')
  html = html.replace(/🟠\s*高风险|HIGH/gi, '<span class="risk-high">🟠 高风险</span>')
  html = html.replace(/🟡\s*关注|WATCH/gi, '<span class="risk-watch">🟡 关注</span>')
  html = html.replace(/🟢\s*低风险|LOW/gi, '<span class="risk-low">🟢 低风险</span>')

  // Blockquotes
  html = html.replace(/^&gt;\s?(.+)$/gm, '<blockquote>$1</blockquote>')

  // Headings
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')

  // Horizontal rules
  html = html.replace(/^---$/gm, '<hr>')

  // Bold and italic
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  // Inline code
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')

  // Tables
  html = html.replace(/(\|.+\|\n\|[-| :]+\|\n((?:\|.+\|\n?)*))/g, (match, header, rows) => {
    const headers = header.split('|').filter(c => c.trim()).map(h => `<th>${h.trim()}</th>`).join('')
    const bodyRows = rows.split('\n').filter(r => r.trim())
      .map(r => '<tr>' + r.split('|').filter(c => c.trim()).map(c => `<td>${c.trim()}</td>`).join('') + '</tr>')
      .join('')
    return `<table><thead><tr>${headers}</tr></thead><tbody>${bodyRows}</tbody></table>`
  })

  // Lists
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/((?:<li>.+<\/li>\n?)+)/g, '<ul>$1</ul>')

  // Collapse adjacent blockquotes
  html = html.replace(/<\/blockquote>\n?<blockquote>/g, '<br>')

  // Paragraphs
  const lines = html.split('\n')
  const result = []
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) { result.push('<br>'); continue }
    if (trimmed.startsWith('<')) { result.push(trimmed); continue }
    result.push(`<p>${trimmed}</p>`)
  }
  html = result.join('\n')

  return html
}

function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}
