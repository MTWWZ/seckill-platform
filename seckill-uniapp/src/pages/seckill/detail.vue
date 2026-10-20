<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import AppNavBar from '@/components/AppNavBar.vue'
import CountDown from '@/components/CountDown.vue'
import { activityDetail, orderResult, seckill } from '@/api/seckill'
import { isLogin, BizError } from '@/utils/request'
import { formatTime, money, moneyParts, pad2 } from '@/utils/format'
import { RESULT_STATUS, type SeckillActivity, type SeckillResult } from '@/api/types'

const activityId = ref(0)
const activity = ref<SeckillActivity | null>(null)
const loading = ref(true)
const submitting = ref(false)
/** 当前用户在这一场的既有结果，用来判断"我已抢到/排队中" */
const mine = ref<SeckillResult | null>(null)

const imgFailed = ref(false)

const rules = [
  '同一账号每场限购 1 件，重复下单会被唯一索引直接拦截',
  '秒杀库存与商品总库存分开维护，售完即止',
  '下单请求限流 5 次/秒，脚本刷单会返回 429',
  '订单由消息队列异步生成，抢购后请稍候查看结果'
]

const remainStock = computed(() => activity.value?.redisStock ?? activity.value?.seckillStock ?? 0)
const price = computed(() => moneyParts(activity.value?.seckillPrice))
const soldPercent = computed(() => {
  const total = activity.value?.seckillStock || 0
  if (!total) return 0
  const sold = Math.min(total, Math.max(0, total - remainStock.value))
  return Math.round((sold / total) * 100)
})
const saved = computed(() => {
  const origin = Number(activity.value?.originPrice ?? 0)
  const now = Number(activity.value?.seckillPrice ?? 0)
  return money(Math.max(0, origin - now))
})

/** 按钮形态：用一个明确的模式枚举驱动，避免在模板里堆 if */
const mode = computed<
  'loading' | 'ordered' | 'queuing' | 'ended' | 'upcoming' | 'soldout' | 'ready'
>(() => {
  if (mine.value?.status === RESULT_STATUS.SUCCESS) return 'ordered'
  if (mine.value?.status === RESULT_STATUS.QUEUING) return 'queuing'
  if (!activity.value) return 'loading'
  if (activity.value.statusText === '已结束') return 'ended'
  if (activity.value.statusText === '未开始') return 'upcoming'
  if (remainStock.value <= 0) return 'soldout'
  return 'ready'
})

const cta = computed(() => {
  switch (mode.value) {
    case 'ordered':
      return { label: 'Order Placed', sub: mine.value?.orderNo || '订单已生成', text: '查看我的订单', disabled: false }
    case 'queuing':
      return { label: 'Queuing', sub: '订单正在生成中', text: '查看抢购进度', disabled: false }
    case 'ended':
      return { label: 'Closed', sub: '本场已结束', text: '本场已结束', disabled: true }
    case 'upcoming':
      return { label: 'Upcoming', sub: '活动尚未开始', text: '即将开始', disabled: true }
    case 'soldout':
      return { label: 'Sold Out', sub: '库存已被抢完', text: '已售罄', disabled: true }
    case 'loading':
      return { label: 'Loading', sub: '正在读取活动信息', text: '加载中…', disabled: true }
    default:
      return {
        label: 'Buy Now',
        sub: `￥${money(activity.value?.seckillPrice)} 限时价`,
        text: '立即抢购',
        disabled: false
      }
  }
})

const statusClass = computed(() => {
  if (activity.value?.statusText === '进行中') return 'badge--acid'
  if (activity.value?.statusText === '已结束') return 'badge--line'
  return 'badge--ink'
})

let pollTimer: ReturnType<typeof setInterval> | null = null

async function loadDetail(silent = false) {
  if (!activityId.value) return
  if (!silent) loading.value = true
  try {
    activity.value = await activityDetail(activityId.value)
  } catch {
    // request 层已 toast
  } finally {
    loading.value = false
  }
}

async function loadMine() {
  if (!isLogin() || !activityId.value) return
  try {
    mine.value = await orderResult(activityId.value)
  } catch {
    mine.value = null
  }
}

/** 每 3 秒静默刷新一次：库存变化、以及"未开始 -> 进行中"的自动切换 */
async function poll() {
  if (!activity.value || activity.value.statusText === '已结束') return
  await loadDetail(true)
  if (mode.value === 'queuing') await loadMine()
}

async function handleCta() {
  if (mode.value === 'ordered') {
    uni.reLaunch({ url: '/pages/order/my' })
    return
  }
  if (mode.value === 'queuing') {
    goResult()
    return
  }
  if (cta.value.disabled || submitting.value) return

  if (!isLogin()) {
    uni.showToast({ title: '请先登录再抢购', icon: 'none' })
    setTimeout(() => uni.navigateTo({ url: '/pages/login/login' }), 600)
    return
  }

  submitting.value = true
  try {
    await seckill(activityId.value)
    goResult()
  } catch (err) {
    const error = err as BizError
    if (error.code === 409) {
      uni.showToast({ title: '这一场你已经抢到过了', icon: 'none' })
      goResult()
    } else if (error.code === 429) {
      uni.showToast({ title: '手速太快啦，歇一秒再试', icon: 'none' })
    } else if (error.code === 401) {
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => uni.navigateTo({ url: '/pages/login/login' }), 600)
    } else {
      uni.showToast({ title: error.message || '抢购失败', icon: 'none' })
    }
  } finally {
    submitting.value = false
  }
}

