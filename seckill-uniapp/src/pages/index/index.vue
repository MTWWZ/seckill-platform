<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import AppNavBar from '@/components/AppNavBar.vue'
import TabBar from '@/components/TabBar.vue'
import ActivityCard from '@/components/ActivityCard.vue'
import { pageActivity } from '@/api/seckill'
import { getProfile, isLogin } from '@/utils/request'
import { pad2 } from '@/utils/format'
import type { SeckillActivity } from '@/api/types'

const SEGMENTS = ['进行中', '未开始', '已结束'] as const
type Segment = (typeof SEGMENTS)[number]

const loading = ref(true)
const activities = ref<SeckillActivity[]>([])
const segment = ref<Segment>('进行中')

const logged = ref(isLogin())
const nickname = ref(getProfile()?.nickname || getProfile()?.username || '')

const list = computed(() => activities.value.filter((item) => item.statusText === segment.value))

function countOf(status: Segment): number {
  return activities.value.filter((item) => item.statusText === status).length
}

const runningCount = computed(() => countOf('进行中'))

async function load() {
  loading.value = true
  try {
    const page = await pageActivity(1, 50)
    activities.value = page.records
    // 如果当前分段没有数据，自动切到有数据的分段，避免进来就是空屏
    if (!activities.value.some((item) => item.statusText === segment.value)) {
      const fallback = SEGMENTS.find((s) => activities.value.some((item) => item.statusText === s))
      if (fallback) segment.value = fallback
    }
  } catch {
    // 错误提示由 request 层统一 toast
  } finally {
    loading.value = false
  }
}

