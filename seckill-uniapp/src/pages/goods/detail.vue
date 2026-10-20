<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppNavBar from '@/components/AppNavBar.vue'
import CountDown from '@/components/CountDown.vue'
import { goodsDetail } from '@/api/goods'
import { pageActivity } from '@/api/seckill'
import { money, moneyParts } from '@/utils/format'
import type { Goods, SeckillActivity } from '@/api/types'

const STATUS_WEIGHT: Record<string, number> = { 进行中: 0, 未开始: 1, 已结束: 2 }

const goodsId = ref(0)
const goods = ref<Goods | null>(null)
/** 该商品关联的秒杀活动，优先挑"进行中"的那一场 */
const activity = ref<SeckillActivity | null>(null)
const loading = ref(true)
const imgFailed = ref(false)

const price = computed(() => moneyParts(goods.value?.originPrice))
const remainStock = computed(() => activity.value?.redisStock ?? activity.value?.seckillStock ?? null)

const activityStatusClass = computed(() => {
  if (activity.value?.statusText === '进行中') return 'badge--acid'
  if (activity.value?.statusText === '已结束') return 'badge--line'
  return 'badge--ink'
})

function pickActivity(list: SeckillActivity[], id: number): SeckillActivity | null {
  const matched = list.filter((item) => item.goodsId === id)
  if (!matched.length) return null
  return [...matched].sort(
    (a, b) => (STATUS_WEIGHT[a.statusText] ?? 3) - (STATUS_WEIGHT[b.statusText] ?? 3)
  )[0]
}

async function load() {
  loading.value = true
  try {
    const [detail, page] = await Promise.all([goodsDetail(goodsId.value), pageActivity(1, 100)])
    goods.value = detail
    activity.value = pickActivity(page.records, goodsId.value)
  } catch {
    // request 层已 toast
  } finally {
    loading.value = false
  }
}

function goActivity() {
  if (!activity.value) return
  uni.navigateTo({ url: `/pages/seckill/detail?id=${activity.value.id}` })
}

onLoad((query) => {
  goodsId.value = Number((query as Record<string, string>)?.id ?? 0)
  load()
})
</script>

<template>
  <view class="screen paper-grain" :class="{ 'screen--cta': !!activity }">
    <AppNavBar back title="商品详情" subtitle="Product" />

    <view class="screen__body">
      <template v-if="goods">
        <view class="media rise">
          <image
            v-if="goods.imgUrl && !imgFailed"
            class="media__img"
            :src="goods.imgUrl"
            mode="aspectFill"
            @error="imgFailed = true"
          />
          <view v-else class="media__fallback paper-grain">
            <text class="media__fallback-mark num">{{ goods.name?.slice(0, 1) }}</text>
          </view>
          <view v-if="goods.category" class="media__cat">
            <text class="badge badge--acid">{{ goods.category }}</text>
          </view>
        </view>

        <!-- 名称与价格 -->
        <view class="head rise" style="animation-delay: 60ms">
          <text class="overline">No. {{ goods.id }} / 商品编号</text>
          <text class="head__name">{{ goods.name }}</text>
          <text v-if="goods.title" class="head__title">{{ goods.title }}</text>

          <view class="head__price">
            <text class="head__cur">¥</text>
            <text class="head__int num">{{ price.int }}</text>
            <text class="head__dec num">.{{ price.dec }}</text>
          </view>
        </view>

        <!-- 三个数据格 -->
        <view class="stats rise" style="animation-delay: 100ms">
          <view class="stats__cell">
            <text class="stats__label overline">Stock</text>
            <text class="stats__value num">{{ goods.stock }}</text>
            <text class="stats__unit">商品库存</text>
          </view>
          <view class="stats__cell">
            <text class="stats__label overline">Category</text>
            <text class="stats__value stats__value--text">{{ goods.category || '未分类' }}</text>
            <text class="stats__unit">所属分类</text>
          </view>
          <view class="stats__cell">
            <text class="stats__label overline">Status</text>
            <text class="stats__value stats__value--text">{{ goods.status === 1 ? '在售' : '已下架' }}</text>
            <text class="stats__unit">上架状态</text>
          </view>
        </view>

        <!-- 限时抢购入口 -->
        <view v-if="activity" class="flash ticket rise" style="animation-delay: 140ms">
          <view class="flash__head">
            <text class="overline">Flash Sale / 限时抢购</text>
            <text class="badge" :class="activityStatusClass">{{ activity.statusText }}</text>
          </view>

          <view class="flash__body">
            <view class="flash__price">
              <text class="flash__cur">¥</text>
              <text class="flash__int num">{{ moneyParts(activity.seckillPrice).int }}</text>
              <text class="flash__dec num">.{{ moneyParts(activity.seckillPrice).dec }}</text>
              <text class="flash__origin num">¥{{ money(activity.originPrice) }}</text>
            </view>
            <CountDown
              size="sm"
              :start-time="activity.startTime"
              :end-time="activity.endTime"
              :status-text="activity.statusText"
              @finish="load"
            />
          </view>

          <view class="dash-line flash__dash" />

          <view class="flash__foot">
            <text class="flash__foot-text num">
              限量 {{ activity.seckillStock }} 份 ·
              {{ remainStock === null ? '待预热' : `实时余量 ${remainStock}` }}
            </text>
            <view class="btn btn--red flash__btn" hover-class="btn--hover" @tap="goActivity">
              <text>去抢购</text>
            </view>
          </view>
        </view>

        <view v-else class="panel quiet rise" style="animation-delay: 140ms">
          <text class="overline">Flash Sale</text>
          <text class="quiet__text">这件商品当前没有进行中的秒杀活动，可以先去活动页看看其他限量发售。</text>
          <view class="btn btn--ghost quiet__btn" hover-class="btn--hover" @tap="() => uni.reLaunch({ url: '/pages/index/index' })">
            <text>去活动页</text>
          </view>
        </view>

        <!-- 描述 -->
        <view v-if="goods.description" class="panel desc rise" style="animation-delay: 180ms">
          <text class="overline">Description / 商品描述</text>
          <text class="desc__text">{{ goods.description }}</text>
        </view>
      </template>

      <view v-else-if="loading" class="detail-skeleton">
        <view class="skeleton detail-skeleton__media" />
        <view class="skeleton detail-skeleton__line" />
        <view class="skeleton detail-skeleton__block" />
      </view>
    </view>

    <!-- 有活动时才出现底部条 -->
    <view v-if="activity" class="cta">
      <view class="cta__info">
        <text class="cta__label overline">Seckill</text>
        <text class="cta__sub num">￥{{ money(activity.seckillPrice) }} · 限量 {{ activity.seckillStock }} 份</text>
      </view>
      <view class="btn btn--red cta__btn" hover-class="btn--hover" @tap="goActivity">
        <text>立即抢购</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.screen--cta {
  padding-bottom: calc(190rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(190rpx + env(safe-area-inset-bottom));
}

/* ---------- 主图 ---------- */
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
  height: 560rpx;
  display: block;
}

