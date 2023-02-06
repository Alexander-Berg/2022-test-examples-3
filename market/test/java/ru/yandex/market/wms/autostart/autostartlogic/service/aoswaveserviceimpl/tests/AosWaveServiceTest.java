package ru.yandex.market.wms.autostart.autostartlogic.service.aoswaveserviceimpl.tests;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.RandomOrdersGenerator;
import ru.yandex.market.wms.autostart.autostartlogic.service.AosWaveServiceImpl;
import ru.yandex.market.wms.autostart.autostartlogic.service.aoswaveserviceimpl.config.ParentTestConfiguration;
import ru.yandex.market.wms.autostart.autostartlogic.service.aoswaveserviceimpl.config.TestConfigurations;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.GroupOrders;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettings;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.wavetypes.DefaultWaveFlow;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.wavetypes.WaveFlow;
import ru.yandex.market.wms.autostart.model.AosWave;
import ru.yandex.market.wms.autostart.model.entity.StationToCarrier;
import ru.yandex.market.wms.autostart.service.OrderFlowService;
import ru.yandex.market.wms.autostart.settings.service.AutostartSettingsService;
import ru.yandex.market.wms.common.spring.dao.entity.OrderFlowType;
import ru.yandex.market.wms.common.spring.dao.entity.OrderInventoryDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.SubBatch;
import ru.yandex.market.wms.common.spring.dao.implementation.DeliveryServiceCutoffsDao;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.autostart.autostartlogic.service.aoswaveserviceimpl.config.TestData.sortStationLink;
import static ru.yandex.market.wms.autostart.autostartlogic.service.aoswaveserviceimpl.config.TestData.toCategorizedOrders;

public class AosWaveServiceTest extends ParentTestConfiguration {
    @Autowired
    private AosWaveServiceImpl aosWaveService;
    @Autowired
    private TestConfigurations.Properties properties;

    @TestConfiguration
    public static class LocalConfiguration {
        @Mock
        private DeliveryServiceCutoffsDao deliveryServiceCutoffsDao;
        @Mock
        private OrderFlowService orderFlowService;
        @Mock
        private AutostartSettingsService settings;

        @Bean
        @Primary
        public TestConfigurations.Properties properties() {
            LocalDateTime currentTime = LocalDateTime.now();
            int numberOfOrders = 8;

            RandomOrdersGenerator generator = new RandomOrdersGenerator();

            List<OrderWithDetails> owdListC1 = IntStream.range(0, numberOfOrders / 2).boxed()
                    .map(i -> generator.genOwd(i, "C1", currentTime.plusHours(2)))
                    .toList();
            List<OrderWithDetails> owdListC2 = IntStream.range(numberOfOrders / 2, numberOfOrders).boxed()
                    .map(i -> generator.genOwd(i, "C2", currentTime.plusHours(2)))
                    .toList();
            List<OrderWithDetails> owdList =
                    Stream.concat(owdListC1.stream(), owdListC2.stream()).collect(Collectors.toList());
            List<OrderInventoryDetail> orderInventoryDetails = generator.genInventoryExactly(owdList, 2, 1, 0);
            return TestConfigurations.Properties.builder()
                    .orderWithDetails(owdList)
                    .orderInventoryDetails(orderInventoryDetails)
                    .currentTime(currentTime)
                    .build();
        }

        @Bean
        @Primary
        public DeliveryServiceCutoffsDao deliveryServiceCutoffsDao() {
            MockitoAnnotations.openMocks(this);
            when(deliveryServiceCutoffsDao.getDeliveryServices()).thenReturn(Collections.emptySet());
            return deliveryServiceCutoffsDao;
        }

        @Bean
        @Primary
        public OrderFlowService orderFlowService() {
            MockitoAnnotations.openMocks(this);
            when(orderFlowService.getTypeById(anyInt())).thenReturn(OrderFlowType.builder().nonSortable(false).build());
            return orderFlowService;
        }

