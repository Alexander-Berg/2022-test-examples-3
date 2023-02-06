package ru.yandex.market.wms.autostart.autostartlogic;

import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.autostartlogic.service.SortingStationService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.common.spring.dao.entity.SortStation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;
import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.mapOf;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_01;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_02;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_03;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_04;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_05;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.STATIONS_USABLE_FOR_AUTOSTART;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.capacitiesOfAutostartStations;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.genStationTypes;

class SortingStationServiceTestCapacitiesOfStationsWithEnoughFreeSlots extends AutostartIntegrationTest {

    List<SortStation> stations1 = listOf(
            SortStation.builder().sortStation("S01").activeBatchesPerPutwall(1).build(),
            SortStation.builder().sortStation("S02").activeBatchesPerPutwall(1).build(),
            SortStation.builder().sortStation("TEST-TRS").activeBatchesPerPutwall(1).build(),
            SortStation.builder().sortStation("TEST-TRS1").activeBatchesPerPutwall(1).build()
    );

    List<SortStation> stations100 = listOf(
            SortStation.builder().sortStation("S01").activeBatchesPerPutwall(100).build(),
            SortStation.builder().sortStation("S02").activeBatchesPerPutwall(100).build(),
            SortStation.builder().sortStation("TEST-TRS").activeBatchesPerPutwall(100).build(),
            SortStation.builder().sortStation("TEST-TRS1").activeBatchesPerPutwall(100).build()
    );

    @Test
    void activeBatchesPerPutWall1OccupancyInSlotsEmpty() {
        // NB: order is important, checked

        assertThat(
                SortingStationService.capacitiesOfStationsWithEnoughFreeSlots(
                        STATIONS_USABLE_FOR_AUTOSTART,
                        mapOf(new Pair<>(DOOR_S_01, 2L),
                                new Pair<>(DOOR_S_02, 1L),
                                new Pair<>(DOOR_S_05, 1L)),
                        mapOf(),    // empty => means occupancy in slots is 0  => does not affect
                        capacitiesOfAutostartStations(),
                        genStationTypes(STATIONS_USABLE_FOR_AUTOSTART)
                ),
                is(equalTo(new LinkedHashSet<>(List.of(
                        station(DOOR_S_03, 4, 0, 0),   // 0 occupied slots
                        station(DOOR_S_04, 4, 0, 0),   // 0 occupied slots
                        station(DOOR_S_02, 4, 0, 1),   // 1 occupied slots
                        station(DOOR_S_05, 4, 0, 1),   // 1 occupied slots
                        station(DOOR_S_01, 4, 0, 2)    // 2 occupied slots
                ))))
        );
    }

    @Test
    void activeBatchesPerPutWallActiveBatchesPerPutWall1() {
        // NB: order is important, checked
        assertThat(
                SortingStationService.capacitiesOfStationsWithEnoughFreeSlots(
                        stations1,
                        mapOf(new Pair<>(DOOR_S_01, 1L)),
                        mapOf(new Pair<>(DOOR_S_01, 1L)),
                        mapOf(new Pair<>("TEST-TRS", 10),
                                new Pair<>("TEST-TRS1", 1),
                                new Pair<>(DOOR_S_01, 14)),
                        genStationTypes(stations1)
                ),
                is(equalTo(new LinkedHashSet<>(List.of(
                        station("TEST-TRS", 10, 0, 0),
                        station("TEST-TRS1", 1, 0, 0)
                ))))
        );
    }

    @Test
    void activeBatchesPerPutWallActiveBatchesPerPutWall100() {
        // NB: order is important, checked
        assertThat(
                SortingStationService.capacitiesOfStationsWithEnoughFreeSlots(
                        stations100,
                        mapOf(new Pair<>(DOOR_S_01, 1L)),
                        mapOf(new Pair<>(DOOR_S_01, 1L)),
                        mapOf(new Pair<>("TEST-TRS", 10),
                                new Pair<>("TEST-TRS1", 1),
                                new Pair<>(DOOR_S_01, 14)),
                        genStationTypes(stations100)
                ),
                is(equalTo(new LinkedHashSet<>(List.of(
                        station("TEST-TRS", 10, 0, 0),
                        station("TEST-TRS1", 1, 0, 0),
                        station(DOOR_S_01, 14, 1, 1)
                ))))
        );
    }

    private CandidateSortStation station(String name, int capacity, int batches, int slots) {
        return CandidateSortStation.builder()
                .name(name)
                .capacity(capacity)
                .occupancyInBatches(batches)
                .occupancyInSlots(slots)
                .build();
    }
}
