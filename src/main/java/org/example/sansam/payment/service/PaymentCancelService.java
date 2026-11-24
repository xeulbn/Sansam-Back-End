package org.example.sansam.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;
import org.example.sansam.order.domain.Order;
import org.example.sansam.order.repository.OrderRepository;
import org.example.sansam.payment.adapter.CancelResponseNormalize;
import org.example.sansam.payment.dto.CancelResponse;
import org.example.sansam.payment.dto.PaymentCancelRequest;
import org.example.sansam.payment.policy.CancellationPolicy;
import org.example.sansam.payment.util.IdempotencyKeyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentCancelService {

    private final PaymentApiClient paymentApiClient;
    private final AfterConfirmTransactionService afterConfirmTransactionService;
    private final OrderRepository orderRepository;
    private final CancellationPolicy cancellationPolicy;
    private final CancelResponseNormalize normalizeResponse;

    @Transactional
    public CancelResponse wantToCancel(PaymentCancelRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Order order = orderRepository.findOrderByOrderNumber(request.getOrderId());
        long refundWon = cancellationPolicy.quoteRefund(order, request.getItems()).won();

        String paymentKey = Optional.ofNullable(order.getPaymentKey())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENTS_NOT_FOUND));

        String reason = request.getCancelReason();
        String idempotencyKey = IdempotencyKeyUtil.forCancel(paymentKey, refundWon, reason);

        Map<String, Object> response = paymentApiClient.tossPaymentCancel(
                paymentKey, refundWon, reason, idempotencyKey
        );
        CancelResponseNormalize.ParsedCancel parsed =
                normalizeResponse.parseTossCancelResponse(response);

        return afterConfirmTransactionService.saveCancellation(
                order, parsed, request, idempotencyKey
        );
    }
}
