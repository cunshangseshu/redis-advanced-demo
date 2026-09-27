# Redis Advanced Demo

基于 **Java 21 + Spring Boot 3.5.16 + Spring Data Redis + Redis 7.4** 的 Redis 学习与实践项目。

本项目以“**边学、边写、边验证**”为核心方式，通过 Controller → Service → Redis / MySQL / RabbitMQ 的完整调用链，逐步学习 Redis 的常用数据结构、序列化、缓存保护、缓存一致性、RabbitMQ 异步补偿、分布式锁、高可用与集群等内容。

---

## 技术栈

- Java 21
- Spring Boot 3.5.16
- Spring Web
- Spring Data Redis
- Lettuce
- Redis 7.4
- MySQL 8.4
- MyBatis-Plus 3.5.17
- RabbitMQ 4（Management）
- Spring AMQP
- Spring Retry
- Docker Compose
- Spring Boot Actuator
- Maven

---

## 当前项目结构

```text
redis-advanced-demo
├── docker-compose.yml
├── pom.xml
├── src
│   └── main
│       ├── java/com/cunshang/redisadvanced
│       │   ├── RedisAdvancedDemoApplication.java
│       │   ├── common
│       │   │   ├── ApiResponse.java
│       │   │   └── ResultCode.java
│       │   ├── config
│       │   │   ├── RedisConfig.java
│       │   │   ├── RabbitMqConfig.java
│       │   │   └── mq
│       │   │       └── CacheInvalidationConsumer.java
│       │   ├── controller
│       │   │   └── RedisFoundationController.java
│       │   ├── entity
│       │   │   └── UserProfile.java
│       │   ├── exception
│       │   │   ├── BusinessException.java
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── mapper
│       │   │   └── UserProfileMapper.java
│       │   ├── model
│       │   │   ├── RedisUserProfile.java
│       │   │   ├── message
│       │   │   │   └── CacheInvalidationMessage.java
│       │   │   └── request
│       │   │       └── UpdateUserProfileRequest.java
│       │   └── service
│       │       ├── CacheAsideService.java
│       │       └── RedisFoundationService.java
│       └── resources
│           └── application.yml
├── mysql-data
└── redis-data
```

> `redis-data/` 与 `mysql-data/` 为本地持久化目录，应通过 `.gitignore` 排除，不提交到 Git 仓库。

---

## 运行方式

### 1. 启动 Redis、MySQL 与 RabbitMQ

```bash
docker compose up -d
```

当前 Docker 配置：

```text
Redis
├── 镜像：redis:7.4-alpine
├── 容器名称：redis-advanced-learning
├── 宿主机端口：6380
├── 容器端口：6379
└── AOF：开启

MySQL
├── 镜像：mysql:8.4
├── 容器名称：redis-advanced-mysql
├── 宿主机端口：3307
├── 容器端口：3306
├── 数据库：redis_learning
├── 字符集：utf8mb4
└── 排序规则：utf8mb4_0900_ai_ci

RabbitMQ
├── 镜像：rabbitmq:4-management
├── 容器名称：redis-advanced-rabbitmq
├── AMQP 端口：5672
├── Management 端口：15672
├── 用户名：guest
└── 密码：guest
```

### 2. 验证 Redis

```bash
docker exec -it redis-advanced-learning redis-cli
```

```redis
PING
```

预期：

```text
PONG
```

### 3. 验证 MySQL

```bash
docker exec -it redis-advanced-mysql mysql -uroot -p123456
```

```sql
USE redis_learning;
SELECT * FROM user_profile;
```

### 4. 启动 Spring Boot

应用默认端口：

```text
8080
```

中间件 / 数据源连接：

```text
Redis：localhost:6380
MySQL：localhost:3307/redis_learning
RabbitMQ：localhost:5672
RabbitMQ Management：http://localhost:15672
```

Actuator 当前开放：

```text
/actuator/health
/actuator/info
```

---

# 学习进度

| 模块 | 状态 |
|---|---|
| String | ✅ |
| Hash | ✅ |
| List | ✅ |
| Set | ✅ |
| ZSet | ✅ |
| 统一 API 响应 | ✅ |
| HTTP / 业务状态码 | ✅ |
| 全局异常处理 | ✅ |
| WRONGTYPE 类型冲突处理 | ✅ |
| Bitmap | ✅ |
| HyperLogLog | ✅ |
| GEO | ✅ |
| Stream | ✅ |
| Spring Boot Redis 对象存储与序列化 | ✅ |
| Pipeline / 批量操作 | ✅ |
| RedisTemplate / StringRedisTemplate / Serializer | ✅ |
| Cache Aside | ✅ |
| 缓存穿透 / 击穿 / 雪崩 | ✅ |
| Redis + MySQL 缓存一致性 | ⏳（写路径 + 同步重试 + RabbitMQ 异步补偿 + Consumer Retry + Error Queue 已完成；最终兜底待实现） |
| 分布式锁 | ⏳ |
| Lua / MULTI / EXEC / WATCH | ⏳ |
| RDB / AOF | ⏳ |
| 主从复制 | ⏳ |
| Sentinel | ⏳ |
| Redis Cluster | ⏳ |
| Hot Key / Big Key / Slowlog | ⏳ |
| Spring Boot 日志规范 / AOP 链路日志 | ⏳ |

---

# 统一 API 响应

项目目前使用：

```java
public record ApiResponse<T>(
        String code,
        String message,
        T data
) {
}
```

成功响应示例：

```json
{
  "code": "SUCCESS",
  "message": "success",
  "data": "hello"
}
```

异常响应示例：

```json
{
  "code": "BAD_REQUEST",
  "message": "请求参数错误",
  "data": null
}
```

当前已定义业务状态：

| 业务码 | HTTP Status | 含义 |
|---|---:|---|
| SUCCESS | 200 | 请求成功 |
| BAD_REQUEST | 400 | 请求参数错误 |
| NOT_FOUND | 404 | 资源不存在 |
| CONFLICT | 409 | 资源状态冲突 |
| REDIS_KEY_TYPE_CONFLICT | 409 | Redis Key 类型冲突 |
| REDIS_UNAVAILABLE | 503 | Redis 暂时不可用 |
| INTERNAL_SERVER_ERROR | 500 | 服务器内部错误 |

---

# 全局异常处理

项目通过：

```java
@RestControllerAdvice
```

统一处理异常。

当前主要处理：

- `BusinessException`
- `IllegalArgumentException`
- `RedisConnectionFailureException`
- `RedisSystemException`
- 其他未处理异常

对于 Redis：

```text
WRONGTYPE Operation against a key holding the wrong kind of value
```

项目会识别异常链中的 `WRONGTYPE`，并转换为：

```text
HTTP 409 Conflict
REDIS_KEY_TYPE_CONFLICT
```

避免所有 Redis 类型冲突都直接返回模糊的 `500`。

---

# Redis String

## 特点

Redis 最基础的数据类型，可以存储字符串、数字字符串等数据。

Spring Data Redis：

```java
redisTemplate.opsForValue()
```

## 已实现功能

| Redis 命令 | Service 方法 | 功能 |
|---|---|---|
| SET | `set()` | 写入数据 |
| GET | `get()` | 获取数据 |
| SET + EX | `setWithTtl()` | 写入并设置过期时间 |
| INCR | `increment()` | 原子自增 |
| TTL | `ttl()` | 查询剩余 TTL |
| DEL | `delete()` | 删除 Key |

## 核心代码

```java
public void set(String key, String value) {
    redisTemplate.opsForValue().set(key, value);
}

public String get(String key) {
    return redisTemplate.opsForValue().get(key);
}

public void setWithTtl(String key, String value, long seconds) {
    redisTemplate.opsForValue()
            .set(key, value, Duration.ofSeconds(seconds));
}

public Long increment(String key) {
    return redisTemplate.opsForValue().increment(key);
}
```

## 真实业务场景

- 登录验证码：`SET + TTL`
- 短期 Token / 临时状态
- 浏览量统计：`INCR`
- 接口调用次数统计
- 简单缓存
- 简单限流计数

## 已验证内容

```text
SET / GET
SET + TTL
INCR
DEL
TTL
```

## 注意事项

- 同一个 Redis Key 同一时刻只能对应一种数据类型。
- 普通 `SET` 会覆盖旧值。
- `TTL > 0`：剩余秒数。
- `TTL = -1`：Key 存在，但没有过期时间。
- `TTL = -2`：Key 不存在。
- `INCR` 是 Redis 单命令原子操作。
- `setWithTtl()` 当前对 `seconds <= 0` 做了参数校验，并返回 `400 BAD_REQUEST`。

---

# Redis Hash

## 特点

