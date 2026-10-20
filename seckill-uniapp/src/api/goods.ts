import { get } from '@/utils/request'
import type { Goods, PageResult } from './types'

export interface GoodsQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  category?: string
}

export function pageGoods(query: GoodsQuery): Promise<PageResult<Goods>> {
  return get<PageResult<Goods>>('/goods/page', {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    keyword: query.keyword || undefined,
    category: query.category || undefined
  })
}

export function goodsDetail(id: number): Promise<Goods> {
  return get<Goods>(`/goods/${id}`)
}
