# 校园限量抢购平台

一个完整的校园限定商品秒杀系统：**Spring Boot 后端 + Vue3 管理后台 + uni-app 用户端**。

核心要解决的问题是「高并发下的不超卖」：库存扣减放在 Redis 用 Lua 原子完成，订单落库交给 RabbitMQ 异步削峰，未支付订单到期自动取消并回补库存。

> 具体链路、并发正确性论证、故障兜底见 [docs/architecture.md](docs/architecture.md)。

```
用户端 (uni-app)  ──┐
                    ├──> Spring Boot (8080) ──> MySQL 8     业务数据
管理后台 (Vue3)  ──┘                        ├──> Redis        库存 / 缓存 / 限流
                                            └──> RabbitMQ     异步下单 / 延迟关单
```

---

## 技术栈

| 模块 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.5.16、MyBatis-Plus 3.5.17、MySQL 8、Redis、RabbitMQ 4.4.0、JWT (jjwt 0.12.7)、BCrypt |
| 管理后台 | Vue 3.5、TypeScript、Vite 8、Element Plus 2.14、Pinia、vue-router、axios |
| 用户端 | uni-app 3（Vue 3.4）+ Vite 5 + TypeScript + Sass，一套代码编译 H5 / 微信小程序 |
| 运维 | Actuator + Micrometer（Prometheus 指标） |

---

## 功能清单

### 用户端
- 活动首页：进行中 / 未开始 / 已结束分段筛选，倒计时、库存消耗进度条
- 活动详情：Redis 实时库存轮询、倒计时到点自动解锁、抢购按钮状态机
- 抢购：下单后跳转结果页，轮询异步下单结果（排队中 → 抢到 / 未抢到）
- 我的订单：订单列表、待支付订单可直接支付、累计成交只统计已支付
- 商品列表：关键词搜索 + 分类筛选 + 上拉加载；商品详情可跳转关联活动
- 登录注册：JWT，登录态本地持久化

### 管理后台
- 登录（axios 拦截器自动带 JWT，401 自动跳登录）
- 商品管理：增删改查、分页、关键词 / 分类搜索
- 秒杀活动：新建（创建即预热库存）、手动预热、查看 Redis 实时库存
- 订单管理：分页查看全部订单与状态

### 后端核心能力
- 用户注册 / 登录 / JWT 鉴权（拦截器 + ThreadLocal 传递当前用户）
- 商品详情缓存：Cache-Aside、空值防穿透、TTL 随机偏移防雪崩
- 秒杀下单：Lua 原子扣减 + 一人一单 + Redis 补偿
- 异步下单：RabbitMQ 手动 ACK、双重幂等、死信队列
- 超时关单：TTL + 死信延迟队列，15 分钟未支付自动取消并回补库存，另有定时任务兜底
- 接口限流：`@RateLimit` 注解 + AOP + Redis Lua 固定窗口（USER / IP / GLOBAL 三个维度）
- 监控：`/actuator/health`、`/actuator/metrics`、`/actuator/prometheus`
- 自动化测试：JUnit5 + MockMvc，覆盖并发不超卖、一人一单、限流、支付与超时关单幂等

---

## 快速开始

### 1. 环境准备

| 依赖 | 版本 | 默认端口 |
|---|---|---|
| JDK | 17 | - |
| Maven | 3.6+ | - |
| MySQL | 8.x | 3306 |
| Redis | 3.x / 5.x 均可 | 6379 |
| RabbitMQ | 4.x（需先装 Erlang） | 5672 / 15672 |
| Node.js | 18+（开发时使用 22.14） | - |

> RabbitMQ 在 Windows 上如果主机名包含中文，Erlang 节点名必须是 ASCII，启动前要显式指定：
>
> ```powershell
> $env:ERLANG_HOME = 'D:\RabbitMQ\Erlang'
> $env:RABBITMQ_BASE = 'D:\RabbitMQ\data'
> $env:RABBITMQ_NODENAME = 'rabbit@localhost'
> & 'D:\RabbitMQ\rabbitmq_server-4.4.0\sbin\rabbitmq-server.bat'
> ```
>
> 管理台：http://localhost:15672 （guest / guest），启用命令：`rabbitmq-plugins enable rabbitmq_management`

### 2. 初始化数据库

先建库（表结构由后端启动时的 Flyway 自动创建）：

