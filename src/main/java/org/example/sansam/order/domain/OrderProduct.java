package org.example.sansam.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;
import org.example.sansam.global.domain.Money;
import org.example.sansam.global.persistence.MoneyConverter;
import org.example.sansam.product.domain.Product;
import org.example.sansam.status.domain.Status;
import org.example.sansam.status.domain.StatusEnum;

@Entity
@Table(name = "order_product")
@Getter
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private int quantity;
    private int canceledQuantity;

    @Convert(converter = MoneyConverter.class)
    private Money orderedProductPrice;

    private String orderedProductSize;
    private String orderedProductColor;
    private String representativeURL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    protected OrderProduct() {
    }

    private OrderProduct(Product product, Money unitPrice, int quantity, String size,
                         String color, String imageUrl, Status status) {
        this.product = product;
        this.orderedProductPrice = unitPrice;
        this.quantity = quantity;
        this.orderedProductSize = size;
        this.orderedProductColor = color;
        this.representativeURL = imageUrl;
        this.status = status;
    }

    public static OrderProduct create(Product product, Long unitPriceWon, int quantity,
                                      String size, String color, String imageUrl, Status status) {
        if (unitPriceWon == null || quantity <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return new OrderProduct(product, Money.wons(unitPriceWon), quantity,
                size, color, imageUrl, status);
    }

    private int remainingQuantity() {
        return quantity - canceledQuantity;
    }

    private Money unitPrice() {
        if (orderedProductPrice == null) {
            throw new IllegalStateException("주문 시점의 상품 단가가 누락되었습니다.");
        }
        return orderedProductPrice;
    }

    public Money remainingAmount() {
        return unitPrice().times(remainingQuantity());
    }

    public Money refundFor(int requestedQuantity) {
        requireCancelableQuantity(requestedQuantity);
        return unitPrice().times(requestedQuantity);
    }

    private void requireCancelableQuantity(int requestedQuantity) {
        if (isFullyCanceled() || status.getStatusName() == StatusEnum.ORDER_PRODUCT_PAID_AND_REVIEW_COMPLETED) {
            throw new CustomException(ErrorCode.ORDER_NOT_CANCELABLE);
        }
        if (requestedQuantity <= 0) {
            throw new CustomException(ErrorCode.CANCEL_QUANTITY_MUST_MORE_THEN_ZERO);
        }
        if (requestedQuantity > remainingQuantity()) {
            throw new CustomException(ErrorCode.CANNOT_CANCEL_MORE_THAN_ORDERED_QUANTITY);
        }
    }

    public void cancel(int requestedQuantity, Status canceledStatus, Status partiallyCanceledStatus) {
        requireCancelableQuantity(requestedQuantity);
        int next = canceledQuantity + requestedQuantity;
        updateOrderProductStatus(next == quantity ? canceledStatus : partiallyCanceledStatus);
        canceledQuantity = next;
    }

    public boolean isFullyCanceled() {
        return status.getStatusName() == StatusEnum.ORDER_PRODUCT_CANCELED;
    }

    public boolean hasCancellation() {
        return isFullyCanceled() || status.getStatusName() == StatusEnum.ORDER_PRODUCT_PARTIALLY_CANCELED;
    }

    public void reviewCompletedStatusChange() {
        updateOrderProductStatus(new Status(StatusEnum.ORDER_PRODUCT_REVIEW_COMPLETED));
    }

    protected void updateOrderProductStatus(Status newStatus) {
        if (newStatus == null) {
            throw new CustomException(ErrorCode.CHECK_STATUS);
        }
        this.status = newStatus;
    }
}
