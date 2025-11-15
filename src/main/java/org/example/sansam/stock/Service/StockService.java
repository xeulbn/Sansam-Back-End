package org.example.sansam.stock.Service;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;
import org.example.sansam.stock.domain.Stock;
import org.example.sansam.stock.repository.StockRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;


//    @Transactional
//    public void decreaseStock(Long productDetailId, int quantity) {
//        int updated = stockRepository.decreaseIfEnough(productDetailId, quantity);
//        if (updated == 0) {
//            throw new CustomException(ErrorCode.NOT_ENOUGH_STOCK);
//        }
//    }

    private static final int MAX_RETRY = 5;
    private final MeterRegistry meterRegistry;

    private Counter stockOptimisticFailureCounter;
    private Counter stockOptimisticRetryCounter;

    @PostConstruct
    public void initCounters() {
        stockOptimisticFailureCounter =
                Counter.builder("stock.optimistic_lock.failures")
                        .description("Number of optimistic lock conflicts on stock")
                        .register(meterRegistry);

        stockOptimisticRetryCounter =
                Counter.builder("stock.optimistic_lock.retries")
                        .description("Number of retries due to optimistic lock")
                        .register(meterRegistry);
    }


    @Transactional
    public void decreaseStock(Long detailId, int qty) {
        int attempt = 0;

        while (true) {
            try {
                attempt++;

                doDecrease(detailId, qty);

                // 재시도가 있었다면 카운트
                if (attempt > 1) {
                    stockOptimisticRetryCounter.increment();
                }
                return;

            } catch (OptimisticLockingFailureException e) {

                stockOptimisticFailureCounter.increment();
                if (attempt >= MAX_RETRY) {
                    throw new CustomException(ErrorCode.STOCK_OPTIMISTIC_LOCK_FAILED);
                }

                // 로그에 재시도 횟수 남기기
                log.warn("[STOCK][OPT-LOCK][RETRY] detailId={}, qty={}, attempt={}",
                        detailId, qty, attempt);

                // 간단한 backoff (optional)
                try {
                    Thread.sleep(10L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CustomException(ErrorCode.STOCK_OPTIMISTIC_LOCK_FAILED);
                }
            }
        }
    }

    private void doDecrease(Long detailId, int qty) {
        Stock stock = stockRepository.findByProductDetailsId(detailId)
                .orElseThrow(() -> new CustomException(ErrorCode.ZERO_STOCK));

        stock.decrease(qty);

        stockRepository.saveAndFlush(stock);
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
