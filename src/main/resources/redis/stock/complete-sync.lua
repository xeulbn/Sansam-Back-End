-- KEYS[1]: 옵션 해시
-- ARGV[1]: 실제로 MySQL에 반영한 배치일
if redis.call('HGET', KEYS[1], 'lastBatchDate') ~= ARGV[1] then
    return 0
end

-- DB에 반영한 수량만 정리한다. 배치 중 새 주문이 올린 used는 보존
return redis.call('HDEL', KEYS[1], 'pendingQty')
