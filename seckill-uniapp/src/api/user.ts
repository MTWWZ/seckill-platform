import { get, post, saveProfile, setToken } from '@/utils/request'
import type { LoginResult } from './types'

export interface RegisterParams {
  username: string
  password: string
  nickname?: string
  phone?: string
}

/** 登录并落地 token / 用户信息 */
export async function login(data: { username: string; password: string }): Promise<LoginResult> {
  const result = await post<LoginResult>('/user/login', data)
  setToken(result.token)
  saveProfile({
    userId: result.userId,
    username: result.username,
    nickname: result.nickname,
    role: result.role
  })
  return result
}

export function register(data: RegisterParams): Promise<void> {
  return post<void>('/user/register', data)
}

export function fetchMe(): Promise<{ userId: number; username: string; role: number }> {
  return get('/user/me')
}
