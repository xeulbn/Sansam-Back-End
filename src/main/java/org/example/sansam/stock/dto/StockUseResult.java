package org.example.sansam.stock.dto;

import lombok.Getter;

@Getter
public class StockUseResult {
    boolean success;
    boolean duplicate;
    boolean soldOut;
    int usedCount;


    private StockUseResult(boolean success, boolean duplicate, boolean soldOut, int usedCount) {
        this.success = success;
        this.duplicate = duplicate;
        this.soldOut = soldOut;
        this.usedCount = usedCount;
    }

    public static StockUseResult success(int usedCount) {
        return new StockUseResult(true, false, false, usedCount);
    }

    public static StockUseResult duplicate() {
        return new StockUseResult(false, true, false, 0);
    }

    public static StockUseResult soldOut() {
        return new StockUseResult(false, false, true, 0);
    }

}
