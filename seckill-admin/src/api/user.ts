import { get, post } from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  nickname: string
  role: number
}

export interface CurrentUser {
  userId: number
  username: string
  role: number
}

export function login(data: LoginParams) {
  return post<LoginResult>('/user/login', data)
}

export function getCurrentUser() {
  return get<CurrentUser>('/user/me')
}
