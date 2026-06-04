<template>
  <div class="pipeline-progress">
    <h4>诊断流水线</h4>
    <div :class="['progress-bar', status === 'RUNNING' ? 'active' : '']">
      <div class="fill" :style="{ width: progress + '%' }"></div>
    </div>
    <div class="steps">
      <div v-for="(step, i) in steps" :key="i" :class="['step', stepClass(i + 1)]">
        <span class="step-num">{{ i + 1 }}</span>
        <span class="step-name">{{ step }}</span>
        <span v-if="i + 1 === currentStep && status === 'RUNNING'" class="spinner">⏳</span>
        <span v-else-if="i + 1 < currentStep" class="check">✅</span>
        <span v-else class="pending">○</span>
      </div>
    </div>
    <div v-if="status === 'COMPLETED'" class="done">✅ 诊断完成</div>
    <div v-if="status === 'FAILED'" class="failed">❌ 诊断失败</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ currentStep: Number, status: String })

const steps = ['文档解析', '问题提取', '问题归并', '风险分类', '重点风险', '趋势分析', '建议生成', '报告生成']
const stepCount = steps.length

const progress = computed(() => props.currentStep ? (props.currentStep / stepCount * 100) : 0)

function stepClass(step) {
  if (step < props.currentStep) return 'done'
  if (step === props.currentStep && props.status === 'RUNNING') return 'current'
  return ''
}
</script>

<style scoped>
.pipeline-progress { margin-top: auto; padding-top: 16px; border-top: 1px solid var(--border); }
.pipeline-progress h4 { font-size: 12px; color: var(--text-secondary); text-transform: uppercase; margin-bottom: 10px; }
.progress-bar { height: 4px; background: var(--bg-input); border-radius: 2px; margin-bottom: 12px; overflow: hidden; }
.progress-bar .fill { height: 100%; background: var(--accent); transition: width 0.5s ease; border-radius: 2px; }
.progress-bar.active .fill { animation: pulse 2s infinite; }
.steps { display: flex; flex-direction: column; gap: 4px; }
.step { display: flex; align-items: center; gap: 8px; font-size: 12px; padding: 4px 0; }
.step-num { width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 10px; background: var(--bg-input); font-weight: 600; }
.step.current .step-num { background: var(--accent); color: #000; }
.step.done .step-num { background: var(--success); }
.step-name { flex: 1; }
.step.done .step-name { color: var(--text-secondary); text-decoration: line-through; }
.step.current .step-name { color: var(--accent); font-weight: 600; }
.spinner { font-size: 12px; }
.check, .pending { font-size: 10px; color: var(--text-secondary); }
.done { color: var(--success); font-size: 13px; font-weight: 600; margin-top: 8px; }
.failed { color: var(--critical); font-size: 13px; font-weight: 600; margin-top: 8px; }
</style>
