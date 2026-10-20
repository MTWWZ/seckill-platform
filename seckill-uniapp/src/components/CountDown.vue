<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { pad2, parseTime, splitSeconds } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    startTime: string
    endTime: string
    /** 后端算好的状态文案：未开始 / 进行中 / 已结束 */
    statusText: string
    size?: 'sm' | 'lg'
  }>(),
  { size: 'sm' }
)

const emit = defineEmits<{ (e: 'finish'): void }>()

const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

/** 当前处于哪个阶段；'未开始' 到期后会自己切到 running，不需要父组件改文案 */
const phase = computed<'before' | 'running' | 'ended'>(() => {
  if (props.statusText === '已结束') return 'ended'
  if (props.statusText === '未开始' && now.value < parseTime(props.startTime)) return 'before'
  return 'running'
})

const target = computed(() =>
  phase.value === 'before' ? parseTime(props.startTime) : parseTime(props.endTime)
)

const remain = computed(() => Math.max(0, Math.floor((target.value - now.value) / 1000)))

const label = computed(() => {
  if (phase.value === 'ended') return '已结束'
  return phase.value === 'before' ? '距开始' : '距结束'
})

const parts = computed(() => splitSeconds(remain.value))
/** 最后 60 秒进入紧迫态：整块变朱红并轻微跳动 */
const urgent = computed(() => phase.value === 'running' && remain.value > 0 && remain.value <= 60)

let previous = remain.value
watch(remain, (val) => {
  if (val === 0 && previous > 0) emit('finish')
  previous = val
})

function tick() {
  now.value = Date.now()
}

function startTimer() {
  if (timer) return
  timer = setInterval(tick, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

watch(
  () => props.statusText,
  (text) => {
    if (text === '已结束') stopTimer()
    else startTimer()
  }
)

onMounted(() => {
  if (phase.value !== 'ended') startTimer()
})

onUnmounted(stopTimer)
</script>

<template>
  <view class="cd" :class="[`cd--${size}`, { 'cd--urgent': urgent, 'cd--ended': phase === 'ended' }]">
    <text class="cd__label">{{ label }}</text>

    <view v-if="phase !== 'ended'" class="cd__cells">
      <view v-if="parts.d > 0" class="cd__cell">
        <text class="cd__val num">{{ parts.d }}</text>
        <text class="cd__unit">天</text>
      </view>
      <view class="cd__cell">
        <text class="cd__val num">{{ pad2(parts.h) }}</text>
      </view>
      <text class="cd__colon num">:</text>
      <view class="cd__cell">
        <text class="cd__val num">{{ pad2(parts.m) }}</text>
      </view>
      <text class="cd__colon num">:</text>
      <view class="cd__cell">
        <text class="cd__val num">{{ pad2(parts.s) }}</text>
      </view>
    </view>
    <text v-else class="cd__over num">已结束</text>
  </view>
</template>

<style lang="scss" scoped>
.cd {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.cd__label {
  font-size: 22rpx;
  color: $ink-mute;
  flex: none;
}

.cd__cells {
  display: flex;
  align-items: center;
  gap: 4rpx;
}

.cd__cell {
  display: flex;
  align-items: baseline;
  gap: 2rpx;
  padding: 4rpx 10rpx;
  border-radius: 8rpx;
  background: $ink;
}

.cd__val {
  color: $paper;
  font-weight: 700;
  line-height: 1.2;
}

.cd__unit {
  font-size: 18rpx;
  color: rgba(244, 241, 234, 0.7);
}

.cd__colon {
  color: $ink-mute;
  font-weight: 700;
  font-size: 24rpx;
}

.cd__over {
  font-size: 26rpx;
  color: $ink-mute;
  letter-spacing: 0.08em;
}

/* 小号：用在列表卡片里 */
.cd--sm {
  .cd__cell {
    padding: 2rpx 8rpx;
    background: rgba(22, 21, 15, 0.86);
  }

  .cd__val {
    font-size: 24rpx;
  }
}

/* 大号：用在活动详情页，数字放大并换成品红强调 */
.cd--lg {
  gap: 16rpx;

  .cd__label {
    font-size: 26rpx;
  }

  .cd__cells {
    gap: 8rpx;
  }

  .cd__cell {
    padding: 10rpx 18rpx;
    border-radius: 12rpx;
  }

  .cd__val {
    font-size: 52rpx;
    color: $acid;
  }

  .cd__unit {
    font-size: 22rpx;
  }

  .cd__colon {
    font-size: 40rpx;
  }
}

.cd--urgent {
  .cd__cell {
    background: $vermilion;
    animation: urgentPulse 1s ease-in-out infinite;
  }

  .cd__val {
    color: #fff;
  }

  .cd__label {
    color: $vermilion;
  }
}

@keyframes urgentPulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.06);
  }
}
</style>