function openDetail(activity: SeckillActivity) {
  uni.navigateTo({ url: `/pages/seckill/detail?id=${activity.id}` })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function goOrders() {
  uni.reLaunch({ url: '/pages/order/my' })
}

onShow(() => {
  logged.value = isLogin()
  nickname.value = getProfile()?.nickname || getProfile()?.username || ''
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<template>
  <view class="screen screen--tabbed paper-grain">
    <AppNavBar title="限时抢购" subtitle="Flash Sale">
      <view v-if="logged" class="nav-user" hover-class="nav-user--hover" @tap="goOrders">
        <text class="nav-user__name">{{ nickname }}</text>
        <view class="nav-user__dot" />
      </view>
      <view v-else class="nav-login" hover-class="nav-login--hover" @tap="goLogin">
        <text class="nav-login__text">登录</text>
      </view>
    </AppNavBar>

    <view class="screen__body">
      <!-- Hero：不放整块深色，只用超大数字 + 两段细线做版式骨架 -->
      <view class="hero rise">
        <view class="hero__top">
          <text class="overline">Flash Sale / 限时抢购</text>
          <view class="stamp hero__stamp">LIMITED</view>
        </view>

        <view class="hero__figure">
          <text class="hero__num num">{{ pad2(runningCount) }}</text>
          <view class="hero__figure-side">
            <text class="hero__unit">场</text>
            <text class="hero__unit-sub">正在进行</text>
            <view class="hero__bar" />
          </view>
        </view>

        <view class="hero__meta">
          <text class="hero__meta-item num">即将开始 {{ countOf('未开始') }}</text>
          <view class="hero__sep" />
          <text class="hero__meta-item num">已结束 {{ countOf('已结束') }}</text>
          <view class="hero__sep" />
          <text class="hero__meta-item num">总计 {{ activities.length }}</text>
        </view>
      </view>

      <!-- 分段 -->
      <view class="segs">
        <view
          v-for="item in SEGMENTS"
          :key="item"
          class="segs__item"
          :class="{ 'segs__item--on': item === segment }"
          @tap="segment = item"
        >
          <text class="segs__text">{{ item }}</text>
          <text class="segs__count num">{{ countOf(item) }}</text>
        </view>
      </view>

      <!-- 列表 -->
      <view class="list">
        <template v-if="loading">
          <view v-for="n in 3" :key="n" class="skeleton-row panel">
            <view class="skeleton skeleton-row__media" />
            <view class="skeleton-row__body">
              <view class="skeleton skeleton-row__line skeleton-row__line--lg" />
              <view class="skeleton skeleton-row__line" />
              <view class="skeleton skeleton-row__line skeleton-row__line--sm" />
            </view>
          </view>
        </template>

        <template v-else-if="list.length">
          <ActivityCard
            v-for="(activity, index) in list"
            :key="activity.id"
            :activity="activity"
            :index="index"
            @tap="openDetail(activity)"
            @refresh="load"
          />
        </template>

        <view v-else class="empty">
          <text class="empty__mark">— —</text>
          <text class="empty__text">这个分段暂时没有活动</text>
        </view>
      </view>
    </view>

    <TabBar current="index" />
  </view>
</template>

<style lang="scss" scoped>
.nav-user {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  border: 2rpx solid $line-strong;
  max-width: 220rpx;
}

.nav-user--hover {
  background: rgba(22, 21, 15, 0.05);
}

.nav-user__name {
  font-size: 24rpx;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-user__dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: $acid-deep;
  flex: none;
}

.nav-login {
  padding: 8rpx 22rpx;
  border-radius: 999rpx;
  background: $ink;
}

.nav-login--hover {
  opacity: 0.85;
}

.nav-login__text {
  font-size: 24rpx;
  color: $paper;
  font-weight: 600;
  letter-spacing: 0.06em;
}

/* ---------- Hero ---------- */
.hero {
  padding: 36rpx 0 28rpx;
  border-bottom: 2rpx solid $line-strong;
  position: relative;
}

.hero__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero__stamp {
  transform: rotate(6deg);
}

.hero__figure {
  display: flex;
  align-items: flex-end;
  gap: 20rpx;
  margin-top: 12rpx;
}

.hero__num {
  font-size: 168rpx;
  font-weight: 700;
  line-height: 0.86;
  letter-spacing: -0.04em;
}

.hero__figure-side {
  padding-bottom: 10rpx;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.hero__unit {
  font-size: 30rpx;
  font-weight: 700;
}

.hero__unit-sub {
  font-size: 22rpx;
  color: $ink-mute;
}

.hero__bar {
  width: 96rpx;
  height: 8rpx;
  border-radius: 4rpx;
  background: $acid;
  margin-top: 6rpx;
}

.hero__meta {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 24rpx;
}

.hero__meta-item {
  font-size: 22rpx;
  color: $ink-soft;
}

.hero__sep {
  width: 2rpx;
  height: 20rpx;
  background: $line-strong;
}

/* ---------- 分段 ---------- */
.segs {
  display: flex;
  gap: 12rpx;
  margin: 28rpx 0 22rpx;
}

.segs__item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 22rpx;
  border-radius: 999rpx;
  border: 2rpx solid $line-strong;
  transition: all 0.18s ease;
}

.segs__item--on {
  background: $ink;
  border-color: $ink;
}

.segs__text {
  font-size: 26rpx;
  color: $ink-soft;
}

.segs__item--on .segs__text {
  color: $paper;
  font-weight: 600;
}

.segs__count {
  font-size: 22rpx;
  color: $ink-mute;
}

.segs__item--on .segs__count {
  color: $acid;
}

/* ---------- 列表 ---------- */
.list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding-bottom: 24rpx;
}

.skeleton-row {
  display: flex;
  gap: 22rpx;
  padding: 20rpx;
  background: $card;
}

.skeleton-row__media {
  width: 220rpx;
  height: 220rpx;
  flex: none;
  border-radius: 18rpx;
}

.skeleton-row__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  padding-top: 8rpx;
}

.skeleton-row__line {
  height: 26rpx;
  width: 60%;
}

.skeleton-row__line--lg {
  height: 34rpx;
  width: 80%;
}

.skeleton-row__line--sm {
  width: 40%;
}
</style>
