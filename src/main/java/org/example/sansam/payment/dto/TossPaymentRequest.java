package org.example.sansam.payment.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.sansam.exception.pay.CustomException;
import org.example.sansam.exception.pay.ErrorCode;
import org.example.sansam.global.domain.Money;

@Getter
@Builder
public class TossPaymentRequest {

    private String paymentKey;
    private String orderId; // orderNumber
    private Long amount;

    public TossPaymentRequest(String paymentKey, String orderId, Long amount) {
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.amount = amount;
    }

    public Money paymentAmount() {
        if (amount == null || amount < 0) {
            throw new CustomException(ErrorCode.ORDER_AND_PAY_NOT_EQUAL);
        }
        return Money.wons(amount);
    }
}
