package org.example.sansam.global.domain;

public record Money(long won) {
    public static final Money ZERO = new Money(0);

    public Money {
        if (won < 0) {
            throw new IllegalArgumentException("금액은 0원 이상이어야 합니다.");
        }
    }

    public static Money wons(long won) {
        return new Money(won);
    }

    public Money plus(Money other) {
        return wons(Math.addExact(won, other.won));
    }

    public Money times(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다.");
        }
        return wons(Math.multiplyExact(won, (long) quantity));
    }
}
