package org.example.sansam.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sansam.order.domain.Order;
import org.example.sansam.order.domain.OrderProduct;
import org.example.sansam.order.domain.nameformatter.KoreanOrdernameFormatter;
import org.example.sansam.order.domain.ordernumber.OrderNumberPolicy;
import org.example.sansam.order.domain.pricing.PricingPolicy;
import org.example.sansam.order.dto.OrderItemDto;
import org.example.sansam.order.dto.OrderResponse;
import org.example.sansam.order.mapper.OrderResponseMapper;
import org.example.sansam.order.repository.OrderRepository;
import org.example.sansam.product.domain.Product;
import org.example.sansam.product.service.ProductService;
import org.example.sansam.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfterConfirmOrderService {

    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final StockService stockService;
    private final OrderNumberPolicy orderNumberPolicy;
    private final PricingPolicy pricingPolicy;
    private final OrderResponseMapper orderResponseMapper;


    @Transactional
    public OrderResponse placeOrderTransaction(Preloaded pre,
                                                  List<OrderItemDto> items,
                                                  Map<Long, String> productImageUrl) {
        // 재고 차감시키기
        for (OrderItemDto it : items) {
            Long detailId = productService.getDetailId(
                    it.getProductColor(), it.getProductSize(), it.getProductId()
            );
            stockService.decreaseStock(detailId, it.getQuantity());
        }
        log.error("thread={}, virtual={}", Thread.currentThread(), Thread.currentThread().isVirtual());


        // 주문/주문상품 생성
        Order order = Order.create(pre.user(), pre.waiting(), orderNumberPolicy, LocalDateTime.now());

        for (OrderItemDto it : items) {
            Product p = pre.productMap().get(it.getProductId());
            String repUrl = productImageUrl.get(p.getId());
            OrderProduct op = OrderProduct.create(
                    p, p.getPrice(), it.getQuantity(),
                    it.getProductSize(), it.getProductColor(), repUrl, pre.opWaiting()
            );
            order.addOrderProduct(op);
        }
        log.error("thread={}, virtual={}", Thread.currentThread(), Thread.currentThread().isVirtual());
        order.addOrderName(KoreanOrdernameFormatter.INSTANCE);
        order.calcTotal(pricingPolicy);
        orderRepository.save(order);
        return orderResponseMapper.toDto(order);
    }

}