function goResult() {
  uni.redirectTo({ url: `/pages/seckill/result?activityId=${activityId.value}` })
}

onLoad((query) => {
  activityId.value = Number((query as Record<string, string>)?.id ?? 0)
  loadDetail()
  loadMine().then(() => {
    pollTimer = setInterval(poll, 3000)
  })
})

onUnload(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<template>
  <view class="screen paper-grain">
    <AppNavBar back subtitle="Flash Sale" :title="activity?.goodsName || '活动详情'" />

    <view class="screen__body">
      <template v-if="activity">
        <!-- 商品图：不铺满整屏，做成一张带边框的"展品窗口" -->
        <view class="media rise">
          <image
            v-if="activity.goodsImgUrl && !imgFailed"
            class="media__img"
            :src="activity.goodsImgUrl"
            mode="aspectFill"
            @error="imgFailed = true"
          />
          <view v-else class="media__fallback paper-grain">
            <text class="media__fallback-mark num">{{ activity.goodsName?.slice(0, 1) }}</text>
          </view>

          <view class="media__status">
            <text class="badge" :class="statusClass">{{ activity.statusText }}</text>
          </view>
          <view class="media__stamp stamp">限量 {{ activity.seckillStock }} 份</view>
        </view>

        <!-- 价格票根：中间一条虚线撕口，左右信息分栏 -->
        <view class="pticket ticket rise" style="animation-delay: 60ms">
          <view class="pticket__left">
            <text class="overline">Seckill Price / 秒杀价</text>
            <view class="pticket__price">
              <text class="pticket__cur">¥</text>
              <text class="pticket__int num">{{ price.int }}</text>
              <text class="pticket__dec num">.{{ price.dec }}</text>
            </view>
            <text class="pticket__origin num">
              原价 ¥{{ money(activity.originPrice) }} · 立省 ¥{{ saved }}
            </text>
          </view>
          <view class="pticket__split" />
          <view class="pticket__right">
            <text class="overline">Stock</text>
            <text class="pticket__stock num">{{ remainStock }}</text>
            <text class="pticket__stock-sub">剩余份数</text>
          </view>
        </view>

        <!-- 倒计时 -->
        <view class="panel cd-panel rise" style="animation-delay: 110ms">
          <CountDown
            size="lg"
            :start-time="activity.startTime"
            :end-time="activity.endTime"
            :status-text="activity.statusText"
            @finish="loadDetail"
          />
          <text class="cd-panel__hint">
            {{
              activity.statusText === '未开始'
                ? '开始后按钮会自动解锁，无需手动刷新'
                : activity.statusText === '已结束'
                  ? '本场活动已结束'
                  : '库存实时同步自 Redis，页面每 3 秒自动刷新'
            }}
          </text>
        </view>

        <!-- 库存消耗 -->
        <view class="panel meter-panel rise" style="animation-delay: 150ms">
          <view class="meter-panel__head">
            <text class="overline">Inventory / 库存消耗</text>
            <text class="meter-panel__pct num">{{ soldPercent }}%</text>
          </view>
          <view class="meter">
            <view class="meter__fill meter__fill--striped" :style="{ width: `${soldPercent}%` }" />
          </view>
          <view class="meter-panel__foot">
            <text class="num">已抢 {{ activity.seckillStock - remainStock }} 份</text>
            <text class="num">
              {{ activity.redisStock === null ? '尚未预热' : `Redis 实时余量 ${activity.redisStock}` }}
            </text>
          </view>
        </view>

        <!-- 活动信息 -->
        <view class="panel info rise" style="animation-delay: 190ms">
          <text class="overline">Schedule / 活动信息</text>
          <view class="info__row">
            <text class="info__key">商品</text>
            <text class="info__val">{{ activity.goodsName }}</text>
          </view>
          <view v-if="activity.goodsTitle" class="info__row">
            <text class="info__key">卖点</text>
            <text class="info__val">{{ activity.goodsTitle }}</text>
          </view>
          <view class="info__row">
            <text class="info__key">开始</text>
            <text class="info__val num">{{ formatTime(activity.startTime) }}</text>
          </view>
          <view class="info__row">
            <text class="info__key">结束</text>
            <text class="info__val num">{{ formatTime(activity.endTime) }}</text>
          </view>
        </view>

        <!-- 规则 -->
        <view class="panel rules rise" style="animation-delay: 230ms">
          <text class="overline">Rules / 抢购规则</text>
          <view v-for="(rule, index) in rules" :key="index" class="rules__row">
            <text class="rules__no num">{{ pad2(index + 1) }}</text>
            <text class="rules__text">{{ rule }}</text>
          </view>
        </view>
      </template>

      <view v-else-if="loading" class="detail-skeleton">
        <view class="skeleton detail-skeleton__media" />
        <view class="skeleton detail-skeleton__line" />
        <view class="skeleton detail-skeleton__block" />
      </view>
    </view>

    <!-- 底部抢购条 -->
    <view class="cta">
      <view class="cta__info">
        <text class="cta__label overline">{{ cta.label }}</text>
        <text class="cta__sub num">{{ cta.sub }}</text>
      </view>
      <view
        class="btn cta__btn"
        :class="cta.disabled ? 'btn--disabled' : 'btn--red'"
        :hover-class="cta.disabled ? 'none' : 'btn--hover'"
        @tap="handleCta"
      >
        <text>{{ submitting ? '提交中…' : cta.text }}</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.screen {
  padding-bottom: calc(190rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(190rpx + env(safe-area-inset-bottom));
}

/* ---------- 展品窗口 ---------- */
.media {
  position: relative;
  margin-top: 24rpx;
  border-radius: $radius-lg;
  overflow: hidden;
  border: 2rpx solid $line-strong;
  background: $paper-deep;
}

.media__img {
  width: 100%;
  height: 460rpx;
  display: block;
}

.media__fallback {
  height: 460rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $paper-deep;
}

.media__fallback-mark {
  font-size: 160rpx;
  font-weight: 700;
  color: rgba(22, 21, 15, 0.18);
}

.media__status {
  position: absolute;
  left: 18rpx;
  top: 18rpx;
}

.media__stamp {
  position: absolute;
  right: 26rpx;
  bottom: 26rpx;
  background: rgba(244, 241, 234, 0.94);
}

/* ---------- 价格票根 ---------- */
.pticket {
  display: flex;
  align-items: stretch;
  margin-top: 24rpx;
  padding: 26rpx 24rpx;
  background: $card;
}

.pticket__left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.pticket__price {
  display: flex;
  align-items: baseline;
  gap: 2rpx;
}

.pticket__cur {
  font-size: 28rpx;
  font-weight: 700;
  color: $vermilion;
}

.pticket__int {
  font-size: 72rpx;
  font-weight: 700;
  line-height: 1;
  color: $vermilion;
}

.pticket__dec {
  font-size: 32rpx;
  font-weight: 700;
  color: $vermilion;
}

.pticket__origin {
  font-size: 22rpx;
  color: $ink-mute;
}

.pticket__split {
  width: 0;
  border-left: 2rpx dashed $line-strong;
  margin: 0 22rpx;
}

.pticket__right {
  width: 150rpx;
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 2rpx;
}

.pticket__stock {
  font-size: 56rpx;
  font-weight: 700;
  line-height: 1.1;
}

.pticket__stock-sub {
  font-size: 20rpx;
  color: $ink-mute;
}

/* ---------- 倒计时面板 ---------- */
.panel {
  margin-top: 24rpx;
  padding: 26rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.cd-panel__hint {
  font-size: 22rpx;
  color: $ink-mute;
  line-height: 1.6;
}

/* ---------- 库存消耗 ---------- */
.meter-panel__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.meter-panel__pct {
  font-size: 34rpx;
  font-weight: 700;
  color: $vermilion;
}

.meter-panel__foot {
  display: flex;
  justify-content: space-between;
  font-size: 21rpx;
  color: $ink-mute;
}

/* ---------- 活动信息 ---------- */
.info__row {
  display: flex;
  gap: 20rpx;
  align-items: flex-start;
}

.info__key {
  width: 84rpx;
  flex: none;
  font-size: 24rpx;
  color: $ink-mute;
}

.info__val {
  flex: 1;
  font-size: 26rpx;
  color: $ink;
}

/* ---------- 规则 ---------- */
.rules__row {
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
}

.rules__no {
  width: 44rpx;
  flex: none;
  font-size: 22rpx;
  font-weight: 700;
  color: $vermilion;
}

.rules__text {
  flex: 1;
  font-size: 24rpx;
  color: $ink-soft;
  line-height: 1.6;
}

/* ---------- 骨架 ---------- */
.detail-skeleton {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.detail-skeleton__media {
  height: 460rpx;
  border-radius: $radius-lg;
}

.detail-skeleton__line {
  height: 60rpx;
  width: 60%;
}

.detail-skeleton__block {
  height: 160rpx;
  border-radius: $radius;
}

/* ---------- 底部 CTA ---------- */
.cta {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 80;
  background: $paper;
  border-top: 2rpx solid $line-strong;
  padding: 18rpx 32rpx calc(18rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(18rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.cta__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2rpx;
}

.cta__sub {
  font-size: 24rpx;
  color: $ink-soft;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cta__btn {
  width: 300rpx;
  flex: none;
}

.btn--hover {
  opacity: 0.88;
}
</style>
