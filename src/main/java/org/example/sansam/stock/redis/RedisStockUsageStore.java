package org.example.sansam.stock.redis;

import lombok.RequiredArgsConstructor;
import org.example.sansam.stock.service.StockUsageStore;
import org.example.sansam.stock.service.StockUsageSyncService.PendingUsage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.example.sansam.stock.redis.RedisStockService.STOCK_KEY_PREFIX;

@Component
@RequiredArgsConstructor
public class RedisStockUsageStore implements StockUsageStore {

    private static final ScanOptions OPTIONS = ScanOptions.scanOptions()
            .match(STOCK_KEY_PREFIX + "*")
            .count(200)
            .build();

    private static final RedisScript<?> PREPARE = RedisScript.of(
            new ClassPathResource("redis/stock/prepare-sync.lua"), List.class
    );

    private static final RedisScript<Long> COMPLETE = RedisScript.of(
            new ClassPathResource("redis/stock/complete-sync.lua"), Long.class
    );

    private final StringRedisTemplate redis;

    @Override
    public Stream<Long> scanDetailIds() {
        // 호출자가 스트림을 닫으면 Redis SCAN 커서도 함께 닫힌다.
        return redis.scan(OPTIONS).stream()
                .map(key -> Long.parseLong(key.substring(STOCK_KEY_PREFIX.length())));
    }

    @Override
    public Optional<PendingUsage> prepareSync(long detailId, LocalDate batchDate) {
        Object result = redis.execute(
                PREPARE,
                List.of(STOCK_KEY_PREFIX + detailId),
                batchDate.toString()
        );

        if (!(result instanceof List<?> fields)) {
            throw new IllegalStateException("배치 Lua 응답 누락. detailId=" + detailId);
        }

        // Lua가 빈 배열을 반환하면 반영할 수량이 없다.
        if (fields.isEmpty()) {
            return Optional.empty();
        }

        // Lua 응답 순서는 [배치일, 반영할 수량]이다.
        if (fields.size() != 2 || !(fields.get(0) instanceof String date)
                || !(fields.get(1) instanceof String amount)) {
            throw new IllegalStateException("배치 Lua 응답 형식 오류. detailId=" + detailId);
        }

        PendingUsage usage = new PendingUsage(
                LocalDate.parse(date),
                Integer.parseInt(amount)
        );

        if (usage.batchDate().isAfter(batchDate) || usage.quantity() <= 0) {
            throw new IllegalStateException("배치 날짜 또는 수량 오류. detailId=" + detailId);
        }

        return Optional.of(usage);
    }

    @Override
    public void completeSync(long detailId, LocalDate appliedDate) {
        Long result = redis.execute(
                COMPLETE,
                List.of(STOCK_KEY_PREFIX + detailId),
                appliedDate.toString()
        );

        // 1이면 이번 호출이 정리했고, 0이면 이미 정리됐거나 다른 배치일이다.
        if (result == null || (result != 0L && result != 1L)) {
            throw new IllegalStateException("배치 완료 응답 오류. detailId=" + detailId);
        }
    }
}
