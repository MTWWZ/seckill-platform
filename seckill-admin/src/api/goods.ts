import { del, get, post, put } from './request'

/** 后端分页返回结构（MyBatis-Plus 的 IPage 序列化结果） */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface Goods {
  id?: number
  name: string
  title?: string
  description?: string
  imgUrl?: string
  originPrice: number
  stock: number
  category?: string
  status?: number
}

export function pageGoods(params: {
  pageNum: number
  pageSize: number
  keyword?: string
  category?: string
}) {
  return get<PageResult<Goods>>('/goods/page', params)
}

export function createGoods(data: Goods) {
  return post<number>('/admin/goods', data)
}

export function updateGoods(data: Goods) {
  return put<void>('/admin/goods', data)
}

export function removeGoods(id: number) {
  return del<void>(`/admin/goods/${id}`)
}
