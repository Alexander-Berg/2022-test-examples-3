package ru.yandex.market.tpl.billing.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.tpl.billing.model.entity.enums.PartnerType;
import ru.yandex.market.tpl.common.util.EnumConverter;

class PartnerTypeConversionTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.PartnerType.class)
    void checkSegmentStatusConversion(ru.yandex.market.logistics.lom.model.enums.PartnerType lomPartnerType) {
        Assertions.assertNotNull(enumConverter.convert(lomPartnerType, PartnerType.class));
    }
}
