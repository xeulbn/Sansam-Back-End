package org.example.sansam.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;
import org.example.sansam.order.domain.Order;
import org.example.sansam.order.repository.OrderRepository;
import org.example.sansam.payment.adapter.TossApprovalNormalizer;
import org.example.sansam.payment.adapter.TossApprovalNormalizer.Normalized;
import org.example.sansam.payment.compensation.service.PaymentCancelOutBoxService;
import org.example.sansam.payment.dto.TossPaymentRequest;
import org.example.sansam.payment.dto.TossPaymentResponse;
import org.example.sansam.payment.util.IdempotencyKeyGenerator;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String DB_FAILED_REASON = "db-failed";

    private final OrderRepository orderRepository;
    private final PaymentApiClient paymentApiClient;
    private final TossApprovalNormalizer normalizer;
    private final AfterConfirmTransactionService afterConfirmTransactionService;
    private final PaymentCancelOutBoxService outboxService;
    private final IdempotencyKeyGenerator idemGen;

    public TossPaymentResponse confirmPayment(TossPaymentRequest request) {
        Order order = orderRepository.findByOrderNumber(request.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        order.verifyPaymentAmount(request.paymentAmount());

        Map<String, Object> response;
        try {
            response = paymentApiClient.confirmPayment(request);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.API_FAILED, e);
        }

        Normalized approved = normalizer.normalize(response, request.getPaymentKey());

        try {
            return afterConfirmTransactionService.approveInTransaction(
                    order.getOrderNumber(), approved
            );
        } catch (RuntimeException e) {
            String paymentKey = approved.paymentKey();
            long approvedAmount = approved.totalAmount();
            String idempotencyKey = idemGen.forCancel(
                    paymentKey, approvedAmount, DB_FAILED_REASON
            );

            log.error("[CONFIRM] approveInTransaction failed. orderId={}, paymentKey={}",
                    order.getOrderNumber(), paymentKey, e);

            boolean canceled = bestEffortCancel(
                    paymentKey, approvedAmount, DB_FAILED_REASON, idempotencyKey
            );
            if (!canceled) {
                outboxService.enqueue(
                        paymentKey, approvedAmount, DB_FAILED_REASON, idempotencyKey
                );
            }

            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
    }

    private boolean bestEffortCancel(String paymentKey, long amount,
                                     String reason, String idempotencyKey) {
        try {
            paymentApiClient.tossPaymentCancel(paymentKey, amount, reason, idempotencyKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