Hash 可以在一个 Redis Key 下保存多个 field-value。

结构类似：

```text
user:1001
├── name -> cunshang
├── age  -> 24
└── city -> Guangzhou
```

Spring Data Redis：

```java
redisTemplate.opsForHash()
```

## 已实现功能

| Redis 命令 | Service 方法 | 功能 |
|---|---|---|
| HSET | `hashSet()` | 写入字段 |
| HGET | `hashGet()` | 获取字段 |
| HGETALL | `hashGetAll()` | 获取全部字段 |
| HDEL | `hashDelete()` | 删除字段 |

## 核心代码

```java
public void hashSet(String key, String field, String value) {
    redisTemplate.opsForHash().put(key, field, value);
}

public Object hashGet(String key, String field) {
    return redisTemplate.opsForHash().get(key, field);
}

public Map<Object, Object> hashGetAll(String key) {
    return redisTemplate.opsForHash().entries(key);
}
```

## 真实业务场景

- 用户基础信息
- 商品部分属性
- 配置项集合
- 一个对象中多个字段的独立更新

## 已验证内容

```text
HSET
HGET
HGETALL
HDEL
```

## 注意事项

- `name / age / city` 是 field，不是独立 Redis Key。
- Hash 的 Redis Key 仍然只有一个。
- 如果同一个 Key 已经是 String，再执行 Hash 命令会出现 `WRONGTYPE`。
- 当前项目已将 `WRONGTYPE` 转换为 `409 REDIS_KEY_TYPE_CONFLICT`。
- 当前 `hashDelete()` Controller 仍直接返回 `Long`，尚未统一成 `ApiResponse<Long>`。

---

# Redis List

## 特点

List 是：

```text
有顺序
允许重复
支持左右两端操作
```

可以理解为：

```text
LPUSH  ← [ A ][ B ][ C ] → RPUSH
LPOP   ← [ A ][ B ][ C ] → RPOP
```

Spring Data Redis：

```java
redisTemplate.opsForList()
```

## 已实现功能

| Redis 命令 | Service 方法 | 功能 |
|---|---|---|
| LPUSH | `listLeftPush()` | 左侧添加 |
| RPUSH | `listRightPush()` | 右侧添加 |
| LRANGE | `listRange()` | 范围查询 |
| LPOP | `listLeftPop()` | 左侧弹出 |
| RPOP | `listRightPop()` | 右侧弹出 |
| LLEN | `listSize()` | 获取长度 |

## 核心代码

```java
public Long listLeftPush(String key, String value) {
    return redisTemplate.opsForList().leftPush(key, value);
}

public Long listRightPush(String key, String value) {
    return redisTemplate.opsForList().rightPush(key, value);
}

public List<String> listRange(String key, long start, long end) {
    return redisTemplate.opsForList().range(key, start, end);
}
```

## 真实业务场景

- 最近浏览记录
- 最近操作记录
- 简单任务队列
- 顺序消息列表
- 固定顺序的数据集合

## 已验证内容

```text
LPUSH
RPUSH
LRANGE
LPOP
RPOP
LLEN
```

## 注意事项

- List 有顺序，并允许重复元素。
- `LRANGE key 0 -1` 表示查询整个 List。
- `POP` 不只是读取，而是“读取 + 删除”。
- List 为空或 Key 不存在时，`LPOP / RPOP` 返回 `null`。
- `LLEN` 对不存在的 Key 返回 `0`。
- 当最后一个元素被 POP 后，对应 Key 会消失。

---

# Redis Set

## 特点

Set：

```text
无序
元素唯一
自动去重
```

可以类比 Java：

```java
HashSet<String>
```

Spring Data Redis：

```java
redisTemplate.opsForSet()
```

## 已实现功能

| Redis 命令 | Service 方法 | 功能 |
|---|---|---|
| SADD | `setAdd()` | 添加元素 |
| SMEMBERS | `setMembers()` | 获取全部元素 |
| SISMEMBER | `setIsMember()` | 判断成员是否存在 |
| SREM | `setRemove()` | 删除元素 |
| SCARD | `setSize()` | 获取数量 |
| SINTER | `setIntersect()` | 交集 |
| SUNION | `setUnion()` | 并集 |
| SDIFF | `setDifference()` | 差集 |

## 核心代码

```java
public Long setAdd(String key, String member) {
    return redisTemplate.opsForSet().add(key, member);
}

public Boolean setIsMember(String key, String member) {
    return redisTemplate.opsForSet().isMember(key, member);
}

public Set<String> setIntersect(String key1, String key2) {
    return redisTemplate.opsForSet().intersect(key1, key2);
}
```

## 真实业务场景

- 点赞用户去重：`SADD`
- 判断用户是否点赞：`SISMEMBER`
- 共同好友：`SINTER`
- 共同关注：`SINTER`
- 共同兴趣标签：`SINTER`
- 汇总全部兴趣标签：`SUNION`
- A 有、B 没有的关注关系：`SDIFF`

## 已验证内容

```text
SADD
SMEMBERS
SISMEMBER
SREM
SCARD
SINTER
SUNION
SDIFF
自动去重
```

## 注意事项

- Set 不保证返回顺序。
- 重复执行 `SADD` 不会产生重复数据。
- 新增成功返回 `1`，已存在时通常返回 `0`。
- 差集有方向：
  - `A - B`
  - `B - A`
  - 两者结果可能完全不同。

---

# Redis ZSet

## 特点

ZSet 是：

```text
Set + score + 自动排序
```

每个 member 唯一，并关联一个 `score`：

```text
member        score
-------------------
player:1001    800
player:1002   1200
player:1003    950
```

Spring Data Redis：

```java
redisTemplate.opsForZSet()
```

## 已实现功能

| Redis 命令 | Service 方法 | 功能 |
|---|---|---|
| ZADD | `zSetAdd()` | 添加 member + score |
| ZRANGE | `zSetRange()` | 从低分到高分查询 |
| ZREVRANGE | `zSetReverseRange()` | 从高分到低分查询 |
| ZSCORE | `zSetScore()` | 查询 score |
| ZINCRBY | `zSetIncrementScore()` | 增加 score |
| ZRANK | `zSetRank()` | 查询正序排名 |
| ZREVRANK | `zSetReverseRank()` | 查询倒序排名 |
| ZREM | `zSetRemove()` | 删除 member |
| ZCARD | `zSetSize()` | 获取元素数量 |

## 核心代码

```java
public Boolean zSetAdd(String key, String member, double score) {
    return redisTemplate.opsForZSet().add(key, member, score);
}

public Set<String> zSetReverseRange(String key, long start, long end) {
    return redisTemplate.opsForZSet().reverseRange(key, start, end);
}

public Double zSetIncrementScore(
        String key,
        String member,
        double increment
) {
    return redisTemplate.opsForZSet()
            .incrementScore(key, member, increment);
}

public Long zSetReverseRank(String key, String member) {
    return redisTemplate.opsForZSet().reverseRank(key, member);
}
```

## 真实业务场景

- 游戏积分排行榜
- 商品销量榜
- 文章热度榜
- 直播热度榜
- 用户贡献榜
- 活动排行榜

业务映射：

```text
ZADD
→ 用户加入排行榜

ZINCRBY
→ 用户获得积分 / 热度增加

ZSCORE
→ 查询用户当前积分

ZREVRANGE
→ 查询 Top N

ZREVRANK
→ 查询“我排第几名”

ZREM
→ 用户退出排行榜
```

## 已验证内容

```text
ZADD
ZSCORE
ZRANGE
ZREVRANGE
ZINCRBY
ZRANK
ZREVRANK
ZCARD
ZREM
```

测试场景使用：

```text
demo:zset:game:ranking
```

并验证了用户积分变化后，ZSet 会依据新的 score 自动调整排名。

## 注意事项

- member 唯一。
- score 用于排序。
- 更新已有 member 的 score 不会生成重复元素。
- `ZADD` 对已存在 member 更新 score 时，返回值不应简单理解为“操作失败”。
- Redis Rank 从 `0` 开始。
- 如果业务展示“第 1 名”，通常需要使用：

```text
业务排名 = Redis Rank + 1
```

---


# Redis Bitmap

## 特点

Bitmap 可以把一个 String 中的每一个 bit 位作为布尔状态使用：

```text
0 -> false
1 -> true
```

本质上仍然基于 Redis String，只是通过 bit 位进行读写和统计。

Spring Data Redis：

```java
redisTemplate.opsForValue()
```

其中 `BITCOUNT` 当前通过底层 Redis Connection 调用。

## 已实现功能

