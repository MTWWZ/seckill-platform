<script setup lang="ts">
const props = defineProps<{ current: 'index' | 'goods' | 'order' }>()

const items = [
  { key: 'index', en: 'DROP', zh: '抢购', path: '/pages/index/index' },
  { key: 'goods', en: 'GOODS', zh: '商品', path: '/pages/goods/list' },
  { key: 'order', en: 'ORDERS', zh: '订单', path: '/pages/order/my' }
] as const

function go(item: (typeof items)[number]) {
  if (item.key === props.current) return
  uni.reLaunch({ url: item.path })
}
</script>

<template>
  <view class="tabbar">
    <view class="tabbar__hairline" />
    <view class="tabbar__row">
      <view
        v-for="item in items"
        :key="item.key"
        class="tabbar__item"
        :class="{ 'tabbar__item--on': item.key === current }"
        hover-class="tabbar__item--hover"
        @tap="go(item)"
      >
        <view class="tabbar__mark" />
        <text class="tabbar__en">{{ item.en }}</text>
        <text class="tabbar__zh">{{ item.zh }}</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 90;
  background: $paper;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

.tabbar__hairline {
  height: 2rpx;
  background: $line-strong;
}

.tabbar__row {
  display: flex;
}

.tabbar__item {
  flex: 1;
  padding: 16rpx 0 14rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rpx;
}

.tabbar__item--hover {
  background: rgba(22, 21, 15, 0.04);
}

/* 选中态：顶部一小段酸黄色横杠 + 墨黑文字，不用图标也不显单调 */
.tabbar__mark {
  width: 0;
  height: 6rpx;
  border-radius: 3rpx;
  background: $acid;
  margin-bottom: 8rpx;
  transition: width 0.22s cubic-bezier(0.2, 0.7, 0.3, 1);
}

.tabbar__item--on .tabbar__mark {
  width: 44rpx;
}

.tabbar__en {
  font-family: $font-num;
  font-size: 24rpx;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: $ink-mute;
  line-height: 1.3;
}

.tabbar__zh {
  font-size: 20rpx;
  color: $ink-mute;
  line-height: 1.3;
}

.tabbar__item--on .tabbar__en {
  color: $ink;
}

.tabbar__item--on .tabbar__zh {
  color: $ink-soft;
}
</style>
