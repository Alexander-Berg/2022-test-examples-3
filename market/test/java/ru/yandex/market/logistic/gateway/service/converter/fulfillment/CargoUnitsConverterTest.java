package ru.yandex.market.logistic.gateway.service.converter.fulfillment;

import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.gruzin.model.CargoUnitCreateDto;
import ru.yandex.market.logistic.api.model.fulfillment.CargoUnit;
import ru.yandex.market.logistic.api.model.fulfillment.UnitCargoType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitType;
import ru.yandex.market.logistic.api.utils.DateTime;

class CargoUnitsConverterTest {
    @Test
    void convertCargoUnitListFromApiToTM() {
        Assertions.assertEquals(
            new CargoUnitCreateDto(
                "DRP0001",
                null,
                ru.yandex.market.delivery.gruzin.model.UnitType.PALLET,
                ru.yandex.market.delivery.gruzin.model.UnitCargoType.XDOCK,
                null,
                Instant.parse("2022-03-09T11:46:33.0Z"),
                null
            ),
            CargoUnitsConverter.convertCargoUnitListFromApiToTM(new CargoUnit(
                "DRP0001",
                null,
                UnitType.PALLET,
                UnitCargoType.XDOCK,
                null,
                new DateTime("2022-03-09T14:46:33+03:00"),
                null
            ))
        );
    }
}
