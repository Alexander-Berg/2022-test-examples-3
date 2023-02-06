package ru.yandex.market.wms.autostart.autostartlogic;

import java.time.Clock;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.autostartlogic.service.SortingStationService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.autostart.service.WaveService;
import ru.yandex.market.wms.autostart.settings.repository.SettingsHistoryDao;
import ru.yandex.market.wms.autostart.settings.service.AutostartSettingsService;
import ru.yandex.market.wms.common.lgw.util.CollectionUtil;
import ru.yandex.market.wms.common.spring.dao.implementation.BatchDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.SortingStationDao;
import ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_01;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_02;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_03;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_04;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.DOOR_S_05;
import static ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData.STATIONS_USABLE_FOR_AUTOSTART;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/2/order_flow_types.xml", connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/fixtures/autostart/2/sorting_stations.xml", connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/fixtures/autostart/2/batches_assigned_to_sorting_stations.xml", connection =
                "wmwhseConnection"),
        @DatabaseSetup(value = "/fixtures/autostart/2/orders_assigned_to_sorting_stations.xml", connection =
                "wmwhseConnection", type = DatabaseOperation.REFRESH),
})
@ContextConfiguration(classes = {
        SortingStationServiceTest.Configuration.class,
})
class SortingStationServiceTest extends AutostartIntegrationTest {

    @Autowired
    protected SortingStationService sortingStationService;

    @Test
    void findSortingStationOccupancy() {
        assertThat(
                sortingStationService.findSortingStationOccupancy(),
                is(equalTo(CollectionUtil.mapOf(
                        DOOR_S_01, 2L,
                        DOOR_S_02, 1L,
                        DOOR_S_05, 1L
                )))
        );
    }

    @Test
    void unsortedBatchCounts() {
        assertThat(
                SortingStationService.unsortedBatchCounts(
                        Arrays.asList(
                                BatchDao.UnsortedBatchSummary.builder().door("S01")
                                        .orderCount(29).unsortedOrderCount(1).build(),
                                BatchDao.UnsortedBatchSummary.builder().door("S01")
                                        .orderCount(12).unsortedOrderCount(12).build(),
                                BatchDao.UnsortedBatchSummary.builder().door("S02")
                                        .orderCount(12).unsortedOrderCount(1).build()
                        ),
                        0.3f
                ),
                is(equalTo(CollectionUtil.mapOf(
                        DOOR_S_01, 1L
                )))
        );
    }

    @Test
    void capacitiesOfStationsWithEnoughFreeSlots() {
        assertThat(
                sortingStationService.capacitiesOfStationsWithEnoughFreeSlots(
                        STATIONS_USABLE_FOR_AUTOSTART,
                        1, AutoStartSortingStationMode.ORDERS
                ),
                is(equalTo(new LinkedHashSet<>(List.of(
                    station(DOOR_S_03, 4, 0, 0),
                    station(DOOR_S_04, 4, 0, 0),
                    station(DOOR_S_02, 4, 0, 1),
                    station(DOOR_S_05, 4, 0, 1),
                    station(DOOR_S_01, 4, 0, 2)
                ))))
        );
    }

    @Test
    void rotateTo() {
        assertThat(
                SortingStationService.rotatedTo("C", Arrays.asList("A", "B", "C", "D")),
                is(equalTo(Arrays.asList("C", "D", "A", "B")))
        );
        assertThat(
                SortingStationService.rotatedTo("Z", Arrays.asList("A", "B", "C", "D")),
                is(equalTo(Arrays.asList("A", "B", "C", "D")))
        );
    }

    @Test
    void nextAfter() {
        assertThat(
                SortingStationService.nextAfter("C", Arrays.asList("A", "B", "C", "D")),
                is(equalTo(Optional.of("D")))
        );
        assertThat(
                SortingStationService.nextAfter("D", Arrays.asList("A", "B", "C", "D")),
                is(equalTo(Optional.of("A")))
        );
        assertThat(
                SortingStationService.nextAfter("Z", Arrays.asList("A", "B", "C", "D")),
                is(equalTo(Optional.empty()))
        );
    }

    @TestConfiguration
    public static class Configuration {

        @Bean
        @Primary
        @SuppressWarnings("checkstyle:ParameterNumber")
        public SortingStationService sortingStationService(
                SortingStationDao sortingStationDao,
                OrderDao orderDao,
                BatchDao batchDao,
                AutostartSettingsService autostartSettingsService,
                SettingsHistoryDao settingsHistoryDao,
                SecurityDataProvider securityDataProvider,
                Clock clock,
                WaveService waveService
                ) {
            return new SortingStationService(
                    sortingStationDao,
                    orderDao,
                    batchDao,
                    waveService,
                    autostartSettingsService,
                    settingsHistoryDao,
                    securityDataProvider,
                    clock
            );
        }
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
