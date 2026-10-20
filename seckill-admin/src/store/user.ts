import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, type LoginParams } from '@/api/user'
import { clearToken, getToken, setToken } from '@/api/request'

const USERNAME_KEY = 'seckill_admin_username'
const ROLE_KEY = 'seckill_admin_role'

/**
 * 登录用户状态
 * <p>
 * token 存 localStorage，刷新页面不丢；后续用 pinia 管理，组件里直接读。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const username = ref(localStorage.getItem(USERNAME_KEY) ?? '')
  const role = ref(Number(localStorage.getItem(ROLE_KEY) ?? 0))

  async function login(params: LoginParams) {
    const result = await loginApi(params)
    token.value = result.token
    username.value = result.username
    role.value = result.role
    setToken(result.token)
    localStorage.setItem(USERNAME_KEY, result.username)
    localStorage.setItem(ROLE_KEY, String(result.role))
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = 0
    clearToken()
    localStorage.removeItem(USERNAME_KEY)
    localStorage.removeItem(ROLE_KEY)
  }

  return { token, username, role, login, logout }
})
