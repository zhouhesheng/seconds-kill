local n = tonumber(ARGV[1])
if not n  or n == 0 then
    return 0
end
local key = KEYS[1]
local goodsInfo = redis.call("HMGET", key, "totalCount", "seckillCount")
local total = tonumber(goodsInfo[1])
local alloc = tonumber(goodsInfo[2])
if not total then
    return 0
end
if total >= alloc + n  then
    local ret = redis.call("HINCRBY", key, "seckillCount" ,n)
    return n
end
return 0