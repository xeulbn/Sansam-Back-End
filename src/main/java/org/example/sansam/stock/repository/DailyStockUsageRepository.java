package org.example.sansam.stock.repository;

import org.example.sansam.stock.domain.DailyStockUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyStockUsageRepository extends JpaRepository<DailyStockUsage, Long> {
    Optional<DailyStockUsage> findByProductDetailsIdAndUsageDate(Long productDetailsId, LocalDate usageDate);

}
