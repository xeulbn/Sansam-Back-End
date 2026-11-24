package org.example.sansam.stock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockUsageSyncService {

    private final StockUsageStore store;
    private final StockUsageWriter writer;

    public void sync(LocalDate batchDate) {
        int failed = 0;

        // Redis에서 옵션 ID를 순회하고, 배치가 끝나면 스캔 커서를 닫는다.
        try (Stream<Long> ids = store.scanDetailIds()) {
            var iterator = ids.iterator();

            while (iterator.hasNext()) {
                long detailId = iterator.next();

                try {
                    syncOption(detailId, batchDate);
                } catch (RuntimeException e) {
                    // 실패한 옵션의 반영 대기 수량은 보존하고 다음 옵션을 처리한다.
                    failed++;
                    log.error("재고 동기화 실패. detailId={}, batchDate={}",
                            detailId, batchDate, e);
                }
            }
        }

        if (failed > 0) {
            throw new IllegalStateException("재고 동기화 실패 건수: " + failed);
        }
    }

    private void syncOption(long detailId, LocalDate batchDate) {
        while (true) {
            Optional<PendingUsage> pending = store.prepareSync(detailId, batchDate);
            if (pending.isEmpty()) {
                return;
            }

            PendingUsage usage = pending.get();

            // Lua가 반환한 날짜와 수량으로 DB에 반영한다.
            // 이미 반영한 건이면 추가 차감 없이 정상 반환한다.
            writer.applyOnce(detailId, usage);

            // DB 커밋이 성공한 뒤에만 해당 날짜의 pendingQty를 지운다.
            store.completeSync(detailId, usage.batchDate());

            if (usage.batchDate().equals(batchDate)) {
                return;
            }

            // 지난 배치의 미완료 건을 처리했다면 같은 옵션의 이번 배치분도 확인한다.
        }
    }

    public record PendingUsage(LocalDate batchDate, int quantity) {}

}
