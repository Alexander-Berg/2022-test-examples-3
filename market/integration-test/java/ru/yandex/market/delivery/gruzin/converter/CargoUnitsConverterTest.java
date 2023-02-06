package ru.yandex.market.delivery.gruzin.converter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.gruzin.dto.PalletsDto;
import ru.yandex.market.delivery.gruzin.model.CargoUnitCreateDto;
import ru.yandex.market.delivery.gruzin.model.CargoUnitsCreateDto;
import ru.yandex.market.delivery.gruzin.model.UnitCargoType;
import ru.yandex.market.delivery.gruzin.model.UnitType;
import ru.yandex.market.delivery.gruzin.model.WarehouseId;
import ru.yandex.market.delivery.gruzin.model.WarehouseIdType;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.Box;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.CenterType;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.Pallet;
import ru.yandex.market.delivery.transport_manager.service.distribution_center.update_scope.DirectionScope;

import static org.assertj.core.api.Assertions.assertThat;

class CargoUnitsConverterTest extends AbstractContextualTest {

    @Autowired
    private CargoUnitsConverter converter;

    private static final OffsetDateTime INBOUND_DATETIME = OffsetDateTime.of(2021, 4, 29, 10, 0, 0, 0, ZoneOffset.UTC);
    public static final List<Pallet> PALLETS = List.of(
        new Pallet(
            "PALLET010",
            2L,
            "inbound2",
            INBOUND_DATETIME,
            List.of(
                new Box("BOX001", "inbound2", INBOUND_DATETIME),
                new Box("BOX002", "inbound2", INBOUND_DATETIME),
                new Box("BOX003", "inbound2", INBOUND_DATETIME)
            ),
            CenterType.DISTRIBUTION_CENTER,
            ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.UnitCargoType.XDOCK
        ),
        new Pallet(
            "PALLET020",
            2L,
            "inbound3",
            INBOUND_DATETIME,
            List.of(
                new Box("BOX004", "inbound3", INBOUND_DATETIME)
            ),
            CenterType.SORTING_CENTER,
            ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.UnitCargoType.INTERWAREHOUSE_FIT
        ),
        new Pallet(
            "PALLET030",
            2L,
            "inbound2",
            INBOUND_DATETIME,
            Collections.emptyList(),
            CenterType.SORTING_CENTER,
            ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.UnitCargoType
                .INTERWAREHOUSE_DEFECT
        )
    );

