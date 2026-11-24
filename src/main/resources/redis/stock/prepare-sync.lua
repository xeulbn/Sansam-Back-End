-- KEYS[1]: 옵션 해시
-- ARGV[1]: 서울 기준 배치일(YYYY-MM-DD)
local key = KEYS[1]
local date = ARGV[1]

if redis.call('EXISTS', key) == 0 then
    return {}
end

local fields = redis.call('HMGET', key, 'total', 'used', 'lastBatchDate', 'pendingQty')
local lastDate = fields[3]
local pending = fields[4]

-- 늦게 실행된 과거 배치가 더 최근 배치의 상태를 변경하지 않도록
if lastDate and lastDate > date then
    return {}
end

-- 미완료 건이 있으면 확정해둔 날짜와 수량을 그대로 반환
if pending then
    if not lastDate then
        return redis.error_reply('pendingQty wisthout lastBatchDate')
    end
    return {lastDate, pending}
end

-- 같은 날 배치가 이미 완료됐다. 그 뒤의 새 주문은 다음 배치에
if lastDate == date then
    return {}
end

local total = tonumber(fields[1])
local used = tonumber(fields[2] or '0')

if not total or not used or total < 0 or used < 0 or used > total
    or total > 2147483647 or total ~= math.floor(total) or used ~= math.floor(used) then
    return redis.error_reply('invalid stock quantities')
end

if used == 0 then
    redis.call('HSET', key, 'lastBatchDate', date)
    return {}
end


redis.call('HSET', key, 'total', total - used, 'used', 0,
           'lastBatchDate', date, 'pendingQty', used)

return {date, tostring(used)}
