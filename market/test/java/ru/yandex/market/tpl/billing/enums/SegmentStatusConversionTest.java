package ru.yandex.market.tpl.billing.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.tpl.billing.model.entity.enums.SegmentStatus;
import ru.yandex.market.tpl.common.util.EnumConverter;

class SegmentStatusConversionTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.SegmentStatus.class)
    void checkSegmentStatusConversion(ru.yandex.market.logistics.lom.model.enums.SegmentStatus lomSegmentStatus) {
        Assertions.assertNotNull(enumConverter.convert(lomSegmentStatus, SegmentStatus.class));
    }
}
