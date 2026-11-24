package org.example.sansam.stock.service;

public interface StockUsageWriter {
    void applyOnce(long detailId, StockUsageSyncService.PendingUsage usage);
}
