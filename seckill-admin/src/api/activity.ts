import { get, post } from './request'
import type { PageResult } from './goods'

export interface SeckillActivity {
  id: number
  goodsId: number
  goodsName: string
  goodsTitle?: string
  goodsImgUrl?: string
  originPrice: number
  seckillPrice: number
  /** 数据库里的活动库存 */
  seckillStock: number
  /** Redis 里的实时库存，null 表示还没预热 */
  redisStock: number | null
  startTime: string
  endTime: string
  statusText: string
}

export interface ActivityCreateParams {
  goodsId: number
  seckillPrice: number
  seckillStock: number
  startTime: string
  endTime: string
}

export function pageActivity(params: { pageNum: number; pageSize: number }) {
  return get<PageResult<SeckillActivity>>('/seckill/activity/page', params)
}

export function createActivity(data: ActivityCreateParams) {
  return post<number>('/admin/seckill/activity', data)
}

export function preheatActivity(id: number) {
  return post<void>(`/admin/seckill/activity/${id}/preheat`)
}