.media__fallback {
  height: 560rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $paper-deep;
}

.media__fallback-mark {
  font-size: 190rpx;
  font-weight: 700;
  color: rgba(22, 21, 15, 0.16);
}

.media__cat {
  position: absolute;
  left: 18rpx;
  bottom: 18rpx;
}

/* ---------- 标题 ---------- */
.head {
  margin-top: 30rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.head__name {
  font-size: 54rpx;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.head__title {
  font-size: 25rpx;
  color: $ink-mute;
}

.head__price {
  display: flex;
  align-items: baseline;
  gap: 2rpx;
  margin-top: 10rpx;
}

.head__cur {
  font-size: 30rpx;
  font-weight: 700;
}

.head__int {
  font-size: 76rpx;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.03em;
}

.head__dec {
  font-size: 32rpx;
  font-weight: 700;
}

/* ---------- 数据格 ---------- */
.stats {
  display: flex;
  gap: 14rpx;
  margin-top: 26rpx;
}

.stats__cell {
  flex: 1;
  padding: 18rpx 16rpx;
  border-radius: 14rpx;
  border: 2rpx solid $line;
  display: flex;
  flex-direction: column;
  gap: 2rpx;
  min-width: 0;
}

.stats__label {
  font-size: 17rpx;
}

.stats__value {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.2;
}

.stats__value--text {
  font-size: 30rpx;
}

.stats__unit {
  font-size: 19rpx;
  color: $ink-mute;
}

/* ---------- 限时抢购 ---------- */
.flash {
  margin-top: 26rpx;
  padding: 26rpx 24rpx;
  background: $card;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.flash__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.flash__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  flex-wrap: wrap;
}

.flash__price {
  display: flex;
  align-items: baseline;
  gap: 2rpx;
}

.flash__cur {
  font-size: 26rpx;
  font-weight: 700;
  color: $vermilion;
}

.flash__int {
  font-size: 60rpx;
  font-weight: 700;
  line-height: 1;
  color: $vermilion;
}

.flash__dec {
  font-size: 28rpx;
  font-weight: 700;
  color: $vermilion;
}

.flash__origin {
  margin-left: 12rpx;
  font-size: 22rpx;
  color: $ink-mute;
  text-decoration: line-through;
}

.flash__dash {
  margin: 2rpx 0;
}

.flash__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.flash__foot-text {
  flex: 1;
  font-size: 22rpx;
  color: $ink-mute;
}

.flash__btn {
  width: 220rpx;
  height: 78rpx;
  flex: none;
  font-size: 28rpx;
}

/* ---------- 无活动 ---------- */
.quiet {
  margin-top: 26rpx;
  padding: 26rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.quiet__text {
  font-size: 24rpx;
  color: $ink-soft;
  line-height: 1.65;
}

.quiet__btn {
  width: 260rpx;
  height: 78rpx;
  font-size: 27rpx;
}

/* ---------- 描述 ---------- */
.desc {
  margin-top: 26rpx;
  padding: 26rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.desc__text {
  font-size: 25rpx;
  color: $ink-soft;
  line-height: 1.75;
}

/* ---------- 骨架 ---------- */
.detail-skeleton {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.detail-skeleton__media {
  height: 560rpx;
  border-radius: $radius-lg;
}

.detail-skeleton__line {
  height: 56rpx;
  width: 55%;
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
  padding: 18rpx 32rpx;
  padding-bottom: calc(18rpx + constant(safe-area-inset-bottom));
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
  width: 280rpx;
  flex: none;
}

.btn--hover {
  opacity: 0.88;
}
</style>
