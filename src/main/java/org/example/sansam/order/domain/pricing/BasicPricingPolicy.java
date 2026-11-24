package org.example.sansam.order.domain.pricing;

import org.example.sansam.global.domain.Money;
import org.example.sansam.order.domain.OrderProduct;

import java.util.List;

public final class BasicPricingPolicy implements PricingPolicy{

    @Override
    public Money totalOf(List<OrderProduct> products) {
        return products.stream()
                .map(OrderProduct::remainingAmount)
                .reduce(Money.ZERO, Money::plus);
    }
}
