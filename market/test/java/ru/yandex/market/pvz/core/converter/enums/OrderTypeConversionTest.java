package ru.yandex.market.pvz.core.converter.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.pvz.client.model.order.OrderType;
import ru.yandex.market.tpl.common.util.EnumConverter;

public class OrderTypeConversionTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ru.yandex.market.pvz.core.domain.order.model.OrderType.class)
    void checkSegmentStatusConversion(ru.yandex.market.pvz.core.domain.order.model.OrderType internalOrderType) {
        Assertions.assertNotNull(enumConverter.convert(internalOrderType, OrderType.class));
    }
}
