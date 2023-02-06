package ru.yandex.market.tpl.billing.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.tpl.billing.model.entity.enums.CourierShiftType;
import ru.yandex.market.tpl.common.util.EnumConverter;

public class CourierTariffTypeConversionTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.tarifficator.model.enums.tpl.CourierTariffOptionType.class)
    void checkSegmentStatusConversion(
        ru.yandex.market.logistics.tarifficator.model.enums.tpl.CourierTariffOptionType courierTariffOptionType
    ) {
        Assertions.assertNotNull(enumConverter.convert(courierTariffOptionType, CourierShiftType.class));
    }
}
