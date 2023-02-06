package ru.yandex.market.delivery.gruzin.converter;

import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.gruzin.model.CargoUnitDtoFilter;
import ru.yandex.market.delivery.gruzin.model.UnitCargoType;
import ru.yandex.market.delivery.gruzin.model.UnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitCargoType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitType;
import ru.yandex.market.delivery.transport_manager.domain.filter.DistributionCenterRootUnitsFilter;

class CargoUnitFilterConverterTest {
    @Test
    void convert() {
        Assertions
            .assertThat(
                CargoUnitFilterConverter.convert(
                    CargoUnitDtoFilter.builder()
                        .logisticsPointFromId(1L)
                        .logisticsPointToId(2L)
                        .cargoUnitType(UnitCargoType.XDOCK)
                        .frozen(false)
                        .inboundExternalId("inbb")
                        .unitId("UNIT")
                        .inboundTimeFrom(Instant.parse("2021-04-29T10:00:00Z"))
                        .inboundTimeTo(Instant.parse("2021-04-29T10:00:00Z"))
                        .unitType(UnitType.PALLET)
                        .build()
                )
            )
            .isEqualTo(
                DistributionCenterRootUnitsFilter.builder()
                    .logisticPointFrom(1L)
                    .logisticPointTo(2L)
                    .cargoType(DistributionCenterUnitCargoType.XDOCK)
                    .frozen(false)
                    .inboundExternalId("inbb")
                    .unitId("UNIT")
                    .inboundTimeFrom(Instant.parse("2021-04-29T10:00:00Z"))
                    .inboundTimeTo(Instant.parse("2021-04-29T10:00:00Z"))
                    .type(DistributionCenterUnitType.PALLET)
                    .build()
            );
    }
}
