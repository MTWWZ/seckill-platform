<script setup lang="ts">
import { useNavBar } from '@/utils/navbar'

withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
    /** 左侧显示返回箭头，否则显示品牌标记 */
    back?: boolean
    /** 透明底：内容需要压在大图上时用 */
    transparent?: boolean
    /** 是否给底部加一条撕线 */
    hairline?: boolean
  }>(),
  { title: '', subtitle: '', back: false, transparent: false, hairline: true }
)

const metrics = useNavBar()

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.reLaunch({ url: '/pages/index/index' })
  }
}
</script>

<template>
  <!-- holder 占住高度，fixed 层浮在上面；整体下移方案见 utils/navbar.ts -->
  <view class="nav-holder" :style="{ height: metrics.total + 'px' }">
    <view
      class="nav"
      :class="{ 'nav--transparent': transparent, 'nav--solid': !transparent }"
      :style="{ paddingTop: metrics.top + 'px' }"
    >
      <view class="nav__inner" :style="{ height: metrics.height + 'px' }">
        <view v-if="back" class="nav__side nav__side--tap" hover-class="nav__side--hover" @tap="goBack">
          <view class="nav__chev" />
        </view>
        <view v-else class="nav__side">
          <view class="nav__brand">
            <view class="nav__brand-bar" />
            <view class="nav__brand-bar nav__brand-bar--short" />
          </view>
        </view>

        <view class="nav__titles">
          <text v-if="subtitle" class="nav__subtitle overline">{{ subtitle }}</text>
          <text class="nav__title">{{ title }}</text>
        </view>

        <view class="nav__right">
          <slot />
        </view>
      </view>
      <view v-if="hairline && !transparent" class="nav__hairline" />
    </view>
  </view>
</template>

<style lang="scss" scoped>
.nav-holder {
  width: 100%;
}

.nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

.nav--solid {
  background: $paper;
}

.nav--transparent {
  background: transparent;
}

.nav__inner {
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  gap: 16rpx;
}

.nav__side {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  border-radius: 12rpx;
}

.nav__side--hover {
  background: rgba(22, 21, 15, 0.06);
}

.nav__chev {
  width: 22rpx;
  height: 22rpx;
  border-left: 4rpx solid $ink;
  border-bottom: 4rpx solid $ink;
  transform: rotate(45deg);
  margin-left: 6rpx;
}

/* 品牌标记：两条不等长的墨黑横杠，比图标更克制 */
.nav__brand {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.nav__brand-bar {
  width: 34rpx;
  height: 6rpx;
  background: $ink;
  border-radius: 3rpx;
}

.nav__brand-bar--short {
  width: 20rpx;
  background: $vermilion;
}

.nav__titles {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.nav__subtitle {
  font-size: 18rpx;
  line-height: 1.2;
}

.nav__title {
  font-size: 34rpx;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.25;
}

.nav__right {
  flex: none;
  display: flex;
  align-items: center;
}

.nav__hairline {
  height: 2rpx;
  margin: 0 24rpx;
  background: $line;
}
</style>
