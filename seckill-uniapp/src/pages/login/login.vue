<script setup lang="ts">
import { computed, ref } from 'vue'
import AppNavBar from '@/components/AppNavBar.vue'
import { login, register } from '@/api/user'

type Mode = 'login' | 'register'

const mode = ref<Mode>('login')
const busy = ref(false)
const showPassword = ref(false)

const form = ref({
  username: '',
  password: '',
  nickname: '',
  phone: ''
})

const DEMO = { username: 'student01', password: '123456' }

const headline = computed(() => (mode.value === 'login' ? '欢迎回来' : '创建账号'))
const subtitle = computed(() =>
  mode.value === 'login' ? '登录后即可参与限量抢购' : '注册成功会自动登录，马上就能抢'
)

const canSubmit = computed(() => form.value.username.length >= 4 && form.value.password.length >= 6)

function switchMode(next: Mode) {
  if (mode.value === next) return
  mode.value = next
  form.value.password = ''
}

function fillDemo() {
  mode.value = 'login'
  form.value.username = DEMO.username
  form.value.password = DEMO.password
}

async function submit() {
  if (busy.value) return
  if (form.value.username.length < 4 || form.value.username.length > 20) {
    uni.showToast({ title: '账号需 4-20 位', icon: 'none' })
    return
  }
  if (form.value.password.length < 6 || form.value.password.length > 32) {
    uni.showToast({ title: '密码需 6-32 位', icon: 'none' })
    return
  }

  busy.value = true
  try {
    if (mode.value === 'register') {
      await register({
        username: form.value.username,
        password: form.value.password,
        nickname: form.value.nickname || undefined,
        phone: form.value.phone || undefined
      })
      uni.showToast({ title: '注册成功，自动登录中', icon: 'none' })
    }

    const result = await login({ username: form.value.username, password: form.value.password })
    uni.showToast({ title: `欢迎，${result.nickname || result.username}`, icon: 'none' })
    setTimeout(goBack, 520)
  } catch {
    // 错误提示由 request 层统一 toast
  } finally {
    busy.value = false
  }
}

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
  <view class="screen paper-grain">
    <AppNavBar back title="账号" subtitle="Sign In" />

    <view class="screen__body">
      <!-- 标题 -->
      <view class="hero rise">
        <text class="overline">Campus Limited Drop</text>
        <text class="hero__title">{{ headline }}</text>
        <text class="hero__sub">{{ subtitle }}</text>
        <view class="hero__bar" />
      </view>

      <!-- 模式切换 -->
      <view class="switch rise" style="animation-delay: 60ms">
        <view
          class="switch__item"
          :class="{ 'switch__item--on': mode === 'login' }"
          @tap="switchMode('login')"
        >
          <text class="switch__en">SIGN IN</text>
          <text class="switch__zh">登录</text>
        </view>
        <view
          class="switch__item"
          :class="{ 'switch__item--on': mode === 'register' }"
          @tap="switchMode('register')"
        >
          <text class="switch__en">SIGN UP</text>
          <text class="switch__zh">注册</text>
        </view>
      </view>

      <!-- 表单 -->
      <view class="form rise" style="animation-delay: 110ms">
        <view class="field">
          <text class="field__label overline">Username / 账号</text>
          <input
            v-model="form.username"
            class="field__input"
            type="text"
            placeholder="4-20 位，字母或数字"
            placeholder-class="field__ph"
            :maxlength="20"
          />
        </view>

        <view class="field">
          <text class="field__label overline">Password / 密码</text>
          <view class="field__row">
            <input
              v-model="form.password"
              class="field__input field__input--flex"
              type="text"
              :password="!showPassword"
              placeholder="6-32 位"
              placeholder-class="field__ph"
              :maxlength="32"
            />
            <view class="field__toggle" hover-class="field__toggle--hover" @tap="showPassword = !showPassword">
              <text class="field__toggle-text">{{ showPassword ? '隐藏' : '显示' }}</text>
            </view>
          </view>
        </view>

        <template v-if="mode === 'register'">
          <view class="field">
            <text class="field__label overline">Nickname / 昵称（选填）</text>
            <input
              v-model="form.nickname"
              class="field__input"
              type="text"
              placeholder="取个好记的名字"
              placeholder-class="field__ph"
              :maxlength="20"
            />
          </view>

          <view class="field">
            <text class="field__label overline">Phone / 手机号（选填）</text>
            <input
              v-model="form.phone"
              class="field__input"
              type="number"
              placeholder="用于接收通知"
              placeholder-class="field__ph"
              :maxlength="11"
            />
          </view>
        </template>
      </view>

      <view
        class="btn submit rise"
        :class="canSubmit && !busy ? 'btn--ink' : 'btn--disabled'"
        style="animation-delay: 160ms"
        :hover-class="canSubmit && !busy ? 'btn--hover' : 'none'"
        @tap="submit"
      >
        <text>{{ busy ? '处理中…' : mode === 'login' ? '登录' : '注册并登录' }}</text>
      </view>

      <!-- 演示账号 -->
      <view class="demo rise" style="animation-delay: 210ms">
        <view class="demo__head">
          <text class="overline">Demo Account / 演示账号</text>
          <view class="demo__fill" hover-class="demo__fill--hover" @tap="fillDemo">
            <text class="demo__fill-text">一键填入</text>
          </view>
        </view>
        <view class="dash-line demo__dash" />
        <view class="demo__row">
          <text class="demo__key">账号</text>
          <text class="demo__val num">student01 / student02</text>
        </view>
        <view class="demo__row">
          <text class="demo__key">密码</text>
          <text class="demo__val num">123456</text>
        </view>
        <text class="demo__note">种子数据里的普通用户，直接拿来演示抢购流程最方便</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.screen {
  padding-bottom: calc(80rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(80rpx + env(safe-area-inset-bottom));
}

/* ---------- 标题 ---------- */
.hero {
  margin-top: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.hero__title {
  font-size: 76rpx;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.hero__sub {
  font-size: 25rpx;
  color: $ink-soft;
  line-height: 1.6;
}

.hero__bar {
  width: 96rpx;
  height: 8rpx;
  border-radius: 4rpx;
  background: $acid;
  margin-top: 12rpx;
}

/* ---------- 模式切换 ---------- */
.switch {
  display: flex;
  gap: 14rpx;
  margin-top: 46rpx;
}

.switch__item {
  flex: 1;
  padding: 18rpx 20rpx;
  border-radius: 14rpx;
  border: 2rpx solid $line-strong;
  display: flex;
  flex-direction: column;
  gap: 2rpx;
  transition: all 0.18s ease;
}

.switch__item--on {
  background: $ink;
  border-color: $ink;
}

.switch__en {
  font-family: $font-num;
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: $ink-mute;
}

.switch__zh {
  font-size: 28rpx;
  color: $ink-soft;
}

.switch__item--on .switch__en {
  color: $acid;
}

.switch__item--on .switch__zh {
  color: $paper;
  font-weight: 600;
}

/* ---------- 表单 ---------- */
.form {
  margin-top: 44rpx;
  display: flex;
  flex-direction: column;
  gap: 34rpx;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.field__row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.field__input {
  height: 68rpx;
  font-size: 32rpx;
  color: $ink;
  border-bottom: 2rpx solid $line-strong;
  padding-bottom: 8rpx;
}

.field__input--flex {
  flex: 1;
}

.field__ph {
  color: $ink-mute;
  font-size: 28rpx;
}

.field__toggle {
  flex: none;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  border: 2rpx solid $line-strong;
}

.field__toggle--hover {
  background: rgba(22, 21, 15, 0.05);
}

.field__toggle-text {
  font-size: 22rpx;
  color: $ink-soft;
}

/* ---------- 提交 ---------- */
.submit {
  margin-top: 52rpx;
}

/* ---------- 演示账号 ---------- */
.demo {
  margin-top: 46rpx;
  padding: 24rpx 22rpx;
  border-radius: $radius-lg;
  border: 2rpx solid $line;
  background: $card;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.demo__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.demo__fill {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: $acid;
}

.demo__fill--hover {
  opacity: 0.85;
}

.demo__fill-text {
  font-size: 21rpx;
  font-weight: 600;
  color: $ink;
}

.demo__dash {
  margin: 6rpx 0 8rpx;
}

.demo__row {
  display: flex;
  gap: 16rpx;
}

.demo__key {
  width: 80rpx;
  flex: none;
  font-size: 23rpx;
  color: $ink-mute;
}

.demo__val {
  flex: 1;
  font-size: 25rpx;
}

.demo__note {
  margin-top: 6rpx;
  font-size: 21rpx;
  color: $ink-mute;
  line-height: 1.6;
}

.btn--hover {
  opacity: 0.88;
}
</style>
