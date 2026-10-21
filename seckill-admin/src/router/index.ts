import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearToken, getToken } from '@/api/request'

const ADMIN_ROLE = 1

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/',
      component: () => import('@/layout/AdminLayout.vue'),
      children: [
        { path: '', redirect: '/goods' },
        { path: 'goods', name: 'goods', component: () => import('@/views/GoodsView.vue') },
        { path: 'activity', name: 'activity', component: () => import('@/views/ActivityView.vue') },
        { path: 'order', name: 'order', component: () => import('@/views/OrderView.vue') }
      ]
    }
  ]
})

/**
 * 路由守卫
 * <p>
 * 后端已经拦了 /api/admin/**，这里只是避免普通用户进到一个空壳页面。
 * 真正的权限判断永远以后端为准，前端守卫只是体验层。
 */
router.beforeEach((to) => {
  const token = getToken()
  const isLoginPage = to.path === '/login'

  if (isLoginPage) {
    return token ? '/goods' : true
  }

  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const role = Number(localStorage.getItem('seckill_admin_role') ?? 0)
  if (role !== ADMIN_ROLE) {
    // 本地存的是普通用户 token，别让他停在后台里
    clearToken()
    localStorage.removeItem('seckill_admin_username')
    localStorage.removeItem('seckill_admin_role')
    ElMessage.error('该账号没有管理员权限')
    return '/login'
  }

  return true
})

export default router