| Redis 命令 | Service 方法 | 功能 |
|---|---|---|
| SETBIT | `bitmapSet()` | 设置指定 bit 位 |
| GETBIT | `bitmapGet()` | 查询指定 bit 位 |
| BITCOUNT | `bitmapCount()` | 统计值为 1 的 bit 数量 |

## 核心代码

```java
public Boolean bitmapSet(String key, long offset, boolean value) {
    return redisTemplate.opsForValue()
            .setBit(key, offset, value);
}

public Boolean bitmapGet(String key, long offset) {
    return redisTemplate.opsForValue()
            .getBit(key, offset);
}

public long bitmapCount(String key) {
    byte[] rawKey = redisTemplate
            .getStringSerializer()
            .serialize(key);

    if (rawKey == null) {
        return 0L;
    }

    Long count = redisTemplate.execute(
            (RedisCallback<Long>) connection ->
                    connection.stringCommands().bitCount(rawKey)
    );

    return count == null ? 0L : count;
}
```

## 可应用场景（示例）

- 用户签到状态
- 用户在线 / 离线状态
- 活动参与状态
- 大量布尔状态记录
- 功能开关或某类状态标记

> 当前项目只实现并验证了 Bitmap 基础操作，上述内容是可应用方向示例，并非已经实现的完整业务系统。

## 已验证内容

```text
SETBIT
GETBIT
BITCOUNT
SETBIT 返回旧值的行为
offset 与业务含义的映射方式
```

## 注意事项

- Bitmap 的 `offset` 只是 bit 位下标，Redis 本身不知道它代表日期、用户或其他业务含义。
- `SETBIT` 返回的是该 bit 位修改之前的旧值，不是“操作是否成功”。
- 没有设置过的 bit 位默认为 `0 / false`。
- `BITCOUNT` 统计的是当前 Key 中值为 `1` 的 bit 数量。
- Bitmap 适合大量布尔状态场景，但业务层需要自行定义 offset 映射规则。

---

# Redis HyperLogLog

## 特点

HyperLogLog 用于进行近似去重计数，主要回答：

```text
有多少个不同元素？
```

而不是保存并返回全部具体成员。

Spring Data Redis：

```java
redisTemplate.opsForHyperLogLog()
```

## 已实现功能

| Redis 命令 | Service 方法 | 功能 |
|---|---|---|
| PFADD | `hyperLogLogAdd()` | 添加统计元素 |
| PFCOUNT | `hyperLogLogCount()` | 获取近似去重数量 |
| PFMERGE | `hyperLogLogMerge()` | 合并多个 HyperLogLog |

## 核心代码

```java
public Long hyperLogLogAdd(String key, String value) {
    return redisTemplate.opsForHyperLogLog()
            .add(key, value);
}

public long hyperLogLogCount(String key) {
    Long count = redisTemplate.opsForHyperLogLog()
            .size(key);

    return count == null ? 0L : count;
}

public Long hyperLogLogMerge(
        String destinationKey,
        String... sourceKeys
) {
    return redisTemplate.opsForHyperLogLog()
            .union(destinationKey, sourceKeys);
}
```

## 可应用场景（示例）

- 网站 / 页面 UV
- 独立访问用户数
- 独立设备数
- 活动独立参与人数
- 某功能的独立使用人数
- 多天或多个统计周期的去重汇总

> 当前项目只实现并验证了 HyperLogLog 基础能力，上述内容是可应用方向示例，并非已经实现的数据分析系统。

## 已验证内容

测试 Key：

```text
demo:hll:uv:day1
demo:hll:uv:day2
demo:hll:uv:total
```

验证：

```text
PFADD
重复元素去重统计
PFCOUNT
PFMERGE
跨 Key 合并后的去重统计
```

示例验证结果：

```text
day1 独立元素 ≈ 3
day2 独立元素 ≈ 4
合并后独立元素 ≈ 5
```

说明多个 HyperLogLog 合并后统计的是联合后的近似基数，而不是简单相加。

## HyperLogLog 与 Set

```text
Set
-> 精确去重
-> 可以获取具体有哪些 member

HyperLogLog
-> 近似去重计数
-> 主要回答“有多少个不同元素”
```

## 注意事项

- HyperLogLog 是近似基数统计，不保证绝对精确。
- 小规模测试数据经常会得到与精确值一致的结果，但不能因此把它当作精确 Set。
- HyperLogLog 不能像 Set 一样获取完整成员列表。
- 重复 `PFADD` 同一个元素不会按多个独立元素计数。
- `PFMERGE` 可合并多个 HyperLogLog，再对合并结果执行 `PFCOUNT`。

---


# Redis GEO

## 特点

Redis GEO 用于存储和查询地理位置数据，并支持：

```text
坐标存储
坐标查询
两点距离计算
指定半径附近搜索
```

Redis GEO 本身不会获取 GPS 定位，它只负责对已经得到的经纬度进行存储、距离计算和附近检索。

Spring Data Redis：

```java
redisTemplate.opsForGeo()
```

## 已实现功能

| Redis 命令 / 能力 | Service 方法 | 功能 |
|---|---|---|
| GEOADD | `geoAdd()` | 添加成员及经纬度 |
| GEOPOS | `geoPosition()` | 查询成员坐标 |
| GEODIST | `geoDistance()` | 计算两个成员之间的距离 |
| GEOSEARCH | `geoSearchNearby()` | 按坐标和半径搜索附近成员 |
| REMOVE | `geoRemove()` | 删除 GEO 成员 |

## 核心代码

```java
public Long geoAdd(
        String key,
        String member,
        double longitude,
        double latitude
) {
    Point point = new Point(longitude, latitude);

    return redisTemplate.opsForGeo()
            .add(key, point, member);
}
```

```java
public List<Point> geoPosition(
        String key,
        String member
) {
    return redisTemplate.opsForGeo()
            .position(key, member);
}
```

```java
public Double geoDistance(
        String key,
        String member1,
        String member2
) {
    Distance distance = redisTemplate.opsForGeo()
            .distance(
                    key,
                    member1,
                    member2,
                    Metrics.KILOMETERS
            );

    return distance == null
            ? null
            : distance.getValue();
}
```

```java
public List<String> geoSearchNearby(
        String key,
        double longitude,
        double latitude,
        double radiusKm
) {
    GeoReference<String> reference =
            GeoReference.fromCoordinate(longitude, latitude);

    Distance radius =
            new Distance(radiusKm, Metrics.KILOMETERS);

    RedisGeoCommands.GeoSearchCommandArgs args =
            RedisGeoCommands.GeoSearchCommandArgs
                    .newGeoSearchArgs()
                    .includeDistance()
                    .sortAscending();

    GeoResults<RedisGeoCommands.GeoLocation<String>> results =
            redisTemplate.opsForGeo()
                    .search(
                            key,
                            reference,
                            radius,
                            args
                    );

    if (results == null) {
        return List.of();
    }

    return results.getContent()
            .stream()
            .map(result -> {
                String member = result.getContent().getName();
                Distance distance = result.getDistance();

                return member
                        + " -> "
                        + distance.getValue()
                        + " km";
            })
            .toList();
}
```

```java
public Long geoRemove(
        String key,
        String member
) {
    return redisTemplate.opsForGeo()
            .remove(key, member);
}
```

## 可应用场景（示例）

- 附近门店
- 附近骑手
- 附近充电桩
- 附近车辆
- 设备位置缓存
- 用户与门店距离计算

> 当前项目只实现并验证了 Redis GEO 的基础能力，上述内容属于可应用方向示例，并非已经实现的完整定位、地图或配送业务系统。

## 已验证内容

测试 Key：

```text
demo:geo:stores
```

已完成：

```text
GEOADD
GEOPOS
GEODIST
GEOSEARCH
REMOVE
```

测试中验证了：

```text
按经纬度写入多个成员
查询成员坐标
计算两个成员之间的公里距离
按指定坐标 + 半径搜索附近成员
附近搜索结果按距离从近到远排序
删除成员后无法继续查询其位置
```

## 坐标精度

测试写入：

```text
longitude = 113.2644
latitude  = 23.1291
```

通过 `GEOPOS` 查询时返回过类似：

```json
{
  "x": 113.26440006494522,
  "y": 23.129101186703004
}
```

这类极小的小数偏差属于正常现象。

因此业务代码不应把 GEO 查询返回的经纬度与原始输入做严格的浮点绝对相等判断。

## 注意事项

- Redis GEO 使用经度和纬度，顺序不能写反：

```text
longitude
latitude
```

- Java `Point` 当前使用：

```java
new Point(longitude, latitude)
```

