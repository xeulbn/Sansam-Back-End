package org.example.sansam.stock.domain;


import jakarta.persistence.*;
import lombok.Getter;
import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;


@Entity
@Table(name = "stock")
@Getter
public class Stock {

    @Id
    @Column(name = "stock_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "product_details_id", nullable = false)
    private Long productDetailsId;


    protected Stock() {
        //JPA only
    }

    public boolean decrease(int qty) {
        if (qty <= 0) {
            throw new CustomException(ErrorCode.INVALID_STOCK_QUANTITY);
        }

        if (this.stockQuantity < qty) {
            return false; // 서비스단에서 별도 처리 (메트릭 + 예외)
        }

        this.stockQuantity -= qty;
        return true;
    }


}
