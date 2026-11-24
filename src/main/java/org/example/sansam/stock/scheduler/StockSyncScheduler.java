package org.example.sansam.stock.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sansam.stock.service.StockUsageSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
@Component
public class StockSyncScheduler {

    private final StockUsageSyncService stockUsageSyncService;
    private final Clock clock;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void syncDailyStock(){
        LocalDate batchDate = LocalDate.now(clock);
        log.info("[STOCK][SYNC] 재고 동기화 시작. batchDate={}", batchDate);

        stockUsageSyncService.sync(batchDate);
    }
}
