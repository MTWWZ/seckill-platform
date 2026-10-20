<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import AppNavBar from '@/components/AppNavBar.vue'
import { activityDetail, orderResult } from '@/api/seckill'
import { BizError, isLogin } from '@/utils/request'
import { money } from '@/utils/format'
import { RESULT_STATUS, type SeckillActivity, type SeckillResult } from '@/api/types'

/** 最多轮询 15 次，之后转成"稍后可在订单页查看"的兜底文案 */
const MAX_ATTEMPTS = 15
const INTERVAL = 1000

const activityId = ref(0)
const result = ref<SeckillResult | null>(null)
const activity = ref<SeckillActivity | null>(null)
const attempts = ref(0)
const polling = ref(false)

let timer: ReturnType<typeof setTimeout> | null = null

const stage = computed<'queuing' | 'success' | 'failed' | 'timeout' | 'unauthorized'>(() => {
  const status = result.value?.status
  if (status === RESULT_STATUS.SUCCESS) return 'success'
  if (status === RESULT_STATUS.FAILED) return 'failed'
  if (status === -1) return 'unauthorized'
  if (attempts.value >= MAX_ATTEMPTS) return 'timeout'
  return 'queuing'
})

const headline = computed(() => {
  switch (stage.value) {
    case 'success':
      return '抢到了'
    case 'failed':
      return '没抢到'
    case 'timeout':
      return '仍在处理'
    case 'unauthorized':
      return '登录失效'
    default:
      return '排队中'
  }
})

const describe = computed(() => {
  switch (stage.value) {
    case 'success':
      return '订单已由消息队列异步生成，可在"我的订单"中查看'
    case 'failed':
      return result.value?.message || '本场库存已被抢完，下次再试试'
    case 'timeout':
      return '消息还在消费中，可以稍后到"我的订单"确认结果'
    case 'unauthorized':
      return '登录已过期，请重新登录后再查看结果'
    default:
      return result.value?.message || '已进入队列，正在等待订单生成'
  }
})

const progressPercent = computed(() => Math.min(100, Math.round((attempts.value / MAX_ATTEMPTS) * 100)))

const stockLeft = computed(() => activity.value?.redisStock ?? activity.value?.seckillStock ?? null)

async function poll() {
  if (polling.value) return
  polling.value = true
  attempts.value += 1
  try {
    result.value = await orderResult(activityId.value)
    if (result.value.status === RESULT_STATUS.SUCCESS || result.value.status === RESULT_STATUS.FAILED) {
      stop()
      return
    }
  } catch (err) {
    if ((err as BizError).code === 401) {
      stop()
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      result.value = { activityId: activityId.value, orderNo: null, status: -1, statusText: '未登录', message: '登录已过期' }
      return
    }
    // 其它错误先忽略，下一轮继续
  } finally {
    polling.value = false
  }

  if (attempts.value >= MAX_ATTEMPTS) {
    stop()
    return
  }
  timer = setTimeout(poll, INTERVAL)
}

function stop() {
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
}

function manualRefresh() {
  stop()
  attempts.value = 0
  result.value = null
  poll()
}

function goOrders() {
  uni.reLaunch({ url: '/pages/order/my' })
}

function goHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

onLoad((query) => {
  activityId.value = Number((query as Record<string, string>)?.activityId ?? 0)

  if (!isLogin()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(goLogin, 600)
    return
  }

  // 活动信息只是为了在结果页显示商品名与价格，取不到也不影响主流程
  activityDetail(activityId.value)
    .then((detail) => (activity.value = detail))
    .catch(() => undefined)

  poll()
})

onUnload(stop)
</script>

