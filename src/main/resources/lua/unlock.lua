-- 文件：src/main/resources/lua/unlock.lua
-- 注释维护时间：2026-10-27 14:32（UTC+08:00）
-- 调用入口：CacheAsideService.unlock(lockKey, token)
--
-- 目的：安全释放由 UUID Token 标识所有权的 Redis 分布式锁。
-- 参数：
--   KEYS[1] = 锁 Key，例如 lock:user:profile:1001
--   ARGV[1] = 当前调用方成功加锁时持有的 UUID Token
-- 返回：
--   1 = Token 匹配，DEL 成功删除 Key
--   0 = 锁不存在或 Token 不匹配，不删除任何 Key
--
-- 原子性：比较 Token 和删除 Key 在 Redis 的一次 Lua 执行中完成，
--         避免独立 GET、DEL 两条命令之间锁过期并被其他请求重获的竞态。
-- 边界：此脚本不延长锁 TTL、不提供看门狗续期，也不保证业务操作一定在锁有效期内结束。
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
end

return 0