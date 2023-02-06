package ru.yandex.market.pvz.internal.controller.pi.inventory.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.inventory.model.InventoryItemType;
import ru.yandex.market.pvz.core.domain.inventory.params.InventoryItemParams;
import ru.yandex.market.pvz.core.domain.inventory.params.InventoryItemPlaceCodeParams;
import ru.yandex.market.pvz.core.domain.inventory.params.InventoryItemReportParams;
import ru.yandex.market.pvz.core.domain.inventory.params.InventoryParams;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryDtoMapperTest {

    @Mock
    private InventoryItemDtoMapper itemDtoMapper;
    @Mock
    private TestableClock clock;

    private InventoryDtoMapper inventoryDtoMapper;

    @BeforeEach
    void setup() {
        inventoryDtoMapper = new InventoryDtoMapper(itemDtoMapper, clock);
    }

    @Test
    void testOkMappingForReport() {

        String emptySymbol = "-";
        int timeOffset = 6;

        var time900 = Instant.parse("2022-07-07T09:00:00.00Z");
        var time930 = Instant.parse("2022-07-07T09:30:00.00Z");
        var time1000 = Instant.parse("2022-07-07T10:00:00.00Z");
        var time1030 = Instant.parse("2022-07-07T10:30:00.00Z");

        var placeNames = List.of("place 9:00", "place 10:30", "place 9:30");
        var itemNames = List.of("item 10:00", "item 9:00", "item 9:30");

        var place900 = InventoryItemPlaceCodeParams.builder()
                .placeCode(placeNames.get(0))
                .updatedAt(time900)
                .scanned(true)
                .build();
        var place1030 = InventoryItemPlaceCodeParams.builder()
                .placeCode(placeNames.get(1))
                .updatedAt(time1030)
                .scanned(true)
                .build();
        var place1000 = InventoryItemPlaceCodeParams.builder()
                .placeCode("unscanned place")
                .updatedAt(time1000)
                .scanned(false)
                .build();
        var place930 = InventoryItemPlaceCodeParams.builder()
                .placeCode(placeNames.get(2))
                .updatedAt(time930)
                .scanned(true)
                .build();

        var item1000 = InventoryItemParams.builder()
                .externalId(itemNames.get(0))
                .type(InventoryItemType.DROP_OFF)
                .placeCodes(Collections.emptyList())
                .updatedAt(time1000)
                .build();

        var item900 = InventoryItemParams.builder()
                .externalId(itemNames.get(1))
                .type(InventoryItemType.PICKUP_POINT)
                .placeCodes(List.of(place1030, place1000, place900))
                .updatedAt(time900)
                .build();

        var item930 = InventoryItemParams.builder()
                .externalId(itemNames.get(2))
                .type(InventoryItemType.UNKNOWN)
                .placeCodes(List.of(place930))
                .updatedAt(time930)
                .build();

        var testParams = new InventoryParams();
        testParams.setTimeOffset(timeOffset);
        testParams.setItems(List.of(item1000, item930, item900));

        var firstExpectedParam = InventoryItemReportParams.builder()
                .externalId(itemNames.get(0))
                .placeCode(emptySymbol)
                .type("Дропоф")
                .updatedAt(convertWithOffset(time1000, timeOffset))
                .build();

        var secondExpectedParam = InventoryItemReportParams.builder()
                .externalId(itemNames.get(2))
                .placeCode(placeNames.get(2))
                .type(emptySymbol)
                .updatedAt(convertWithOffset(time930, timeOffset))
                .build();

        var thirdExpectedParam = InventoryItemReportParams.builder()
                .externalId(itemNames.get(1))
                .placeCode(placeNames.get(1))
                .type("ПВЗ")
                .updatedAt(convertWithOffset(time1030, timeOffset))
                .build();

        var fourExpectedParam = InventoryItemReportParams.builder()
                .externalId(itemNames.get(1))
                .placeCode(placeNames.get(0))
                .type("ПВЗ")
                .updatedAt(convertWithOffset(time900, timeOffset))
                .build();


        var expected = List.of(firstExpectedParam,
                secondExpectedParam,
                thirdExpectedParam,
                fourExpectedParam);

        var actual = inventoryDtoMapper.mapForReport(testParams);

        assertThat(actual).isEqualTo(expected);

    }

    private OffsetDateTime convertWithOffset(Instant time, int offset) {
        return OffsetDateTime.ofInstant(time, ZoneOffset.ofHours(offset));
    }

}
