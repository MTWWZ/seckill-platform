/**
 * 自定义导航栏的尺寸计算
 *
 * 小程序端右上角有微信胶囊按钮，如果导航栏内容跟胶囊同高，
 * 右边就会被胶囊压住、左侧元素只好横向缩进，整页看起来是歪的。
 * 这里按「整体下移」的思路处理：导航内容整体放到胶囊下方，左右留白一致，
 * 竖屏下视觉重心更稳。（这是长期偏好，别改成左右避让的方案）
 */

export interface NavMetrics {
  /** 导航内容距屏幕顶部的距离（px） */
  top: number
  /** 导航内容区高度（px） */
  height: number
  /** 占位总高度 = top + height（px） */
  total: number
}

let cached: NavMetrics | null = null

export function useNavBar(): NavMetrics {
  if (cached) return cached

  const sys = uni.getSystemInfoSync()
  const statusBar = sys.statusBarHeight ?? 0

  let top = statusBar + 8
  let height = 44

  // #ifdef MP-WEIXIN
  const capsule = uni.getMenuButtonBoundingClientRect()
  // 整体下移：导航内容放到胶囊底边以下，避免与胶囊抢同一行
  top = capsule.bottom + 8
  height = 44
  // #endif

  // #ifdef H5
  top = statusBar + 10
  height = 46
  // #endif

  cached = { top, height, total: top + height }
  return cached
}
