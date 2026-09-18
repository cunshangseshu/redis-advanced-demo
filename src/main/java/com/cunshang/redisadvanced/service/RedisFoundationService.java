package com.cunshang.redisadvanced.service;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RedisFoundationService {

    private final StringRedisTemplate redisTemplate;

    public RedisFoundationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // String 类型========================================================================

    /**
     * 写入 String 数据。
     * <p>
     * 对应 Redis：
     * <p>
     * SET key value
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 获取 String 数据。
     * <p>
     * 对应 Redis：
     * <p>
     * GET key
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }


    /**
     * 写入带 TTL 的数据。
     * <p>
     * 对应：
     * <p>
     * SET key value EX seconds
     */
    public void setWithTtl(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
    }

    /**
     * 原子自增。
     * <p>
     * 对应：
     * <p>
     * INCR key
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 查询剩余 TTL。
     * <p>
     * 对应：
     * <p>
     * TTL key
     */
    public Long ttl(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 删除 Key。
     * <p>
     * 对应：
     * <p>
     * DEL key
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    // hash 类型========================================================================

    /**
     * Hash：写入一个字段
     * <p>
     * HSET key field value
     */
    public void hashSet(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * Hash：获取一个字段
     * <p>
     * HGET key field
     */
    public Object hashGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * Hash：获取全部字段
     * <p>
     * HGETALL key
     */
    public Map<Object, Object> hashGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Hash：删除字段
     * <p>
     * HDEL key field
     */
    public Long hashDelete(String key, String field) {
        return redisTemplate.opsForHash().delete(key, field);
    }


    //  List 类型========================================================================
    //  List 有下标 0 1 2 3...

    /**
     * 从 List 左侧添加元素。
     * 对应 Redis：LPUSH key value
     *
     * @return 添加后 List 的元素数量
     */
    public Long listLeftPush(String key, String value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 从 List 右侧添加元素。
     * 对应 Redis：RPUSH key value
     *
     * @return 添加后 List 的元素数量
     */
    public Long listRightPush(String key, String value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 获取 List 指定范围内的元素。
     * 对应 Redis：LRANGE key start end
     * <p>
     * 例如：
     * 0, -1 表示获取全部元素
     */
    public List<String> listRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 从 List 左侧取出并删除一个元素。
     * 对应 Redis：LPOP key
     * <p>
     * List 为空或 Key 不存在时返回 null。
     */
    public String listLeftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 从 List 右侧取出并删除一个元素。
     * 对应 Redis：RPOP key
     * <p>
     * List 为空或 Key 不存在时返回 null。
     */
    public String listRightPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取 List 当前元素数量。
     * 对应 Redis：LLEN key
     * <p>
     * Key 不存在时 Redis 返回 0。
     */
    public long listSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size == null ? 0L : size;
    }

    //  Set 类型========================================================================
    //  Set 无序 + 元素唯一 + 自动去重；

    /**
     * 向 Set 中添加一个元素。
     * 对应 Redis：SADD key member
     *
     * @return 新增成功的元素数量；
     * 元素已经存在时返回 0
     */
    public Long setAdd(String key, String member) {
        return redisTemplate.opsForSet().add(key, member);
    }

    /**
     * 获取 Set 中的全部元素。
     * 对应 Redis：SMEMBERS key
     */
    public Set<String> setMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 判断指定元素是否存在于 Set 中。
     * 对应 Redis：SISMEMBER key member
     */
    public Boolean setIsMember(String key, String member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /**
     * 从 Set 中删除指定元素。
     * 对应 Redis：SREM key member
     *
     * @return 实际删除的元素数量
     */
    public Long setRemove(String key, String member) {
        return redisTemplate.opsForSet().remove(key, member);
    }

    /**
     * 获取 Set 当前元素数量。
     * 对应 Redis：SCARD key
     * <p>
     * Key 不存在时返回 0。
     */
    public long setSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    /**
     * 获取两个 Set 的交集。
     * 对应 Redis：SINTER key1 key2
     */
    public Set<String> setIntersect(String key1, String key2) {
        return redisTemplate.opsForSet().intersect(key1, key2);
    }

    /**
     * 获取两个 Set 的并集。
     * 对应 Redis：SUNION key1 key2
     */
    public Set<String> setUnion(String key1, String key2) {
        return redisTemplate.opsForSet().union(key1, key2);
    }

    /**
     * 获取两个 Set 的差集。
     * 对应 Redis：SDIFF key1 key2
     * <p>
     * 注意：
     * 差集有方向，这里表示 key1 - key2。
     */
    public Set<String> setDifference(String key1, String key2) {
        return redisTemplate.opsForSet().difference(key1, key2);
    }

    //  Set 类型========================================================================
    //  特点：不重复元素 + 每个元素有一个 score + 按 score 排序
    //  游戏积分排行榜、文章热度榜、直播排行榜、销售、用户贡献榜

    /**
     * 向 ZSet 添加元素及分数。
     * 对应 Redis：ZADD key score member
     *
     * @return 新增成功返回 true；
     * 已存在的 member 更新 score 时通常返回 false
     * <p>
     * 此功能对应的真实业务可以：
     * → 用户获得初始积分
     * → 商品加入销量榜
     * → 文章加入热度榜
     */
    public Boolean zSetAdd(String key, String member, double score) {
        return redisTemplate.opsForZSet().add(key, member, score);
    }

    /**
     * 按 score 从低到高获取元素。
     * 对应 Redis：ZRANGE key start end
     *
     */
    public Set<String> zSetRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 按 score 从高到低获取元素(reserveRange <-> range 两极反转~~~)。
     * <p>
     * 适合排行榜场景。
     */
    public Set<String> zSetReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取指定 member 的 score。
     * 对应 Redis：ZSCORE key member
     * <p>
     * member 不存在时返回 null。
     */
    public Double zSetScore(String key, String member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    /**
     * 增加指定 member 的 score。
     * 对应 Redis：ZINCRBY key increment member
     *
     * @return 修改后的新 score
     */
    public Double zSetIncrementScore(String key, String member, double increment) {
        return redisTemplate.opsForZSet().incrementScore(key, member, increment);
    }

    /**
     * 获取指定 member 的正序排名。
     * <p>
     * 排名从 0 开始。
     */
    public Long zSetRank(String key, String member) {
        return redisTemplate.opsForZSet().rank(key, member);
    }

    /**
     * 获取 member 的倒序排名。
     * <p>
     * score 越高，排名越靠前。
     * 排名从 0 开始。
     */
    public Long zSetReverseRank(String key, String member) {
        return redisTemplate.opsForZSet().reverseRank(key, member);
    }

    /**
     * 删除 ZSet 中的元素。
     * 对应 Redis：ZREM key member
     */
    public Long zSetRemove(String key, String member) {
        return redisTemplate.opsForZSet().remove(key, member);
    }

    /**
     * 获取 ZSet 元素数量。
     * 对应 Redis：ZCARD key
     */
    public long zSetSize(String key) {
        Long size = redisTemplate.opsForZSet().size(key);
        return size == null ? 0L : size;
    }

    //  Bitmap 类型========================================================================

    /**
     * 设置 Bitmap 指定位的状态。
     * 对应 Redis：SETBIT key offset value
     * <p>
     * 真实场景：
     * 用户签到、是否在线、活动参与状态。
     */
    public Boolean bitmapSet(String key, long offset, boolean value) {
        return redisTemplate.opsForValue().setBit(key, offset, value);
    }

    /**
     * 查询 Bitmap 指定位的状态。
     * 对应 Redis：GETBIT key offset
     * <p>
     * 真实场景：
     * 判断用户某天是否签到。
     */
    public Boolean bitmapGet(String key, long offset) {
        return redisTemplate.opsForValue().getBit(key, offset);
    }

    /**
     * 统计 Bitmap 中值为 1 的 bit 数量。
     * 对应 Redis：BITCOUNT key
     * <p>
     * 真实场景：
     * 统计用户本月签到多少天。
     */
    public long bitmapCount(String key) {
        byte[] rawKey = redisTemplate.getStringSerializer().serialize(key);
        if (rawKey == null) {
            return 0L;
        }
        Long count = redisTemplate.execute((RedisCallback<Long>) connection -> connection.stringCommands().bitCount(rawKey));
        return count == null ? 0L : count;
    }

}