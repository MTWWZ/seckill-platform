/** 后端统一响应结构 */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/** 业务异常：带上后端返回的 code，调用方可根据 401 / 409 / 429 做不同处理 */
export class BizError extends Error {
  code: number

  constructor(message: string, code: number) {
    super(message)
    this.name = 'BizError'
    this.code = code
  }
}

const TOKEN_KEY = 'seckill_user_token'
const PROFILE_KEY = 'seckill_user_profile'

export interface UserProfile {
  userId: number
  username: string
  nickname: string
  role: number
}

/* ------------------------------------------------------------
 * 请求地址：H5 走 vite 代理，小程序直连后端
 * ------------------------------------------------------------ */
let BASE_URL = ''
// #ifdef H5
BASE_URL = '/api'
// #endif
// #ifndef H5
BASE_URL = 'http://localhost:8080/api'
// #endif

/* ------------------------------------------------------------
 * 登录态读写
 * ------------------------------------------------------------ */
export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function getProfile(): UserProfile | null {
  const raw = uni.getStorageSync(PROFILE_KEY)
  return raw ? (raw as UserProfile) : null
}

export function saveProfile(profile: UserProfile) {
  uni.setStorageSync(PROFILE_KEY, profile)
}

export function clearAuth() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(PROFILE_KEY)
}

export function isLogin(): boolean {
  return !!getToken()
}

/* ------------------------------------------------------------
 * 统一的请求入口
 * ------------------------------------------------------------ */
interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, unknown>
  /** 失败时是否自动 toast，默认 true */
  toast?: boolean
}

export function request<T>(options: RequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const header: Record<string, string> = { 'Content-Type': 'application/json' }
    const token = getToken()
    if (token) {
      header.Authorization = `Bearer ${token}`
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method ?? 'GET',
      data: options.data,
      header,
      success: (res) => {
        const body = res.data as Result<T> | undefined
        if (body && body.code === 200) {
          resolve(body.data)
          return
        }
        if (body && body.code === 401) {
          clearAuth()
        }
        const message = body?.message || `请求失败（${res.statusCode}）`
        if (options.toast !== false) {
          uni.showToast({ title: message, icon: 'none' })
        }
        reject(new BizError(message, body?.code ?? res.statusCode))
      },
      fail: (err) => {
        const message = err.errMsg || '网络异常，请确认后端已启动'
        if (options.toast !== false) {
          uni.showToast({ title: message, icon: 'none' })
        }
        reject(new BizError(message, -1))
      }
    })
  })
}

export function get<T>(url: string, params?: Record<string, unknown>, toast = true): Promise<T> {
  return request<T>({ url, method: 'GET', data: params, toast })
}

export function post<T>(url: string, data?: Record<string, unknown>, toast = true): Promise<T> {
  return request<T>({ url, method: 'POST', data, toast })
}
