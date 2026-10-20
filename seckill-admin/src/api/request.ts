import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/** 后端统一响应结构 */
export interface Result<T> {
  code: number
  message: string
  data: T
}

const TOKEN_KEY = 'seckill_admin_token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动带上 JWT，省得每个接口都手动加
instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：把 {code,message,data} 拆开，业务成功时直接返回 data
instance.interceptors.response.use(
  (response) => {
    const result = response.data as Result<unknown>
    if (result.code === 200) {
      return result.data
    }
    if (result.code === 401) {
      clearToken()
      router.push('/login')
    }
    ElMessage.error(result.message || '请求失败')
    return Promise.reject(new Error(result.message))
  },
  (error) => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export function get<T>(url: string, params?: object): Promise<T> {
  return instance.get(url, { params }) as unknown as Promise<T>
}

export function post<T>(url: string, data?: object): Promise<T> {
  return instance.post(url, data) as unknown as Promise<T>
}

export function put<T>(url: string, data?: object): Promise<T> {
  return instance.put(url, data) as unknown as Promise<T>
}

export function del<T>(url: string): Promise<T> {
  return instance.delete(url) as unknown as Promise<T>
}
