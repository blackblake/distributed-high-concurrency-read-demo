# 高并发读作业示例项目

本项目用于完成“分布式软件原理与技术”课程作业中的必做要求：

- 引入 Redis，实现商品详情页缓存
- 处理缓存穿透、缓存击穿、缓存雪崩问题
- 搭建 MySQL 读写分离环境，并在代码中验证读写分离效果

项目未实现 ElasticSearch，因为题目中将其标为可选。

## 技术栈

- Java 17+
- Spring Boot 3.4
- MyBatis
- Redis
- MySQL 8 主从复制
- Docker Compose

## 项目结构

- `src/main/java/com/example/hw1/controller`
  - REST 接口
- `src/main/java/com/example/hw1/service`
  - 缓存逻辑、主从读写逻辑、Redis 实现
- `src/main/java/com/example/hw1/routing`
  - 动态数据源与读写路由注解
- `src/main/java/com/example/hw1/mapper`
  - MyBatis Mapper
- `src/main/resources/mapper`
  - Mapper XML
- `docker`
  - MySQL Master/Slave 初始化与配置
- `scripts/demo.sh`
  - 演示脚本

## 运行环境

需要本机安装：

- `docker`
- `docker compose`
- `java`
- `mvn`

## 启动基础设施

```bash
docker compose up -d
```

查看容器状态：

```bash
docker compose ps
```

如需查看从库复制状态：

```bash
docker exec -it hw1-mysql-slave mysql -uroot -prootpass -e "SHOW REPLICA STATUS\G"
```

重点字段：

- `Replica_IO_Running: Yes`
- `Replica_SQL_Running: Yes`

## 运行测试

```bash
mvn -Dmaven.repo.local=.m2 test
```

说明：

- 在 Codex 沙箱内我使用了项目内 `.m2` 目录避免权限问题
- 你在本机直接执行时也可以继续沿用这个命令

## 启动应用

```bash
mvn -Dmaven.repo.local=.m2 spring-boot:run
```

应用默认端口：

- `8080`
- Redis 映射端口：`6380`

## 核心接口

### 1. 新增商品

```bash
curl -s -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Mechanical Keyboard",
    "price":499.00,
    "stock":80,
    "description":"Hot-sale keyboard for cache demo",
    "status":"ONLINE"
  }'
```

### 2. 查询商品详情

```bash
curl -s http://localhost:8080/api/products/1
```

返回中的 `source` 字段用于演示数据来源：

- `CACHE`：命中 Redis
- `SLAVE_DB`：从库读取并回填缓存
- `MASTER_DB`：主库写入后返回
- `NULL_CACHE`：命中空值缓存

### 3. 更新商品

```bash
curl -s -X PUT http://localhost:8080/api/products/1 \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Mechanical Keyboard Pro",
    "price":599.00,
    "stock":66,
    "description":"Updated description",
    "status":"ONLINE"
  }'
```

### 4. 调试读路由

```bash
curl -s http://localhost:8080/api/debug/db-route/1
```

### 5. 预热缓存

```bash
curl -s -X POST http://localhost:8080/api/debug/cache/warm/1
```

## 作业要求对应说明

### 分布式缓存

- 商品详情接口 `GET /api/products/{id}` 采用 Cache-Aside
- 缓存穿透：使用空值缓存
- 缓存击穿：使用 Redis 互斥锁重建缓存
- 缓存雪崩：使用基础 TTL + 随机抖动 TTL

### 读写分离

- 写操作走 MySQL Master
- 读操作走 MySQL Slave
- 通过 `@ReadOnlyRoute + AbstractRoutingDataSource` 实现路由
- 返回体中的 `source` 字段可直接观察缓存/主库/从库来源

## 推荐演示流程

1. 启动 `docker compose`
2. 启动 Spring Boot 应用
3. 先查询一个已有商品两次
   - 第一次看到 `SLAVE_DB`
   - 第二次看到 `CACHE`
4. 更新商品
   - 观察返回 `MASTER_DB`
5. 再次查询商品
   - 观察缓存已删除并重新回填
6. 查询不存在的商品 ID 两次
   - 第二次应命中 `NULL_CACHE`

## 一键演示

```bash
bash scripts/demo.sh
```
