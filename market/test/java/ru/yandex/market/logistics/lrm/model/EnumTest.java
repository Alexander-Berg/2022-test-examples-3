package ru.yandex.market.logistics.lrm.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.converter.EnumConverter;
import ru.yandex.market.logistics.lrm.model.entity.enums.DestinationPointType;
import ru.yandex.market.logistics.lrm.model.entity.enums.LogisticPointType;

@DisplayName("Конвертация внутренних enum-ов")
class EnumTest extends LrmTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("DestinationPointType -> LogisticPointType")
    @ParameterizedTest
    @EnumSource(DestinationPointType.class)
    void destinationPointTypeToLogisticPointType(DestinationPointType type) {
        softly.assertThat(enumConverter.convert(type, LogisticPointType.class)).isNotNull();
    }

}
