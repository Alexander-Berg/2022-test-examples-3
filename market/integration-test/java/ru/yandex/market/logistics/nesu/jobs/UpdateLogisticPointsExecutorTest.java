package ru.yandex.market.logistics.nesu.jobs;

import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.UpdateLogisticPointsExecutor;
import ru.yandex.market.logistics.nesu.service.logisticpointavailability.LogisticPointAvailabilityService;
import ru.yandex.market.logistics.nesu.service.sender.SenderRegionSettingsService;
import ru.yandex.market.logistics.nesu.service.sender.SenderSettingsService;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.mockito.Mockito.doReturn;

@DisplayName("Обновление ссылок на логистические точки")
class UpdateLogisticPointsExecutorTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LogisticPointAvailabilityService logisticPointAvailabilityService;

    @Autowired
    private SenderRegionSettingsService senderRegionSettingsService;

    @Autowired
    private SenderSettingsService senderSettingsService;

    private UpdateLogisticPointsExecutor updateLogisticPointsExecutor;

    @BeforeEach
    void setup() {
        updateLogisticPointsExecutor = new UpdateLogisticPointsExecutor(
            logisticPointAvailabilityService,
            senderRegionSettingsService,
            senderSettingsService,
            lmsClient
        );
    }

    @JpaQueriesCount(10)
    @DatabaseSetup("/jobs/executors/update_logistic_points/update_logistic_points_setup.xml")
    @ExpectedDatabase(
        value = "/jobs/executors/update_logistic_points/update_logistic_points_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @Test
    void updateLogisticPoints() {
        doReturn(Map.of(
            1L, 3L,
            2L, 3L,
            4L, 5L,
            7L, 77L,
            8L, 88L
        )).when(lmsClient).getLogisticsPointChanges(Set.of(1L, 2L, 4L, 5L, 7L,  8L));
        updateLogisticPointsExecutor.doJob(null);
    }

    @JpaQueriesCount(6)
    @DatabaseSetup("/jobs/executors/update_logistic_points/update_logistic_points_setup.xml")
    @ExpectedDatabase(
        value = "/jobs/executors/update_logistic_points/update_availability_logistic_points_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @Test
    void updateAvailabilityLogisticPoints() {
        doReturn(Map.of(
            1L, 3L,
            2L, 3L,
            4L, 5L
        )).when(lmsClient).getLogisticsPointChanges(Set.of(1L, 2L, 4L, 5L, 7L, 8L));
        updateLogisticPointsExecutor.doJob(null);
    }

    @JpaQueriesCount(5)
    @DatabaseSetup("/jobs/executors/update_logistic_points/update_logistic_points_setup.xml")
    @ExpectedDatabase(
        value = "/jobs/executors/update_logistic_points/update_region_settings_logistic_points_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @Test
    void updateRegionSettingsLogisticPoints() {
        doReturn(Map.of(
            7L, 77L
        )).when(lmsClient).getLogisticsPointChanges(Set.of(1L, 2L, 4L, 5L, 7L, 8L));
        updateLogisticPointsExecutor.doJob(null);
    }

    @JpaQueriesCount(5)
    @DatabaseSetup("/jobs/executors/update_logistic_points/update_logistic_points_setup.xml")
    @ExpectedDatabase(
        value = "/jobs/executors/update_logistic_points/update_delivery_settings_logistic_points_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @Test
    void updateDeliverySettingsLogisticPoints() {
        doReturn(Map.of(
            8L, 88L
        )).when(lmsClient).getLogisticsPointChanges(Set.of(1L, 2L, 4L, 5L, 7L, 8L));
        updateLogisticPointsExecutor.doJob(null);
    }
}