        @Bean
        @Primary
        public AutostartSettingsService autostartSettingsService() {
            MockitoAnnotations.openMocks(this);
            when(settings.getBatchingByPopularZonesEnabled()).thenReturn(true);
            when(settings.isNewVersionEnabled()).thenReturn(true);
            when(settings.getMaxAssignmentSizeForSingleWave()).thenReturn(20);
            return settings;
        }
    }

    @Test
    public void testNoLinkToDs() {
        WaveSettings waveSettings = waveSettings(LinkedToDsType.NO_LINK_TO_DS);
        List<GroupOrders> dscOrders = makeDscOrders(waveSettings);
        List<StationToCarrier> sortStationsLinks = List.of(
                sortStationLink("C1", "S1"),
                sortStationLink("C1", "S3")
        );
        WaveFlow waveFlow = new DefaultWaveFlow(waveSettings, sortStationsLinks);
        dscOrders.forEach(waveFlow::add);
        waveFlow.addCandidateStations(candidateStations());

        Optional<AosWave> aosWave = aosWaveService.process(waveFlow);
        assertEquals(4, aosWave.get().getBatches().size());
        Set<String> actualStations = aosWave.get().getBatches().stream()
                .flatMap(b -> b.getSubBatches().stream())
                .map(SubBatch::getSortingStation).collect(
                        Collectors.toSet());
        assertEquals(candidateStations().stream().map(CandidateSortStation::getName).collect(Collectors.toSet()),
                actualStations);
        List<String> actualOrders = aosWave.get().getBatches().stream()
                .flatMap(b -> b.getSubBatches().stream())
                .flatMap(sb -> sb.getOrders().stream())
                .map(o -> o.getOrder().getOrderKey()).toList();
        assertEquals(8, actualOrders.size());
        assertEquals(8, new HashSet<>(actualOrders).size());
    }

    @Test
    public void testToUnlinkedDs() {
        WaveSettings waveSettings = waveSettings(LinkedToDsType.TO_UNLINKED_DS);
        List<GroupOrders> dscOrders = makeDscOrders(waveSettings);
        List<StationToCarrier> sortStationsLinks = List.of(
                sortStationLink("C1", "S1"),
                sortStationLink("C1", "S3")
        );
        WaveFlow waveFlow = new DefaultWaveFlow(waveSettings, sortStationsLinks);
        dscOrders.forEach(waveFlow::add);
        waveFlow.addCandidateStations(candidateStations());

        Optional<AosWave> aosWave = aosWaveService.process(waveFlow);
        assertEquals(2, aosWave.get().getBatches().size());
        Set<String> actualStations = aosWave.get().getBatches().stream()
                .flatMap(b -> b.getSubBatches().stream())
                .map(SubBatch::getSortingStation).collect(
                        Collectors.toSet());
        assertEquals(Set.of("S2", "S4"), actualStations);
        List<OrderWithDetails> actualOrders = aosWave.get().getBatches().stream()
                .flatMap(b -> b.getSubBatches().stream())
                .flatMap(sb -> sb.getOrders().stream()).toList();
        assertEquals(4, actualOrders.size());
        Set<String> actualCarriers =
                actualOrders.stream().map(owd -> owd.getOrder().getCarrierCode()).collect(Collectors.toSet());
        assertEquals(Set.of("C2"), actualCarriers);
    }