- `GEOSEARCH` 当前按指定经纬度作为中心进行半径搜索。
- 当前距离单位统一使用 `Metrics.KILOMETERS`。
- `includeDistance()` 会返回成员与搜索中心之间的距离。
- `sortAscending()` 会让结果按距离从近到远排列。
- Redis GEO 负责存位置、算距离和查附近，不负责获取设备真实 GPS 位置。
- 当前 `geoSearchNearby()` 为了保持学习阶段简单，返回 `List<String>`；真实项目可进一步使用 DTO 返回 member、distance 等结构化字段。

---


# Redis Stream

## 特点

Redis Stream 是一种面向消息流 / 事件流的数据结构。

每条消息都会包含：

```text
RecordId
+
field-value 消息内容
```

例如：

```text
1758190000000-0
└── type -> CREATE
```

Spring Data Redis：

```java
redisTemplate.opsForStream()
```

## 已实现功能

| Redis 命令 | Service 方法 | 功能 |
|---|---|---|
| XADD | `streamAdd()` | 向 Stream 追加消息 |
| XRANGE | `streamRange()` | 查询当前 Stream 中的历史消息 |
| XLEN | `streamSize()` | 查询消息数量 |

## 核心代码

```java
public String streamAdd(
        String key,
        String field,
        String value
) {
    RecordId recordId = redisTemplate.opsForStream()
            .add(key, Map.of(field, value));

    return recordId == null
            ? null
            : recordId.getValue();
}
```

```java
public List<Map<String, Object>> streamRange(
        String key
) {
    List<MapRecord<String, Object, Object>> records =
            redisTemplate.opsForStream()
                    .range(key, Range.unbounded());

    if (records == null) {
        return List.of();
    }

    return records.stream()
            .map(record -> {
                Map<String, Object> result =
                        new LinkedHashMap<>();

                result.put(
                        "id",
                        record.getId().getValue()
                );

                result.put(
                        "body",
                        record.getValue()
                );

                return result;
            })
            .toList();
}
```

```java
public long streamSize(String key) {
    Long size = redisTemplate.opsForStream()
            .size(key);

    return size == null ? 0L : size;
}
```

## 可应用场景（示例）

- 业务事件流
- 操作日志流
- 异步任务
- 简单消息队列
- 订单状态事件

> 当前项目只实现并验证了 Stream 的基础读写与计数能力，上述内容属于可应用方向示例，并非已经实现的完整消息队列或订单事件系统。

## 已验证内容

测试 Key：

```text
demo:stream:events
```

已完成：

```text
XADD
XRANGE
XLEN
RecordId 自动生成
消息顺序验证
重复消息内容写入验证
```

测试中验证了：

```text
CREATE
PAY
SHIP
PAY
```

即使消息内容重复，每次执行 `XADD` 仍会生成一条新的 Stream 消息，并拥有独立 `RecordId`。

## Stream 与 List

```text
List
-> 普通有序元素
-> 主要按位置进行读写

Stream
-> 有序消息流
-> 每条消息拥有独立 RecordId
-> 后续可继续扩展 XREAD、消费者组、ACK、Pending 等能力
```

## 注意事项

- 当前 `streamAdd()` 由 Redis 自动生成 RecordId。
- Stream 不会因为消息内容相同而自动去重。
- `XRANGE` 当前通过 `Range.unbounded()` 查询全部已有消息。
- 当前返回结果将每条消息整理为 `id + body`，方便 HTTP 接口查看。
- 当前只实现 Stream 基础能力，尚未实现消费者组、ACK、Pending、消息重试等机制。
- Stream 具备消息流能力，但不应简单理解为可以替代所有 Kafka / RabbitMQ 场景。

---


# Redis 对象存储与序列化

## 特点

Java 业务代码中经常需要缓存一个完整对象，而 Redis 本身并不直接理解 Java 对象。

当前项目采用：

```text
Java Object
↓
ObjectMapper
↓
JSON String
↓
Redis
```

读取时执行相反过程：

```text
Redis JSON
↓
ObjectMapper
↓
Java Object
```

当前实现使用：

```text
RedisUserProfile
ObjectMapper
StringRedisTemplate / ValueOperations
```

用于直观验证 Java 对象、JSON 与 Redis 存储之间的关系。

## 已实现对象

当前新增：

```java
public record RedisUserProfile(
        Long id,
        String username,
        Integer age
) {
}
```

## 已实现功能

| 能力 | Service 方法 | 功能 |
|---|---|---|
| 对象序列化 | `objectSet()` | 将 Java 对象转换为 JSON 并写入 Redis |
| 对象反序列化 | `objectGet()` | 从 Redis 获取 JSON 并转换回 Java 对象 |

## 核心代码

```java
public void objectSet(
        String key,
        RedisUserProfile profile
) {
    try {
        String json =
                objectMapper.writeValueAsString(profile);

        redisTemplate.opsForValue()
                .set(key, json);

    } catch (JsonProcessingException e) {
        throw new IllegalStateException(
                "Redis 对象序列化失败",
                e
        );
    }
}
```

```java
public RedisUserProfile objectGet(
        String key
) {
    String json = redisTemplate.opsForValue()
            .get(key);

    if (json == null) {
        return null;
    }

    try {
        return objectMapper.readValue(
                json,
                RedisUserProfile.class
        );

    } catch (JsonProcessingException e) {
        throw new IllegalStateException(
                "Redis 对象反序列化失败",
                e
        );
    }
}
```

## Controller 接口

写入对象：

```text
POST /api/redis/object
```

其中：

```text
key
→ RequestParam

RedisUserProfile
→ RequestBody
```

读取对象：

```text
GET /api/redis/object
```

## 已验证内容

测试 Key：

```text
demo:object:user:1001
```

已完成：

```text
Java 对象序列化为 JSON
JSON 写入 Redis
通过 redis-cli 直接查看 Redis 中的 JSON
从 Redis 读取 JSON
JSON 反序列化为 RedisUserProfile
Controller 返回反序列化后的对象
手动修改 Redis JSON 后再次完成反序列化
```

Redis 中实际可看到类似：

```json
{"id":1001,"username":"cunshang","age":24}
```

这说明当前 Redis 中存储的是 JSON 字符串，而不是 Java 对象本身。

## 可应用场景（示例）

- 用户详情缓存
- 商品详情缓存
- 文章详情缓存
- 订单摘要缓存
- 系统配置缓存

> 当前项目只实现并验证了“Java 对象 ↔ JSON ↔ Redis”的基础能力，上述内容属于可应用方向示例，并非已经实现的完整缓存业务。

## 注意事项

- Redis 不认识 Java 对象，必须经过序列化后才能存储。
- 当前实现明确使用 JSON，方便通过 `redis-cli` 直接观察数据。
- `ObjectMapper.writeValueAsString()` 负责序列化。
- `ObjectMapper.readValue()` 负责反序列化。
- JSON 字段需要能够正确映射到目标 Java 类型。
- 该章节保留手动 `ObjectMapper` 方案用于理解对象与 JSON 的转换过程；项目后续已增加统一 `RedisTemplate` JSON Serializer 配置进行自动序列化。
- 这一能力会作为后续 Cache Aside 与 Redis + MySQL 缓存学习的基础。

---


# Redis Pipeline / 批量操作

## 特点

Pipeline 主要用于：

```text
减少大量 Redis 命令产生的网络往返次数（RTT）
```

普通执行：

```text
命令 1 → Redis → 返回
命令 2 → Redis → 返回
命令 3 → Redis → 返回
```

Pipeline：

```text
命令 1
命令 2
命令 3
   ↓
集中发送
   ↓
Redis 依次执行
   ↓
统一返回结果
```

需要注意：

```text
Pipeline ≠ Redis 事务
```

Pipeline 主要解决的是批量命令的通信效率问题，不提供“全部成功或全部回滚”的事务原子性。

当前项目通过：

```java
redisTemplate.executePipelined(...)
```

实现 Pipeline。

## 已实现功能

| 能力 | Service 方法 | 功能 |
|---|---|---|
| Pipeline 批量 SET | `pipelineSet()` | 批量写入多个 String Key |
| Pipeline 批量 GET | `pipelineGet()` | 批量读取多个 String Key |

## 核心代码

### 批量 SET

```java
public List<Object> pipelineSet(
        Map<String, String> data
) {
    return redisTemplate.executePipelined(
            (RedisCallback<Object>) connection -> {

                StringRedisConnection stringConnection =
                        (StringRedisConnection) connection;

                for (Map.Entry<String, String> entry : data.entrySet()) {
                    stringConnection.set(
                            entry.getKey(),
                            entry.getValue()
                    );
                }

                return null;
            }
    );
}
```

### 批量 GET

