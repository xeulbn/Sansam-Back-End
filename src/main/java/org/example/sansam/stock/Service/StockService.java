package org.example.sansam.stock.Service;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;
import org.example.sansam.stock.domain.Stock;
import org.example.sansam.stock.redis.RedisLockService;
import org.example.sansam.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class StockService {

    private final StockRepository stockRepository;
    private final RedisLockService redisLockService;

    private final Timer stockDecreaseTimer;
    private final Counter stockLockFailBusinessCounter;
    private final Counter stockNotEnoughCounter;
    private static final long LOCK_EXPIRE = 3000;


    public StockService(StockRepository stockRepository,
                        RedisLockService redisLockService,
                        MeterRegistry meterRegistry) {

        this.stockRepository = stockRepository;
        this.redisLockService = redisLockService;

        this.stockDecreaseTimer = Timer.builder("stock.decrease.time")
                .register(meterRegistry);

        this.stockLockFailBusinessCounter = Counter.builder("stock.lock_fail.business.total")
                .register(meterRegistry);

        this.stockNotEnoughCounter = Counter.builder("stock.not_enough.total")
                .register(meterRegistry);
    }


//    @Transactional
//    public void decreaseStock(Long productDetailId, int quantity) {
//        int updated = stockRepository.decreaseIfEnough(productDetailId, quantity);
//        if (updated == 0) {
//            throw new CustomException(ErrorCode.NOT_ENOUGH_STOCK);
//        }
//    }

    @Transactional
    public void decreaseStock(Long detailId, int qty) {
        Timer.Sample sample = Timer.start();  // 타이머 시작

        String lockKey = "stock:lock:" + detailId;
        String token = redisLockService.tryLock(lockKey, LOCK_EXPIRE);
        log.info("재고 시스템 get Lock : lockKey={}, token={}", lockKey, token);

        if (token == null) {
            stockLockFailBusinessCounter.increment(); // 락 실패 카운터
            sample.stop(stockDecreaseTimer);          // 실패도 기록
            throw new CustomException(ErrorCode.STOCK_LOCK_FAIL);
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        boolean unlocked = redisLockService.unlock(lockKey, token);
                        if (!unlocked) {
                            log.error("[LOCK][UNLOCK_FAILED] key={}, token={}", lockKey, token);
                        }
                    }
                }
        );

        try {
            Stock stock = stockRepository.findByProductDetailsId(detailId)
                    .orElseThrow(() -> {
                        return new CustomException(ErrorCode.ZERO_STOCK);
                    });

            if (stock.getStockQuantity() < qty) {
                stockNotEnoughCounter.increment();
                throw new CustomException(ErrorCode.NOT_ENOUGH_STOCK);
            }

            stock.decrease(qty);

            // 정상 흐름 끝에서 타이머 stop
            sample.stop(stockDecreaseTimer);
        } catch (RuntimeException e) {
            // 예외도 타이머에 포함되도록 여기서도 stop 할 수 있음 (이미 위에서 stop 안 했다면)
            sample.stop(stockDecreaseTimer);
            throw e;
        }
    }

    @Transactional
    public void increaseStock(Long productDetailId, int quantity) {
        stockRepository.increase(productDetailId, quantity);
    }

    @Transactional(readOnly = true)
    public int checkItemStock(Long detailId){
        return stockRepository.findByProductDetailsId(detailId)
                .map(Stock::getStockQuantity)
                .orElse(0);
    }

}
