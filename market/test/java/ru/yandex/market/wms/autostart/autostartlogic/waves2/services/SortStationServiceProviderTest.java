package ru.yandex.market.wms.autostart.autostartlogic.waves2.services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.autostart.autostartlogic.service.SortingStationService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStationType;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettings;
import ru.yandex.market.wms.autostart.core.model.dto.StationType;
import ru.yandex.market.wms.autostart.model.entity.StationToCarrier;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
public class SortStationServiceProviderTest extends IntegrationTest {
    @MockBean
    @Autowired
    SortingStationService sortingStationService;

    private static final Set<CandidateSortStation> STATIONS = new LinkedHashSet<>(List.of(
            CandidateSortStation.builder().name("S1").capacity(1).type(CandidateSortStationType.DEFAULT).build(),
            CandidateSortStation.builder().name("S2").capacity(1).type(CandidateSortStationType.DEFAULT).build(),
            CandidateSortStation.builder().name("H1").capacity(1).type(CandidateSortStationType.HOBBIT).build(),
            CandidateSortStation.builder().name("H2").capacity(1).type(CandidateSortStationType.HOBBIT).build()
            ));

    @Test
    public void defaultModeWithStationLinksDefault() {
        WaveSettings waveSettings = WaveSettings.builder()
                .linkedToDsType(LinkedToDsType.NO_LINK_TO_DS)
                .waveType(WaveType.ALL)
                .build();
        List<StationToCarrier> stationToCarriers = List.of(
                StationToCarrier.builder().carrierCode("C1").stationKey("S1").type(StationType.SORT).build()
        );

        SortStationServiceProvider sortStationServiceProvider =
                new SortStationServiceProvider(waveSettings, STATIONS, stationToCarriers);

        assertEquals(Set.of("S1", "S2"), stationNames(sortStationServiceProvider.getStations("")));
    }

    @Test
    public void defaultModeWithStationLinksHobbit() {
        WaveSettings waveSettings = WaveSettings.builder()
                .linkedToDsType(LinkedToDsType.NO_LINK_TO_DS)
                .waveType(WaveType.HOBBIT)
                .build();
        List<StationToCarrier> stationToCarriers = List.of(
                StationToCarrier.builder().carrierCode("C1").stationKey("H1").type(StationType.SORT).build()
        );

        SortStationServiceProvider sortStationServiceProvider =
                new SortStationServiceProvider(waveSettings, STATIONS, stationToCarriers);

        assertEquals(Set.of("H1", "H2"), stationNames(sortStationServiceProvider.getStations("")));
    }

    @Test
    public void defaultModeWithoutStationLinksDefault() {
        WaveSettings waveSettings = WaveSettings.builder()
                .linkedToDsType(LinkedToDsType.NO_LINK_TO_DS)
                .waveType(WaveType.ALL)
                .build();

        SortStationServiceProvider sortStationServiceProvider =
                new SortStationServiceProvider(waveSettings, STATIONS, new ArrayList<>());

        assertEquals(Set.of("S1", "S2"), stationNames(sortStationServiceProvider.getStations("")));
    }

    @Test
    public void defaultModeWithoutStationLinksHobbit() {
        WaveSettings waveSettings = WaveSettings.builder()
                .linkedToDsType(LinkedToDsType.NO_LINK_TO_DS)
                .waveType(WaveType.HOBBIT)
                .build();

        SortStationServiceProvider sortStationServiceProvider =
                new SortStationServiceProvider(waveSettings, STATIONS, new ArrayList<>());

        assertEquals(Set.of("H1", "H2"), stationNames(sortStationServiceProvider.getStations("")));
    }

    @Test
    public void linkedToDsModeDefault() {
        WaveSettings waveSettings = WaveSettings.builder()
                .linkedToDsType(LinkedToDsType.TO_LINKED_DS)
                .waveType(WaveType.ALL)
                .build();
        List<StationToCarrier> stationToCarriers = List.of(
                StationToCarrier.builder().carrierCode("C1").stationKey("S1").type(StationType.SORT).build()
        );

        SortStationServiceProvider sortStationServiceProvider =
                new SortStationServiceProvider(waveSettings, STATIONS, stationToCarriers);

        assertEquals(Set.of("S1"), stationNames(sortStationServiceProvider.getStations("C1")));
    }

    @Test
    public void linkedToDsModeHobbit() {
        WaveSettings waveSettings = WaveSettings.builder()
                .linkedToDsType(LinkedToDsType.TO_LINKED_DS)
                .waveType(WaveType.HOBBIT)
                .build();
        List<StationToCarrier> stationToCarriers = List.of(
                StationToCarrier.builder().carrierCode("C1").stationKey("H1").type(StationType.SORT).build()
        );

        SortStationServiceProvider sortStationServiceProvider =
                new SortStationServiceProvider(waveSettings, STATIONS, stationToCarriers);

        assertEquals(Set.of("H1"), stationNames(sortStationServiceProvider.getStations("C1")));
    }

    @Test
    public void unlinkedToDsModeDefault() {
        WaveSettings waveSettings = WaveSettings.builder()
                .linkedToDsType(LinkedToDsType.TO_UNLINKED_DS)
                .waveType(WaveType.ALL)
                .build();
        List<StationToCarrier> stationToCarriers = List.of(
                StationToCarrier.builder().carrierCode("C1").stationKey("S1").type(StationType.SORT).build()
        );

        SortStationServiceProvider sortStationServiceProvider =
                new SortStationServiceProvider(waveSettings, STATIONS, stationToCarriers);

        assertEquals(Set.of("S2"), stationNames(sortStationServiceProvider.getStations("")));
    }

    @Test
    public void unlinkedToDsModeHobbit() {
        WaveSettings waveSettings = WaveSettings.builder()
                .linkedToDsType(LinkedToDsType.TO_UNLINKED_DS)
                .waveType(WaveType.HOBBIT)
                .build();
        List<StationToCarrier> stationToCarriers = List.of(
                StationToCarrier.builder().carrierCode("C1").stationKey("H1").type(StationType.SORT).build()
        );

        SortStationServiceProvider sortStationServiceProvider =
                new SortStationServiceProvider(waveSettings, STATIONS, stationToCarriers);

        assertEquals(Set.of("H2"), stationNames(sortStationServiceProvider.getStations("")));
    }

    private Set<String> stationNames(Set<CandidateSortStation> stations) {
        return stations.stream().map(CandidateSortStation::getName).collect(Collectors.toSet());
    }
}
