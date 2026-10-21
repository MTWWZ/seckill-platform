<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: 'admin',
  password: '123456'
})

const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const result = await userStore.login(form)
    // 后台只给管理员用：普通用户能通过 /api/user/login，但不该进到这里
    if (result.role !== 1) {
      userStore.logout()
      ElMessage.error('该账号没有管理员权限')
      return
    }
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.push(typeof redirect === 'string' && redirect ? redirect : '/goods')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">限时抢购交易平台</h2>
      <p class="subtitle">管理后台</p>
      <el-form :model="form" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #001529 0%, #1677ff 100%);
}

.login-card {
  width: 380px;
  padding: 12px 8px;
  border-radius: 10px;
}

.title {
  text-align: center;
  margin: 0 0 4px;
  font-size: 20px;
}

.subtitle {
  text-align: center;
  margin: 0 0 24px;
  color: #909399;
  font-size: 13px;
}

.submit {
  width: 100%;
}
</style>
