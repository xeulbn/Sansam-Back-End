package org.example.sansam.stock.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sansam.stock.Service.StockService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
@Component
public class StockSyncScheduler {

    private StockService stockService;

    @Scheduled(cron = "0 0 3 * * *")
    public void syncDailyStock(){
        LocalDate today = LocalDate.now();
        log.info("[STOCK][SYNC] start daily sync. date={}", today.minusDays(1));

        stockService.dailySync(today.minusDays(1));
    }
}
