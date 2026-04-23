# 分布式系统作业 · 高并发读 + 服务治理

覆盖课程两次作业：**高并发读** 与 **服务治理**。单仓库、多模块：

```
.
├── pom.xml                ← 父 POM（Spring Boot 3.4.3 / Cloud 2023.0.4 / Alibaba 2023.0.3.2）
├── service/               ← 业务服务：商品 / 秒杀 / 订单 / 库存
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/example/hw1
│       ├── cache, config, domain, dto, mapper, routing, service        # 读写分离 + Redis 缓存
│       ├── common/SnowflakeIdGenerator.java                            # 订单 ID
│       ├── seckill/{controller,service,messaging,dto}                  # 秒杀下单
│       ├── order/{controller,service,mapper,domain}                    # 订单
│       ├── inventory/{service,mapper,domain}                           # 库存
│       └── outbox/{service,mapper,domain}                              # 事务消息
├── gateway/               ← Spring Cloud Gateway + Nacos + Sentinel
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/example/gateway
├── nginx/                 ← 反向代理 / 动静分离
│   ├── nginx.conf
│   └── html/{index.html, css/, js/}
├── docker/mysql/          ← 主从 + 初始化 SQL（含 orders / inventory / outbox 建表）
├── docker-compose.yml     ← Redis / MySQL 主从 / Kafka / Nacos / 2× app / Gateway / Nginx
├── scripts/demo.sh
└── docs/
```

---

## 0. 一键启动

```bash
# 构建并启动所有基础设施 + 2 个 app 实例 + gateway + nginx
docker compose up -d --build

# 查看状态（等 nacos、kafka 变 healthy 后，app-1/app-2 会自动起来）
docker compose ps
docker compose logs -f app-1 | tail -n 30
```

端口映射一览：

| 组件 | 宿主机端口 | 说明 |
|---|---|---|
| Nginx | 80 | 前端 + 反向代理（`/api/**` → 两实例轮询） |
| app-1 | 8081 | 直连后端实例 1 |
| app-2 | 8082 | 直连后端实例 2 |
| Gateway | 9000 | Spring Cloud Gateway（Nacos 发现 LB） |
| Nacos | 8848 | 控制台：<http://localhost:8848/nacos> |
| Kafka | 9092 | KRaft 单节点 |
| Redis | 6380 | 产品缓存 / 秒杀库存 / 幂等锁 |
| MySQL master | 3307 | 写库 |
| MySQL slave | 3308 | 读库 |

访问：

- **前端页面**：<http://localhost/>  （Nginx 直接返回静态文件）
- **动态 API 经 Nginx**：<http://localhost/api/products/1>
- **API 经 Gateway（Nacos LB）**：<http://localhost:9000/api/products/1>
- **绕过 Nginx 直连实例**：<http://localhost:8081/api/debug/instance>

---

## 作业一：高并发读

### ① 容器环境

所有组件容器化，见 [`docker-compose.yml`](docker-compose.yml)。后端服务镜像由 [`service/Dockerfile`](service/Dockerfile) 构建（多阶段：maven build → JRE runtime）。

启动：

```bash
docker compose up -d --build
```

### ② 负载均衡（Nginx 反向代理 + 多实例）

- `app-1` 和 `app-2` 均运行同一个 Spring Boot jar，容器内端口 8080，映射到宿主机 8081/8082
- Nginx 监听宿主机 80，upstream 配两个后端，默认 **round_robin**

配置位置：[`nginx/nginx.conf`](nginx/nginx.conf)

```nginx
upstream hw1_backend {
    # least_conn;   # 改成 least-connections
    # ip_hash;      # 改成 ip-hash（会话亲和）
    server app-1:8080 max_fails=3 fail_timeout=10s;
    server app-2:8080 max_fails=3 fail_timeout=10s;
    keepalive 64;
}
```

#### 验证

响应头 `X-Upstream-Addr` 会显示命中的后端：

