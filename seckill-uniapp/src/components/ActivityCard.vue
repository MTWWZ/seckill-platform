<script setup lang="ts">
import { computed, ref } from 'vue'
import CountDown from '@/components/CountDown.vue'
import { money, moneyParts } from '@/utils/format'
import type { SeckillActivity } from '@/api/types'

const props = defineProps<{ activity: SeckillActivity; index?: number }>()
const emit = defineEmits<{ (e: 'tap'): void; (e: 'refresh'): void }>()

const imgFailed = ref(false)

const price = computed(() => moneyParts(props.activity.seckillPrice))
const initial = computed(() => props.activity.goodsName?.slice(0, 1) ?? '限')

/** 剩余库存优先看 Redis 实时值，没预热就退回活动初始库存 */
const remainStock = computed(() => props.activity.redisStock ?? props.activity.seckillStock)
const soldPercent = computed(() => {
  const total = props.activity.seckillStock || 0
  if (!total) return 0
  const sold = Math.min(total, Math.max(0, total - remainStock.value))
  return Math.round((sold / total) * 100)
})

const badgeClass = computed(() => {
  if (props.activity.statusText === '进行中') return 'badge--acid'
  if (props.activity.statusText === '已结束') return 'badge--line'
  return 'badge--ink'
})

const soldOut = computed(() => remainStock.value <= 0)
</script>

<template>
  <view
    class="ac ticket rise"
    hover-class="ac--hover"
    :style="{ animationDelay: `${(index ?? 0) * 70}ms` }"
    @tap="emit('tap')"
  >
    <view class="ac__media">
      <image
        v-if="activity.goodsImgUrl && !imgFailed"
        class="ac__img"
        :src="activity.goodsImgUrl"
        mode="aspectFill"
        @error="imgFailed = true"
      />
      <view v-else class="ac__fallback paper-grain">
        <text class="ac__fallback-mark num">{{ initial }}</text>
      </view>

      <view class="ac__status">
        <text class="badge" :class="badgeClass">{{ activity.statusText }}</text>
      </view>
      <view v-if="soldOut" class="ac__soldout">
        <text class="ac__soldout-text">已售罄</text>
      </view>
    </view>

    <view class="ac__body">
      <view class="ac__head">
        <text class="ac__name">{{ activity.goodsName }}</text>
        <text v-if="activity.goodsTitle" class="ac__title">{{ activity.goodsTitle }}</text>
      </view>

      <view class="ac__price">
        <text class="ac__cur">¥</text>
        <text class="ac__int num">{{ price.int }}</text>
        <text class="ac__dec num">.{{ price.dec }}</text>
        <text class="ac__origin num">原价 ¥{{ money(activity.originPrice) }}</text>
      </view>

      <view class="ac__meter">
        <view class="meter">
          <view
            class="meter__fill meter__fill--striped"
            :style="{ width: `${soldPercent}%` }"
          />
        </view>
        <view class="ac__meter-meta">
          <text class="ac__left num">余 {{ remainStock }} / {{ activity.seckillStock }}</text>
          <text class="ac__pct num">{{ soldPercent }}% 被抢</text>
        </view>
      </view>

      <CountDown
        class="ac__cd"
        size="sm"
        :start-time="activity.startTime"
        :end-time="activity.endTime"
        :status-text="activity.statusText"
        @finish="emit('refresh')"
      />
    </view>
  </view>
</template>

<style lang="scss" scoped>
.ac {
  display: flex;
  padding: 20rpx;
  gap: 22rpx;
  background: $card;
}

.ac--hover {
  background: #fbf9f3;
}

.ac__media {
  position: relative;
  width: 220rpx;
  height: 220rpx;
  flex: none;
  border-radius: 18rpx;
  overflow: hidden;
  background: $paper-deep;
}

.ac__img {
  width: 100%;
  height: 100%;
  display: block;
}

.ac__fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $paper-deep;
}

.ac__fallback-mark {
  font-size: 80rpx;
  color: rgba(22, 21, 15, 0.22);
  font-weight: 700;
}

.ac__status {
  position: absolute;
  left: 10rpx;
  top: 10rpx;
}

.ac__soldout {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(22, 21, 15, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ac__soldout-text {
  font-size: 28rpx;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.16em;
}

.ac__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4rpx 0;
}

.ac__head {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.ac__name {
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.3;
}

.ac__title {
  font-size: 22rpx;
  color: $ink-mute;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ac__price {
  display: flex;
  align-items: baseline;
  gap: 2rpx;
}

.ac__cur {
  font-size: 24rpx;
  color: $vermilion;
  font-weight: 700;
}

.ac__int {
  font-size: 46rpx;
  color: $vermilion;
  font-weight: 700;
  line-height: 1;
}

.ac__dec {
  font-size: 26rpx;
  color: $vermilion;
  font-weight: 700;
}

.ac__origin {
  margin-left: 12rpx;
  font-size: 22rpx;
  color: $ink-mute;
  text-decoration: line-through;
}

.ac__meter {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.ac__meter-meta {
  display: flex;
  justify-content: space-between;
}

.ac__left,
.ac__pct {
  font-size: 20rpx;
  color: $ink-mute;
}

.ac__cd {
  margin-top: 2rpx;
}
</style>
