# 高并发读作业 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a runnable Spring Boot homework project that demonstrates Redis-backed product detail caching, cache penetration/breakdown/avalanche protection, and MySQL read/write splitting with observable behavior.

**Architecture:** A single Spring Boot application talks to Redis plus a MySQL master/slave pair started by Docker Compose. Product detail reads use Cache-Aside and route DB reads to the slave, while writes go to the master and evict cache entries. Debug metadata and scripts make the homework easy to demonstrate.

**Tech Stack:** Java 17+, Spring Boot, MyBatis, HikariCP, Redis, MySQL 8, Docker Compose, JUnit 5, Mockito

---

### Task 1: Scaffold build and environment files

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `docker/mysql/master/conf/my.cnf`
- Create: `docker/mysql/slave/conf/my.cnf`
- Create: `docker/mysql/master/init/01-schema.sql`
- Create: `docker/mysql/master/init/02-seed.sql`
- Create: `docker/mysql/slave/init/01-init-slave.sh`
- Create: `src/main/resources/application.yml`

**Step 1: Write the failing environment smoke test**

Create a test that loads Spring context properties needed by Redis and dual MySQL data sources.

**Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ApplicationPropertiesTest test`
Expected: FAIL because build files and configuration are missing.

**Step 3: Write minimal implementation**

Add Maven dependencies, application config, and Docker files for MySQL master/slave plus Redis.

**Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ApplicationPropertiesTest test`
Expected: PASS.

### Task 2: Add product domain and REST contracts

**Files:**
- Create: `src/test/java/com/example/hw1/controller/ProductControllerContractTest.java`
- Create: `src/main/java/com/example/hw1/Hw1Application.java`
- Create: `src/main/java/com/example/hw1/controller/ProductController.java`
- Create: `src/main/java/com/example/hw1/controller/DebugController.java`
- Create: `src/main/java/com/example/hw1/dto/CreateProductRequest.java`
- Create: `src/main/java/com/example/hw1/dto/UpdateProductRequest.java`
- Create: `src/main/java/com/example/hw1/dto/ProductDetailResponse.java`
- Create: `src/main/java/com/example/hw1/domain/Product.java`

**Step 1: Write the failing test**

Write controller contract tests for:
- create product returns 201
- get product returns source metadata
- update product returns updated detail

**Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ProductControllerContractTest test`
Expected: FAIL because controller classes do not exist.

**Step 3: Write minimal implementation**

Create application entrypoint, DTOs, domain model, and placeholder controllers wired to mocked services.

**Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ProductControllerContractTest test`
Expected: PASS.

### Task 3: Implement dynamic data source routing

**Files:**
- Create: `src/test/java/com/example/hw1/routing/DataSourceRoutingTest.java`
- Create: `src/main/java/com/example/hw1/routing/DataSourceType.java`
- Create: `src/main/java/com/example/hw1/routing/DataSourceContextHolder.java`
- Create: `src/main/java/com/example/hw1/routing/ReadOnlyRoute.java`
- Create: `src/main/java/com/example/hw1/routing/ReadOnlyRouteAspect.java`
- Create: `src/main/java/com/example/hw1/config/DataSourceConfig.java`

**Step 1: Write the failing test**

Verify:
- default route is `MASTER`
- annotated read method switches to `SLAVE`
- route is cleared after method exit

**Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=DataSourceRoutingTest test`
Expected: FAIL because routing infrastructure is missing.

**Step 3: Write minimal implementation**

Add routing enum, ThreadLocal holder, annotation, AOP aspect, and dual-data-source configuration with `AbstractRoutingDataSource`.

**Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=DataSourceRoutingTest test`
Expected: PASS.

### Task 4: Implement persistence layer

**Files:**
- Create: `src/test/java/com/example/hw1/mapper/ProductMapperTest.java`
- Create: `src/main/java/com/example/hw1/mapper/ProductMapper.java`
- Create: `src/main/resources/mapper/ProductMapper.xml`

**Step 1: Write the failing test**

Test insert, update, and find-by-id mapper contracts with an in-memory or mocked DB boundary.

**Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ProductMapperTest test`
Expected: FAIL because mapper is missing.

**Step 3: Write minimal implementation**

Create MyBatis mapper interface and XML for CRUD used by the homework.

**Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ProductMapperTest test`
Expected: PASS.

### Task 5: Implement cache policy helpers

**Files:**
- Create: `src/test/java/com/example/hw1/cache/CachePolicyTest.java`
- Create: `src/main/java/com/example/hw1/cache/CacheKeys.java`
- Create: `src/main/java/com/example/hw1/cache/CacheValue.java`
- Create: `src/main/java/com/example/hw1/cache/CachePolicy.java`

**Step 1: Write the failing test**

Verify:
- detail key format is correct
- normal TTL adds jitter
- null-cache TTL is short

**Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=CachePolicyTest test`
Expected: FAIL because cache helpers are missing.

**Step 3: Write minimal implementation**

Add cache key builder, cached payload wrapper, and TTL policy utility.

**Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=CachePolicyTest test`
Expected: PASS.

### Task 6: Implement product service with cache-aside logic

**Files:**
- Create: `src/test/java/com/example/hw1/service/ProductServiceCachingTest.java`
- Create: `src/main/java/com/example/hw1/service/ProductService.java`
- Create: `src/main/java/com/example/hw1/service/ProductServiceImpl.java`

**Step 1: Write the failing test**

Verify:
- cache hit returns cached product with `CACHE`
- DB miss stores null cache and returns not found metadata
- cache miss rebuilds from DB and returns `SLAVE_DB`
- update evicts cache after write

**Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ProductServiceCachingTest test`
Expected: FAIL because service implementation is missing.

**Step 3: Write minimal implementation**

Implement create, update, read, warm-cache, and debug-read methods with Cache-Aside.

**Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ProductServiceCachingTest test`
Expected: PASS.

### Task 7: Add cache-breakdown lock behavior

**Files:**
- Create: `src/test/java/com/example/hw1/service/ProductServiceLockingTest.java`
- Modify: `src/main/java/com/example/hw1/service/ProductServiceImpl.java`

**Step 1: Write the failing test**

Verify that when cache miss occurs and lock acquisition fails, the service retries cache briefly before falling back.

**Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ProductServiceLockingTest test`
Expected: FAIL because locking behavior is absent.

**Step 3: Write minimal implementation**

Add Redis lock acquisition, unlock safety, bounded retry, and fallback behavior.

**Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ProductServiceLockingTest test`
Expected: PASS.

### Task 8: Add integration tests and demonstration docs

**Files:**
- Create: `src/test/java/com/example/hw1/controller/ProductIntegrationTest.java`
- Create: `README.md`
- Create: `scripts/demo.sh`

**Step 1: Write the failing test**

Add a Spring MVC integration test that validates JSON response shape and source fields.

**Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ProductIntegrationTest test`
Expected: FAIL because application wiring is incomplete.

**Step 3: Write minimal implementation**

Complete remaining wiring, write README runbook, and add a demo script with curl commands.

**Step 4: Run test to verify it passes**

Run: `mvn test`
Expected: PASS.

### Task 9: Verify runtime environment

**Files:**
- Modify: `README.md`

**Step 1: Start infrastructure**

Run: `docker compose up -d`
Expected: Redis, MySQL master, and MySQL slave become healthy.

**Step 2: Run application**

Run: `mvn spring-boot:run`
Expected: App starts on port 8080.

**Step 3: Execute demo**

Run: `bash scripts/demo.sh`
Expected: Script shows write to master, read from slave on first fetch, cache hit on second fetch, cache eviction after update, and null-cache behavior.
