package ru.yandex.market.logistics.management.queue.processor;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.facade.LogisticsPointFacade;
import ru.yandex.market.logistics.management.queue.model.PickupPointSyncPayload;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Сервис обработки очереди ПВЗ синхронизации")
@DatabaseSetup(
    value = "/data/queue/processor/pickup_point_sync_processing_service_setup.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class PickupPointSyncProcessingServiceTest extends AbstractContextualTest {

    private static final Instant SOME_TIME = Instant.ofEpochSecond(762912000); // 1994-03-06 00:00

    @Autowired
    private LogisticsPointFacade logisticsPointFacade;

    @Autowired
    private TestableClock clock;

    @Autowired
    private PickupPointSyncProcessingService pickupPointSyncProcessingService;

    @Mock
    private PickupPointSyncPayload pickupPointSyncPayload;

    @BeforeEach
    void setup() {
        clock.setFixed(SOME_TIME, ZoneId.of("UTC"));
        when(pickupPointSyncPayload.getExternalHashReset()).thenReturn(false);
    }

    @AfterEach
    void teardown() {
        verifyNoMoreInteractions(logisticsPointFacade);
    }

    @Test
    @DisplayName("Кидает когда метода нету в таблице синхронизации")
    @ExpectedDatabase(
        value = "/data/queue/processor/pickup_point_sync_processing_service_nothing_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processPayload_throwsWhenNoSyncEntityIsPresent() {
        when(pickupPointSyncPayload.getSettingsMethodSyncId()).thenReturn(1L);

        assertThrows(
            RuntimeException.class,
            () -> pickupPointSyncProcessingService.processPayload(pickupPointSyncPayload),
            "Failed to find method sync entity with ID 1"
        );

        verifyNoInteractions(logisticsPointFacade);
    }

    @Test
    @DisplayName("Синхронизирует ПВЗ и соответственно обновляет существующую сущность")
    @ExpectedDatabase(
        value = "/data/queue/processor/pickup_point_sync_processing_service_sync_succeeded.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processPayload_updatesExistingSyncEntityAndSyncsPoints() {
        when(pickupPointSyncPayload.getSettingsMethodSyncId()).thenReturn(3L);

        pickupPointSyncProcessingService.processPayload(pickupPointSyncPayload);

        verify(logisticsPointFacade).syncPickupPoints(3L, false);
    }

    @SneakyThrows
    @Test
    @DisplayName("Кидает когда синхронизация ПВЗ кидает и соответственно обновляет существующую сущность")
    @ExpectedDatabase(
        value = "/data/queue/processor/pickup_point_sync_processing_service_sync_failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processPayload_updatesSyncEntityAndThrowsWhenSyncPickupPointsThrows() {
        final String exceptionMessage = "top exception";
        final String nestedExceptionMessage = "nested exception";

        when(pickupPointSyncPayload.getSettingsMethodSyncId()).thenReturn(3L);
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), any()))
            .thenThrow(new RuntimeException(exceptionMessage, new RuntimeException(nestedExceptionMessage)));

        assertThrows(
            RuntimeException.class,
            () -> pickupPointSyncProcessingService.processPayload(pickupPointSyncPayload),
            exceptionMessage
        );

        verify(logisticsPointFacade).syncPickupPoints(3L, false);
    }
}