```java
public List<Object> pipelineGet(
        List<String> keys
) {
    return redisTemplate.executePipelined(
            (RedisCallback<Object>) connection -> {

                StringRedisConnection stringConnection =
                        (StringRedisConnection) connection;

                for (String key : keys) {
                    stringConnection.get(key);
                }

                return null;
            }
    );
}
```

这里保留 `executePipelined()` 的 Lambda Callback，但内部批量命令采用普通 `for` 循环，使每一条 Redis 操作更加直观。

## Controller 接口

批量写入：

```text
POST /api/redis/pipeline
```

请求体示例：

```json
{
  "demo:pipeline:1": "A",
  "demo:pipeline:2": "B",
  "demo:pipeline:3": "C"
}
```

批量读取：

```text
POST /api/redis/pipeline/get
```

请求体示例：

```json
[
  "demo:pipeline:1",
  "demo:pipeline:2",
  "demo:pipeline:3"
]
```

## 已验证内容

测试 Key：

```text
demo:pipeline:1
demo:pipeline:2
demo:pipeline:3
demo:pipeline:not-found
```

已完成：

```text
Pipeline 批量 SET
Redis 实际数据写入验证
Pipeline 批量 GET
返回结果与命令提交顺序对应
不存在 Key 返回 null
不存在 Key 不影响其他 GET 命令结果
```

批量 GET 示例：

```json
[
  "A",
  null,
  "C"
]
```

说明 Pipeline 中的多条 Redis 命令仍然是独立命令，一个不存在的 Key 不会让其他 GET 自动失败或回滚。

## Pipeline 与批量命令

需要区分：

```text
MGET key1 key2 key3
```

属于：

```text
Redis 自身的一条批量命令
```

而 Pipeline：

```text
GET key1
GET key2
GET key3
```

仍然是多条独立命令，只是由客户端集中发送和统一接收结果。

## 可应用场景（示例）

- 批量写入多个缓存 Key
- 批量读取多个独立缓存 Key
- 批量写入简单状态数据
- 批量处理大量独立 Redis 命令

> 当前项目只实现并验证了 Pipeline 批量 String SET / GET 能力，上述内容属于可应用方向示例，并非已经实现的完整批量缓存业务。

## 注意事项

- Pipeline 的核心价值是减少网络 RTT，而不是让 Redis 单条命令执行得更快。
- Pipeline 中仍然是多条独立 Redis 命令。
- Pipeline 不提供事务原子性，也不会因为某条命令失败自动回滚之前的命令。
- `executePipelined()` 的 Callback 返回 `null`，真正的 Redis 命令结果由 `executePipelined()` 统一收集。
- Pipeline 返回结果与命令提交顺序对应。
- 批量数据非常大时不应无限堆入单个 Pipeline，真实项目通常需要考虑分批执行。

---


# RedisTemplate / StringRedisTemplate / Serializer

## 核心认识

Redis 最终保存的是字节数据：

```text
Java 数据
↓
Serializer
↓
byte[]
↓
Redis
```

读取时执行相反过程：

```text
Redis byte[]
↓
Deserializer
↓
Java 数据
```

当前项目同时保留两种对象存储方式，用于对比理解序列化过程。

### 手动 JSON 方案

```text
RedisUserProfile
↓
ObjectMapper
↓
JSON String
↓
StringRedisTemplate
↓
Redis
```

### RedisTemplate 自动 JSON 方案

```text
RedisUserProfile
↓
RedisTemplate<String, Object>
↓
GenericJackson2JsonRedisSerializer
↓
Redis
```

## 当前 RedisTemplate 配置

项目新增：

```text
config/RedisConfig.java
```

核心配置：

```java
@Bean
public RedisTemplate<String, Object> objectRedisTemplate(
        RedisConnectionFactory connectionFactory
) {
    RedisTemplate<String, Object> template =
            new RedisTemplate<>();

    template.setConnectionFactory(connectionFactory);

    StringRedisSerializer stringSerializer =
            new StringRedisSerializer();

    GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer();

    template.setKeySerializer(stringSerializer);
    template.setValueSerializer(jsonSerializer);
    template.setHashKeySerializer(stringSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();

    return template;
}
```

当前约定：

```text
普通 Key
→ StringRedisSerializer

普通 Value
→ GenericJackson2JsonRedisSerializer

Hash Field
→ StringRedisSerializer

Hash Value
→ GenericJackson2JsonRedisSerializer
```

这样既保证 Key 便于通过 `redis-cli` 阅读和排障，也可以让 Java 对象通过统一 Serializer 自动完成 JSON 序列化与反序列化。

## 已实现接口

自动序列化写入：

```text
POST /api/redis/template/object
```

自动反序列化读取：

```text
GET /api/redis/template/object
```

## 已验证内容

```text
RedisUserProfile 直接写入 RedisTemplate
Value 自动 JSON 序列化
通过 redis-cli 查看序列化后的数据
RedisTemplate 自动反序列化
读取结果恢复为 RedisUserProfile
无需在业务方法中手动调用 ObjectMapper
```

## StringRedisTemplate 与 RedisTemplate

```text
StringRedisTemplate
→ 适合 String Key / String Value
→ JSON 对象需要业务代码自行转换

RedisTemplate<String, Object>
→ 可直接处理 Java Object
→ 实际存储格式由 Serializer 决定
```

两者不是“低级 / 高级”的关系，而是适用的数据模型与序列化责任不同。

## 注意事项

- Redis 最终面对的是字节数据，Java 对象不能未经序列化直接进入 Redis。
- Serializer 属于基础设施配置；统一配置后，业务 Service 不应到处重复编写对象与 JSON 的转换逻辑。
- 如果修改一个 Key 的序列化协议，旧缓存可能无法被新的 Serializer 正确读取，需要考虑旧数据清理或兼容策略。
- `GenericJackson2JsonRedisSerializer` 面向通用对象序列化，Redis 中的 JSON 可能包含用于恢复 Java 类型的信息。
- Key 通常优先保持字符串可读，方便排查线上缓存数据。

---

# Cache Aside

## 当前实现

项目已接入真实：

```text
Spring Boot
+
Redis 7.4
+
MySQL 8.4
+
MyBatis-Plus
```

当前缓存读取链路：

```text
GET /api/redis/cache/users/{id}
        ↓
CacheAsideService
        ↓
查询 Redis
        ↓
命中？
├── YES
│    ↓
│  直接返回 RedisUserProfile
│
└── NO
     ↓
   UserProfileMapper.selectById(id)
     ↓
   查询 MySQL
     ↓
   转换为 RedisUserProfile
     ↓
   RedisTemplate 自动序列化
     ↓
   写入 Redis + 600~720 秒随机 TTL
     ↓
   返回
```

数据库仍然是主数据源，Redis 是可重新构建的缓存副本。

## MySQL 数据模型

当前表：

```text
user_profile
├── id
├── username
└── age
```

MyBatis-Plus Entity：

```text
entity/UserProfile.java
```

Mapper：

```java
@Mapper
public interface UserProfileMapper
        extends BaseMapper<UserProfile> {
}
```

查询使用：

```java
userProfileMapper.selectById(id);
```

## Cache Aside Service

> 下方代码用于展示最基础的 Cache Aside 读取流程。当前项目已经在后续章节继续加入空值缓存、Mutex + Double Check 与随机 TTL 等增强机制。

当前核心读取逻辑：

```java
public RedisUserProfile getUser(Long id) {

    String key = "user:profile:" + id;

    Object cachedValue =
            objectRedisTemplate.opsForValue().get(key);

    if (cachedValue != null) {
        log.info("CACHE HIT, key={}", key);

        if (cachedValue instanceof RedisUserProfile profile) {
            return profile;
        }

        throw new IllegalStateException(
                "Redis 缓存数据类型异常, key=" + key
        );
    }

    log.info("CACHE MISS, key={}", key);
    log.info("QUERY DATABASE, userId={}", id);

    UserProfile entity =
            userProfileMapper.selectById(id);

    if (entity == null) {
        return null;
    }

    RedisUserProfile profile =
            new RedisUserProfile(
                    entity.getId(),
                    entity.getUsername(),
                    entity.getAge()
            );

    objectRedisTemplate.opsForValue().set(
            key,
            profile,
            Duration.ofMinutes(10)
    );

    log.info("CACHE REBUILD, key={}", key);

    return profile;
}
```

## Controller 接口

```text
GET /api/redis/cache/users/{id}    # GET：查询用户缓存
/cache/users/{id}    # PUT：更新 MySQL 后删除对应缓存
```

当前测试 Key：

```text
user:profile:1001
```

## 已验证内容

第一次请求并确保 Redis 无缓存时：

```text
CACHE MISS
↓
QUERY DATABASE
↓
MyBatis-Plus SELECT
↓
CACHE REBUILD
```

实际验证 SQL：

