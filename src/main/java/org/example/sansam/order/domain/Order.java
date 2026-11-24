package org.example.sansam.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;
import org.example.sansam.global.domain.Money;
import org.example.sansam.global.persistence.MoneyConverter;
import org.example.sansam.order.domain.nameformatter.OrderNameFormatter;
import org.example.sansam.order.domain.ordernumber.OrderNumberPolicy;
import org.example.sansam.order.domain.pricing.PricingPolicy;
import org.example.sansam.status.domain.Status;
import org.example.sansam.user.domain.User;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
public class Order {

    @Id
    @Column(name = "orders_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String orderName;
    private String orderNumber;

    @Convert(converter = MoneyConverter.class)
    private Money totalAmount;

    private String paymentKey;
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private final List<OrderProduct> orderProducts = new ArrayList<>();

    protected Order() {
    }

    private Order(User user, Status status, OrderNumberPolicy policy, LocalDateTime createdAt) {
        this.user = user;
        this.status = status;
        addOrderNumber(policy);
        this.createdAt = createdAt;
    }

    public static Order create(User user, Status status, OrderNumberPolicy policy,
                               LocalDateTime createdAt) {
        return new Order(user, status, policy, createdAt);
    }

    protected void addOrderNumber(OrderNumberPolicy policy) {
        this.orderNumber = policy.makeOrderNumber();
    }

    public List<OrderProduct> getOrderProducts() {
        return Collections.unmodifiableList(orderProducts);
    }

    public void addOrderProduct(OrderProduct orderProduct) {
        orderProducts.add(orderProduct);
    }

    public void addOrderName(OrderNameFormatter formatter) {
        this.orderName = buildOrderName(formatter);
    }

    private String buildOrderName(OrderNameFormatter formatter) {
        if (orderProducts.isEmpty()) {
            throw new CustomException(ErrorCode.NO_ITEM_IN_ORDER);
        }
        String firstProductName = orderProducts.get(0).getProduct().getProductName();
        return formatter.format(firstProductName, orderProducts.size() - 1);
    }

    public void calcTotal(PricingPolicy policy) {
        this.totalAmount = policy.totalOf(orderProducts);
    }

    public void verifyPaymentAmount(Money requestedAmount) {
        if (totalAmount == null || !totalAmount.equals(requestedAmount)) {
            throw new CustomException(ErrorCode.ORDER_AND_PAY_NOT_EQUAL);
        }
    }

    public void addPaymentKey(String paymentKey) {
        this.paymentKey = paymentKey;
    }

    private void changeStatus(Status status) {
        this.status = status;
    }

    public void changeStatusMarkExpired(Status orderExpired) {
        changeStatus(orderExpired);
    }

    public void changeStatusWhenCompletePayment(Status orderPaid, Status orderProductPaid) {
        if (orderProducts.isEmpty()) {
            throw new CustomException(ErrorCode.NO_ITEM_IN_ORDER);
        }

        changeStatus(orderPaid);
        for (OrderProduct item : orderProducts) {
            item.updateOrderProductStatus(orderProductPaid);
        }
    }

    public void changeStatusAfterItemCancellation(Status allCanceled, Status partialCanceled) {
        if (orderProducts == null || orderProducts.isEmpty()) {
            throw new CustomException(ErrorCode.NO_ITEM_IN_ORDER);
        }

        if (areAllItemsCanceled()) {
            changeStatus(allCanceled);
        } else if (anyItemCanceled()) {
            changeStatus(partialCanceled);
        }
    }

    private boolean areAllItemsCanceled() {
        return orderProducts.stream().allMatch(OrderProduct::isFullyCanceled);
    }

    private boolean anyItemCanceled() {
        return orderProducts.stream().anyMatch(OrderProduct::hasCancellation);
    }
}
