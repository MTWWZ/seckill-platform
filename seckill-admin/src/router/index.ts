import { createRouter, createWebHistory } from 'vue-router'

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

export default router
