package org.example.sansam.order.domain.pricing;

import org.example.sansam.global.domain.Money;
import org.example.sansam.order.domain.OrderProduct;

import java.util.List;

public interface PricingPolicy {
    Money totalOf(List<OrderProduct> products);
}
