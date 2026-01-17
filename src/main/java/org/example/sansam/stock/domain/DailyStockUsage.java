package org.example.sansam.stock.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class DailyStockUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_details_id", nullable = false)
    private Long productDetailsId;

    @Column(name = "usage_date",nullable = false)
    private LocalDate usageDate;

    @Column(name = "used_quantity", nullable = false)
    private int usedQuantity;

    public DailyStockUsage(Long detailId, LocalDate date, int qty) {
        this.productDetailsId = detailId;
        this.usageDate = date;
        this.usedQuantity = qty;
    }

    public void add(int qty) {
        this.usedQuantity += qty;
    }
}
