package org.example.sansam.stock.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sansam.stock.domain.DailyStockUsage;
import org.example.sansam.stock.domain.Stock;
import org.example.sansam.stock.redis.RedisStockService;
import org.example.sansam.stock.repository.DailyStockUsageRepository;
import org.example.sansam.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final RedisStockService redisStockService;
    private final DailyStockUsageRepository dailyStockUsageRepository;

    @Transactional
    public void decreaseStock(Long detailId, int qty) {
        // Redis 선 체크 & 사용량 증가
        redisStockService.reserve(detailId, qty);

        log.error("[STOCK][DECREASE] detailId={} qty={} redis reserve success", detailId, qty);
    }

    @Transactional
    public void increaseStock(Long productDetailId, int quantity) {
        stockRepository.increase(productDetailId, quantity);
    }

    @Transactional
    public void dailySync(LocalDate date) {
        Map<Long, Integer> usedMap = redisStockService.getAllUsed();

        for (Map.Entry<Long, Integer> entry : usedMap.entrySet()) {
            Long detailId = entry.getKey();
            int usedQty   = entry.getValue();

            DailyStockUsage usage = dailyStockUsageRepository
                    .findByProductDetailsIdAndUsageDate(detailId, date)
                    .orElseGet(() -> new DailyStockUsage(detailId, date, 0));

            int delta = usedQty - usage.getUsedQuantity();
            if (delta > 0) {
                usage.add(delta);


                Stock stock = stockRepository.findByProductDetailsId(detailId)
                        .orElseThrow();
                stock.decrease(delta);
            }

            dailyStockUsageRepository.save(usage);
        }
    }

}
