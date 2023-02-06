package ru.yandex.market.wms.autostart.single;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.autostartlogic.service.AosWaveServiceImpl;
import ru.yandex.market.wms.autostart.autostartlogic.service.interfaces.WavingService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CategorizedOrder;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.services.CandidateStationService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettingsCreator;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.wavetypes.WaveFlow;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.wavetypes.WaveFlowFactory;
import ru.yandex.market.wms.autostart.model.AosWave;
import ru.yandex.market.wms.autostart.nonconveablewave.BaseNonSortAutostartLargeTest;
import ru.yandex.market.wms.common.spring.dao.entity.PickSku;
import ru.yandex.market.wms.common.spring.dao.entity.PickingOrder;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.enums.StartType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.spring.enums.WaveType.ALL;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/singles/pick_locations.xml", connection = "wmwhseConnection",
                type = REFRESH),
        @DatabaseSetup(value = "/fixtures/autostart/singles/skus.xml", connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/fixtures/autostart/singles/sku_locations.xml", connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/fixtures/autostart/singles/orders_to_batch.xml", connection = "wmwhseConnection",
                type = REFRESH),
        @DatabaseSetup(value = "/fixtures/autostart/singles/settings.xml", connection = "wmwhseConnection", type =
                REFRESH)
})
class SingleWaveTest extends BaseNonSortAutostartLargeTest {

    @Autowired
    private CandidateStationService candidateStationService;

    @Autowired
    private WaveSettingsCreator waveSettingsCreator;

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/consolidation_locations.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/settings.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/data.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/singles/after/reserve-happy-default.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void reserveDefaultWaveHappyPathTest() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        var settings = waveSettingsCreator.buildWaveSettings(ALL, LinkedToDsType.NO_LINK_TO_DS, null)
                .toBuilder()
                .maxOrdersIntoPutwall(4)
                .isWaveEnabled(true)
                .maxOrdersIntoWave(100)
                .maxItemsIntoWave(200)
                .serverTime(LocalDateTime.ofInstant(clock.instant(), clock.getZone()))
                .maxItemsIntoPutwall(200)
                .build();

        WaveFlow defaultWaveFlow = WaveFlowFactory.build(settings, Collections.emptyList());
        var stations = candidateStationService.getCandidateStations(defaultWaveFlow);
        assertions.assertThat(stations.stream().map(CandidateSortStation::getName))
                .containsExactlyInAnyOrder("S03", "S04", "S05");
        defaultWaveFlow.addCandidateStations(stations);

