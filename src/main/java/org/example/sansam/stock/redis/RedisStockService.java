package org.example.sansam.stock.redis;

import lombok.RequiredArgsConstructor;
import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;
import org.example.sansam.stock.domain.Stock;
import org.example.sansam.stock.repository.StockRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final StringRedisTemplate redisTemplate;
    private final StockRepository stockRepository;

    static final String STOCK_KEY_PREFIX = "stock:{stock}:option:";

    public void reserveAll(Map<Long, Integer> quantitiesByDetailId) {
        if (quantitiesByDetailId.isEmpty()) {
            throw new CustomException(ErrorCode.NO_ITEM_IN_ORDER);
        }

        List<String> keys = new ArrayList<>(quantitiesByDetailId.size());
        List<String> quantities = new ArrayList<>(quantitiesByDetailId.size());

        for (var entry : quantitiesByDetailId.entrySet()) {
            Long detailId = entry.getKey();
            ensureTotalCached(detailId);
            keys.add(stockKey(detailId));
            quantities.add(String.valueOf(entry.getValue()));
        }

        Long result = redisTemplate.execute(RESERVE_SCRIPT, keys, quantities.toArray());

        if (result == null)
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        if (result == 1L)
            return;
        if (result == 0L)
            throw new CustomException(ErrorCode.NOT_ENOUGH_STOCK);
        if (result == -1L)
            throw new CustomException(ErrorCode.ZERO_STOCK);
        if (result == -2L)
            throw new CustomException(ErrorCode.INVALID_STOCK_QUANTITY);
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private String stockKey(Long detailId) {
        return STOCK_KEY_PREFIX + detailId;
    }

    private static final String RESERVE_LUA = """
    if #KEYS == 0 or #KEYS ~= #ARGV then
        return -2
    end

    local nextUsed = {}

    for i = 1, #KEYS do
        local qty = tonumber(ARGV[i])
        if not qty or qty <= 0 or qty ~= math.floor(qty) then
            return -2
        end

        local fields = redis.call('HMGET', KEYS[i], 'total', 'used')
        if not fields[1] then
            return -1
        end

        local total = tonumber(fields[1])
        local used = tonumber(fields[2] or '0')
        if not total or not used or total < 0 or used < 0
            or total ~= math.floor(total) or used ~= math.floor(used)
            or total > 2147483647 or used > total then
            return -3
        end

        if qty > total - used then
            return 0
        end

        nextUsed[i] = used + qty
    end

    for i = 1, #KEYS do
        redis.call('HSET', KEYS[i], 'used', nextUsed[i])
    end

    return 1
    """;

    private final DefaultRedisScript<Long> RESERVE_SCRIPT =
            new DefaultRedisScript<>(RESERVE_LUA, Long.class);


    private void ensureTotalCached(Long detailId) {
        String key = stockKey(detailId);
        if (redisTemplate.opsForHash().hasKey(key, "total")) {
            return;
        }

        Stock stock = stockRepository.findByProductDetailsId(detailId)
                .orElseThrow(() -> new CustomException(ErrorCode.ZERO_STOCK));

        redisTemplate.opsForHash().putIfAbsent(
                key, "total", String.valueOf(stock.getStockQuantity())
        );
    }
}
