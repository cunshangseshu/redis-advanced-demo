package com.cunshang.redisadvanced.controller;

import com.cunshang.redisadvanced.common.ApiResponse;
import com.cunshang.redisadvanced.service.RedisFoundationService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/redis")
public class RedisFoundationController {
    private final RedisFoundationService redisService;

    public RedisFoundationController(RedisFoundationService redisService) {
        this.redisService = redisService;
    }

    // String 类型========================================================================

    /**
     * SET
     * <p>
     * POST
     * /api/redis/string
     * <p>
     * example:
     * <p>
     * key=user:1001:name
     * value=cunshang
     */
    @PostMapping("/string")
    public ApiResponse<Void> set(
            @RequestParam String key,
            @RequestParam String value
    ) {
        redisService.set(key, value);
        //return "OK";
        return ApiResponse.success();
    }

    /**
     * GET
     */
    @GetMapping("/string")
    public ApiResponse<String> get(@RequestParam String key) {
        //return redisService.get(key);
        return ApiResponse.success(redisService.get(key));
    }

    /**
     * SET + TTL
     */
    @PostMapping("/string/ttl")
    public ApiResponse<Void> setWithTtl(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam long seconds
    ) {
        /*redisService.setWithTtl(key, value, seconds);
        return "OK";*/
        if (seconds <= 0) throw new IllegalArgumentException("你个人才，设置 0 秒？！意义何在啊；");
        redisService.setWithTtl(key, value, seconds);
        return ApiResponse.success();
    }

    /**
     * INCR
     */
    @PostMapping("/counter/increment")
    public ApiResponse<Long> increment(@RequestParam String key) {
        // return redisService.increment(key);
        return ApiResponse.success(redisService.increment(key));
    }

    /**
     * TTL
     */
    @GetMapping("/ttl")
    public ApiResponse<Long> ttl(@RequestParam String key) {
        // return redisService.ttl(key);
        return ApiResponse.success(redisService.ttl(key));
    }

    /**
     * DEL
     */
    @DeleteMapping("/key")
    public ApiResponse<Boolean> delete(@RequestParam String key) {
        // return redisService.delete(key);
        return ApiResponse.success(redisService.delete(key));
    }

    // hash 类型========================================================================

    /**
     * Hash - HSET
     */
    @PostMapping("/hash")
    public ApiResponse<Void> hashSet(
            @RequestParam String key,
            @RequestParam String field,
            @RequestParam String value
    ) {
        redisService.hashSet(key, field, value);
        // return "OK";
        return ApiResponse.success();
    }

    /**
     * Hash - HGET
     */
    @GetMapping("/hash")
    public ApiResponse<Object> hashGet(
            @RequestParam String key,
            @RequestParam String field
    ) {
        //return redisService.hashGet(key, field);
        return ApiResponse.success(redisService.hashGet(key, field));
    }

    /**
     * Hash - HGETALL
     */
    @GetMapping("/hash/all")
    public ApiResponse<Map<Object, Object>> hashGetAll(@RequestParam String key) {
        //return redisService.hashGetAll(key);
        return ApiResponse.success(redisService.hashGetAll(key));
    }

    /**
     * Hash - HDEL
     */
    @DeleteMapping("/hash")
    public Long hashDelete(@RequestParam String key, @RequestParam String field) {
        return redisService.hashDelete(key, field);
    }

    //  List 类型========================================================================

    /**
     * 从 List 左侧添加元素。
     * 对应 Redis：LPUSH key value
     *
     * @return 添加后 List 的元素数量
     */
    @PostMapping("/list/left")
    public ApiResponse<Long> listLeftPush(
            @RequestParam String key,
            @RequestParam String value
    ) {
        return ApiResponse.success(redisService.listLeftPush(key, value));
    }


    /**
     * 从 List 右侧添加元素。
     * 对应 Redis：RPUSH key value
     *
     * @return 添加后 List 的元素数量
     */
    @PostMapping("/list/right")
    public ApiResponse<Long> listRightPush(
            @RequestParam String key,
            @RequestParam String value
    ) {
        return ApiResponse.success(redisService.listRightPush(key, value));
    }

