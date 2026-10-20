<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 小程序/H5 冷启动时清掉可能过期的登录态标记，由接口返回 401 时再兜底清理
  console.log('[seckill-uniapp] launched')
})
</script>

<style lang="scss">
/* ============================================================
 * 全局基座样式
 * 美学方向：「限定发售」票据风 —— 纸感底 + 墨黑 + 朱红 + 荧光酸黄
 * ============================================================ */

page {
  background-color: $paper;
  color: $ink;
  font-family: $font-body;
  font-size: 28rpx;
  line-height: 1.55;
  -webkit-font-smoothing: antialiased;
}

view,
text,
image,
input,
textarea,
scroll-view {
  box-sizing: border-box;
}

/* 页面容器：底部给自定义 TabBar 留出高度 */
.screen {
  min-height: 100vh;
  position: relative;
}

.screen--tabbed {
  padding-bottom: calc(150rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
}

.screen__body {
  padding: 0 32rpx;
}

/* 纸面颗粒：纯色底容易显闷，加一层很淡的网点 */
.paper-grain {
  @include dot-grid;
}

/* ---------- 排版 ---------- */
.overline {
  font-family: $font-num;
  font-size: 20rpx;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: $ink-mute;
}

.num {
  font-family: $font-num;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}

.h1 {
  font-size: 60rpx;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.02em;
}

/* 虚线分隔：票根上的撕线 */
.dash-line {
  height: 0;
  border-top: 2rpx dashed $line-strong;
}

/* ---------- 卡片 ---------- */
.ticket {
  @include card-surface;
  @include ticket-notch;
  overflow: hidden;
}

.panel {
  @include card-surface;
}

/* ---------- 徽章 ---------- */
.badge {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
  font-size: 20rpx;
  line-height: 1.5;
  font-weight: 600;
  letter-spacing: 0.06em;
  border: 2rpx solid transparent;
}

.badge--acid {
  background: $acid;
  color: $ink;
}

.badge--red {
  background: $vermilion;
  color: #fff;
}

.badge--ink {
  background: $ink;
  color: $paper;
}

.badge--line {
  background: transparent;
  border-color: $line-strong;
  color: $ink-soft;
}

/* 印章：稍微转一点角度，像盖上去的 */
.stamp {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6rpx 16rpx;
  border: 4rpx solid $vermilion;
  border-radius: 8rpx;
  color: $vermilion;
  font-family: $font-num;
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: 0.18em;
  transform: rotate(-8deg);
}

/* ---------- 按钮 ---------- */
.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  height: 92rpx;
  border-radius: 14rpx;
  font-size: 30rpx;
  font-weight: 600;
  letter-spacing: 0.04em;
  border: 2rpx solid transparent;
  transition: transform 0.12s ease, opacity 0.12s ease;
}

.btn:active {
  transform: scale(0.975);
  opacity: 0.9;
}

.btn--ink {
  background: $ink;
  color: $paper;
}

.btn--red {
  background: $vermilion;
  color: #fff;
  box-shadow: 0 12rpx 26rpx rgba(228, 50, 27, 0.28);
}

.btn--ghost {
  background: transparent;
  border-color: $line-strong;
  color: $ink;
}

.btn--disabled {
  background: $paper-deep;
  color: $ink-mute;
  border-color: transparent;
  box-shadow: none;
}

/* ---------- 进度条 ---------- */
.meter {
  height: 16rpx;
  border-radius: 999rpx;
  background: $paper-deep;
  overflow: hidden;
}

.meter__fill {
  height: 100%;
  border-radius: 999rpx;
  background: $vermilion;
  transition: width 0.5s cubic-bezier(0.2, 0.7, 0.3, 1);
}

.meter__fill--acid {
  background: $acid;
}

/* 被抢走的部分叠加斜纹，暗示"库存正在流失" */
.meter__fill--striped {
  @include stripes(rgba(255, 255, 255, 0.45), 14rpx);
}

/* ---------- 入场动效 ---------- */
@keyframes riseIn {
  from {
    opacity: 0;
    transform: translateY(26rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.rise {
  animation: riseIn 0.46s cubic-bezier(0.2, 0.7, 0.3, 1) both;
}

@keyframes breathe {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}

/* ---------- 空状态 / 骨架 ---------- */
.empty {
  padding: 120rpx 0;
  text-align: center;
}

.empty__mark {
  font-family: $font-num;
  font-size: 72rpx;
  color: $paper-deep;
  letter-spacing: 0.1em;
}

.empty__text {
  display: block;
  margin-top: 16rpx;
  color: $ink-mute;
  font-size: 26rpx;
}

.skeleton {
  background: linear-gradient(90deg, $paper-deep 25%, #f0ece1 37%, $paper-deep 63%);
  background-size: 400% 100%;
  border-radius: 8rpx;
  animation: shimmer 1.4s ease infinite;
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: 0 0;
  }
}

/* 收起滚动条 */
::-webkit-scrollbar {
  display: none;
}
</style>
