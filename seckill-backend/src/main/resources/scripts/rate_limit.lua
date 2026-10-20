-- 固定窗口限流计数器
--
-- KEYS[1] 计数 key
-- ARGV[1] 时间窗口（秒）
-- ARGV[2] 窗口内允许的最大请求数
--
-- 返回值：剩余的可用次数；返回 -1 表示已被限流
--
-- 为什么用 Lua：GET -> 判断 -> INCR 如果拆成多条命令，
-- 并发下多个请求会同时读到"未超限"然后一起放行，限流就失效了。
-- 另外"设置计数"和"设置过期时间"也必须是原子的，
-- 否则可能出现 key 永不过期（第一次请求设置了计数但没设 TTL）。

local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

local current = redis.call('GET', key)
if not current then
    -- 第一次请求：计数置 1 并设置窗口过期时间
    redis.call('SET', key, 1, 'EX', window)
    return limit - 1
end

current = tonumber(current)
if current >= limit then
    return -1
end

redis.call('INCR', key)
return limit - current - 1
