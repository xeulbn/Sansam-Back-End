package org.example.sansam.global.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.sansam.global.domain.Money;

@Converter
public class MoneyConverter implements AttributeConverter<Money, Long> {

    @Override
    public Long convertToDatabaseColumn(Money value) {
        return value == null ? null : value.won();
    }

    @Override
    public Money convertToEntityAttribute(Long value) {
        return value == null ? null : Money.wons(value);
    }
}