```bash
for i in {1..6}; do curl -sSI http://localhost/api/debug/instance | grep -i x-upstream-addr; done
curl -s http://localhost/api/debug/instance      # → {"instanceId":"app-1"} 或 "app-2"
```

切换算法：编辑 `nginx/nginx.conf`，放开 `least_conn;` 或 `ip_hash;`，`docker compose restart nginx`。

#### JMeter 压测

`JMeter → HTTP Sampler → http://localhost/api/products/1`，线程组 200 线程 × 60 s。观察：

- Nginx 默认 round_robin 下，`docker logs hw1-app-1 | wc -l` 与 `docker logs hw1-app-2 | wc -l` 应接近 1:1
- 切 `ip_hash` 后，单客户端 IP 请求会集中到同一实例

### ③ 动静分离

Nginx 同一 server 内：

- `/` 、`*.html` 、`*.{css,js,png,...}` → `/usr/share/nginx/html` 静态返回
- `/api/**` 、`/actuator/**` → 代理到 upstream

前端文件位于 [`nginx/html/`](nginx/html)。压测对比：

```bash
# 静态：~0.3 ms RT
ab -n 10000 -c 100 http://localhost/css/style.css

# 动态：~几 ms RT（命中 Redis 缓存时）
ab -n 10000 -c 100 http://localhost/api/products/1
```

### ④ 分布式缓存（沿用作业 1 初版，已保留）

- 商品详情 `GET /api/products/{id}` 采用 **Cache-Aside**
- **缓存穿透**：查库为空写 `NULL_CACHE` 短 TTL
- **缓存击穿**：Redis `SETNX` 互斥锁重建
- **缓存雪崩**：基础 TTL + 随机抖动 TTL

关键类：[`ProductServiceImpl`](service/src/main/java/com/example/hw1/service/ProductServiceImpl.java)、[`RedisProductCacheRepository`](service/src/main/java/com/example/hw1/service/RedisProductCacheRepository.java)、[`CachePolicy`](service/src/main/java/com/example/hw1/cache/CachePolicy.java)。

### ⑤ 消息队列（Kafka 削峰 + 雪花 ID + 幂等）

**秒杀流程**（见 [`SeckillService`](service/src/main/java/com/example/hw1/seckill/service/SeckillService.java)）：

```
POST /api/seckill/orders
        │
        ▼
  [Redis Lua 原子]          ← 防超卖
  DECRBY seckill:stock:{productId}
        │
        ▼
  [Redis SETNX]            ← 幂等：同一用户同一商品只能成功一次
  seckill:idem:{user}:{product}  = orderId
        │
        ▼
  Snowflake.nextId()  →  orderId
        │
        ▼
  Kafka send(seckill.order.create, key=orderId, value=OrderCreateEvent)
        │
        ▼
  立刻返回 {accepted, orderId, remainingStock}
```

**消费端**（同一进程，逻辑独立）：

```
seckill.order.create  → OrderService.createPendingOrder()
                           ├─ INSERT IGNORE INTO orders   (UNIQUE (user,product) 再兜底)
                           └─ 事务内写 outbox_message: inventory.deduct
                                                                │
                     OutboxPublisher 定时扫描 → 发 Kafka
                                                                ▼
inventory.deduct      → InventoryService.applyDeduct()
                           ├─ INSERT IGNORE INTO inventory_log(message_id)  ← 幂等
                           ├─ UPDATE inventory 扣减（乐观 WHERE available>=delta）
                           └─ 事务内写 outbox_message: order.status.update
                                                                │
                                                                ▼
order.status.update   → OrderService.applyStatus()   ← 更新订单状态
```

#### 雪花算法

[`SnowflakeIdGenerator`](service/src/main/java/com/example/hw1/common/SnowflakeIdGenerator.java)：64 位 = 1 符号 + 41 时间戳 + 5 datacenter + 5 worker + 12 序列。workerId / datacenterId 由 `APP_SNOWFLAKE_*` 环境变量传入（两个实例分别配 1/2）。

#### 按用户 ID / 订单 ID 查询

```bash
curl -s http://localhost/api/orders/{orderId}
curl -s http://localhost/api/orders?userId=1001
```

