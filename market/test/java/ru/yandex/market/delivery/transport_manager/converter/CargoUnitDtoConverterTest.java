package ru.yandex.market.delivery.transport_manager.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.model.dto.trip.CargoUnitIdWithDirection;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:LineLength")
public class CargoUnitDtoConverterTest {

    @Test
    public void modelToDomain() {
        assertThat(
            CargoUnitDtoConverter.convert(new CargoUnitIdWithDirection()
                .setUnitId("unitId")
                .setLogisticPointFromId(1L)
                .setLogisticPointToId(2L)
            )
        )
            .isEqualTo(new ru.yandex.market.delivery.transport_manager.domain.dto.trip_included_outbounds.CargoUnitIdWithDirection()
                .setUnitId("unitId")
                .setLogisticPointFromId(1L)
                .setLogisticPointToId(2L)
            );
    }

    @Test
    public void domainToModel() {
        assertThat(
            CargoUnitDtoConverter.convert(new ru.yandex.market.delivery.transport_manager.domain.dto.trip_included_outbounds.CargoUnitIdWithDirection()
                .setUnitId("unitId")
                .setLogisticPointFromId(1L)
                .setLogisticPointToId(2L)
            )
        )
            .isEqualTo(new CargoUnitIdWithDirection()
                .setUnitId("unitId")
                .setLogisticPointFromId(1L)
                .setLogisticPointToId(2L)
            );
    }
}
