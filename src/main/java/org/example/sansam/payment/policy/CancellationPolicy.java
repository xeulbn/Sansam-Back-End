package org.example.sansam.payment.policy;

import org.example.sansam.global.domain.Money;
import org.example.sansam.order.domain.Order;
import org.example.sansam.payment.dto.CancelProductRequest;

import java.util.List;

public interface CancellationPolicy {
    Money quoteRefund(Order order, List<CancelProductRequest> requests);
}