#### 幂等

三层：

1. **前置 Redis 幂等**：`seckill:idem:{user}:{product}` SETNX 拒绝重复下单
2. **DB 唯一键兜底**：`UNIQUE KEY uk_user_product (user_id, product_id)`
3. **Kafka 消费幂等**：`inventory_log.message_id` PK + `INSERT IGNORE`

#### 不超卖

- Redis Lua `DECRBY` 原子判断（跨实例、跨线程）
- 库存最终落库 `UPDATE ... WHERE available >= delta`（第二道保险）

### ⑥ 事务与一致性

**订单服务 + 库存服务**：逻辑独立 + 不同表（`orders`、`inventory`），通过 Kafka 通信，**事务消息（Transactional Outbox 模式）** 保证一致性：

- 任何业务写都**同事务**写一条 `outbox_message`；
- [`OutboxPublisher`](service/src/main/java/com/example/hw1/outbox/service/OutboxPublisher.java) 每 1 s 拉一批未发送、用 `SELECT ... FOR UPDATE SKIP LOCKED` 避免多实例重复投递，发送成功标记 `SENT`，失败计数重试；
- 消费端通过 `inventory_log` / 状态迁移（`UPDATE ... WHERE status=?`）保证幂等。

**下单 + 库存扣减一致性**：见上图。若 `inventory.available` 不足，`InventoryService.applyDeduct` 会发送 `CANCELLED` 状态事件，`SeckillEventConsumer#onOrderStatusUpdate` 收到后回滚 Redis 预扣与幂等槽。

**订单支付 + 订单状态更新一致性**：[`OrderService.pay`](service/src/main/java/com/example/hw1/order/service/OrderService.java) 在同一事务中：

1. `UPDATE orders SET status='PAID' WHERE id=? AND status='PENDING_PAYMENT'`（CAS）
2. `INSERT INTO outbox_message(... order.status.update ...)`

事务提交后，outbox 发布器推送状态事件，下游系统（物流、通知等）可订阅。

```bash
curl -X POST http://localhost/api/orders/{orderId}/pay
```

---

## 作业二：服务治理

### ① Nacos 注册发现 + 配置管理 + Spring Cloud Gateway

- `hw1-service` 和 `hw1-gateway` 都在 `@EnableDiscoveryClient` + `bootstrap.yml` 的 Nacos 配置下启动后自动注册
- Gateway 通过 `lb://hw1-service` 访问，由 Nacos 客户端做负载均衡
- 动态路由、限流规则可以写进 Nacos 配置（Data ID: `hw1-gateway.yaml` / `hw1-service.yaml`），参考 [`docs/nacos-seed.md`](docs/nacos-seed.md)

#### 使用网关调用

```bash
curl -s http://localhost:9000/api/products/1             # 网关 → Nacos 发现 → hw1-service
curl -s http://localhost:9000/hw1-service/api/products/1 # DiscoveryLocator 自动路由
```

#### 动态更新属性

1. Nacos 控制台发布 `hw1-service.yaml`：

   ```yaml
   app:
     dynamic:
       welcome-message: "Hello from Nacos!"
   ```

2. 访问：

   ```bash
   curl -s http://localhost:9000/api/dynamic/welcome
   # → Hello from Nacos! | instance=app-1
   ```

3. 修改为其它值并发布，**无需重启**，下一次请求即刻生效（由 `@RefreshScope` 驱动，见 [`DynamicConfigController`](service/src/main/java/com/example/hw1/controller/DynamicConfigController.java)）。

### ② 流量治理（限流 / 熔断 / 降级）

使用 Alibaba Sentinel：

| 维度 | 资源名 | 规则 | 位置 |
|---|---|---|---|
| 限流 | `seckill` | 200 QPS / 实例 | [`SentinelConfig`](service/src/main/java/com/example/hw1/config/SentinelConfig.java) |
| 限流 | `product-detail` | 500 QPS / 实例 | 同上 |
| 熔断 | `product-detail` | 10 s 内 ≥20 请求 且错误率 ≥50%，熔断 10 s | 同上 |
| 网关限流 | `hw1-service-api` | 500 req/s | [`GatewaySentinelConfig`](gateway/src/main/java/com/example/gateway/config/GatewaySentinelConfig.java) |

