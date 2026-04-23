# Nacos 初始化配置

Nacos 启动后（`docker compose up -d nacos`），打开控制台 <http://localhost:8848/nacos> (用户名/密码均为 `nacos/nacos`)，
在 **配置管理 → 配置列表 → 公共 (public)** 下新建以下两条配置。

## 1. `hw1-service.yaml`（Data ID）

```yaml
app:
  dynamic:
    welcome-message: "Hello from Nacos!"
```

发布后调用 <http://localhost:8081/api/dynamic/welcome> 即可看到新值（无需重启）。

若修改内容后未生效，确认 service 侧启动日志包含：

```
Located property source: [BootstrapPropertySource {name='bootstrapProperties-hw1-service.yaml,public'} ...]
```

## 2. `hw1-gateway.yaml`（Data ID）

可用于网关路由/限流的热更新，示例：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: dynamic-echo
          uri: lb://hw1-service
          predicates:
            - Path=/echo/**
          filters:
            - StripPrefix=1
```

## 3. 服务注册发现

启动 `app-1`、`app-2`、`gateway` 后，进入 **服务管理 → 服务列表** 应看到：

- `hw1-service` 实例 2 个
- `hw1-gateway` 实例 1 个

调用 <http://localhost:9000/api/products/1> 即可通过 Gateway → Nacos 负载均衡路由到 hw1-service。
