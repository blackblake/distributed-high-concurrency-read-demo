# 高并发读作业设计

**作业目标**

构建一个从零可运行的 Spring Boot 示例项目，覆盖课程图片中的必做要求：
- 引入 Redis，实现商品详情页缓存
- 处理缓存穿透、缓存击穿、缓存雪崩
- 搭建 MySQL 读写分离环境，并在代码中验证读写分离效果

**总体方案**

项目采用单体应用方案：`Spring Boot + MyBatis + Redis + MySQL Master/Slave + Docker Compose`。
应用内部通过动态数据源路由实现读写分离，通过 Cache-Aside 模式实现商品详情缓存。为了方便课程验收，系统保留最小业务闭环，只实现 `Product` 领域对象及少量调试接口。

**架构设计**

系统由四部分组成：
- `app`：Spring Boot 应用，对外提供 REST API
- `redis`：缓存商品详情，并承担热点重建锁
- `mysql-master`：写库，负责新增和更新
- `mysql-slave`：读库，通过主从复制承担读请求

应用内部分层如下：
- `controller`：对外接口
- `service`：缓存逻辑、写后删缓存、读写路由控制
- `mapper`：MyBatis 数据访问
- `routing`：动态数据源、ThreadLocal 上下文、AOP 注解
- `cache`：Redis 序列化、空值缓存、互斥锁与 TTL 策略

**核心数据流**

1. 查询商品详情
   - 先查 Redis
   - 命中则直接返回
   - 未命中则尝试获取热点锁
   - 拿到锁后查从库，写回缓存并返回
   - 未拿到锁则短暂等待并重试缓存

2. 新增或更新商品
   - 写主库
   - 删除对应详情缓存
   - 返回主库最新结果

3. 查询不存在商品
   - 从库查不到时写入短 TTL 空值缓存
   - 后续相同请求直接命中空值缓存，避免穿透

**缓存策略**

- Key：`product:detail:{id}`
- 正常商品缓存：基础 TTL 30 分钟，外加 0-10 分钟随机值
- 空值缓存：短 TTL 2 分钟
- 热点锁：`lock:product:{id}`，短 TTL 10 秒

对应问题处理方式：
- 缓存穿透：空值缓存
- 缓存击穿：Redis 互斥锁重建缓存
- 缓存雪崩：随机过期时间 + 预热接口

**读写分离策略**

使用 `AbstractRoutingDataSource` 挂载双数据源：
- `MASTER`
- `SLAVE`

路由方式：
- 写接口默认走 `MASTER`
- 读接口在 Service 层通过注解切换到 `SLAVE`
- 若需要返回写后最新数据，则直接使用主库查询并返回

为了便于验证，查询接口响应体中会包含本次命中的数据来源，例如：
- `CACHE`
- `SLAVE_DB`
- `MASTER_DB`
- `NULL_CACHE`

**接口设计**

- `POST /api/products`
  - 新增商品
- `PUT /api/products/{id}`
  - 更新商品
- `GET /api/products/{id}`
  - 查询商品详情，体现缓存与读写分离
- `POST /api/debug/cache/warm/{id}`
  - 预热商品缓存
- `GET /api/debug/db-route/{id}`
  - 返回当前商品读取来源，便于演示读写分离与缓存命中

**测试与验收**

自动化测试分三层：
- 单元测试：缓存 TTL、空值缓存、随机过期、路由上下文
- 服务测试：缓存命中、缓存未命中回填、缓存击穿锁逻辑
- 集成测试：控制器与响应结构

验收方式分两部分：
- `docker compose up -d` 拉起 Redis 与 MySQL 主从
- `mvn test` 验证自动化测试
- 使用 README 中的 curl 命令完成演示：
  - 新增商品
  - 首次查详情触发从库读取
  - 再次查详情命中缓存
  - 更新商品并删缓存
  - 再次查详情触发缓存重建
  - 查询不存在商品验证空值缓存

**取舍说明**

- 不实现前端页面，使用 REST API 代表“商品详情页缓存”
- 不实现 ElasticSearch，因为图片中将其标为可选
- 不扩展用户、订单、搜索等无关领域，确保作业重点集中在缓存与读写分离
