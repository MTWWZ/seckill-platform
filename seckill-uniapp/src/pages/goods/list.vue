<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import AppNavBar from '@/components/AppNavBar.vue'
import TabBar from '@/components/TabBar.vue'
import { pageGoods } from '@/api/goods'
import { moneyParts, pad2 } from '@/utils/format'
import type { Goods } from '@/api/types'

const PAGE_SIZE = 20

const loading = ref(false)
const list = ref<Goods[]>([])
const total = ref(0)
const pageNum = ref(1)
const hasMore = ref(true)

const keyword = ref('')
const category = ref('')
const failedIds = ref<number[]>([])

const categories = computed(() => {
  const set = new Set<string>()
  list.value.forEach((item) => {
    if (item.category) set.add(item.category)
  })
  return Array.from(set)
})

let searchTimer: ReturnType<typeof setTimeout> | null = null

async function fetchPage(reset = false) {
  if (loading.value) return
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
  }
  loading.value = true
  try {
    const page = await pageGoods({
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      keyword: keyword.value.trim(),
      category: category.value
    })
    list.value = reset ? page.records : [...list.value, ...page.records]
    total.value = page.total
    hasMore.value = pageNum.value < page.pages
  } catch {
    if (reset) list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearchInput(event: { detail: { value: string } }) {
  keyword.value = event.detail.value
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => fetchPage(true), 420)
}

function pickCategory(value: string) {
  if (category.value === value) return
  category.value = value
  fetchPage(true)
}

function clearKeyword() {
  keyword.value = ''
  fetchPage(true)
}

function openDetail(item: Goods) {
  uni.navigateTo({ url: `/pages/goods/detail?id=${item.id}` })
}

function markFailed(id: number) {
  failedIds.value = [...failedIds.value, id]
}

function isFailed(id: number) {
  return failedIds.value.includes(id)
}

onLoad(() => {
  fetchPage(true)
})

onReachBottom(() => {
  if (!hasMore.value || loading.value) return
  pageNum.value += 1
  fetchPage()
})

onPullDownRefresh(async () => {
  await fetchPage(true)
  uni.stopPullDownRefresh()
})
</script>