```bash
mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

表结构迁移脚本在 `seckill-backend/src/main/resources/db/migration/`，启动后端时会自动执行未应用过的版本。

想要演示数据（4 件商品、3 个用户、3 场秒杀活动），再执行一次种子脚本：

```bash
mysql -uroot -p123456 --default-character-set=utf8mb4 < seckill-backend/sql/init.sql
```

> `init.sql` 会**先删表再建表**并写入种子数据，属于「重置演示环境」脚本。
> 日常改表请新增 `V2__xxx.sql` 这样的迁移文件，不要修改已执行过的迁移脚本（Flyway 会校验 checksum）。

### 3. 启动后端

```bash
cd seckill-backend
mvn spring-boot:run
# 或
mvn -DskipTests package
java -jar target/seckill-backend-0.0.1-SNAPSHOT.jar
```

启动后确认依赖都通：

```bash
curl http://localhost:8080/actuator/health   # db / redis / rabbit 都应为 UP
```

### 4. 启动管理后台

```bash
cd seckill-admin
npm install
npm run dev      # http://localhost:5173
```

Vite 已配置 `/api`、`/actuator` 代理到 `http://localhost:8080`，开发环境不需要处理跨域。

### 5. 启动用户端

```bash
cd seckill-uniapp
npm install
npm run dev:h5          # http://localhost:5273
npm run build:mp-weixin # 小程序产物在 dist/build/mp-weixin
```

- H5：vite 代理 `/api` 到 8080
- 小程序：直连 `http://localhost:8080/api`，需在微信开发者工具里勾选「不校验合法域名」

### 演示账号

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | 123456 | 管理员 |
| student01 | 123456 | 普通用户 |
| student02 | 123456 | 普通用户 |

### 6. 运行测试（可选）

测试用独立的 `seckill_test` 库 + Redis 15 号库，不会污染开发环境的数据：

```bash
# 只需执行一次，建测试库（表由测试启动时自动建）
mysql -uroot -p123456 --default-character-set=utf8mb4 < seckill-backend/sql/init-test.sql

cd seckill-backend
mvn test
```

覆盖场景：

| 测试类 | 用例 |
|---|---|
| `SeckillConcurrencyTest` | 30 个用户并发抢 10 件，恰好 10 单成功、库存为 0；同一用户重复抢购返回 409 且库存只扣一次 |
| `SeckillApiTest` | 无 token / 非法 token 返回 401；1 秒内连打 10 次抢购，5 次通过限流、其余 429 |
| `OrderLifecycleTest` | 支付幂等；他人订单返回 404；超时关单库存只回补一次；已支付订单不会被取消；已取消订单不能支付 |

测试中 `RabbitTemplate` 被 mock（`@MockitoBean`），不会真的往开发环境的队列投消息；Rabbit 监听器通过 `spring.rabbitmq.listener.simple.auto-startup=false` 关闭。

---

## API 速览

统一响应结构，业务状态放在 `code` 里（HTTP 状态码仍为 200）：

```json
{ "code": 200, "message": "操作成功", "data": {} }
```

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数错误 / 库存不足 / 活动未开始或已结束 |
| 401 | 未登录或 token 失效 |
| 404 | 资源不存在 |
| 409 | 重复下单 / 订单状态冲突 |
| 429 | 触发限流 |
| 500 | 服务端异常 |

### 用户与商品（公开）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/user/register` | 注册 |
| POST | `/api/user/login` | 登录，返回 `{token, userId, username, nickname, role}` |
| GET | `/api/user/me` | 当前登录用户（需 token） |
| GET | `/api/goods/page` | 商品分页，支持 `keyword`、`category`，`pageSize` 上限 100 |
| GET | `/api/goods/{id}` | 商品详情（走 Redis 缓存） |

### 秒杀活动（公开浏览）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/seckill/activity/page` | 活动分页 |
| GET | `/api/seckill/activity/{id}` | 活动详情，含 Redis 实时库存与状态文案 |

### 秒杀下单（需登录）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/seckill/order/{activityId}` | 抢购，立即返回「排队中」；限流 5 次/秒/用户 |
| GET | `/api/seckill/order/result/{activityId}` | 轮询抢购结果（0 排队中 / 1 成功 / 2 未抢到） |
| GET | `/api/seckill/order/my` | 我的订单 |
| POST | `/api/seckill/order/{orderNo}/pay` | 支付订单（演示实现：直接置为已支付） |

### 管理接口（需管理员 token）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST / PUT / DELETE | `/api/admin/goods`、`/api/admin/goods/{id}` | 商品增改删 |
| POST | `/api/admin/seckill/activity` | 新建活动（自动预热库存） |
| POST | `/api/admin/seckill/activity/{id}/preheat` | 手动预热库存 |
| POST | `/api/admin/seckill/activity/preheat-pending` | 批量预热即将开始的活动 |
| GET | `/api/admin/order/page` | 订单分页 |

