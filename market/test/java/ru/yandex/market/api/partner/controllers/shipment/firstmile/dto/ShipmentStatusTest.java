package ru.yandex.market.api.partner.controllers.shipment.firstmile.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatus;

/**
 * Тест на конвертацию статусов отгрузок из модели Несу в модель ПАПИ и обратно.
 * Статусы должны соответствовать один-к-одному!
 * Если добавляете/меняете статус в одном из этих мест, нужно поправить второе и исправить документацию для партнеров.
 */
public class ShipmentStatusTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(PartnerShipmentStatus.class)
    void testConvertFromNesuModel(PartnerShipmentStatus nesuStatus) {
        Assertions.assertDoesNotThrow(() -> ShipmentStatus.from(nesuStatus));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ShipmentStatus.class)
    void testConvertToNesuModel(ShipmentStatus status) {
        Assertions.assertDoesNotThrow(() -> ShipmentStatus.convertToNesuStatus(status));
    }
}
