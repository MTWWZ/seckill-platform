/* 时间与金额的展示工具 */

/** 把后端返回的 `2026-10-20T17:07:04` 解析成时间戳
 *  换成斜杠写法是为了兼容 iOS 与小程序端的 Date 解析 */
export function parseTime(value?: string | null): number {
  if (!value) return 0
  return new Date(value.replace(/-/g, '/').replace('T', ' ')).getTime()
}

export function formatTime(value?: string | null): string {
  if (!value) return '--'
  return value.replace('T', ' ')
}

/** 只保留 月-日 时:分，列表里更紧凑 */
export function formatTimeShort(value?: string | null): string {
  if (!value) return '--'
  const t = value.replace('T', ' ')
  return t.slice(5, 16)
}

/** 金额：去掉多余的小数 0，比如 99.00 -> 99，99.50 -> 99.5 */
export function money(value?: number | string | null): string {
  if (value === null || value === undefined || value === '') return '--'
  const n = Number(value)
  if (Number.isNaN(n)) return String(value)
  return n.toFixed(2).replace(/\.?0+$/, '')
}

/** 拆成整数与小数字符串，价格需要大号整数 + 小号小数分两段排版 */
export function moneyParts(value?: number | string | null): { int: string; dec: string } {
  const n = Number(value ?? 0)
  const fixed = Number.isNaN(n) ? '0.00' : n.toFixed(2)
  const [int, dec] = fixed.split('.')
  return { int, dec }
}

/** 秒 -> { d, h, m, s } */
export function splitSeconds(total: number) {
  const t = Math.max(0, Math.floor(total))
  return {
    d: Math.floor(t / 86400),
    h: Math.floor((t % 86400) / 3600),
    m: Math.floor((t % 3600) / 60),
    s: t % 60
  }
}

export function pad2(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}