    /**
     * 获取 List 指定范围内的元素。
     * 对应 Redis：LRANGE key start end
     * <p>
     * 默认 0 ~ -1，表示获取全部元素。
     */
    @GetMapping("/list")
    public ApiResponse<List<String>> listRange(
            @RequestParam String key,
            @RequestParam(defaultValue = "0") long start,
            @RequestParam(defaultValue = "-1") long end
    ) {
        return ApiResponse.success(redisService.listRange(key, start, end));
    }

    /**
     * 从 List 左侧取出并删除一个元素。
     * 对应 Redis：LPOP key
     * <p>
     * List 为空或 Key 不存在时，data 为 null。
     */
    @DeleteMapping("/list/left")
    public ApiResponse<String> listLeftPop(@RequestParam String key) {
        return ApiResponse.success(redisService.listLeftPop(key));
    }

    /**
     * 从 List 右侧取出并删除一个元素。
     * 对应 Redis：RPOP key
     * <p>
     * List 为空或 Key 不存在时，data 为 null。
     */
    @DeleteMapping("/list/right")
    public ApiResponse<String> listRightPop(@RequestParam String key) {
        return ApiResponse.success(redisService.listRightPop(key));
    }

    /**
     * 获取 List 当前元素数量。
     * 对应 Redis：LLEN key
     * <p>
     * Key 不存在时返回 0。
     */
    @GetMapping("/list/size")
    public ApiResponse<Long> listSize(@RequestParam String key) {
        return ApiResponse.success(redisService.listSize(key));
    }

    //  Set 类型========================================================================

    /**
     * 向 Set 添加元素。
     * 对应 Redis：SADD key member
     * 可用此三层业务链体验 Set 的自动去重
     */
    @PostMapping("/set")
    public ApiResponse<Long> setAdd(
            @RequestParam String key,
            @RequestParam String member
    ) {
        return ApiResponse.success(redisService.setAdd(key, member)
        );
    }

    /**
     * 获取 Set 全部元素。
     * 对应 Redis：SMEMBERS key
     */
    @GetMapping("/set")
    public ApiResponse<Set<String>> setMembers(
            @RequestParam String key
    ) {
        return ApiResponse.success(redisService.setMembers(key));
    }

    /**
     * 判断元素是否存在。
     * 对应 Redis：SISMEMBER key member
     */
    @GetMapping("/set/member")
    public ApiResponse<Boolean> setIsMember(
            @RequestParam String key,
            @RequestParam String member
    ) {
        return ApiResponse.success(redisService.setIsMember(key, member));
    }

    /**
     * 删除 Set 中的元素。
     * 对应 Redis：SREM key member
     */
    @DeleteMapping("/set")
    public ApiResponse<Long> setRemove(
            @RequestParam String key,
            @RequestParam String member
    ) {
        return ApiResponse.success(redisService.setRemove(key, member));
    }

    /**
     * 获取 Set 元素数量。
     * 对应 Redis：SCARD key
     */
    @GetMapping("/set/size")
    public ApiResponse<Long> setSize(
            @RequestParam String key
    ) {
        return ApiResponse.success(redisService.setSize(key));
    }

    /**
     * 获取两个 Set 的交集。
     * 对应 Redis：SINTER key1 key2
     * 对应社交软件的做法是：共同标签 / 共同兴趣 / 共同好友；
     */
    @GetMapping("/set/intersect")
    public ApiResponse<Set<String>> setIntersect(
            @RequestParam String key1,
            @RequestParam String key2
    ) {
        return ApiResponse.success(redisService.setIntersect(key1, key2));
    }

    /**
     * 获取两个 Set 的并集。
     * 对应 Redis：SUNION key1 key2。
     * 此做法的特点是：两者都有的元素基础上去重；
     */
    @GetMapping("/set/union")
    public ApiResponse<Set<String>> setUnion(
            @RequestParam String key1,
            @RequestParam String key2
    ) {
        return ApiResponse.success(redisService.setUnion(key1, key2));
    }

    /**
     * 获取两个 Set 的差集。
     * 对应 Redis：SDIFF key1 key2
     * <p>
     * 这里表示 key1 - key2。
     */
    @GetMapping("/set/difference")
    public ApiResponse<Set<String>> setDifference(
            @RequestParam String key1,
            @RequestParam String key2
    ) {
        return ApiResponse.success(redisService.setDifference(key1, key2));
    }

