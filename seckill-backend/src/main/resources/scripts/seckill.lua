-- 秒杀库存原子扣减 + 一人一单
--
-- KEYS[1] 库存 key         seckill:activity:stock:{activityId}
-- KEYS[2] 参与用户集合 key  seckill:activity:users:{activityId}
-- ARGV[1] 用户 id
--
-- 返回值：0=抢购成功  1=库存不足  2=重复下单  -1=活动未预热
--
-- 为什么必须用 Lua：Redis 执行 Lua 脚本时是单线程、不可中断的，
-- 所以"判断库存"和"扣减库存"之间不会有其它命令插进来。
-- 如果拆成 GET 再 DECR 两条命令，并发下多个请求会读到同样的库存值，
-- 导致扣成负数（超卖）。

local stockKey = KEYS[1]
local usersKey = KEYS[2]
local userId = ARGV[1]

local stock = redis.call('GET', stockKey)
if not stock then
    return -1
end

-- 一人一单：用户已在集合里说明抢过了
if redis.call('SISMEMBER', usersKey, userId) == 1 then
    return 2
end

if tonumber(stock) <= 0 then
    return 1
end

redis.call('DECR', stockKey)
redis.call('SADD', usersKey, userId)
return 0
