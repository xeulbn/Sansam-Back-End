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

    @Version
    @Column(name = "version")
    private Long version;


    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "product_details_id", nullable = false)
    private Long productDetailsId;


    protected Stock() {
        //JPA only
    }

    public void decrease(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("qty must be positive");
        }
        if (this.stockQuantity < qty) {
            throw new CustomException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        this.stockQuantity -= qty;
    }


}