<template>
  <view class="screen paper-grain">
    <AppNavBar back title="抢购结果" subtitle="Async Order" />

    <view class="screen__body">
      <!-- 状态票根 -->
      <view class="result ticket rise" :class="`result--${stage}`">
        <view class="result__top">
          <text class="overline">Order Status / 抢购状态</text>
          <text class="badge" :class="{
            'badge--acid': stage === 'success',
            'badge--red': stage === 'failed',
            'badge--ink': stage === 'timeout' || stage === 'unauthorized',
            'badge--line': stage === 'queuing'
          }">{{ result?.statusText || '排队中' }}</text>
        </view>

        <text class="result__headline">{{ headline }}</text>
        <text class="result__desc">{{ describe }}</text>

        <!-- 排队中：转动的虚线圆环 + 轮询次数，让等待这件事有形状 -->
        <view v-if="stage === 'queuing'" class="ring-wrap">
          <view class="ring">
            <view class="ring__inner">
              <text class="ring__num num">{{ attempts }}</text>
              <text class="ring__label">次查询</text>
            </view>
          </view>
        </view>

        <view v-if="stage === 'queuing'" class="progress">
          <view class="progress__bar">
            <view class="progress__fill" :style="{ width: `${progressPercent}%` }" />
          </view>
          <text class="progress__text num">{{ progressPercent }}%</text>
        </view>

        <view class="dash-line result__dash" />

        <view class="facts">
          <view v-if="result?.orderNo" class="facts__row">
            <text class="facts__key">订单号</text>
            <text class="facts__val num">{{ result.orderNo }}</text>
          </view>
          <view class="facts__row">
            <text class="facts__key">活动</text>
            <text class="facts__val">{{ activity?.goodsName || `#${activityId}` }}</text>
          </view>
          <view v-if="activity" class="facts__row">
            <text class="facts__key">成交价</text>
            <text class="facts__val num">¥{{ money(activity.seckillPrice) }}</text>
          </view>
          <view v-if="stockLeft !== null" class="facts__row">
            <text class="facts__key">剩余库存</text>
            <text class="facts__val num">{{ stockLeft }} 份</text>
          </view>
        </view>
      </view>

      <!-- 说明 -->
      <view class="note panel rise" style="animation-delay: 70ms">
        <text class="overline">How it works / 发生了什么</text>
        <text class="note__text">
          你的抢购请求先在 Redis 里用 Lua 脚本原子扣减库存，扣减成功才把消息投进 RabbitMQ，
          订单由消费者异步落库 —— 所以这里会先看到"排队中"，再变成最终结果。
        </text>
      </view>
    </view>

    <!-- 底部操作 -->
    <view class="cta">
      <template v-if="stage === 'success'">
        <view class="btn btn--ghost cta__btn" hover-class="btn--hover" @tap="goHome">
          <text>继续逛</text>
        </view>
        <view class="btn btn--red cta__btn" hover-class="btn--hover" @tap="goOrders">
          <text>查看我的订单</text>
        </view>
      </template>
      <template v-else-if="stage === 'unauthorized'">
        <view class="btn btn--ink cta__btn cta__btn--full" hover-class="btn--hover" @tap="goLogin">
          <text>去登录</text>
        </view>
      </template>
      <template v-else-if="stage === 'queuing'">
        <view class="btn btn--ghost cta__btn" hover-class="btn--hover" @tap="goHome">
          <text>先回去看看</text>
        </view>
        <view class="btn btn--ink cta__btn" hover-class="btn--hover" @tap="manualRefresh">
          <text>立即再查一次</text>
        </view>
      </template>
      <template v-else>
        <view class="btn btn--ink cta__btn" hover-class="btn--hover" @tap="manualRefresh">
          <text>重新查询</text>
        </view>
        <view class="btn btn--ghost cta__btn" hover-class="btn--hover" @tap="goHome">
          <text>返回活动列表</text>
        </view>
      </template>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.screen {
  padding-bottom: calc(190rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(190rpx + env(safe-area-inset-bottom));
}

.result {
  margin-top: 24rpx;
  padding: 28rpx 26rpx;
  background: $card;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.result__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result__headline {
  font-size: 76rpx;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.03em;
  margin-top: 8rpx;
}

.result--success .result__headline {
  color: $vermilion;
}

.result--failed .result__headline,
.result--timeout .result__headline {
  color: $ink;
}

.result__desc {
  font-size: 24rpx;
  color: $ink-soft;
  line-height: 1.65;
}

.result__dash {
  margin: 18rpx 0 16rpx;
}

/* ---------- 等待动画 ---------- */
.ring-wrap {
  display: flex;
  justify-content: center;
  padding: 30rpx 0 10rpx;
}

.ring {
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  border: 4rpx dashed $line-strong;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: spin 9s linear infinite;
}

.ring__inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: spinBack 9s linear infinite;
}

.ring__num {
  font-size: 76rpx;
  font-weight: 700;
  line-height: 1;
  color: $vermilion;
}

.ring__label {
  font-size: 22rpx;
  color: $ink-mute;
  margin-top: 4rpx;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes spinBack {
  to {
    transform: rotate(-360deg);
  }
}

/* ---------- 进度 ---------- */
.progress {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 22rpx;
}

.progress__bar {
  flex: 1;
  height: 14rpx;
  border-radius: 999rpx;
  background: $paper-deep;
  overflow: hidden;
}

.progress__fill {
  height: 100%;
  border-radius: 999rpx;
  background: $ink;
  transition: width 0.4s ease;
}

.progress__text {
  font-size: 22rpx;
  color: $ink-mute;
  flex: none;
}

/* ---------- 事实清单 ---------- */
.facts {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.facts__row {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
}

.facts__key {
  width: 130rpx;
  flex: none;
  font-size: 23rpx;
  color: $ink-mute;
}

.facts__val {
  flex: 1;
  font-size: 25rpx;
  word-break: break-all;
}

/* ---------- 说明 ---------- */
.note {
  margin-top: 24rpx;
  padding: 26rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.note__text {
  font-size: 24rpx;
  color: $ink-soft;
  line-height: 1.7;
}

/* ---------- 底部 ---------- */
.cta {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 80;
  background: $paper;
  border-top: 2rpx solid $line-strong;
  padding: 18rpx 32rpx;
  padding-bottom: calc(18rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(18rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 20rpx;
}

.cta__btn {
  flex: 1;
}

.cta__btn--full {
  width: 100%;
}

.btn--hover {
  opacity: 0.88;
}
</style>
