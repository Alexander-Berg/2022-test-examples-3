package ru.yandex.market.delivery.transport_manager.service.xdoc.converter;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.domain.yt.wms.DcStateDto;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.DistributionCenterStateDto;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.Box;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.CenterType;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.Pallet;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.UnitCargoType;

class YtDcStateConverterTest {

    public static final YtDcStateConverter CONVERTER = new YtDcStateConverter();
    public static final long WAREHOUSE_ID = 600L;

    @Test
    void getPallets() {
        String[] palletDcIds = {"PAL1", "PAL2"};
        Long[] outboundExternalIds = {100L};
        String boxDcId = "BOX1";

        DcStateDto deletedPallet = DcStateDto.builder()
            .type(UnitType.PALLET)
            .dcUnitId("DELETED")
            .outboundExternalId(12345L)
            .outboundTime(OffsetDateTime.MIN)
            .updatedTime(OffsetDateTime.MIN)
            .deletedTime(OffsetDateTime.MIN)
            .parentId(null)
            .whId(WAREHOUSE_ID)
            .build();

        Assertions.assertEquals(
            CONVERTER.getPallets(List.of(
                createTestDcStateDto(UnitType.PALLET, palletDcIds[0], outboundExternalIds[0]),
                createTestDcStateDto(UnitType.PALLET, palletDcIds[1], null),
                createTestDcStateDto(UnitType.BOX, boxDcId, null),
                deletedPallet
            )),
            Map.of(
                createTestKey(palletDcIds[0]), createTestPallet(palletDcIds[0], outboundExternalIds[0].toString()),
                createTestKey(palletDcIds[1]), createTestPallet(palletDcIds[1], null)
            )
        );
    }

    private Pallet createTestPallet(String palletDcId, String inboundId) {
        return createTestPallet(palletDcId, inboundId, List.of());
    }

    private Pallet createTestPallet(String palletDcId, String inboundId, List<Box> of) {
        return new Pallet(
            palletDcId,
            null,
            inboundId,
            OffsetDateTime.MIN,
            of,
            CenterType.DISTRIBUTION_CENTER_WMS,
            UnitCargoType.XDOCK
        );
    }

    private DcStateDto createTestDcStateDto(
        UnitType unitType,
        String dcUnitId,
        Long outboundExternalId
    ) {
        return createTestDcStateDto(unitType, dcUnitId, outboundExternalId, null);
    }

    private DcStateDto createTestDcStateDto(
        UnitType unitType,
        String dcUnitId,
        Long outboundExternalId,
        String parentId
    ) {
        return DcStateDto.builder()
            .type(unitType)
            .dcUnitId(dcUnitId)
            .outboundExternalId(outboundExternalId)
            .outboundTime(OffsetDateTime.MIN)
            .updatedTime(OffsetDateTime.MIN)
            .parentId(parentId)
            .whId(WAREHOUSE_ID)
            .build();
    }

    @Test
    void setBoxesToPallets() {
        String[] palletDcIds = {"PAL1", "PAL2"};
        String[] boxDcIds = {"BOX1", "BOX2"};
        Long[] outboundExternalIds = {100L};

        DcStateDto deletedBox = DcStateDto.builder()
            .type(UnitType.BOX)
            .dcUnitId("DELETED_BOX")
            .outboundExternalId(23456L)
            .outboundTime(OffsetDateTime.MIN)
            .updatedTime(OffsetDateTime.MIN)
            .deletedTime(OffsetDateTime.MIN)
            .parentId("PAL1")
            .whId(WAREHOUSE_ID)
            .build();

        List<DcStateDto> ytTableData = List.of(
            createTestDcStateDto(UnitType.PALLET, palletDcIds[0], outboundExternalIds[0]),
            createTestDcStateDto(UnitType.PALLET, palletDcIds[1], null),
            createTestDcStateDto(UnitType.BOX, boxDcIds[0], outboundExternalIds[0], palletDcIds[1]),
            createTestDcStateDto(UnitType.BOX, boxDcIds[1], outboundExternalIds[0], palletDcIds[1]),
            deletedBox
        );
        Map<YtDcStateConverter.DcStateDtoKey, Pallet> palletMap = Map.of(
            createTestKey(palletDcIds[0]), createTestPallet(palletDcIds[0], outboundExternalIds[0].toString()),
            createTestKey(palletDcIds[1]), createTestPallet(palletDcIds[1], null)
        );
        CONVERTER.setBoxesToPallets(ytTableData, palletMap);
        Assertions.assertEquals(
            palletMap.get(createTestKey(palletDcIds[0])).getBoxes(),
            List.of()
        );
        Assertions.assertEquals(
            palletMap.get(createTestKey(palletDcIds[1])).getBoxes(),
            List.of(
                createTestBox(boxDcIds[0], outboundExternalIds[0].toString()),
                createTestBox(boxDcIds[1], outboundExternalIds[0].toString())
            )
        );
    }

