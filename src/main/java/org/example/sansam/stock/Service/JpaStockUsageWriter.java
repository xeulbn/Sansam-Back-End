package org.example.sansam.stock.service;

import lombok.RequiredArgsConstructor;
import org.example.sansam.stock.domain.DailyStockUsage;
import org.example.sansam.stock.repository.DailyStockUsageRepository;
import org.example.sansam.stock.repository.StockRepository;
import org.example.sansam.stock.service.StockUsageSyncService.PendingUsage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JpaStockUsageWriter implements StockUsageWriter {

    private final StockRepository stocks;
    private final DailyStockUsageRepository usages;

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED
    )
    public void applyOnce(long detailId, PendingUsage usage) {
        if (usage.batchDate() == null || usage.quantity() <= 0) {
            throw new IllegalArgumentException("배치 날짜와 양수 수량이 필요합니다.");
        }

        // 같은 옵션을 두 배치가 동시에 처리하면 재고 행에서 순서대로 진행한다.
        stocks.findForUsageSync(detailId)
                .orElseThrow(() ->
                        new IllegalStateException("재고 행 없음. detailId=" + detailId));

        var previous = usages.findByProductDetailsIdAndUsageDate(
                detailId, usage.batchDate()
        );

        if (previous.isPresent()) {
            // 동일한 이력이면 DB 커밋까지 끝난 건이므로 재차감하지 않는다.
            if (previous.get().getUsedQuantity() != usage.quantity()) {
                throw new IllegalStateException(
                        "배치 이력 수량 불일치. detailId=" + detailId
                                + ", batchDate=" + usage.batchDate()
                );
            }
            return;
        }

        // 기존 DB 조건부 UPDATE를 사용한다. 정확히 한 행이 차감돼야 한다.
        if (stocks.decreaseIfEnough(detailId, usage.quantity()) != 1) {
            throw new IllegalStateException("재고 차감 실패. detailId=" + detailId);
        }

        // 이력 저장에 실패하면 위 재고 차감도 함께 롤백된다.
        usages.save(new DailyStockUsage(
                detailId, usage.batchDate(), usage.quantity()
        ));
    }
}
