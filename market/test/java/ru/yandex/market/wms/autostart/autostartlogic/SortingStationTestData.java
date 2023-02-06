package ru.yandex.market.wms.autostart.autostartlogic;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStationType;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.services.SortStationServiceProvider;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettings;
import ru.yandex.market.wms.common.spring.dao.entity.SortStation;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

public interface SortingStationTestData {

    String DOOR_S_01 = "S01";
    String DOOR_S_02 = "S02";
    String DOOR_S_03 = "S03";
    String DOOR_S_04 = "S04";
    String DOOR_S_05 = "S05";

    List<SortStation> STATIONS_USABLE_FOR_AUTOSTART = CollectionsUtils.listOf(
            SortStation.builder().sortStation(DOOR_S_01).activeBatchesPerPutwall(1).build(),
            SortStation.builder().sortStation(DOOR_S_02).activeBatchesPerPutwall(1).build(),
            SortStation.builder().sortStation(DOOR_S_03).activeBatchesPerPutwall(1).build(),
            SortStation.builder().sortStation(DOOR_S_04).activeBatchesPerPutwall(1).build(),
            SortStation.builder().sortStation(DOOR_S_05).activeBatchesPerPutwall(1).build()
    );


    static Set<CandidateSortStation> usableSlotsDefault() {
        return new LinkedHashSet<>(List.of(
                CandidateSortStation.builder().name(DOOR_S_03).capacity(4).build(),
                CandidateSortStation.builder().name(DOOR_S_04).capacity(4).build())
        );
    }

    static SortStationServiceProvider stationProvider() {
        WaveSettings waveSettings = WaveSettings.builder()
                .waveType(WaveType.ALL)
                .linkedToDsType(LinkedToDsType.NO_LINK_TO_DS)
                .build();
        return new SortStationServiceProvider(waveSettings, usableSlotsDefault(), new ArrayList<>());
    }

    static Map<String, Integer> capacitiesOfAutostartStations() {
        return CollectionsUtils.mapOf(
                new Pair<>(DOOR_S_01, 4),
                new Pair<>(DOOR_S_02, 4),
                new Pair<>(DOOR_S_03, 4),
                new Pair<>(DOOR_S_04, 4),
                new Pair<>(DOOR_S_05, 4)
        );
    }

    static Map<String, CandidateSortStationType> genStationTypes(List<SortStation> stations) {
        return stations.stream()
                .collect(Collectors.toMap(
                        SortStation::getSortStation,
                        s -> CandidateSortStationType.DEFAULT
                ));
    }
}
