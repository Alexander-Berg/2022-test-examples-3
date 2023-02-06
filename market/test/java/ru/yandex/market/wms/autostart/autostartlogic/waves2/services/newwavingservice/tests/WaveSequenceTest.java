package ru.yandex.market.wms.autostart.autostartlogic.waves2.services.newwavingservice.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.wms.autostart.autostartlogic.dao.WaveLogDao;
import ru.yandex.market.wms.autostart.autostartlogic.nonsort.AosWaveTypeStartSequenceProvider;
import ru.yandex.market.wms.autostart.autostartlogic.nonsort.NonSortLocationReserver;
import ru.yandex.market.wms.autostart.autostartlogic.nonsort.NonSortService;
import ru.yandex.market.wms.autostart.autostartlogic.service.SortingStationService;
import ru.yandex.market.wms.autostart.autostartlogic.service.interfaces.IDeliveryCutOffServiceShipmentDateTime;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.AutostartWavingService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStationType;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.WaveInitialization;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.services.OrderCategorizator;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettings;
import ru.yandex.market.wms.autostart.core.model.dto.StationType;
import ru.yandex.market.wms.autostart.model.entity.StationToCarrier;
import ru.yandex.market.wms.autostart.repository.StationToCarrierRepository;
import ru.yandex.market.wms.autostart.settings.service.AutostartSettingsService;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dto.LocationBalanceDto;
import ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode;
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class WaveSequenceTest extends IntegrationTest {
    @Autowired
    private AosWaveTypeStartSequenceProvider  startSequenceProvider;
    @Autowired
    private IDeliveryCutOffServiceShipmentDateTime deliveryCutOffService;
    @Autowired
    private OrderCategorizator orderCategorizator;
    @Autowired
    private WaveInitialization waveInitialization;
    @Autowired
    private WaveLogDao waveLogDao;
    @Autowired
    private AutostartSettingsService autostartSettingsService;
    @Autowired
    private StationToCarrierRepository stationToCarrierRepository;

    @TestConfiguration
    public static class DefaultTestConfiguration {
        @Mock
        AutostartSettingsService autostartSettingsService;
        @Mock
        SortingStationService sortingStationService;
        @Mock
        StationToCarrierRepository stationToCarrierRepository;
        @Mock
        NonSortService nonSortService;

        @Bean
        @Primary
        public AutostartSettingsService autostartSettingsService() {
            MockitoAnnotations.openMocks(this);

            when(autostartSettingsService.isNonSortSingleOrdersEnabled()).thenReturn(true);
            when(autostartSettingsService.isNonSortOversizeWavesEnabled()).thenReturn(true);
            when(autostartSettingsService.isHobbitWavesEnabled()).thenReturn(true);

            when(autostartSettingsService.getMinOrdersIntoPutWall()).thenReturn(0);
            when(autostartSettingsService.getMinOrdersInSingleWave()).thenReturn(0);
            when(autostartSettingsService.getMinOrdersInOversizeWave()).thenReturn(0);

            return autostartSettingsService;
        }

        @Bean
        @Primary
        public SortingStationService sortingStationService() {
            MockitoAnnotations.openMocks(this);
            LinkedHashSet<CandidateSortStation> stations = new LinkedHashSet<>(List.of(
                    station("S1", CandidateSortStationType.DEFAULT),
                    station("S2", CandidateSortStationType.DEFAULT),
                    station("H1", CandidateSortStationType.HOBBIT),
                    station("H2", CandidateSortStationType.HOBBIT)));
            when(sortingStationService.stationsAndUsableSlots(
                    false, AutoStartSortingStationMode.ORDERS)).thenReturn(stations);
            when(sortingStationService.stationsAndUsableSlots(
                    anyBoolean(), any(AutoStartSortingStationMode.class), anyInt())).thenReturn(stations);
            return sortingStationService;
        }

        @Bean
        @Primary
        public StationToCarrierRepository stationToCarrierRepository() {
            MockitoAnnotations.openMocks(this);
            return stationToCarrierRepository;
        }

        @Bean
        @Primary
        public NonSortService nonSortService() {
            MockitoAnnotations.openMocks(this);
            var balances = LocationBalanceDto.builder().quantity(1).location("l").build();
            NonSortLocationReserver reserver = new NonSortLocationReserver(
                Map.of(ConsolidationLocationType.SINGLES, List.of(balances),
                        ConsolidationLocationType.OVERSIZE, List.of(balances)),
                new HashMap<>()
            );
            when(nonSortService.getAosReserver(
                    any(LinkedToDsType.class), anyList(), nullable(String.class), nullable(Integer.class)))
                    .thenReturn(reserver);
            when(nonSortService.getAosReserver(nullable(Integer.class))).thenReturn(reserver);
            return nonSortService;
        }

        private CandidateSortStation station(String name, CandidateSortStationType type) {
            return CandidateSortStation.builder().name(name).capacity(1).type(type).build();
        }
    }

    @Test
    public void checkWaveSequenceWithOnLinkedToDs() {
        AutostartWavingService service = new AutostartWavingService(
                startSequenceProvider,
                deliveryCutOffService,
                orderCategorizator,
                waveInitialization,
                waveLogDao
        );

        when(autostartSettingsService.isLinkDsToStationEnabled()).thenReturn(true);
        when(stationToCarrierRepository.getStationToCarrierLinks()).thenReturn(
                List.of(buildDefaultStationToCarrier(), buildHobbitStationToCarrier(),
                        buildSingleLineToCarrier(), buildOversizeLineToCarrier())
        );

        WaveSettings waveSettings;
        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.OVERSIZE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.HOBBIT, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.ALL, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.OVERSIZE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.HOBBIT, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.ALL, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());
    }

    @Test
    public void checkWaveSequenceWithOnLinkedToDsTwoBuildings() {
        AutostartWavingService service = new AutostartWavingService(
                startSequenceProvider,
                deliveryCutOffService,
                orderCategorizator,
                waveInitialization,
                waveLogDao
        );

        when(autostartSettingsService.isLinkDsToStationEnabled()).thenReturn(true);
        when(stationToCarrierRepository.getStationToCarrierLinks()).thenReturn(
                List.of(buildDefaultStationToCarrier(), buildHobbitStationToCarrier(),
                        buildSingleLineToCarrier(), buildOversizeLineToCarrier())
        );

        WaveSettings waveSettings;
        waveSettings = service.getWaveFlow(1).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(2).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(1).get().getWaveSettings();
        assertEquals(WaveType.OVERSIZE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(2).get().getWaveSettings();
        assertEquals(WaveType.OVERSIZE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(1).get().getWaveSettings();
        assertEquals(WaveType.HOBBIT, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(2).get().getWaveSettings();
        assertEquals(WaveType.HOBBIT, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(1).get().getWaveSettings();
        assertEquals(WaveType.ALL, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(2).get().getWaveSettings();
        assertEquals(WaveType.ALL, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(1).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(2).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(1).get().getWaveSettings();
        assertEquals(WaveType.OVERSIZE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(2).get().getWaveSettings();
        assertEquals(WaveType.OVERSIZE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(1).get().getWaveSettings();
        assertEquals(WaveType.HOBBIT, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(2).get().getWaveSettings();
        assertEquals(WaveType.HOBBIT, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(1).get().getWaveSettings();
        assertEquals(WaveType.ALL, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(2).get().getWaveSettings();
        assertEquals(WaveType.ALL, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_LINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(1).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(2).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.TO_UNLINKED_DS, waveSettings.getLinkedToDsType());
    }

    @Test
    public void checkWaveSequenceWithOffLinkedToDs() {
        AutostartWavingService service = new AutostartWavingService(
                startSequenceProvider,
                deliveryCutOffService,
                orderCategorizator,
                waveInitialization,
                waveLogDao
        );

        when(autostartSettingsService.isLinkDsToStationEnabled()).thenReturn(false);
        when(stationToCarrierRepository.getStationToCarrierLinks())
                .thenReturn(List.of(buildDefaultStationToCarrier(), buildHobbitStationToCarrier()));

        WaveSettings waveSettings;
        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.NO_LINK_TO_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.OVERSIZE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.NO_LINK_TO_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.HOBBIT, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.NO_LINK_TO_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.ALL, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.NO_LINK_TO_DS, waveSettings.getLinkedToDsType());

        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.NO_LINK_TO_DS, waveSettings.getLinkedToDsType());

    }

    @Test
    public void checkWaveSequenceWithOnLinkedToDsButEmptyLinkedStations() {
        AutostartWavingService service = new AutostartWavingService(
                startSequenceProvider,
                deliveryCutOffService,
                orderCategorizator,
                waveInitialization,
                waveLogDao
        );

        when(autostartSettingsService.isLinkDsToStationEnabled()).thenReturn(true);
        when(stationToCarrierRepository.getStationToCarrierLinks()).thenReturn(new ArrayList<>());

        WaveSettings waveSettings;
        waveSettings = service.getWaveFlow(null).get().getWaveSettings();
        assertEquals(WaveType.SINGLE, waveSettings.getWaveType());
        assertEquals(LinkedToDsType.NO_LINK_TO_DS, waveSettings.getLinkedToDsType());
    }

    private StationToCarrier buildDefaultStationToCarrier() {
        return StationToCarrier.builder().carrierCode("C1").stationKey("S1").type(StationType.SORT).build();
    }

    private StationToCarrier buildHobbitStationToCarrier() {
        return StationToCarrier.builder().carrierCode("C1").stationKey("H1").type(StationType.SORT).build();
    }

    private StationToCarrier buildSingleLineToCarrier() {
        return StationToCarrier.builder()
                .carrierCode("C1")
                .stationKey("S1")
                .type(StationType.CONSOLIDATION)
                .consolidationLocationType(ConsolidationLocationType.SINGLES)
                .build();
    }

    private StationToCarrier buildOversizeLineToCarrier() {
        return StationToCarrier.builder()
                .carrierCode("C1")
                .stationKey("S1")
                .type(StationType.CONSOLIDATION)
                .consolidationLocationType(ConsolidationLocationType.OVERSIZE)
                .build();
    }
}