    private static final CargoUnitsCreateDto STATE_DTO = new CargoUnitsCreateDto()
        .setPartnerId(10L)
        .setTargetWarehouse(new WarehouseId(
            WarehouseIdType.PARTNER,
            20L
        ))
        .setUnits(List.of(
            new CargoUnitCreateDto().setId("PALLET010")
                .setUnitType(UnitType.PALLET)
                .setCreationOutboundId("inbound2")
                .setUnitCargoType(UnitCargoType.XDOCK)
                .setPlannedOutboundDate(INBOUND_DATETIME.toInstant()),
            new CargoUnitCreateDto().setId("PALLET020")
                .setUnitType(UnitType.PALLET)
                .setCreationOutboundId("inbound3")
                .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                .setPlannedOutboundDate(INBOUND_DATETIME.toInstant()),
            new CargoUnitCreateDto().setId("PALLET030")
                .setUnitType(UnitType.PALLET)
                .setCreationOutboundId("inbound2")
                .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_DEFECT)
                .setPlannedOutboundDate(INBOUND_DATETIME.toInstant()),
            new CargoUnitCreateDto()
                .setId("BOX001")
                .setParentId("PALLET010")
                .setUnitType(UnitType.BOX)
                .setCreationOutboundId("inbound2")
                .setUnitCargoType(UnitCargoType.XDOCK)
                .setPlannedOutboundDate(INBOUND_DATETIME.toInstant()),
            new CargoUnitCreateDto()
                .setId("BOX002")
                .setParentId("PALLET010")
                .setUnitType(UnitType.BOX)
                .setCreationOutboundId("inbound2")
                .setUnitCargoType(UnitCargoType.XDOCK)
                .setPlannedOutboundDate(INBOUND_DATETIME.toInstant()),
            new CargoUnitCreateDto()
                .setId("BOX003")
                .setParentId("PALLET010")
                .setUnitType(UnitType.BOX)
                .setCreationOutboundId("inbound2")
                .setUnitCargoType(UnitCargoType.XDOCK)
                .setPlannedOutboundDate(INBOUND_DATETIME.toInstant()),
            new CargoUnitCreateDto()
                .setId("BOX004")
                .setParentId("PALLET020")
                .setUnitType(UnitType.BOX)
                .setCreationOutboundId("inbound3")
                .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                .setPlannedOutboundDate(INBOUND_DATETIME.toInstant())
        ));

    @Test
    void doDcState() {
        assertThat(converter.toDcState(STATE_DTO, Map.of(
            10L, 1L,
            20L, 2L,
            30L, 3L
        )))
            .isEqualTo(new PalletsDto(
                new DirectionScope(1L, 2L),
                PALLETS
            ));
    }

    @Test
    void getBoxesByParentId() {
        assertThat(converter.getBoxesByParentId(STATE_DTO))
            .isEqualTo(Map.of(
                "PALLET010", List.of(
                    new Box("BOX001", "inbound2", INBOUND_DATETIME),
                    new Box("BOX002", "inbound2", INBOUND_DATETIME),
                    new Box("BOX003", "inbound2", INBOUND_DATETIME)
                ),
                "PALLET020", List.of(
                    new Box("BOX004", "inbound3", INBOUND_DATETIME)
                )
            ));
    }

    @Test
    void toBox() {
        assertThat(
            converter.toBox(
                new CargoUnitCreateDto()
                    .setId("BOX1")
                    .setUnitType(UnitType.BOX)
                    .setCreationOutboundId("123")
                    .setPlannedOutboundDate(INBOUND_DATETIME.toInstant())
                    .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
            )
        )
            .isEqualTo(new Box("BOX1", "123", INBOUND_DATETIME));
    }

    @Test
    void getPallets() {
        Map<String, List<Box>> boxes = Map.of(
            "PALLET010", List.of(
                new Box("BOX001", "inbound2", INBOUND_DATETIME),
                new Box("BOX002", "inbound2", INBOUND_DATETIME),
                new Box("BOX003", "inbound2", INBOUND_DATETIME)
            ),
            "PALLET020", List.of(
                new Box("BOX004", "inbound3", INBOUND_DATETIME)
            )
        );

        assertThat(
            converter.getPallets(
                STATE_DTO,
                boxes,
                2L
            )
        )
            .containsExactlyInAnyOrderElementsOf(PALLETS);
    }

    @Test
    void createPallets() {
        assertThat(
            converter.createPallets(
                STATE_DTO,
                2L
            )
        )
            .containsExactlyInAnyOrderElementsOf(PALLETS);
    }

    @Test
    void toPallet() {
        Map<String, List<Box>> boxes = Map.of(
            "PALLET_0", List.of(
                new Box("BOX1", "123", INBOUND_DATETIME),
                new Box("BOX2", "123", INBOUND_DATETIME)
            ),
            "PALLET_1", List.of(
                new Box("BOX3", "456", INBOUND_DATETIME))
        );
        Map<Long, Long> logisticsPointsByPartner = Map.of(
            10L, 1L,
            20L, 2L
        );
        assertThat(
            converter.toPallet(
                boxes,
                new CargoUnitCreateDto()
                    .setId("PALLET_0")
                    .setUnitType(UnitType.PALLET)
                    .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                    .setCreationOutboundId("123")
                    .setPlannedOutboundDate(INBOUND_DATETIME.toInstant())
                    .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT),
                1L
            )
        )
            .isEqualTo(new Pallet(
                "PALLET_0",
                1L,
                "123",
                INBOUND_DATETIME,
                List.of(
                    new Box("BOX1", "123", INBOUND_DATETIME),
                    new Box("BOX2", "123", INBOUND_DATETIME)
                ),
                CenterType.SORTING_CENTER,
                ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.UnitCargoType
                    .INTERWAREHOUSE_FIT
            ));
    }

    @Test
    void getCenterType() {
        assertThat(converter.getCenterType(new CargoUnitCreateDto().setUnitCargoType(UnitCargoType.XDOCK)))
            .isEqualTo(CenterType.DISTRIBUTION_CENTER);

        for (UnitCargoType value : UnitCargoType.values()) {
            if (value != UnitCargoType.XDOCK) {
                assertThat(converter.getCenterType(new CargoUnitCreateDto().setUnitCargoType(value)))
                    .isEqualTo(CenterType.SORTING_CENTER);
            }
        }
    }

    static Pallet pallet(
        String id,
        Long targetPointId,
        List<Box> boxes, CenterType centerType
    ) {
        return new Pallet(
            id,
            targetPointId,
            "inbound" + targetPointId,
            INBOUND_DATETIME,
            boxes,
            centerType,
            ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.UnitCargoType.XDOCK
        );
    }

    static Box box(String id, Long targetPointId) {
        return new Box(
            id,
            "inbound" + targetPointId,
            INBOUND_DATETIME
        );
    }
}