```sql
SELECT id, username, age
FROM user_profile
WHERE id = ?
```

第二次请求相同用户：

```text
CACHE HIT
```

并验证 MySQL 不再执行对应的 `SELECT`。

同时验证缓存 TTL。

在后续缓存雪崩防护中，正常缓存 TTL 已改为：

```text
基础 TTL：600 秒
+
随机扰动：0 ~ 120 秒
=
最终 TTL：600 ~ 720 秒
```

## 缓存旧数据实验

在 Redis 已经存在用户缓存后，直接修改 MySQL：

```sql
UPDATE user_profile
SET username = 'new-name'
WHERE id = 1001;
```

在没有删除缓存的情况下再次查询，仍然命中 Redis 中的旧数据。

手动执行：

```redis
DEL user:profile:1001
```

下一次请求重新经历：

```text
CACHE MISS
↓
MySQL
↓
读取新值
↓
CACHE REBUILD
```

从而验证了：

```text
数据库更新
+
缓存未失效
→ 可能读取旧缓存
```

以及：

```text
缓存失效
→ 下一次读取重新从数据库构建缓存
```

> 当前项目已经进一步实现正式写接口：先更新 MySQL，再删除对应 Redis 缓存；缓存删除还加入了短同步重试。更完整的异步补偿机制仍将在后续继续实现。

## Cache Hit / Cache Miss

```text
Cache Hit
→ Redis 中存在目标缓存
→ 直接返回
→ 不访问数据库

Cache Miss
→ Redis 中不存在目标缓存
→ 查询数据库
→ 回填 Redis
```

正常的 Cache Miss 是 Cache Aside 的标准流程，并不等同于缓存击穿。

缓存击穿通常强调：

```text
热点 Key 失效
+
高并发请求
+
大量请求同时回源数据库
```

## 注意事项

- 当前正常缓存采用基础 TTL + 随机扰动，基础值为 10 分钟，并增加随机 TTL 以降低大量 Key 集中过期风险。
- 当前缓存 Value 统一通过 `objectRedisTemplate` 的 JSON Serializer 处理。
- Cache Aside 本身不会自动保证 Redis 与 MySQL 强一致。
- 当前对数据库中不存在的用户使用空值缓存，并设置较短 TTL，降低重复无效请求持续回源 MySQL 的风险。
- 当前已使用 Redis Mutex + Double Check 保护热点 Key 的缓存重建，避免大量并发请求同时回源 MySQL。
- 当前尚未实现数据库写操作与缓存失效的并发一致性策略。

---


# 缓存穿透 / 缓存击穿 / 缓存雪崩

这一阶段基于现有 `CacheAsideService` 继续完善缓存保护能力，并通过 Redis、MySQL、MyBatis-Plus 与 JMeter 进行实际验证。

三类问题的核心区别：

| 问题 | 本质 |
|---|---|
| 缓存穿透 | 请求的数据在 Redis 和数据库中都不存在 |
| 缓存击穿 | 某一个热点 Key 失效后，大量并发请求同时回源数据库 |
| 缓存雪崩 | 大量 Key 集中过期，或 Redis 整体不可用，导致大量请求回源 |

---

## 缓存穿透

### 问题

如果请求一个数据库中不存在的用户：

```text
GET /api/redis/cache/users/999999
```

未做保护时：

```text
Redis MISS
↓
MySQL MISS
↓
返回 null
```

下一次请求仍然会重复：

```text
Redis MISS
↓
MySQL MISS
```

因此不存在的数据会持续穿过缓存层并访问数据库。

### 当前实现：空值缓存

项目使用特殊空值标记：

```java
private static final String NULL_CACHE_VALUE = "NULL";
```

数据库未查询到数据时，将空值写入 Redis，并设置较短 TTL：

```java
if (entity == null) {

    objectRedisTemplate.opsForValue().set(
            key,
            NULL_CACHE_VALUE,
            Duration.ofMinutes(2)
    );

    log.info("CACHE NULL REBUILD, key={}", key);

    return null;
}
```

读取缓存时优先识别空值：

```java
if (NULL_CACHE_VALUE.equals(cachedValue)) {
    log.info("CACHE NULL HIT, key={}", key);
    return null;
}
```

### 已验证

第一次请求不存在用户：

```text
CACHE MISS
↓
QUERY DATABASE
↓
CACHE NULL REBUILD
```

第二次请求相同不存在用户：

```text
CACHE HIT
↓
CACHE NULL HIT
```

第二次不会再次执行 MySQL `SELECT`。

### 注意事项

- 空值缓存适合降低重复无效请求对数据库的压力。
- 空值 TTL 通常应短于正常缓存 TTL。
- “当前不存在”不代表永远不存在，因此不应长期缓存空值。
- 大量随机不存在 ID 的恶意请求仍可能产生大量首次回源请求，后续可以继续学习 Bloom Filter 等方案。

---

## 缓存击穿

### 问题

热点 Key 失效时，如果大量并发请求同时进入：

```text
Thread-1 → Redis MISS
Thread-2 → Redis MISS
Thread-3 → Redis MISS
...
```

所有线程都可能同时执行：

```text
userProfileMapper.selectById(id)
```

从而导致同一个热点数据在极短时间内被大量重复查询。

### JMeter 问题复现

使用 JMeter 5.6.3：

```text
Threads：20
Ramp-Up：1 秒
Loop Count：1
Synchronizing Timer：20 个线程同时放行
```

目标接口：

```text
GET /api/redis/cache/users/1001
```

在未加入击穿保护时，实际观察到：

```text
CACHE MISS       ≈ 20 次
QUERY DATABASE   ≈ 20 次
SELECT           ≈ 20 次
CACHE REBUILD    ≈ 20 次
```

说明同一个热点 Key 失效后，多个并发请求同时回源 MySQL。

### 当前实现：Redis Mutex

项目使用 Redis `SET NX + TTL` 思路实现教学版互斥锁：

```java
private boolean tryLock(String lockKey) {

    Boolean success =
            objectRedisTemplate.opsForValue()
                    .setIfAbsent(
                            lockKey,
                            "LOCK",
                            LOCK_TTL
                    );

    return Boolean.TRUE.equals(success);
}
```

概念上对应：

```redis
SET lock:user:profile:1001 LOCK NX EX 10
```

作用：

```text
多个线程同时 CACHE MISS
↓
只有一个线程获得缓存重建资格
↓
该线程查询 MySQL 并重建缓存
↓
其他线程等待
```

### Double Check

线程获得锁后不会直接查询数据库，而是再次检查 Redis：

```text
第一次 Check
→ 抢锁之前检查缓存

第二次 Check
→ 获得锁之后再次检查缓存
```

原因：

```text
线程 A 获得锁
↓
查询 MySQL
↓
重建 Redis
↓
释放锁

线程 B 后续获得锁
↓
再次检查 Redis
↓
发现 A 已经完成重建
↓
直接使用缓存
```

避免线程 B 再次无意义查询数据库。

### JMeter 优化后验证

使用完全相同的 20 并发测试条件，实际日志结果：

```text
CACHE MISS              = 20 次
LOCK ACQUIRED           = 1 次
QUERY DATABASE          = 1 次
SELECT                  = 1 次
CACHE REBUILD           = 1 次
LOCK RELEASED           = 1 次
CACHE HIT AFTER WAIT    = 19 次
```

因此实际验证：

```text
优化前：
20 并发
→ 20 次 SELECT

优化后：
20 并发
→ 1 次 SELECT
```

说明 Mutex + Double Check 已有效避免同一个热点 Key 的大量并发请求同时回源数据库。

### 教学实验说明

为放大并发现象，测试阶段曾临时在数据库查询前加入：

```java
Thread.sleep(500);
```

该代码仅用于复现缓存击穿，验收后应删除，不属于正式业务逻辑。

### 当前锁实现的边界

当前 `unlock()` 为教学版实现：

```java
objectRedisTemplate.delete(lockKey);
```

目前还没有解决：

```text
锁超时
↓
其他线程获得新锁
↓
旧线程执行结束
↓
误删其他线程持有的锁
```

该问题不会在缓存击穿章节提前展开。

后续分布式锁专项会继续学习：

```text
唯一锁 Value
→ 锁所有权
→ Lua 原子校验并删除
→ Redisson
→ Watchdog
→ 可重入锁
```

---

## 缓存雪崩

### 问题

如果大量缓存使用完全相同的 TTL，并且在相近时间写入：

```text
user:1001 → 600 秒
user:1002 → 600 秒
user:1003 → 600 秒
user:1004 → 600 秒
...
```

可能在相近时间集中失效：

```text
大量 Key 同时过期
↓
大量 CACHE MISS
↓
大量请求回源 MySQL
```

