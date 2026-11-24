package org.example.sansam.payment.policy;

import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;
import org.example.sansam.global.domain.Money;
import org.example.sansam.order.domain.Order;
import org.example.sansam.order.domain.OrderProduct;
import org.example.sansam.payment.dto.CancelProductRequest;
import org.example.sansam.status.domain.StatusEnum;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class DefaultCancellationPolicy implements CancellationPolicy {

    private static final Set<StatusEnum> BLOCKED_CANCEL_STATUSES = EnumSet.of(
            StatusEnum.ORDER_PRODUCT_PAID_AND_REVIEW_COMPLETED,
            StatusEnum.ORDER_PRODUCT_CANCELED,
            StatusEnum.ORDER_ALL_CANCELED
    );

    @Override
    public Money quoteRefund(Order order, List<CancelProductRequest> requests) {
        if (order == null) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (requests.isEmpty()) {
            throw new CustomException(ErrorCode.CANCEL_NOT_FOUND);
        }
        if (BLOCKED_CANCEL_STATUSES.contains(order.getStatus().getStatusName())) {
            throw new CustomException(ErrorCode.ORDER_NOT_CANCELABLE);
        }

        LocalDateTime createdAt = order.getCreatedAt();
        if (createdAt != null && Duration.between(createdAt, LocalDateTime.now()).toHours() > 24) {
            throw new CustomException(ErrorCode.ORDER_NOT_CANCELABLE);
        }

        Map<Long, Integer> quantityByItemId = new HashMap<>();
        for (CancelProductRequest request : requests) {
            if (request == null || request.getOrderProductId() == null
                    || request.getCancelQuantity() <= 0) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
            quantityByItemId.merge(
                    request.getOrderProductId(),
                    request.getCancelQuantity(),
                    Math::addExact
            );
        }

        Map<Long, OrderProduct> itemsById = new HashMap<>();
        for (OrderProduct item : order.getOrderProducts()) {
            itemsById.put(item.getId(), item);
        }

        Money refund = Money.ZERO;
        for (Map.Entry<Long, Integer> entry : quantityByItemId.entrySet()) {
            OrderProduct item = itemsById.get(entry.getKey());
            if (item == null) {
                throw new CustomException(ErrorCode.ORDER_PRODUCT_NOT_BELONGS_TO_ORDER);
            }
            refund = refund.plus(item.refundFor(entry.getValue()));
        }
        return refund;
    }
}
