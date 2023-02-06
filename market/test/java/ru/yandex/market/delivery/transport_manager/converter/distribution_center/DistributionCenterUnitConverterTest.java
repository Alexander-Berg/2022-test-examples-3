package ru.yandex.market.delivery.transport_manager.converter.distribution_center;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.CenterType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitCargoType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialId;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.DistributionCenterStateDto;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.Box;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.Pallet;
import ru.yandex.market.delivery.transport_manager.service.distribution_center.update_scope.OutboundPointScope;
import ru.yandex.market.delivery.transport_manager.service.xdoc.converter.DistributionCenterUnitConverter;

public class DistributionCenterUnitConverterTest {

    private static final DistributionCenterUnitConverter CONVERTER = new DistributionCenterUnitConverter();
    private static final OffsetDateTime INBOUND_DATETIME = OffsetDateTime.of(2021, 4, 29, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final Long LOGISTICS_POINT_FROM = 0L;
    private static final Long TARGET_POINT_ID = 1L;
    private static final String INBOUND_ID = "inbound1";

    @Test
    void convertEmptyState() {
        Assertions.assertTrue(
            CONVERTER.collectBoxesToPalletIdsMap(
                    new OutboundPointScope(0L),
                    new DistributionCenterStateDto(Collections.emptyList(), List.of(), List.of())
                )
                .isEmpty()
        );
    }

    @Test
    void convertEmptyPallet() {
        String palletDcId = "PAL1";
        Assertions.assertTrue(
            CONVERTER.collectBoxesToPalletIdsMap(
                new OutboundPointScope(LOGISTICS_POINT_FROM),
                new DistributionCenterStateDto().setPallets(List.of(mockPallet(palletDcId, Collections.emptyList())))
            ).isEmpty()
        );
    }

    @Test
    void convertPalletsWithBoxes() {
        String[] palletDcIds = {"PAL1", "PAL2", "PAL3"};
        String[] boxDcIds = {"BOX1", "BOX2", "BOX3"};

        Assertions.assertEquals(
            CONVERTER.collectBoxesToPalletIdsMap(
                new OutboundPointScope(LOGISTICS_POINT_FROM),
                new DistributionCenterStateDto().setPallets(List.of(
                    mockPallet(palletDcIds[0], List.of(mockBox(boxDcIds[0]), mockBox(boxDcIds[1]))),
                    mockPallet(palletDcIds[1], List.of(mockBox(boxDcIds[2])))
                ))
            ),
            Map.of(
                mockTargetBox(boxDcIds[0]), palletDcIds[0],
                mockTargetBox(boxDcIds[1]), palletDcIds[0],
                mockTargetBox(boxDcIds[2]), palletDcIds[1]
            )
        );
    }

    @Test
    void getAllDcUnitIds() {
        List<String> palletDcIds = List.of("PAL1", "PAL2");
        List<String> boxDcIds = List.of("BOX1", "BOX2", "BOX3");

        Set<String> resultUnitIds = CONVERTER.getAllDcUnitIds(new DistributionCenterStateDto().setPallets(List.of(
            mockPallet(palletDcIds.get(0), List.of(mockBox(boxDcIds.get(0)), mockBox(boxDcIds.get(1)))),
            mockPallet(palletDcIds.get(1), List.of(mockBox(boxDcIds.get(2))))
        )));

        Assertions.assertTrue(resultUnitIds.containsAll(palletDcIds), "contains all pallets");
        Assertions.assertTrue(resultUnitIds.containsAll(boxDcIds), "contains all boxes");
        Assertions.assertEquals(resultUnitIds.size(), palletDcIds.size() + boxDcIds.size());
    }

    @Test
    void getAllPallets() {
        List<String> palletDcIds = List.of("PAL1", "PAL2");
        List<String> boxDcIds = List.of("BOX1", "BOX2", "BOX3");

        List<String> resultPalletIds = CONVERTER.getAllPallets(
                1L,
                new DistributionCenterStateDto().setPallets(List.of(
                    mockPallet(palletDcIds.get(0), List.of(mockBox(boxDcIds.get(0)), mockBox(boxDcIds.get(1)))),
                    mockPallet(palletDcIds.get(1), List.of(mockBox(boxDcIds.get(2))))
                )),
                new OutboundPointScope(1L)
            )
            .stream()
            .map(DistributionCenterUnit::getDcUnitId)
            .toList();

        Assertions.assertTrue(resultPalletIds.containsAll(palletDcIds), "contains all pallets");
        Assertions.assertEquals(resultPalletIds.size(), palletDcIds.size());
    }

    @Test
    void toRegisterUnit() {
        long registerId = 1L;
        Assertions.assertEquals(
            new RegisterUnit()
                .setRegisterId(registerId)
                .setType(UnitType.BOX)
                .setBarcode("abc")
                .setPartialIds(List.of(
                    new PartialId().setIdType(IdType.BOX_ID).setValue("abc")
                ))
                .setCounts(List.of(new UnitCount().setQuantity(1).setCountType(CountType.FIT))),
            CONVERTER.toRegisterUnit(
                new DistributionCenterUnit()
                    .setId(1L)
                    .setType(DistributionCenterUnitType.BOX)
                    .setInboundExternalId("ext")
                    .setDcUnitId("abc")
                    .setFrozen(false),
                registerId
            )
        );
    }

    private static DistributionCenterUnit mockTargetBox(String dcUnitId) {
        return mockTargetUnit()
            .setDcUnitId(dcUnitId)
            .setCenterType(CenterType.DISTRIBUTION_CENTER)
            .setType(DistributionCenterUnitType.BOX);
    }

    private static DistributionCenterUnit mockTargetUnit() {
        return new DistributionCenterUnit()
            .setCenterType(CenterType.DISTRIBUTION_CENTER)
            .setCargoType(DistributionCenterUnitCargoType.XDOCK)
            .setLogisticPointFromId(LOGISTICS_POINT_FROM)
            .setLogisticPointToId(TARGET_POINT_ID)
            .setInboundTime(INBOUND_DATETIME.toInstant())
            .setInboundExternalId(INBOUND_ID)
            .setFrozen(false);
    }

    private static Pallet mockPallet(
        String palletDcId,
        List<Box> boxes
    ) {
        return new Pallet(
            palletDcId,
            TARGET_POINT_ID,
            INBOUND_ID,
            INBOUND_DATETIME,
            boxes,
            null,
            null
        );
    }

    private static Box mockBox(String id) {
        return new Box(
            id,
            INBOUND_ID,
            INBOUND_DATETIME
        );
    }
}
