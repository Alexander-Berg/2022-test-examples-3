package ru.yandex.market.pvz.core.converter.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.les.dto.PvzReturnStatusType;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnStatus;
import ru.yandex.market.tpl.common.util.EnumConverter;

public class ReturnStatusConversionTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ReturnStatus.class)
    void checkSegmentStatusConversion(ReturnStatus returnStatus) {
        Assertions.assertNotNull(enumConverter.convert(returnStatus, PvzReturnStatusType.class));
    }
}