    @Test
    public void testToLinkedDs() {
        WaveSettings waveSettings = waveSettings(LinkedToDsType.TO_LINKED_DS);
        List<GroupOrders> dscOrders = makeDscOrders(waveSettings);
        List<StationToCarrier> sortStationsLinks = List.of(
                sortStationLink("C1", "S1"),
                sortStationLink("C1", "S3")
        );
        WaveFlow waveFlow = new DefaultWaveFlow(waveSettings, sortStationsLinks);
        dscOrders.forEach(waveFlow::add);
        waveFlow.addCandidateStations(candidateStations());

        Optional<AosWave> aosWave = aosWaveService.process(waveFlow);
        assertEquals(2, aosWave.get().getBatches().size());
        Set<String> actualStations = aosWave.get().getBatches().stream()
                .flatMap(b -> b.getSubBatches().stream())
                .map(SubBatch::getSortingStation).collect(
                        Collectors.toSet());
        assertEquals(Set.of("S1", "S3"), actualStations);
        List<OrderWithDetails> actualOrders = aosWave.get().getBatches().stream()
                .flatMap(b -> b.getSubBatches().stream())
                .flatMap(sb -> sb.getOrders().stream()).toList();
        assertEquals(4, actualOrders.size());
        Set<String> actualCarriers =
                actualOrders.stream().map(owd -> owd.getOrder().getCarrierCode()).collect(Collectors.toSet());
        assertEquals(Set.of("C1"), actualCarriers);
    }

    @Test
    public void testToLinkedDsExactly() {
        WaveSettings waveSettings = waveSettings(LinkedToDsType.TO_LINKED_DS);
        List<GroupOrders> dscOrders = makeDscOrders(waveSettings);
        List<StationToCarrier> sortStationsLinks = List.of(
                sortStationLink("C1", "S1"),
                sortStationLink("C2", "S2"),
                sortStationLink("C1", "S3"),
                sortStationLink("C2", "S4")
        );
        WaveFlow waveFlow = new DefaultWaveFlow(waveSettings, sortStationsLinks);
        dscOrders.forEach(waveFlow::add);
        waveFlow.addCandidateStations(candidateStations());

        Optional<AosWave> aosWave = aosWaveService.process(waveFlow);
        assertEquals(4, aosWave.get().getBatches().size());
        Set<String> actualCarriers = aosWave.get().getBatches().stream()
                .flatMap(b -> b.getSubBatches().stream())
                .filter(sb -> Set.of("S1", "S3").contains(sb.getSortingStation()))
                .flatMap(sb -> sb.getOrders().stream())
                .map(o -> o.getOrder().getCarrierCode()).collect(Collectors.toSet());
        assertEquals(Set.of("C1"), actualCarriers);
        actualCarriers = aosWave.get().getBatches().stream()
                .flatMap(b -> b.getSubBatches().stream())
                .filter(sb -> Set.of("S2", "S4").contains(sb.getSortingStation()))
                .flatMap(sb -> sb.getOrders().stream())
                .map(o -> o.getOrder().getCarrierCode()).collect(Collectors.toSet());
        assertEquals(Set.of("C2"), actualCarriers);
        List<OrderWithDetails> actualOrders = aosWave.get().getBatches().stream()
                .flatMap(b -> b.getSubBatches().stream())
                .flatMap(sb -> sb.getOrders().stream()).toList();
        assertEquals(8, actualOrders.size());
    }

    private WaveSettings waveSettings(LinkedToDsType type) {
        return WaveSettings.builder()
                .waveType(WaveType.ALL)
                .initTime(LocalDateTime.now())
                .serverTime(LocalDateTime.now())
                .maxItemsIntoWave(20)
                .maxItemsIntoPutwall(20)
                .minOrdersIntoPutwall(1)
                .maxOrdersIntoPutwall(2)
                .maxOrdersIntoWave(10)
                .linkedToDsType(type)
                .warehouseCutoffShift(3)
                .build();
    }

    private Set<CandidateSortStation> candidateStations() {
        return Set.of(station("S1"), station("S2"), station("S3"), station("S4"));
    }

    private CandidateSortStation station(String name) {
        return CandidateSortStation.builder().name(name).capacity(1).build();
    }

    private List<GroupOrders> makeDscOrders(WaveSettings waveSettings) {
        GroupOrders groupOrders =
                new GroupOrders(toCategorizedOrders(properties.getOrderWithDetails(), WaveType.ALL), waveSettings);
        return List.of(groupOrders);
    }
}