    private YtDcStateConverter.DcStateDtoKey createTestKey(String dcUnitId) {
        return new YtDcStateConverter.DcStateDtoKey(dcUnitId, WAREHOUSE_ID);
    }

    @Test
    void setBoxesToPallets_boxesWithoutPallet() {
        String[] palletDcIds = {"PAL1", "PAL2"};
        String[] boxDcIds = {"BOX1", "BOX2"};
        Long[] outboundExternalIds = {100L};

        List<DcStateDto> ytTableData = List.of(
            createTestDcStateDto(UnitType.PALLET, palletDcIds[0], outboundExternalIds[0]),
            createTestDcStateDto(UnitType.BOX, boxDcIds[0], outboundExternalIds[0], palletDcIds[1]),
            createTestDcStateDto(UnitType.BOX, boxDcIds[1], outboundExternalIds[0], palletDcIds[1])
        );
        Map<YtDcStateConverter.DcStateDtoKey, Pallet> palletMap = Map.of(
            createTestKey(palletDcIds[0]), createTestPallet(palletDcIds[0], outboundExternalIds[0].toString())
        );

        CONVERTER.setBoxesToPallets(ytTableData, palletMap);
        Assertions.assertEquals(
            palletMap.get(createTestKey(palletDcIds[0])).getBoxes(),
            List.of()
        );
    }

    private Box createTestBox(String boxDcId, String outboundExternalId) {
        return new Box(boxDcId, outboundExternalId, OffsetDateTime.MIN);
    }

    @Test
    void convertYtState() {
        String[] palletDcIds = {"PAL1", "PAL2", "PAL3"};
        String[] boxDcIds = {"BOX1", "BOX2"};
        String[] outboundExternalIds = {"100", "200"};

        List<Pallet> allPallets = List.of(
            createTestPallet(palletDcIds[0], outboundExternalIds[0]),
            createTestPallet(palletDcIds[1], null, List.of(
                createTestBox(boxDcIds[0], outboundExternalIds[0]),
                createTestBox(boxDcIds[1], outboundExternalIds[0])
            )),
            createTestPallet(palletDcIds[2], outboundExternalIds[1])
        );
        long targetLogisticsPointId = 1000L;
        Assertions.assertEquals(
            CONVERTER.convertYtState(
                Map.of(
                    outboundExternalIds[0], targetLogisticsPointId
                ),
                allPallets
            ),
            new DistributionCenterStateDto().setPallets(allPallets)
        );
        Assertions.assertEquals(
            allPallets.stream().map(Pallet::getTargetPointId).distinct().toList(),
            Arrays.asList(targetLogisticsPointId, null)
        );
    }

    @Test
    void convertYtState_ambiguousTargetForPallet() {
        String[] palletDcIds = {"PAL1"};
        String[] boxDcIds = {"BOX1"};
        String[] outboundExternalIds = {"100", "200"};

        List<Pallet> allPallets = List.of(
            createTestPallet(palletDcIds[0], outboundExternalIds[0], List.of(
                createTestBox(boxDcIds[0], outboundExternalIds[1])
            ))
        );

        Map<String, Long> outboundExternalId = Map.of(
            outboundExternalIds[0], 1000L,
            outboundExternalIds[1], 2000L
        );
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> CONVERTER.convertYtState(
                outboundExternalId,
                allPallets
            ),
            "Ambiguous target for pallet should cause an exception"
        );
    }
}