        Assertions.assertTrue(waveFlow.isPresent());
        defaultWaveFlow.add(waveFlow.get().getGroupOrders().stream().findFirst().get());

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(defaultWaveFlow.getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(defaultWaveFlow);
        assertThat(aosWave).isPresent();
        assertThat(aosWave.get().getWaveType()).isEqualTo(ALL);
        assertThat(aosWave.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(aosWave.get().getNotSuitableOrders().size()).isZero();
        assertThat(aosWave.get().getBatches().size()).isOne();

        List<PickingOrder> pickingOrders = aosWave.get().getBatches().get(0).getPickingOrders();
        assertThat(pickingOrders.size()).isOne();
        assertThat(pickingOrders.stream().mapToInt(a -> a.getItems().stream().mapToInt(PickSku::getQty).sum()).sum())
                .isEqualTo(4);

        reservationWaveLogic.reserve(aosWave.get());
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/consolidation_locations.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/clear-sort-station-enabled.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/settings.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/data.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/singles/after/reserve-happy-default-empty.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void reserveDefaultWaveHappyPathTestWithEmptyStation() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);

        var settings = waveSettingsCreator.buildWaveSettings(ALL, LinkedToDsType.NO_LINK_TO_DS, null)
                .toBuilder()
                .maxOrdersIntoPutwall(4)
                .isWaveEnabled(true)
                .maxOrdersIntoWave(100)
                .maxItemsIntoWave(200)
                .serverTime(LocalDateTime.ofInstant(clock.instant(), clock.getZone()))
                .maxItemsIntoPutwall(200)
                .build();

        WaveFlow defaultWaveFlow = WaveFlowFactory.build(settings, Collections.emptyList());
        assertThat(defaultWaveFlow.getWaveSettings().isHoldFreeStationsForEmptyStationWaves()).isFalse();
        assertThat(defaultWaveFlow.getWaveSettings().isNoStationForDefaultWaves()).isTrue();

        var stations = candidateStationService.getCandidateStations(defaultWaveFlow);
        assertions.assertThat(stations.stream().map(CandidateSortStation::getName))
                .containsExactlyInAnyOrder("S03", "S04", "S05");
        defaultWaveFlow.addCandidateStations(stations);

        Assertions.assertTrue(waveFlow.isPresent());
        defaultWaveFlow.add(waveFlow.get().getGroupOrders().stream().findFirst().get());

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(defaultWaveFlow.getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(defaultWaveFlow);
        assertThat(aosWave).isPresent();
        assertThat(aosWave.get().getWaveType()).isEqualTo(ALL);
        assertThat(aosWave.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(aosWave.get().getNotSuitableOrders().size()).isZero();
        assertThat(aosWave.get().getBatches().size()).isOne();

        List<PickingOrder> pickingOrders = aosWave.get().getBatches().get(0).getPickingOrders();
        assertThat(pickingOrders.size()).isOne();
        assertThat(pickingOrders.stream().mapToInt(a -> a.getItems().stream().mapToInt(PickSku::getQty).sum()).sum())
                .isEqualTo(4);

        reservationWaveLogic.reserve(aosWave.get());
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/consolidation_locations.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/clear-hold-sort-station-enabled.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/settings.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/data-2.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/singles/after/reserve-happy-default-empty-2.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void reserveDefaultWaveHappyPathTestWithEmptyStationAndHoldTest() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);

        var settings = waveSettingsCreator.buildWaveSettings(ALL, LinkedToDsType.NO_LINK_TO_DS, null)
                .toBuilder()
                .maxOrdersIntoPutwall(4)
                .isWaveEnabled(true)
                .maxOrdersIntoWave(100)
                .maxItemsIntoWave(200)
                .serverTime(LocalDateTime.ofInstant(clock.instant(), clock.getZone()))
                .maxItemsIntoPutwall(200)
                .build();
        WaveFlow defaultWaveFlow = WaveFlowFactory.build(settings, Collections.emptyList());

        assertThat(defaultWaveFlow.getWaveSettings().isHoldFreeStationsForEmptyStationWaves()).isTrue();
        assertThat(defaultWaveFlow.getWaveSettings().isNoStationForDefaultWaves()).isTrue();
        var stations = candidateStationService.getCandidateStations(defaultWaveFlow);
        assertions.assertThat(stations.stream().map(CandidateSortStation::getName))
                .containsExactlyInAnyOrder("S05");
        defaultWaveFlow.addCandidateStations(stations);

        Assertions.assertTrue(waveFlow.isPresent());
        defaultWaveFlow.add(waveFlow.get().getGroupOrders().stream().findFirst().get());

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(defaultWaveFlow.getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(defaultWaveFlow);
        assertThat(aosWave).isPresent();
        assertThat(aosWave.get().getWaveType()).isEqualTo(ALL);
        assertThat(aosWave.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(aosWave.get().getNotSuitableOrders().size()).isZero();
        assertThat(aosWave.get().getBatches().size()).isOne();

        List<PickingOrder> pickingOrders = aosWave.get().getBatches().get(0).getPickingOrders();
        assertThat(pickingOrders.size()).isOne();
        assertThat(pickingOrders.stream().mapToInt(a -> a.getItems().stream().mapToInt(PickSku::getQty).sum()).sum())
                .isEqualTo(4);

        reservationWaveLogic.reserve(aosWave.get());
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/consolidation_locations.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/settings.xml", type = REFRESH)
    @ExpectedDatabase(value = "/fixtures/autostart/singles/after/reserve-happy.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void reserveSingleWaveHappyPathTest() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        Assertions.assertTrue(waveFlow.isPresent());
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(waveFlow.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(waveFlow.get().getOrders().size()).isEqualTo(4);
        assertThat(waveFlow.get().getNotSuitableOrders().size()).isZero();
        assertThat(waveFlow.get().getOversizeOrders().size()).isOne();

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(waveFlow.get().getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(waveFlow.get());
        assertThat(aosWave).isPresent();
        assertThat(aosWave.get().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(aosWave.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(aosWave.get().getNotSuitableOrders().size()).isZero();
        assertThat(aosWave.get().getBatches().size()).isOne();

        List<PickingOrder> pickingOrders = aosWave.get().getBatches().get(0).getPickingOrders();
        assertThat(pickingOrders.size()).isOne();
        assertThat(pickingOrders.stream().mapToInt(a -> a.getItems().stream().mapToInt(PickSku::getQty).sum()).sum())
                .isEqualTo(4);

        reservationWaveLogic.reserve(aosWave.get());
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/settings.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/singles/consolidation_locations.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/singles/enable_shipmentdatetime.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/singles/after/reserve-happy.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void reserveSingleWaveShipmentDateTimeHappyPathTest() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        Assertions.assertTrue(waveFlow.isPresent());
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(waveFlow.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(waveFlow.get().getOrders().size()).isEqualTo(4);
        assertThat(waveFlow.get().getNotSuitableOrders().size()).isZero();
        assertThat(waveFlow.get().getOversizeOrders().size()).isOne();

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(waveFlow.get().getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(waveFlow.get());
        assertThat(aosWave).isPresent();
        assertThat(aosWave.get().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(aosWave.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(aosWave.get().getNotSuitableOrders().size()).isZero();
        assertThat(aosWave.get().getBatches().size()).isOne();

        List<PickingOrder> pickingOrders = aosWave.get().getBatches().get(0).getPickingOrders();
        assertThat(pickingOrders.size()).isOne();
        assertThat(pickingOrders.stream().mapToInt(a -> a.getItems().stream().mapToInt(PickSku::getQty).sum()).sum())
                .isEqualTo(4);

        reservationWaveLogic.reserve(aosWave.get());
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/consolidation_locations.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/singles/settings-oversizefilter-enabled.xml",
            type = REFRESH)
    @ExpectedDatabase(value = "/fixtures/autostart/singles/after/reserve-happy.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void reserveSingleWaveHappyPathNewOversizeOrdersFilterEnabledTest() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        Assertions.assertTrue(waveFlow.isPresent());
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(waveFlow.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(waveFlow.get().getOrders().size()).isEqualTo(4);
        assertThat(waveFlow.get().getNotSuitableOrders().size()).isZero();
        assertThat(waveFlow.get().getOversizeOrders().size()).isOne();

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(waveFlow.get().getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(waveFlow.get());
        assertThat(aosWave).isPresent();
        assertThat(aosWave.get().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(aosWave.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(aosWave.get().getNotSuitableOrders().size()).isZero();
        assertThat(aosWave.get().getBatches().size()).isOne();

        List<PickingOrder> pickingOrders = aosWave.get().getBatches().get(0).getPickingOrders();
        assertThat(pickingOrders.size()).isOne();
        assertThat(pickingOrders.stream().mapToInt(a -> a.getItems().stream().mapToInt(PickSku::getQty).sum()).sum())
                .isEqualTo(4);

        reservationWaveLogic.reserve(aosWave.get());
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/1/settings.xml", type = REFRESH)
    void minSingleOrdersSettingTest() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        Assertions.assertTrue(waveFlow.isEmpty());
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/2/settings.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/singles/consolidation_locations.xml", type = DatabaseOperation.INSERT)
    void maxSingleOrdersSettingTest() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        Assertions.assertTrue(waveFlow.isPresent());
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(waveFlow.get().getOrders()).hasSize(4);

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(waveFlow.get().getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(waveFlow.get());
        assertThat(aosWave).isPresent();
        assertThat(aosWave.get().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(aosWave.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(aosWave.get().getBatches()).hasSize(2);
        assertThat(aosWave.get().getBatches().get(0).getSubBatches()).hasSize(1);
        assertThat(aosWave.get().getBatches().get(0).getSubBatches().get(0).getOrders()).hasSize(2);
        assertThat(aosWave.get().getBatches().get(1).getSubBatches()).hasSize(1);
        assertThat(aosWave.get().getBatches().get(1).getSubBatches().get(0).getOrders()).hasSize(2);
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/3/settings.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/singles/consolidation_locations.xml", type = DatabaseOperation.INSERT)
    void singleLineLimitSettingTest() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        Assertions.assertTrue(waveFlow.isPresent());
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(waveFlow.get().getOrders().size()).isEqualTo(3);

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(waveFlow.get().getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(waveFlow.get());
        assertThat(aosWave).isPresent();
        assertThat(aosWave.get().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(aosWave.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(aosWave.get().getBatches().size()).isOne();
        assertThat(aosWave.get().getBatches().get(0).getSubBatches().size()).isOne();
        assertThat(aosWave.get().getBatches().get(0).getSubBatches().get(0).getOrders().size()).isEqualTo(3);

        List<PickingOrder> pickingOrders = aosWave.get().getBatches().get(0).getPickingOrders();
        assertThat(pickingOrders.size()).isEqualTo(2);
        assertThat(pickingOrders.stream().mapToInt(a -> a.getItems().stream()
                .mapToInt(PickSku::getQty).sum()).sum())
                .isEqualTo(3);
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/4/settings.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/singles/consolidation_locations.xml", type = DatabaseOperation.INSERT)
    void assignmentSizeForSingleWavesSettingTest() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        Assertions.assertTrue(waveFlow.isPresent());
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(waveFlow.get().getOrders().size()).isEqualTo(4);
        assertThat(waveFlow.get().getNotSuitableOrders().size()).isZero();
        assertThat(waveFlow.get().getOversizeOrders().size()).isOne();

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(waveFlow.get().getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(waveFlow.get());
        assertThat(aosWave).isPresent();
        assertThat(aosWave.get().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(aosWave.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertThat(aosWave.get().getNotSuitableOrders().size()).isZero();
        assertThat(aosWave.get().getBatches().size()).isOne();
        assertThat(aosWave.get().getBatches().get(0).getSubBatches().size()).isOne();
        assertThat(aosWave.get().getBatches().get(0).getSubBatches().get(0).getOrders().size()).isEqualTo(4);

        List<PickingOrder> pickingOrders = aosWave.get().getBatches().get(0).getPickingOrders();
        assertThat(pickingOrders.size()).isEqualTo(4);
        assertThat(pickingOrders.stream().mapToInt(a -> a.getItems().stream()
                .mapToInt(PickSku::getQty).sum()).sum())
                .isEqualTo(4);
    }


    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/0/settings.xml", type = REFRESH)
    void noAvailableConsolidationLocations() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        assertThat(waveFlow).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/sorting_stations.xml", connection = "wmwhseConnection",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/singles/settings-oversizefilter-enabled.xml",
            type = REFRESH)
    void defaultWaveWhenSingleWasNotProcessed() {
        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        assertThat(waveFlow).isPresent();
        assertThat(waveFlow.get().getOrders().size()).isEqualTo(8);

        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(ALL);
        assertThat(waveFlow.get().getStartType()).isEqualTo(StartType.DEFAULT);

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(waveFlow.get().getOrders());
        when(aosWaveService.getOrderDetails(anyList())).thenReturn(
                orderDetailDao.findOrderDetails(waveFlow.get().getOrders().stream().map(CategorizedOrder::getOrder)
                        .collect(Collectors.toList())));
        Optional<AosWave> aosWave = aosWaveService.process(waveFlow.get());
        assertThat(aosWave).isPresent();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/sorting_stations.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/singles/5/settings.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/singles/consolidation_locations.xml", type = DatabaseOperation.INSERT)
    void singleOrdersLowPriorityForDefaultWave() {
        WavingService wavingService = buildWavingService(6);
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        assertThat(waveFlow).isPresent();
        assertThat(waveFlow.get().getOrders().size()).isEqualTo(4);
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(waveFlow.get().getStartType()).isEqualTo(StartType.DEFAULT);

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(waveFlow.get().getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(waveFlow.get());
        assertThat(aosWave).isNotEmpty();
        assertThat(aosWave.get().getWaveType()).isEqualTo(WaveType.SINGLE);

        waveFlow = wavingService.getWaveFlow(null);
        assertThat(waveFlow).isPresent();
        assertThat(waveFlow.get().getOrders().stream().map(o -> o.getOrder().getOrderKey()))
                .containsExactlyInAnyOrder("000000003", "000000011", "000000012", "000000013", "000000014",
                        "000000015");
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(ALL);
        assertThat(waveFlow.get().getStartType()).isEqualTo(StartType.DEFAULT);
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/singles/sorting_stations.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/singles/5/settings.xml", type = REFRESH)
    void singleOrdersInDefaultWavesWhenNoSingleLinesCapacity() {
        WavingService wavingService = buildWavingService(4);
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        assertThat(waveFlow).isPresent();

        waveFlow = wavingService.getWaveFlow(null);
        assertThat(waveFlow).isPresent();
        assertThat(waveFlow.get().getOrders().size()).isEqualTo(10);
        assertThat(waveFlow.get().getOrders().stream().map(o -> o.getOrder().getOrderKey()))
                .containsExactlyInAnyOrder("000000001", "000000002", "000000004", "000000005",
                        "000000003", "000000011", "000000012", "000000013", "000000014",
                        "000000015");
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(ALL);
        assertThat(waveFlow.get().getStartType()).isEqualTo(StartType.DEFAULT);
    }


    @Test
    @Disabled
    @DatabaseSetup(value = "/fixtures/autostart/singles/sorting_stations.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/singles/6/settings.xml", type = REFRESH)
    void singleOrdersLowPriorityForDefaultWaveDisabled() {
        WavingService wavingService = buildWavingService(10);
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        assertThat(waveFlow).isPresent();
        assertThat(waveFlow.get().getOrders().size()).isEqualTo(5);
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(WaveType.SINGLE);
        assertThat(waveFlow.get().getStartType()).isEqualTo(StartType.DEFAULT);

        AosWaveServiceImpl aosWaveService = configureAutoStartLogic(waveFlow.get().getOrders());
        Optional<AosWave> aosWave = aosWaveService.process(waveFlow.get());
        assertThat(aosWave).isEmpty();

        waveFlow = wavingService.getWaveFlow(null);
        assertThat(waveFlow).isPresent();
        assertThat(waveFlow.get().getOrders().size()).isEqualTo(10);
        assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(ALL);
        assertThat(waveFlow.get().getStartType()).isEqualTo(StartType.DEFAULT);
    }
}
