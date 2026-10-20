import { get } from './request'
import type { PageResult } from './goods'

export interface AdminOrder {
  id: number
  orderNo: string
  userId: number
  username?: string
  nickname?: string
  activityId: number
  goodsId: number
  goodsName?: string
  amount: number
  status: number
  statusText: string
  createTime: string
}

export function pageOrder(params: { pageNum: number; pageSize: number }) {
  return get<PageResult<AdminOrder>>('/admin/order/page', params)
}
