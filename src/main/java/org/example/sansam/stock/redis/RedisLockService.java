package org.example.sansam.stock.redis;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisLockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    private Counter stockLockAcquireSuccessCounter;
    private Counter stockLockAcquireFailCounter;
    private Counter stockUnlockFailCounter;

    @PostConstruct
    public void init() {
        this.stockLockAcquireSuccessCounter = Counter.builder("stock.lock.acquire.success")
                .register(meterRegistry);
        this.stockLockAcquireFailCounter = Counter.builder("stock.lock.acquire.fail")
                .register(meterRegistry);
        this.stockUnlockFailCounter = Counter.builder("stock.lock.unlock.fail")
                .register(meterRegistry);
    }

    public String tryLock(String key, long expireMillis) {
        String token = UUID.randomUUID().toString();

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, token, expireMillis, TimeUnit.MILLISECONDS);

        if (Boolean.TRUE.equals(success)) {
            stockLockAcquireSuccessCounter.increment();
            return token;
        } else {
            stockLockAcquireFailCounter.increment();
            return null;
        }
    }

    public boolean unlock(String key, String token) {

        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "   return redis.call('del', KEYS[1]) " +
                        "else " +
                        "   return 0 " +
                        "end";

        Long result = redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                java.util.Collections.singletonList(key),
                token
        );

        boolean success = result != null && result > 0;
        if (!success) {
            stockUnlockFailCounter.increment();
        }
        return success;
    }
}