---

## 关键设计

### 1. 为什么库存扣减放在 Redis + Lua

秒杀场景下，数据库行锁扛不住瞬时并发。把「判断库存 → 扣减库存 → 记录参与用户」合成一段 Lua 脚本，Redis 单线程执行、不可中断，天然没有并发穿插：

```lua
if redis.call('SISMEMBER', usersKey, userId) == 1 then return 2 end   -- 一人一单
if tonumber(stock) <= 0 then return 1 end                            -- 库存不足
redis.call('DECR', stockKey)
redis.call('SADD', usersKey, userId)
return 0
```

拆成多条命令（GET + DECR）会出现多个请求读到同一库存值，直接超卖。

### 2. 异步下单与削峰

抢购接口只做「扣 Redis + 发消息」，不写数据库；消费者按自己的节奏落库。请求高峰被 MQ 吸收，数据库压力被摊平。

可靠性：

- 手动 ACK：落库成功才确认，失败 `nack` 进死信队列，避免消息丢
- 双重幂等：先查一次 + 数据库唯一索引 `uk_user_activity` 兜底
- 发送失败回滚：MQ 投递异常时把 Redis 库存加回去并移除参与标记，避免少卖

### 3. 超时关单与库存回补

```
下单落库 ──> 延迟队列(TTL 15min) ──死信──> 超时队列 ──> 消费者
                                                      │
                        订单仍待支付? ──是──> 置为已取消 + Redis 库存 +1
                                     └─否──> 忽略（已支付）
```

幂等靠 `UPDATE ... WHERE status = 0` 的条件更新：延迟消息和定时补偿任务同时盯上同一订单时，只有真正把状态改掉的那一次才回补库存。

### 4. 缓存策略

- Cache-Aside：读未命中查库后回写，更新/删除商品时删缓存
- 防穿透：不存在的商品缓存 `__EMPTY__` 占位并给短 TTL
- 防雪崩：TTL 加随机偏移，避免大量 key 同时失效

---

## 实测记录

| 场景 | 方法 | 结果 |
|---|---|---|
| 超卖验证 | 30 并发抢 10 件库存 | 恰好 10 单成功，库存不为负 |
| 一人一单 | 同一账号重复请求 | 首次 200，后续 409 |
| 接口限流 | 1 秒内连打 10 次抢购接口 | 5 次通过限流，其余 429 |
| 压测 | 100 并发抢购 | 总耗时 653ms，QPS ≈ 153，零超卖 |
| 异步最终一致 | 活动库存 50，观察订单数变化 | 27 → 45 → 50，最终完全一致 |
| 超时关单 | 未支付订单到期 | 状态变「已取消」，Redis 库存 +1 |
| 支付 | 支付后等待超时到期 | 状态保持「已支付」，未被取消 |

> 超时时长默认 15 分钟，可用 `--seckill.order.timeout-minutes=1` 临时缩短验证。注意延迟队列的 TTL 是声明时写入 Broker 的，改值需要先删掉 `seckill.order.delay.queue` 才会生效。

---

## 目录结构

```
seckill-platform/
├── seckill-backend/            # Spring Boot 后端
│   ├── sql/init.sql            # 建表 + 种子数据
│   └── src/main/java/com/seckill/
│       ├── annotation/         # @RateLimit
│       ├── aspect/             # 限流切面
│       ├── common/             # Result / CacheKeys / UserContext
│       ├── config/             # RabbitMQ、WebMvc、MyBatisPlus
│       ├── controller/         # 接口
│       ├── dto/ entity/ vo/    # 请求体 / 实体 / 展示对象
│       ├── exception/          # 业务异常 + 全局异常处理
│       ├── interceptor/        # 登录拦截器
│       ├── listener/           # 订单消费者 / 超时消费者
│       ├── mapper/ service/    # 持久层与业务层
│       └── task/               # 库存预热、超时关单补偿
├── seckill-admin/              # Vue3 管理后台
├── seckill-uniapp/             # uni-app 用户端
└── docs/architecture.md        # 架构与核心链路详解
```

---

## 已知限制

- 支付是演示实现（直接把订单置为已支付），没有接真实支付渠道
- 测试覆盖核心链路（下单并发、限流、订单状态），商品与活动的 CRUD 尚未覆盖
- 定时任务基于单机 `@Scheduled`，多实例部署需要加分布式锁
- JWT 没有 refresh token 与登出黑名单