这属于缓存雪崩的一种常见场景。

### 当前实现：TTL Jitter

项目在正常缓存的基础 TTL 上增加随机扰动：

```java
private static final Duration CACHE_BASE_TTL =
        Duration.ofMinutes(10);

private static final long CACHE_TTL_JITTER_SECONDS =
        120;
```

生成随机 TTL：

```java
private Duration buildCacheTtl() {

    long jitterSeconds =
            ThreadLocalRandom.current()
                    .nextLong(
                            0,
                            CACHE_TTL_JITTER_SECONDS + 1
                    );

    return CACHE_BASE_TTL.plusSeconds(jitterSeconds);
}
```

最终正常缓存 TTL：

```text
600 ~ 720 秒
```

例如：

```text
user:1001 → 617 秒
user:1002 → 684 秒
user:1003 → 631 秒
user:1004 → 709 秒
```

这样可以将大量缓存的过期时间打散，降低集中失效产生的瞬时数据库压力。

### 已验证

通过多个不同用户 Key 写入缓存后查询 TTL，验证不同 Key 的剩余 TTL 不再完全一致。

### TTL Jitter 的边界

随机 TTL 解决的是：

```text
大量 Key 集中过期
```

但不能解决：

```text
Redis 整体不可用
```

如果 Redis 整体故障：

```text
Redis DOWN
↓
大量缓存访问失败
↓
请求可能直接回源数据库
```

工程上还需要继续配合：

```text
Redis 高可用
限流
熔断
降级
本地缓存
缓存预热
Sentinel / Cluster
```

这些能力将在后续模块继续学习。

---

## 当前缓存保护总结

```text
缓存穿透
→ 空值缓存 + 短 TTL

缓存击穿
→ Redis Mutex + Double Check

缓存雪崩
→ 基础 TTL + 随机 TTL Jitter
```

当前实现重点用于理解并验证缓存保护机制。

其中缓存击穿使用的 Redis 锁仍为教学版本，完整的生产级分布式锁能力将在后续专项继续完善。

---


# Redis + MySQL 缓存一致性（当前阶段）

这一阶段在现有 Cache Aside 基础上继续实现写路径，并通过正常更新、缓存删除失败、短同步重试等实验验证 Redis 与 MySQL 的一致性问题。

## 核心原则

当前采用：

```text
MySQL
= 主数据源 / Source of Truth

Redis
= 可删除、可重建的缓存副本
```

因此写操作不直接同时维护两份数据，而是：

```text
UPDATE MySQL
↓
DELETE Redis
↓
下一次读取 Redis MISS
↓
重新从 MySQL 加载最新值
↓
CACHE REBUILD
```

当前项目不采用：

```text
UPDATE MySQL
+
UPDATE Redis
```

作为主方案，避免业务代码长期承担“双写两份数据”的同步责任。

---

## 已实现写接口

当前接口：

```text
GET /api/redis/cache/users/{id}    # 查询用户缓存
PUT /api/redis/cache/users/{id}    # 更新 MySQL 后使对应缓存失效
```

请求对象：

```text
model/request/UpdateUserProfileRequest.java
```

示例请求：

```json
{
  "username": "cache-consistency-test",
  "age": 25
}
```

核心业务顺序：

```java
int rows = userProfileMapper.updateById(entity);

if (rows == 0) {
    throw new IllegalStateException(
            "用户不存在, userId=" + id
    );
}

deleteCacheWithRetry(key);
```

也就是：

```text
UPDATE MySQL
↓
DELETE Redis
```

---

## 正常写路径验证

实验前先确保目标用户已经存在 Redis 缓存。

执行：

```text
PUT /api/redis/cache/users/1001
```

验证日志：

```text
UPDATE DATABASE
↓
MyBatis-Plus UPDATE
↓
DATABASE UPDATED
↓
CACHE INVALIDATED
```

此时状态：

```text
MySQL = 新数据
Redis = 无缓存
```

随后再次请求：

```text
GET /api/redis/cache/users/1001
```

会重新经历：

```text
CACHE MISS
↓
QUERY DATABASE
↓
读取 MySQL 最新值
↓
CACHE REBUILD
```

从而恢复为：

```text
MySQL = 新数据
Redis = 新缓存
```

这验证了当前 Cache Aside 写路径：

```text
更新数据库
+
让旧缓存失效
+
由后续读请求重新构建缓存
```

---

## 缓存删除失败实验

为了验证一致性风险，实验阶段曾临时模拟：

```text
MySQL UPDATE 成功
↓
Redis DELETE 失败
```

实际现象：

```text
MySQL = 新值
Redis = 旧值
```

随后再次 GET：

```text
CACHE HIT
```

由于 Redis 中仍然存在旧缓存，读取请求不会立即访问 MySQL，因此用户仍可能拿到旧数据。

这一实验验证：

```text
数据库更新成功
+
缓存失效失败
=
可能产生短暂或持续的数据不一致
```

> 模拟删除失败的代码仅用于教学实验，不属于正常业务路径。

---

## 缓存删除同步重试

当前项目已经为缓存删除加入最小同步重试机制：

```text
最大尝试次数：3
重试间隔：200ms
```

整体流程：

```text
DELETE Redis
↓
失败？
├── NO  → 完成
│
└── YES
     ↓
   等待 200ms
     ↓
   再次 DELETE
```

当前实现主要用于应对：

```text
瞬时 Redis 抖动
短暂网络异常
短暂连接异常
```

### 已验证：第一次失败，第二次成功

实验中临时模拟第一次删除失败：

```text
attempt=1 → FAIL
↓
等待约 200ms
↓
attempt=2 → DELETE 成功
```

说明短暂故障可以通过有限次数的同步重试恢复。

### 已验证：连续三次失败

实验中继续模拟：

```text
attempt=1 → FAIL
attempt=2 → FAIL
attempt=3 → FAIL
```

最终同步重试耗尽。

这一实验验证：

```text
同步短重试
只能降低瞬时故障带来的失败概率

但无法解决：
Redis 长时间不可用
持续网络故障
持续服务异常
```

---

## DELETE 返回 false 不等于 Redis 故障

当前代码会记录：

```text
existed=true / false
```

需要区分：

```text
delete(key) == false
```

与：

```text
DELETE Redis 抛出异常
```

`false` 通常只表示：

```text
目标 Key 本来就不存在
```

而当前业务目标本身就是：

```text
让旧缓存不存在
```

因此 Key 已经不存在时，不需要继续重试。

真正需要进入重试逻辑的是 Redis 命令执行过程出现异常。

---

## 当前方案边界

当前已完成：

```text
Cache Aside 正常写路径 ✅
UPDATE MySQL → DELETE Redis ✅
缓存删除短同步重试 ✅
单次失败后重试恢复实验 ✅
连续三次失败实验 ✅
缓存删除失败导致旧缓存继续命中的实验 ✅
RabbitMQ 异步缓存失效补偿 ✅
Consumer 异步删除 Redis ✅
同步线程与 MQ 消费线程分离验证 ✅
Consumer Retry（最多 3 次）✅
500ms 重试间隔验证 ✅
正常 Exchange / Queue / Binding 显式声明 ✅
Error Exchange / Error Queue / Binding ✅
RepublishMessageRecoverer 失败消息重新发布 ✅
```

当前尚未实现：

```text
Error Queue 中失败消息的后续自动处理
Publisher Confirm / Return
失败任务持久化
定时任务最终兜底
更完整的最终一致性机制
```

因此当前状态仍然属于：

```text
Redis + MySQL 缓存一致性
= 进行中
```

当前完整补偿链已经推进到：

```text
同步删除 Redis 重试仍失败
↓
RabbitMQ 正常 Exchange
↓
cache.invalidation.queue
↓
Consumer 处理消息
↓
Consumer 最多尝试 3 次
↓
仍然失败
↓
RepublishMessageRecoverer
↓
Error Exchange
↓
cache.invalidation.error.queue
```

> 当前这里使用的是 Spring 应用层的 `RepublishMessageRecoverer` 重新发布失败消息，不是 RabbitMQ Broker 原生的 `Reject → DLX → DLQ` 机制。

下一阶段继续处理：

```text
Error Queue 中仍未完成的缓存失效任务
↓
自动补偿 / 人工补偿
↓
必要时失败任务持久化
↓
定时任务最终兜底
```

---


# RabbitMQ 异步缓存失效补偿

当同步删除 Redis 连续重试 3 次仍然失败时，当前项目不再让 HTTP 请求线程继续死磕，而是把“删除这个缓存 Key”的任务交给 RabbitMQ。

当前链路：