被限流/熔断时走 fallback（见 [`ProductController#onDetailBlocked`](service/src/main/java/com/example/hw1/controller/ProductController.java) 与 [`SeckillService#onBlocked`](service/src/main/java/com/example/hw1/seckill/service/SeckillService.java)），返回结构化降级响应：

```json
{ "source": "RATE_LIMITED", "description": "访问过于频繁，限流中", "nullCache": true }
```

#### JMeter 压测流量治理

- **限流**：`POST /api/seckill/orders` 线程组 500 线程、持续 30 s → 一部分返回 `code=RATE_LIMITED`
- **熔断**：构造失败（例如停掉 MySQL `docker stop hw1-mysql-slave`），反复调用 `/api/products/1`，达到错误率阈值后后续请求直接走降级响应；10 s 后自动半开探测

---

## 本地开发（不用 Docker）

适合只想跑原始 MySQL + Redis 的读写分离 + 缓存：

```bash
docker compose up -d redis mysql-master mysql-slave
mvn -pl service -am -Dmaven.repo.local=.m2 spring-boot:run
# 默认 profile=local，不会连 Nacos / Kafka
```

## 运行测试

```bash
mvn -pl service -am -Dmaven.repo.local=.m2 test
```

测试默认 `profile=local`，Kafka/Nacos 自动配置被排除，不需要任何外部依赖。

## 核心接口汇总

| Method | URL | 说明 |
|---|---|---|
| GET | `/api/products/{id}` | 商品详情（缓存） |
| POST | `/api/products` | 新增商品 |
| PUT | `/api/products/{id}` | 更新商品（回写主库 + 删缓存） |
| GET | `/api/debug/instance` | 查看当前实例 ID |
| GET | `/api/debug/db-route/{id}` | 走从库读（直接体现路由效果） |
| POST | `/api/debug/cache/warm/{id}` | 预热商品缓存 |
| POST | `/api/seckill/stock/{pid}?quantity=N` | 初始化 Redis 秒杀库存 |
| GET | `/api/seckill/stock/{pid}` | 查看剩余库存 |
| POST | `/api/seckill/orders` | 秒杀下单 |
| GET | `/api/orders/{id}` | 按订单 ID 查询 |
| GET | `/api/orders?userId=` | 按用户 ID 查询 |
| POST | `/api/orders/{id}/pay` | 支付 |
| GET | `/api/dynamic/welcome` | Nacos 动态属性 |

## 演示完整链路

```bash
# 1. 启动
docker compose up -d --build

# 2. 初始化秒杀库存
curl -X POST "http://localhost/api/seckill/stock/1?quantity=100"

# 3. 并发秒杀（bash 简易模拟；真实压测用 JMeter）
for u in 1001 1002 1003 1004; do
  curl -s -X POST http://localhost/api/seckill/orders \
    -H 'Content-Type: application/json' \
    -d "{\"userId\":$u,\"productId\":1}" &
done
wait

# 4. 查看订单
curl -s http://localhost/api/orders?userId=1001

# 5. 支付
ORDER_ID=$(curl -s http://localhost/api/orders?userId=1001 | jq -r '.[0].id')
curl -s -X POST http://localhost/api/orders/$ORDER_ID/pay

# 6. 再查一次
curl -s http://localhost/api/orders/$ORDER_ID
```

## 版本兼容性备忘

- Spring Boot `3.4.3` ←→ Spring Cloud `2023.0.4` ←→ Spring Cloud Alibaba `2023.0.3.2`（若版本不匹配可降级 SB 到 3.3.9）
- Kafka：`bitnami/kafka:3.8` KRaft 模式，免 Zookeeper
- Nacos：`nacos/nacos-server:v2.4.3` 单机模式，内置 Derby
- JDK 17
