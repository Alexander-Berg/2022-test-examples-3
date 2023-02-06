package ru.yandex.market.tpl.billing.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.tpl.billing.model.entity.enums.OrderType;
import ru.yandex.market.tpl.common.util.EnumConverter;

public class OrderTypeConversionTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ru.yandex.market.pvz.client.model.order.OrderType.class)
    void checkSegmentStatusConversion(ru.yandex.market.pvz.client.model.order.OrderType orderType) {
        Assertions.assertNotNull(enumConverter.convert(orderType, OrderType.class));
    }
}