```text
HTTP PUT
↓
UPDATE MySQL
↓
DELETE Redis
↓
同步重试 3 次仍失败
↓
RabbitTemplate 发送 CacheInvalidationMessage
↓
RabbitMQ
↓
CacheInvalidationConsumer
↓
异步 DELETE Redis
```

消息对象：

```java
public record CacheInvalidationMessage(
        String key
) {
}
```

当前正常消息拓扑：

```text
cache.invalidation.exchange
        ↓ routingKey = cache.invalidation
cache.invalidation.queue
        ↓
CacheInvalidationConsumer
```

失败消息拓扑：

```text
cache.invalidation.error.exchange
        ↓ routingKey = cache.invalidation.error
cache.invalidation.error.queue
```

正常 Queue 与 Error Queue 都采用 durable 声明。

消息通过 JSON `MessageConverter` 在 Java 对象与 RabbitMQ Message 之间完成转换。

---

## 已验证异步补偿

实际实验中人为让同步 Redis 删除连续失败 3 次。

验证链路：

```text
DATABASE UPDATED
↓
CACHE DELETE FAILED, attempt=1/3
↓
CACHE DELETE FAILED, attempt=2/3
↓
CACHE DELETE FAILED, attempt=3/3
↓
SYNC CACHE INVALIDATION FAILED, SEND MQ
↓
CACHE INVALIDATION MESSAGE SENT
↓
MQ CACHE INVALIDATION RECEIVED
↓
MQ CACHE INVALIDATED, existed=true
```

其中：

```text
existed=true
```

说明 Consumer 实际删除的是一个真实存在的旧缓存 Key，而不是“Key 本来就不存在”。

这一实验验证了：

```text
主请求线程未能完成缓存失效
↓
RabbitMQ 接管补偿任务
↓
Consumer 成功清理旧缓存
```

---

## `nio-8080-exec-*` 与 RabbitMQ Consumer 线程

日志中可以看到类似：

```text
nio-8080-exec-3
```

和：

```text
...Container#0-1
```

它们可以先这样理解：

```text
nio-8080-exec-3
= Web 容器处理 HTTP 请求的工作线程

RabbitMQ Listener Container 线程
= Spring AMQP 用来消费消息的独立工作线程
```

因此这确实涉及多线程，但更准确地说，是：

```text
两个独立线程池 / 执行上下文
+
RabbitMQ 作为中间异步边界
```

并不是 HTTP 线程“直接切换”成 MQ 线程。

实际过程是：

```text
HTTP 请求线程
↓
发送消息到 RabbitMQ
↓
HTTP 线程可以结束自己的工作

RabbitMQ 保存 / 投递消息
↓
Listener Container 中的另一个线程
↓
调用 @RabbitListener Consumer
↓
继续执行 Redis 删除
```

所以同一条业务链路可以跨越不同线程、不同时间点继续执行。

当前实验中，这正是“异步补偿”最直观的体现。

---


## Consumer Retry 与失败消息重新发布

为了继续验证 RabbitMQ 消费端自身处理失败的情况，当前项目为 Listener Container 增加了重试机制。

当前配置：

```text
maxAttempts = 3
backOff = 500ms
```

即：

```text
Consumer 第 1 次执行失败
↓
等待约 500ms

Consumer 第 2 次执行失败
↓
等待约 500ms

Consumer 第 3 次执行失败
↓
重试耗尽
```

实验日志中三次 Consumer 调用时间间隔约为：

```text
500ms
500ms
```

从而验证 Spring AMQP Listener Retry 已实际生效。

当前重试逻辑由 Listener Container 统一管理，业务 Consumer 不需要自己手写 `for` 循环。

---

## RepublishMessageRecoverer

当 Consumer 连续尝试 3 次仍然失败时，当前项目使用：

```java
RepublishMessageRecoverer
```

处理最终失败消息。

核心目标：

```text
Retry Exhausted
↓
RepublishMessageRecoverer
↓
重新发布消息
↓
cache.invalidation.error.exchange
↓
routingKey = cache.invalidation.error
↓
cache.invalidation.error.queue
```

已通过日志验证：

```text
Republishing failed message to exchange
'cache.invalidation.error.exchange'
with routing key cache.invalidation.error
```

这里的：

```text
Republishing failed message
```

表示：

```text
正在重新发布“处理失败的消息”
```

不是：

```text
重新发布动作本身失败
```

---

## 为什么没有直接修改原 Queue 的 DLX 参数

项目曾尝试在已经存在的：

```text
cache.invalidation.queue
```

上追加：

```text
x-dead-letter-exchange
x-dead-letter-routing-key
```

这会导致 RabbitMQ 在重新声明同名 Queue 时检测到参数不一致，从而产生：

```text
PRECONDITION_FAILED
inequivalent arg
```

当前方案改为保持原主 Queue 参数不变，并显式注册两套拓扑：

```text
正常链路：
Exchange
→ RoutingKey
→ Queue

失败链路：
Error Exchange
→ Error RoutingKey
→ Error Queue
```

Consumer 重试耗尽后，由 `RepublishMessageRecoverer` 主动把失败消息重新发布到 Error Exchange。

因此：

```text
原 cache.invalidation.queue 不需要删除
也不需要修改原 Queue 的 x-arguments
```

当前机制属于：

```text
Spring 应用层失败消息重新发布
```

而不是：

```text
RabbitMQ Broker 原生 DLX / DLQ
```

两者最终都可以形成失败消息隔离，但触发机制不同。

---

## 当前 RabbitMQ 补偿边界

当前已实现：

```text
同步删除失败 → 发送 RabbitMQ ✅
正常 Exchange / Queue / Binding ✅
显式 RoutingKey ✅
JSON 消息转换 ✅
@RabbitListener Consumer ✅
Consumer 异步删除 Redis ✅
Consumer Retry：最多 3 次 ✅
500ms 重试间隔 ✅
Error Exchange / Error Queue / Binding ✅
RepublishMessageRecoverer ✅
重试耗尽后失败消息进入 Error Queue ✅
```

当前还没有实现：

```text
Error Queue 消息自动再次补偿
Publisher Confirm / Return
失败任务持久化
定时任务兜底
```

因此当前方案已经从“异步补偿第一阶段”推进到：

```text
同步补偿
+
MQ 异步补偿
+
Consumer Retry
+
失败消息隔离
```

但最终一致性的完整兜底链路仍在继续完善。

---

# 当前 Controller API

统一前缀：

```text
/api/redis
```

当前包含：

```text
/string
/string/ttl
/counter/increment
/ttl
/key

/hash
/hash/all

/list
/list/left
/list/right
/list/size

/set
/set/member
/set/size
/set/intersect
/set/union
/set/difference

/zset
/zset/reverse
/zset/score
/zset/score/increment
/zset/rank
/zset/reverse-rank
/zset/size

/bitmap
/bitmap/count

/hyperloglog
/hyperloglog/count
/hyperloglog/merge

/geo
/geo/position
/geo/distance
/geo/nearby

/stream
/stream/size

/object

/pipeline
/pipeline/get

/template/object

/cache/users/{id}    # GET：查询用户缓存
/cache/users/{id}    # PUT：更新 MySQL 后删除对应缓存
```

---

# 当前学习方式

本项目不只实现 API，还会对每个功能进行实际验证。

基本流程：

```text
学习 Redis 能力
        ↓
Service 实现
        ↓
Controller 暴露接口
        ↓
通过 HTTP 请求验证
        ↓
通过 redis-cli 二次确认
        ↓
记录可应用业务场景与注意事项
```

目标不是只记住 Redis 命令，而是理解：

```text
Redis 能做什么
为什么这样做
Java 中怎么调用
真实业务什么时候使用
可能踩什么坑
```

---

# 后续计划

后续将在当前项目上继续逐步增加：

1. Redis + MySQL 缓存一致性：Error Queue 后续补偿 / Publisher Confirm / 失败任务与定时任务兜底
2. TTL 与内存淘汰策略
3. 分布式锁
4. Lua
5. MULTI / EXEC / WATCH
6. RDB / AOF
7. 主从复制
8. Sentinel
9. Redis Cluster
10. Hot Key / Big Key / Slowlog
11. ACL、连接池、超时、重试、监控
12. Spring IoC / DI / Bean 生命周期 / ApplicationContext
13. Spring AOP / 代理机制 / Pointcut / Advice / 常见失效场景
14. Spring Boot 日志规范、SLF4J、Logback、AOP 请求链路日志

---

## 说明

本仓库用于 Redis 的持续学习与实践。

README 会随着项目能力增加持续更新，尽量保证：

```text
代码实现
+
验证结果
+
业务场景
+
注意事项
```

保持一致，避免 README 与实际代码脱节。
