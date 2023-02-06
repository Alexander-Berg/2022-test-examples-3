package ru.yandex.market.logistics.nesu.converter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.converter.delivery.calculator.NumericValueOperationTypeConverter;
import ru.yandex.market.logistics.nesu.dto.enums.NumericValueOperationType;

class DeliveryCalculatorNumericValueOperationTypeConverterTest extends AbstractTest {
    private final NumericValueOperationTypeConverter converter = new NumericValueOperationTypeConverter();

    @ParameterizedTest
    @EnumSource(NumericValueOperationType.class)
    void convertValues(NumericValueOperationType operationType) {
        softly.assertThat(converter.toDeliveryCalculatorOperationType(operationType)).isNotNull();
    }
}
