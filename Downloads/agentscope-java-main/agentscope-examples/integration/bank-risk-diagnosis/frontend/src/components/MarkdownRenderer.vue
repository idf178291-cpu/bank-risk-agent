<template>
  <div></div>
</template>

<script>
export default { name: 'MarkdownRenderer' }
</script>

<style>
h1 { font-size: 20px; font-weight: 700; margin: 16px 0 8px; }
h2 { font-size: 17px; font-weight: 600; margin: 14px 0 6px; }
h3 { font-size: 15px; font-weight: 600; margin: 12px 0 4px; }
p { margin: 6px 0; line-height: 1.6; }
code { background: var(--bg-input); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
blockquote { border-left: 3px solid var(--accent); padding: 8px 14px; margin: 10px 0; background: rgba(34,211,238,0.05); border-radius: 0 8px 8px 0; }
table { width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 13px; }
th, td { padding: 8px 12px; border: 1px solid var(--border); text-align: left; }
th { background: var(--bg-input); font-weight: 600; }
ul { padding-left: 20px; margin: 8px 0; }
li { margin: 4px 0; }
hr { border: none; border-top: 1px solid var(--border); margin: 16px 0; }
</style>
