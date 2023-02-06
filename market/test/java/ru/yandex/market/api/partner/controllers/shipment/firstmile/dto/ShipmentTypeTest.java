package ru.yandex.market.api.partner.controllers.shipment.firstmile.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Тест на конвертацию типов отгрузок из модели Несу в модель ПАПИ.
 * Типы должны соответствовать один-к-одному!
 * Если добавляете/меняете тип в одном из этих мест, нужно поправить второе и исправить документацию для партнеров.
 */
class ShipmentTypeTest {
    @ParameterizedTest(name = "{0}")
    @EnumSource(ru.yandex.market.logistics.nesu.client.enums.ShipmentType.class)
    void testConvertFromNesuModel(ru.yandex.market.logistics.nesu.client.enums.ShipmentType nesuType) {
        Assertions.assertDoesNotThrow(() -> ShipmentType.convertShipmentType(nesuType));
    }
}