    //  Set 类型========================================================================

    /**
     * 向 ZSet 添加元素及分数。
     * 对应 Redis：ZADD key score member
     * <p>
     * 真实场景：
     * 用户加入积分榜、商品加入销量榜。
     * !!!很重要的一点：ZADD 对已存在 member 可以更新 score，返回值不一定代表操作失败。
     */
    @PostMapping("/zset")
    public ApiResponse<Boolean> zSetAdd(
            @RequestParam String key,
            @RequestParam String member,
            @RequestParam double score
    ) {
        return ApiResponse.success(redisService.zSetAdd(key, member, score));
    }

    /**
     * 按 score 从低到高查询。
     * 对应 Redis：ZRANGE key start end
     * <p>
     * 真实场景：
     * 查询最低积分区间、低分段用户。
     */
    @GetMapping("/zset")
    public ApiResponse<Set<String>> zSetRange(
            @RequestParam String key,
            @RequestParam(defaultValue = "0") long start,
            @RequestParam(defaultValue = "-1") long end
    ) {
        return ApiResponse.success(redisService.zSetRange(key, start, end));
    }


    /**
     * 按 score 从高到低查询，
     * 下表从0开始相当于1。
     * <p>
     * 真实场景：
     * 游戏排行榜、热度榜 Top ***。
     */
    @GetMapping("/zset/reverse")
    public ApiResponse<Set<String>> zSetReverseRange(
            @RequestParam String key,
            @RequestParam(defaultValue = "0") long start,
            @RequestParam(defaultValue = "-1") long end
    ) {
        return ApiResponse.success(redisService.zSetReverseRange(key, start, end));
    }


    /**
     * 查询指定 member 的 score。
     * 对应 Redis：ZSCORE key member
     * <p>
     * 真实场景：
     * 查询用户当前积分。
     */
    @GetMapping("/zset/score")
    public ApiResponse<Double> zSetScore(
            @RequestParam String key,
            @RequestParam String member
    ) {
        return ApiResponse.success(redisService.zSetScore(key, member));
    }


    /**
     * 增加指定 member 的 score。
     * 对应 Redis：ZINCRBY key increment member
     * <p>
     * 真实场景：
     * 游戏胜利积分 +10、文章热度 +1。
     */
    @PostMapping("/zset/score/increment")
    public ApiResponse<Double> zSetIncrementScore(
            @RequestParam String key,
            @RequestParam String member,
            @RequestParam double increment
    ) {
        return ApiResponse.success(redisService.zSetIncrementScore(key, member, increment));
    }


    /**
     * 查询正序排名。
     * <p>
     * Redis 排名从 0 开始。
     */
    @GetMapping("/zset/rank")
    public ApiResponse<Long> zSetRank(
            @RequestParam String key,
            @RequestParam String member
    ) {
        return ApiResponse.success(redisService.zSetRank(key, member));
    }


    /**
     * 查询倒序排名。
     * <p>
     * score 越高排名越靠前。
     * Redis 排名从 0 开始。
     * <p>
     * 真实场景：
     * 查询“我在排行榜第几名”。
     */
    @GetMapping("/zset/reverse-rank")
    public ApiResponse<Long> zSetReverseRank(
            @RequestParam String key,
            @RequestParam String member
    ) {
        return ApiResponse.success(redisService.zSetReverseRank(key, member));
    }


    /**
     * 删除 ZSet 中的元素。
     * 对应 Redis：ZREM key member
     * <p>
     * 真实场景：
     * 用户退出排行榜、榜单移除商品。
     */
    @DeleteMapping("/zset")
    public ApiResponse<Long> zSetRemove(
            @RequestParam String key,
            @RequestParam String member
    ) {
        return ApiResponse.success(redisService.zSetRemove(key, member));
    }


    /**
     * 获取 ZSet 元素数量。
     * 对应 Redis：ZCARD key
     */
    @GetMapping("/zset/size")
    public ApiResponse<Long> zSetSize(
            @RequestParam String key
    ) {
        return ApiResponse.success(redisService.zSetSize(key));
    }
}