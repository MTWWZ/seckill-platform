#!/usr/bin/env node
/**
 * 秒杀压测脚本（零依赖，Node 18+）
 *
 * 用法：
 *   # 50 个用户同时抢（脚本会自动注册临时账号）
 *   node scripts/loadtest.mjs --users 50
 *
 *   # 指定活动 + 指定后端地址
 *   node scripts/loadtest.mjs --users 100 --activity 3 --base http://localhost:8080
 *
 *   # 不注册账号，直接用已有 token 压（会触发一人一单，适合测限流）
 *   node scripts/loadtest.mjs --users 10 --token <JWT>
 *
 * 输出：各业务码数量、总耗时、QPS、P50/P95/P99、活动剩余库存。
 */

const args = process.argv.slice(2)
const opt = {
  base: 'http://localhost:8080',
  users: 50,
  activity: 0,
  token: '',
  password: '123456'
}

for (let i = 0; i < args.length; i++) {
  const key = args[i]
  const value = args[i + 1]
  if (key === '--users') { opt.users = Number(value); i++ }
  else if (key === '--activity') { opt.activity = Number(value); i++ }
  else if (key === '--base') { opt.base = value.replace(/\/$/, ''); i++ }
  else if (key === '--token') { opt.token = value; i++ }
  else if (key === '--password') { opt.password = value; i++ }
  else if (key === '--help' || key === '-h') { console.log('用法见脚本头部注释'); process.exit(0) }
}

const jsonHeaders = { 'Content-Type': 'application/json' }

async function api(path, init = {}) {
  const res = await fetch(opt.base + path, init)
  return res.json()
}

async function pickActivity() {
  if (opt.activity > 0) {
    const detail = await api(`/api/seckill/activity/${opt.activity}`)
    return detail.data
  }
  const page = await api('/api/seckill/activity/page?pageNum=1&pageSize=50')
  const running = (page.data?.records ?? []).find((item) => item.statusText === '进行中')
  if (!running) {
    throw new Error('没有找到进行中的活动，请先重置演示数据或在管理后台新建一场')
  }
  return running
}

/** 注册并登录 N 个临时账号，返回 token 列表 */
async function prepareUsers(count) {
  if (opt.token) {
    return Array.from({ length: count }, () => opt.token)
  }

  const runId = Date.now().toString(36)
  const usernames = Array.from({ length: count }, (_, i) => `load_${runId}_${i}`)

  console.log(`注册 ${count} 个压测账号…`)
  await Promise.all(usernames.map(async (username) => {
    await api('/api/user/register', {
      method: 'POST',
      headers: jsonHeaders,
      body: JSON.stringify({ username, password: opt.password, nickname: username })
    })
  }))

  console.log(`登录 ${count} 个账号…`)
  const tokens = await Promise.all(usernames.map(async (username) => {
    const login = await api('/api/user/login', {
      method: 'POST',
      headers: jsonHeaders,
      body: JSON.stringify({ username, password: opt.password })
    })
    if (login.code !== 200) throw new Error(`登录失败：${username} ${login.message}`)
    return login.data.token
  }))
  return tokens
}

function percentile(sorted, p) {
  if (!sorted.length) return 0
  const index = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)
  return sorted[index]
}

async function main() {
  const activity = await pickActivity()
  console.log(`活动：#${activity.id} ${activity.goodsName}，库存 ${activity.seckillStock}，当前剩余 ${activity.redisStock}`)

  const tokens = await prepareUsers(opt.users)
  console.log(`并发用户：${tokens.length}，开始请求…\n`)

  const codes = new Map()
  const latencies = []
  const startedAt = Date.now()

  // 一次性把所有请求发出去，尽量制造真实并发
  await Promise.all(tokens.map(async (token, i) => {
    const begin = performance.now()
    let code = -1
    try {
      const body = await api(`/api/seckill/order/${activity.id}`, {
        method: 'POST',
        headers: { ...jsonHeaders, Authorization: `Bearer ${token}` }
      })
      code = body.code
    } catch (e) {
      code = 'ERR'
    }
    latencies.push(performance.now() - begin)
    codes.set(code, (codes.get(code) ?? 0) + 1)
    if ((i + 1) % 20 === 0) process.stdout.write(`  已发送 ${i + 1}/${tokens.length}\n`)
  }))

  const totalMs = Date.now() - startedAt
  const sorted = [...latencies].sort((a, b) => a - b)

  const after = await api(`/api/seckill/activity/${activity.id}`)
  const redisStock = after.data?.redisStock

  console.log('\n========== 压测结果 ==========')
  console.log(`请求总数 : ${tokens.length}`)
  for (const [code, count] of [...codes.entries()].sort((a, b) => String(a[0]).localeCompare(String(b[0])))) {
    const label = code === 200 ? '抢到（排队中）' : code === 400 ? '库存不足' : code === 409 ? '重复下单' : code === 429 ? '被限流' : '异常'
    console.log(`  code=${String(code).padEnd(4)} ${String(count).padStart(4)}  ${label}`)
  }
  console.log(`总耗时   : ${totalMs} ms`)
  console.log(`QPS      : ${(tokens.length / (totalMs / 1000)).toFixed(1)}`)
  console.log(`P50      : ${percentile(sorted, 50).toFixed(0)} ms`)
  console.log(`P95      : ${percentile(sorted, 95).toFixed(0)} ms`)
  console.log(`P99      : ${percentile(sorted, 99).toFixed(0)} ms`)
  console.log(`活动库存 : ${redisStock}（请求前 ${activity.redisStock}）`)
  console.log('==============================')
}

main().catch((e) => {
  console.error('压测失败：', e.message)
  process.exit(1)
})
