<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import AppNavBar from '@/components/AppNavBar.vue'
import TabBar from '@/components/TabBar.vue'
import { myOrders } from '@/api/seckill'
import { clearAuth, getProfile, isLogin } from '@/utils/request'
import { formatTime, money, moneyParts } from '@/utils/format'
import type { SeckillOrder } from '@/api/types'

const loading = ref(false)
const logged = ref(false)
const nickname = ref('')
const orders = ref<SeckillOrder[]>([])

const totalAmount = computed(() =>
  money(orders.value.reduce((sum, order) => sum + Number(order.amount ?? 0), 0))
)

const pendingCount = computed(() => orders.value.filter((order) => order.status === 0).length)

const successCount = computed(() => orders.value.filter((order) => order.status === 1).length)

function badgeClass(status: number) {
  if (status === 0) return 'badge--red'
  if (status === 1) return 'badge--acid'
  return 'badge--line'
}

async function load() {
  logged.value = isLogin()
  nickname.value = getProfile()?.nickname || getProfile()?.username || ''

  if (!logged.value) {
    orders.value = []
    return
  }

  loading.value = true
  try {
    orders.value = await myOrders()
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    success: (res) => {
      if (!res.confirm) return
      clearAuth()
      load()
      uni.showToast({ title: '已退出登录', icon: 'none' })
    }
  })
}

function goDetail(order: SeckillOrder) {
  uni.navigateTo({ url: `/pages/seckill/detail?id=${order.activityId}` })
}

onShow(load)

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<template>
  <view class="screen screen--tabbed paper-grain">
    <AppNavBar title="我的订单" subtitle="My Orders">
      <view v-if="logged" class="nav-action" hover-class="nav-action--hover" @tap="handleLogout">
        <text class="nav-action__text">退出</text>
      </view>
    </AppNavBar>

    <view class="screen__body">
      <!-- 未登录 -->
      <view v-if="!logged" class="guest ticket rise">
        <text class="stamp guest__stamp">SIGN IN</text>
        <text class="guest__title">还没登录</text>
        <text class="guest__desc">登录后才能查看你的秒杀订单与抢购记录</text>
        <view class="btn btn--ink guest__btn" hover-class="btn--hover" @tap="goLogin">
          <text>去登录 / 注册</text>
        </view>
      </view>

      <template v-else>
        <!-- 概览 -->
        <view class="summary rise">
          <view class="summary__main">
            <text class="overline">Total Spent / 累计成交</text>
            <view class="summary__figure">
              <text class="summary__cur">¥</text>
              <text class="summary__num num">{{ totalAmount }}</text>
            </view>
          </view>
          <view class="summary__side">
            <view class="summary__cell">
              <text class="summary__cell-num num">{{ orders.length }}</text>
              <text class="summary__cell-label">订单</text>
            </view>
            <view class="summary__cell">
              <text class="summary__cell-num num">{{ pendingCount }}</text>
              <text class="summary__cell-label">待支付</text>
            </view>
            <view class="summary__cell">
              <text class="summary__cell-num num">{{ successCount }}</text>
              <text class="summary__cell-label">已支付</text>
            </view>
          </view>
        </view>

        <view class="list-header">
          <text class="overline">Order List / 订单明细</text>
          <text class="list-header__hint num">{{ nickname }} · {{ orders.length }} 条</text>
        </view>

        <view v-if="loading" class="list">
          <view v-for="n in 3" :key="n" class="skeleton order-skeleton" />
        </view>

        <view v-else-if="orders.length" class="list">
          <view
            v-for="(order, index) in orders"
            :key="order.id"
            class="order ticket rise"
            :style="{ animationDelay: `${index * 60}ms` }"
            @tap="goDetail(order)"
          >
            <view class="order__head">
              <text class="order__no num">NO. {{ order.orderNo }}</text>
              <text class="badge" :class="badgeClass(order.status)">{{ order.statusText }}</text>
            </view>

            <view class="dash-line order__dash" />

            <view class="order__body">
              <view class="order__info">
                <text class="order__goods">{{ order.goodsName || `商品 #${order.goodsId}` }}</text>
                <text class="order__time num">{{ formatTime(order.createTime) }}</text>
              </view>
              <view class="order__amount">
                <text class="order__cur">¥</text>
                <text class="order__int num">{{ moneyParts(order.amount).int }}</text>
                <text class="order__dec num">.{{ moneyParts(order.amount).dec }}</text>
              </view>
            </view>
          </view>
        </view>

        <view v-else class="empty">
          <text class="empty__mark">— 0 —</text>
          <text class="empty__text">还没有任何抢购记录</text>
          <view class="btn btn--ghost empty__btn" hover-class="btn--hover" @tap="() => uni.reLaunch({ url: '/pages/index/index' })">
            <text>去逛逛限量发售</text>
          </view>
        </view>
      </template>
    </view>

    <TabBar current="order" />
  </view>
