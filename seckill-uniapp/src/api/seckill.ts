import { get, post } from '@/utils/request'
import type { PageResult, SeckillActivity, SeckillOrder, SeckillResult } from './types'

/** 活动列表（游客可看） */
export function pageActivity(pageNum = 1, pageSize = 20): Promise<PageResult<SeckillActivity>> {
  return get<PageResult<SeckillActivity>>('/seckill/activity/page', { pageNum, pageSize })
}

/** 活动详情（含 Redis 实时库存） */
export function activityDetail(id: number): Promise<SeckillActivity> {
  return get<SeckillActivity>(`/seckill/activity/${id}`)
}

/** 抢购：立刻返回"排队中"，真正的订单由后端消费者异步生成
 *  toast 关掉，限流/重复下单的提示由页面自己渲染，避免和跳转动画打架 */
export function seckill(activityId: number): Promise<SeckillResult> {
  return post<SeckillResult>(`/seckill/order/${activityId}`, undefined, false)
}

/** 轮询抢购结果 */
export function orderResult(activityId: number, toast = false): Promise<SeckillResult> {
  return get<SeckillResult>(`/seckill/order/result/${activityId}`, undefined, toast)
}

/** 我的秒杀订单 */
export function myOrders(): Promise<SeckillOrder[]> {
  return get<SeckillOrder[]>('/seckill/order/my')
}

/** 支付订单（演示用：后端直接把状态改成已支付） */
export function payOrder(orderNo: string): Promise<void> {
  return post<void>(`/seckill/order/${orderNo}/pay`)
}
