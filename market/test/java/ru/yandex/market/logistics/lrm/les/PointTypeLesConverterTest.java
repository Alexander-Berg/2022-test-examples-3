package ru.yandex.market.logistics.lrm.les;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.les.dto.PointType;
import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.converter.EnumConverter;
import ru.yandex.market.logistics.lrm.model.entity.enums.LogisticPointType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentDestinationType;

@DisplayName("Конвертация PointType из LES")
class PointTypeLesConverterTest extends LrmTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("В ShipmentDestinationType из LRM")
    @ParameterizedTest
    @EnumSource(value = PointType.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = "OTHER")
    void toShipmentDestinationType(PointType pointType) {
        softly.assertThatCode(() -> enumConverter.convert(pointType, ShipmentDestinationType.class))
            .doesNotThrowAnyException();
    }

    @DisplayName("В LogisticPointType из LRM")
    @ParameterizedTest
    @EnumSource(value = PointType.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = "OTHER")
    void toLogisticPointType(PointType pointType) {
        softly.assertThatCode(() -> enumConverter.convert(pointType, LogisticPointType.class))
            .doesNotThrowAnyException();
    }
}
