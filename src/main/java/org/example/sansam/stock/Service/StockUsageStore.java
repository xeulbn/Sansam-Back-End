package org.example.sansam.stock.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

public interface StockUsageStore {
    // 반환한 스트림을 닫을 때 Redis SCAN 커서도 함께 닫아야 한다.
    Stream<Long> scanDetailIds();

    Optional<StockUsageSyncService.PendingUsage> prepareSync(long detailId, LocalDate batchDate);

    void completeSync(long detailId, LocalDate appliedDate);
}
