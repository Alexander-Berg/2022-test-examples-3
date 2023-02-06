package ru.yandex.market.tpl.billing.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.tpl.billing.model.order.StorageUnitType;
import ru.yandex.market.tpl.common.util.EnumConverter;

class StorageUnitTypeConversionTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.StorageUnitType.class)
    void checkSegmentStatusConversion(ru.yandex.market.logistics.lom.model.enums.StorageUnitType lomStorageUnitType) {
        Assertions.assertNotNull(enumConverter.convert(lomStorageUnitType, StorageUnitType.class));
    }
}