<template>
  <view class="screen screen--tabbed paper-grain">
    <AppNavBar title="全部商品" subtitle="Catalog">
      <text class="nav-count num">{{ total }} 件</text>
    </AppNavBar>

    <view class="screen__body">
      <!-- 搜索 -->
      <view class="search rise">
        <view class="search__box">
          <view class="search__glyph" />
          <input
            class="search__input"
            type="text"
            confirm-type="search"
            placeholder="搜商品名 / 卖点"
            placeholder-class="search__ph"
            :value="keyword"
            @input="handleSearchInput"
            @confirm="fetchPage(true)"
          />
          <view v-if="keyword" class="search__clear" @tap="clearKeyword">
            <text class="search__clear-text">×</text>
          </view>
        </view>
      </view>

      <!-- 分类 -->
      <scroll-view class="chips" scroll-x :show-scrollbar="false">
        <view class="chips__row">
          <view
            class="chips__item"
            :class="{ 'chips__item--on': category === '' }"
            @tap="pickCategory('')"
          >
            <text class="chips__text">全部</text>
          </view>
          <view
            v-for="item in categories"
            :key="item"
            class="chips__item"
            :class="{ 'chips__item--on': category === item }"
            @tap="pickCategory(item)"
          >
            <text class="chips__text">{{ item }}</text>
          </view>
        </view>
      </scroll-view>

      <!-- 商品网格 -->
      <view v-if="loading && !list.length" class="grid">
        <view v-for="n in 4" :key="n" class="grid__cell">
          <view class="skeleton grid__media" />
          <view class="skeleton grid__line" />
          <view class="skeleton grid__line grid__line--sm" />
        </view>
      </view>

      <view v-else-if="list.length" class="grid">
        <view
          v-for="(item, index) in list"
          :key="item.id"
          class="gcard rise"
          :style="{ animationDelay: `${(index % 8) * 55}ms` }"
          hover-class="gcard--hover"
          @tap="openDetail(item)"
        >
          <view class="gcard__media">
            <image
              v-if="item.imgUrl && !isFailed(item.id)"
              class="gcard__img"
              :src="item.imgUrl"
              mode="aspectFill"
              @error="markFailed(item.id)"
            />
            <view v-else class="gcard__fallback paper-grain">
              <text class="gcard__fallback-mark num">{{ item.name?.slice(0, 1) }}</text>
            </view>
            <text class="gcard__plate num">{{ pad2(index + 1) }}</text>
            <view v-if="item.category" class="gcard__cat">
              <text class="badge badge--acid">{{ item.category }}</text>
            </view>
          </view>

          <view class="gcard__body">
            <text class="gcard__name">{{ item.name }}</text>
            <text v-if="item.title" class="gcard__title">{{ item.title }}</text>

            <view class="gcard__foot">
              <view class="gcard__price">
                <text class="gcard__cur">¥</text>
                <text class="gcard__int num">{{ moneyParts(item.originPrice).int }}</text>
                <text class="gcard__dec num">.{{ moneyParts(item.originPrice).dec }}</text>
              </view>
              <text class="gcard__stock num">库 {{ item.stock }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty">
        <text class="empty__mark">— 0 —</text>
        <text class="empty__text">没有匹配的商品，换个关键词试试</text>
      </view>

      <view v-if="list.length" class="footnote">
        <text v-if="loading" class="footnote__text">加载中…</text>
        <text v-else-if="hasMore" class="footnote__text">上拉加载更多</text>
        <text v-else class="footnote__text">— 已经到底了 —</text>
      </view>
    </view>

    <TabBar current="goods" />
  </view>
</template>

<style lang="scss" scoped>
.nav-count {
  font-size: 24rpx;
  color: $ink-mute;
  letter-spacing: 0.08em;
}

/* ---------- 搜索 ---------- */
.search {
  margin-top: 24rpx;
}

.search__box {
  display: flex;
  align-items: center;
  gap: 14rpx;
  height: 88rpx;
  padding: 0 22rpx;
  border-radius: 14rpx;
  background: $card;
  border: 2rpx solid $line-strong;
}

.search__glyph {
  width: 24rpx;
  height: 24rpx;
  border: 4rpx solid $ink;
  border-radius: 50%;
  position: relative;
  flex: none;
}

.search__glyph::after {
  content: "";
  position: absolute;
  right: -10rpx;
  bottom: -8rpx;
  width: 14rpx;
  height: 4rpx;
  background: $ink;
  border-radius: 2rpx;
  transform: rotate(45deg);
}

.search__input {
  flex: 1;
  font-size: 27rpx;
  color: $ink;
  height: 100%;
}

.search__ph {
  color: $ink-mute;
  font-size: 26rpx;
}

.search__clear {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: $paper-deep;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.search__clear-text {
  font-size: 30rpx;
  color: $ink-soft;
  line-height: 1;
}

/* ---------- 分类 ---------- */
.chips {
  margin: 22rpx -32rpx 0;
  white-space: nowrap;
}

.chips__row {
  display: inline-flex;
  gap: 12rpx;
  padding: 0 32rpx;
}

.chips__item {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  border: 2rpx solid $line-strong;
  flex: none;
}

.chips__item--on {
  background: $ink;
  border-color: $ink;
}

.chips__text {
  font-size: 25rpx;
  color: $ink-soft;
}

.chips__item--on .chips__text {
  color: $paper;
  font-weight: 600;
}

/* ---------- 网格 ---------- */
.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 22rpx;
  margin-top: 26rpx;
}

.grid__cell {
  width: calc(50% - 11rpx);
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.grid__media {
  width: 100%;
  height: 320rpx;
  border-radius: $radius;
}

.grid__line {
  height: 26rpx;
  width: 80%;
}

.grid__line--sm {
  width: 45%;
}

.gcard {
  width: calc(50% - 11rpx);
  background: $card;
  border: 2rpx solid $line;
  border-radius: $radius-lg;
  overflow: hidden;
}

.gcard--hover {
  border-color: $line-strong;
}

.gcard__media {
  position: relative;
  width: 100%;
  height: 320rpx;
  background: $paper-deep;
}

.gcard__img {
  width: 100%;
  height: 100%;
  display: block;
}

.gcard__fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $paper-deep;
}

.gcard__fallback-mark {
  font-size: 110rpx;
  font-weight: 700;
  color: rgba(22, 21, 15, 0.16);
}

/* 编目编号：像产品图录里的图版号 */
.gcard__plate {
  position: absolute;
  right: 14rpx;
  top: 10rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.86);
  text-shadow: 0 2rpx 8rpx rgba(22, 21, 15, 0.5);
  letter-spacing: 0.06em;
}

.gcard__cat {
  position: absolute;
  left: 12rpx;
  bottom: 12rpx;
}

.gcard__body {
  padding: 18rpx 18rpx 20rpx;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.gcard__name {
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.gcard__title {
  font-size: 21rpx;
  color: $ink-mute;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gcard__foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 8rpx;
}

.gcard__price {
  display: flex;
  align-items: baseline;
  gap: 2rpx;
}

.gcard__cur {
  font-size: 22rpx;
  font-weight: 700;
}

.gcard__int {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1;
}

.gcard__dec {
  font-size: 22rpx;
  font-weight: 700;
}

.gcard__stock {
  font-size: 20rpx;
  color: $ink-mute;
}

.footnote {
  padding: 32rpx 0 16rpx;
  text-align: center;
}

.footnote__text {
  font-size: 22rpx;
  color: $ink-mute;
  letter-spacing: 0.08em;
}
</style>
