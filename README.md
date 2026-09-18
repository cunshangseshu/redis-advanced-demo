# Redis Advanced Demo

基于 **Java 21 + Spring Boot 3.5.16 + Spring Data Redis + Redis 7.4** 的 Redis 学习与实践项目。

本项目以“**边学、边写、边验证**”为核心方式，通过 Controller → Service → Redis 的完整调用链，逐步学习 Redis 的常用数据结构、异常处理、缓存、分布式锁、高可用与集群等内容。

---

## 技术栈

- Java 21
- Spring Boot 3.5.16
- Spring Web
- Spring Data Redis
- Lettuce
- Redis 7.4
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
│       │   ├── controller
│       │   │   └── RedisFoundationController.java
│       │   ├── exception
│       │   │   ├── BusinessException.java
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── model
│       │   │   └── RedisUserProfile.java
│       │   └── service
│       │       └── RedisFoundationService.java
│       └── resources
│           └── application.yml
└── redis-data
```

> `redis-data/` 为 Redis 本地持久化目录，应通过 `.gitignore` 排除，不提交到 Git 仓库。

---

## 运行方式

### 1. 启动 Redis

```bash
docker compose up -d
```

当前 Docker 配置：

```text
Redis 镜像：redis:7.4-alpine
容器名称：redis-advanced-learning
宿主机端口：6380
容器端口：6379
AOF：开启
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

### 3. 启动 Spring Boot

应用默认端口：

```text
8080
```

Redis 连接：

```text
localhost:6380
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
| Cache Aside | ⏳ |
| 缓存穿透 / 击穿 / 雪崩 | ⏳ |
| Redis + MySQL 缓存一致性 | ⏳ |
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
- 当前只实现基础对象存取，尚未引入统一 RedisTemplate 序列化配置、泛型对象处理、版本兼容策略等进一步内容。
- 这一能力会作为后续 Cache Aside 与 Redis + MySQL 缓存学习的基础。

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

1. Pipeline / 批量操作
2. Cache Aside
3. 缓存穿透、击穿、雪崩
4. Redis + MySQL 缓存一致性
5. TTL 与内存淘汰策略
6. 分布式锁
7. Lua
8. MULTI / EXEC / WATCH
9. RDB / AOF
10. 主从复制
11. Sentinel
12. Redis Cluster
13. Hot Key / Big Key / Slowlog
14. ACL、连接池、超时、重试、监控
15. Spring Boot 日志规范、SLF4J、Logback、AOP 请求链路日志

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
