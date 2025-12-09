package org.example.sansam.stock.redis;

import lombok.RequiredArgsConstructor;
import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;
import org.example.sansam.stock.domain.Stock;
import org.example.sansam.stock.repository.StockRepository;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final StringRedisTemplate redisTemplate;
    private final StockRepository stockRepository;

    private static final String USED_KEY_PATTERN = "stock:used:*";


    public int getOrLoadTotalStock(Long detailId) {
        String totalKey = totalKey(detailId);
        String cached = redisTemplate.opsForValue().get(totalKey);
        if (cached != null) {
            return Integer.parseInt(cached);
        }

        // 캐시 미스인 경우 RDB 조회
        Stock stock = stockRepository.findByProductDetailsId(detailId)
                .orElseThrow(() -> new CustomException(ErrorCode.ZERO_STOCK));

        int total = stock.getStockQuantity();

        // Redis에 캐시
        redisTemplate.opsForValue().set(totalKey, String.valueOf(total));
        return total;
    }

    private static final String RESERVE_LUA = """
        local total = tonumber(redis.call("GET", KEYS[1]))
        if not total then
          return -1
        end
        local used = tonumber(redis.call("GET", KEYS[2]) or "0")
        local newUsed = used + tonumber(ARGV[1])
        if newUsed > total then
          return 0
        end
        redis.call("INCRBY", KEYS[2], tonumber(ARGV[1]))
        return newUsed
        """;

    private final DefaultRedisScript<Long> RESERVE_SCRIPT =
            new DefaultRedisScript<>(RESERVE_LUA, Long.class);

    public void reserve(Long detailId, int qty) {
        if (qty <= 0) {
            throw new CustomException(ErrorCode.INVALID_STOCK_QUANTITY);
        }

        // total 캐시 보장
        getOrLoadTotalStock(detailId);

        String totalKey = totalKey(detailId);
        String usedKey = usedKey(detailId);

        List<String> keys = List.of(totalKey, usedKey);
        Long result = redisTemplate.execute(RESERVE_SCRIPT, keys, String.valueOf(qty));

        if (result == null || result == -1L) {
            // total 키가 없다 = RDB/캐시 문제
            throw new CustomException(ErrorCode.ZERO_STOCK);
        }
        if (result == 0L) {
            throw new CustomException(ErrorCode.NOT_ENOUGH_STOCK);
        }
    }

    private String totalKey(Long detailId) {
        return "stock:total:" + detailId;
    }

    private String usedKey(Long detailId) {
        return "stock:used:" + detailId;
    }


    public Map<Long, Integer> getAllUsed() {
        Map<Long, Integer> result = new HashMap<>();

        ScanOptions options = ScanOptions.scanOptions().match(USED_KEY_PATTERN).count(100).build();

        try(Cursor<byte[]> cursor =
                redisTemplate.getConnectionFactory()
                        .getConnection()
                        .scan(options)){
            while(cursor.hasNext()){
                String key = new String( cursor.next());
                String value = redisTemplate.opsForValue().get(key);

                if(value !=null){
                    Long detailId = Long.parseLong(key.split(":")[2]);
                    result.put(detailId, getOrLoadTotalStock(detailId));
                }
            }
        }
        return result;
    }
}