</template>

<style lang="scss" scoped>
.nav-action {
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  border: 2rpx solid $line-strong;
}

.nav-action--hover {
  background: rgba(22, 21, 15, 0.05);
}

.nav-action__text {
  font-size: 23rpx;
  color: $ink-soft;
}

/* ---------- 未登录 ---------- */
.guest {
  margin-top: 40rpx;
  padding: 60rpx 36rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14rpx;
  background: $card;
}

.guest__stamp {
  transform: rotate(-4deg);
  margin-bottom: 6rpx;
}

.guest__title {
  font-size: 56rpx;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.guest__desc {
  font-size: 25rpx;
  color: $ink-soft;
  line-height: 1.6;
}

.guest__btn {
  width: 100%;
  margin-top: 20rpx;
}

/* ---------- 概览 ---------- */
.summary {
  margin-top: 24rpx;
  padding-bottom: 26rpx;
  border-bottom: 2rpx solid $line-strong;
}

.summary__figure {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
  margin-top: 4rpx;
}

.summary__cur {
  font-size: 32rpx;
  font-weight: 700;
  color: $vermilion;
}

.summary__num {
  font-size: 92rpx;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.04em;
  color: $vermilion;
}

.summary__side {
  display: flex;
  gap: 14rpx;
  margin-top: 22rpx;
}

.summary__cell {
  flex: 1;
  padding: 14rpx 16rpx;
  border-radius: 14rpx;
  border: 2rpx solid $line;
  display: flex;
  flex-direction: column;
  gap: 2rpx;
}

.summary__cell-num {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.15;
}

.summary__cell-label {
  font-size: 20rpx;
  color: $ink-mute;
}

/* ---------- 列表 ---------- */
.list-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 30rpx 0 20rpx;
}

.list-header__hint {
  font-size: 21rpx;
  color: $ink-mute;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
  padding-bottom: 24rpx;
}

.order {
  padding: 24rpx 22rpx;
  background: $card;
}

.order__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.order__no {
  font-size: 22rpx;
  color: $ink-mute;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order__dash {
  margin: 18rpx 0 16rpx;
}

.order__body {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20rpx;
}

.order__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.order__goods {
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1.3;
}

.order__time {
  font-size: 21rpx;
  color: $ink-mute;
}

.order__amount {
  display: flex;
  align-items: baseline;
  gap: 2rpx;
  flex: none;
}

.order__cur {
  font-size: 24rpx;
  font-weight: 700;
  color: $ink;
}

.order__int {
  font-size: 50rpx;
  font-weight: 700;
  line-height: 1;
  color: $ink;
}

.order__dec {
  font-size: 26rpx;
  font-weight: 700;
  color: $ink;
}

.order-skeleton {
  height: 190rpx;
  border-radius: $radius-lg;
}

.empty__btn {
  width: 420rpx;
  margin: 32rpx auto 0;
}

.btn--hover {
  opacity: 0.88;
}
</style>
