/** 与后端实体 / VO 一一对应的类型 */

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface Goods {
  id: number
  name: string
  title?: string | null
  description?: string | null
  imgUrl?: string | null
  originPrice: number
  stock: number
  category?: string | null
  status: number
}

export interface SeckillActivity {
  id: number
  goodsId: number
  goodsName: string
  goodsTitle?: string | null
  goodsImgUrl?: string | null
  originPrice: number
  seckillPrice: number
  /** 活动初始库存 */
  seckillStock: number
  /** Redis 实时库存，null 表示还没预热 */
  redisStock: number | null
  startTime: string
  endTime: string
  statusText: '未开始' | '进行中' | '已结束' | string
}

/** 抢购结果：status 0-排队中 1-成功 2-未抢到 */
export interface SeckillResult {
  activityId: number
  orderNo: string | null
  status: number
  statusText: string
  message: string
}

export interface SeckillOrder {
  id: number
  orderNo: string
  activityId: number
  goodsId: number
  userId: number
  username: string
  nickname: string
  goodsName: string
  amount: number
  status: number
  statusText: string
  createTime: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  nickname: string
  role: number
}

export const RESULT_STATUS = {
  QUEUING: 0,
  SUCCESS: 1,
  FAILED: 2
} as const
