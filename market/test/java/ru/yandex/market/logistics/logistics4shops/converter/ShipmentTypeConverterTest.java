package ru.yandex.market.logistics.logistics4shops.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.logistics4shops.AbstractTest;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;

@DisplayName("Тесты конвертации типа отгрузки из LOM в B2B")
public class ShipmentTypeConverterTest extends AbstractTest {
    private final EnumConverter converter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ShipmentType.class)
    @DisplayName("Конвертация ShipmentType из LOM в B2B")
    void shipmentType(ShipmentType shipmentType) {
        softly.assertThat(converter.convert(
            shipmentType,
            ru.yandex.market.logistics.logistics4shops.event.model.ShipmentType.class
        ))
            .isNotNull();
    }
}
