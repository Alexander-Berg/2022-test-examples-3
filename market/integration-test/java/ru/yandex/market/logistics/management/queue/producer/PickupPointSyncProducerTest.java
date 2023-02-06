package ru.yandex.market.logistics.management.queue.producer;

import java.time.Duration;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.SettingsMethod;
import ru.yandex.market.logistics.management.queue.model.PickupPointSyncPayload;
import ru.yandex.market.logistics.management.repository.SettingsMethodRepository;
import ru.yandex.market.logistics.management.repository.SettingsMethodSyncRepository;
import ru.yandex.market.logistics.management.util.DbQueueActorChecker;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.money.common.dbqueue.settings.QueueConfig;
import ru.yandex.money.common.dbqueue.settings.QueueId;
import ru.yandex.money.common.dbqueue.settings.QueueLocation;
import ru.yandex.money.common.dbqueue.settings.QueueSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ПВЗ PickupPointSyncProducer")
@DatabaseSetup(
    value = "/data/queue/producer/pickup_point_sync_producer_setup.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class PickupPointSyncProducerTest extends AbstractContextualTest {

    private static final String EXPECTED_ACTOR_NAME_1 = "SYNC_PICKUP_POINTS_1";
    private static final String EXPECTED_ACTOR_NAME_3 = "SYNC_PICKUP_POINTS_3";
    private static final QueueLocation QUEUE_LOCATION = QueueLocation.builder()
        .withQueueId(new QueueId("QUEUE_ID"))
        .withTableName("QUEUE_TABLE_NAME")
        .build();
    private static final QueueConfig QUEUE_CONFIG = new QueueConfig(
        QUEUE_LOCATION,
        QueueSettings.builder()
            .withNoTaskTimeout(Duration.ofMillis(1))
            .withBetweenTaskTimeout(Duration.ofMillis(1))
            .build()
    );

    @Captor
    private ArgumentCaptor<PickupPointSyncPayload> payloadArgumentCaptor;

    @Mock
    private DbQueueActorChecker dbQueueActorChecker;

    @Mock
    private QueueProducer<PickupPointSyncPayload> pickupPointSyncPayloadQueueProducer;

    @Autowired
    private SettingsMethodRepository settingsMethodRepository;

    @Autowired
    private SettingsMethodSyncRepository settingsMethodSyncRepository;

    @Autowired
    private TestableClock clock;

    private PickupPointSyncProducer pickupPointSyncProducer;

    @BeforeEach
    void setup() {
        when(pickupPointSyncPayloadQueueProducer.getRequestId()).thenReturn(REQUEST_ID);
        when(pickupPointSyncPayloadQueueProducer.getQueueConfig()).thenReturn(QUEUE_CONFIG);

        pickupPointSyncProducer = new PickupPointSyncProducer(
            settingsMethodSyncRepository,
            pickupPointSyncPayloadQueueProducer,
            dbQueueActorChecker,
            clock
        );
    }

    @AfterEach
    void teardown() {
        verifyNoMoreInteractions(pickupPointSyncPayloadQueueProducer);
    }

    @Test
    @DisplayName("Создаёт dbqueue таску и создёт сущность в таблице синхронизации методов")
    @ExpectedDatabase(
        value = "/data/queue/producer/pickup_point_sync_producer_created_settings_method_sync.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void produceTask_producesTaskWhenNoTaskIsInQueueAndCreatesSyncEntity() {
        when(dbQueueActorChecker.taskExists(QUEUE_LOCATION, EXPECTED_ACTOR_NAME_1)).thenReturn(false);
        SettingsMethod settingsMethod = settingsMethodRepository.getOne(1L);

        pickupPointSyncProducer.produceTask(settingsMethod, false);

        verify(pickupPointSyncPayloadQueueProducer).getQueueConfig();
        verify(pickupPointSyncPayloadQueueProducer).getRequestId();
        verify(pickupPointSyncPayloadQueueProducer)
            .produceSingle(payloadArgumentCaptor.capture(), eq(0L), eq(EXPECTED_ACTOR_NAME_1));

        PickupPointSyncPayload actualPayload = payloadArgumentCaptor.getValue();
        assertThat(actualPayload.getSettingsMethodSyncId()).isEqualTo(1L);
        assertThat(actualPayload.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(actualPayload.getExternalHashReset()).isFalse();
    }

    @Test
    @DisplayName("Создаёт dbqueue таску и обновляет сущность в таблице синхронизации методов")
    @ExpectedDatabase(
        value = "/data/queue/producer/pickup_point_sync_producer_nothing_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void produceTask_producesTaskWhenNoTaskIsInQueueAndUpdatesSyncEntity() {
        when(dbQueueActorChecker.taskExists(QUEUE_LOCATION, EXPECTED_ACTOR_NAME_3)).thenReturn(false);
        SettingsMethod settingsMethod = settingsMethodRepository.getOne(3L);

        pickupPointSyncProducer.produceTask(settingsMethod, false);

        verify(pickupPointSyncPayloadQueueProducer).getQueueConfig();
        verify(pickupPointSyncPayloadQueueProducer).getRequestId();
        verify(pickupPointSyncPayloadQueueProducer)
            .produceSingle(payloadArgumentCaptor.capture(), eq(0L), eq(EXPECTED_ACTOR_NAME_3));

        PickupPointSyncPayload actualPayload = payloadArgumentCaptor.getValue();
        assertThat(actualPayload.getSettingsMethodSyncId()).isEqualTo(3L);
        assertThat(actualPayload.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(actualPayload.getExternalHashReset()).isFalse();
    }

    @Test
    @DisplayName("Не создаёт dbqueue таску если такая таска уже есть в очереди")
    @ExpectedDatabase(
        value = "/data/queue/producer/pickup_point_sync_producer_nothing_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void produceTask_doesNotProduceTaskWhenTaskIsInQueue() {
        when(dbQueueActorChecker.taskExists(QUEUE_LOCATION, EXPECTED_ACTOR_NAME_3)).thenReturn(true);
        SettingsMethod settingsMethod = settingsMethodRepository.getOne(3L);

        pickupPointSyncProducer.produceTask(settingsMethod, false);

        verify(pickupPointSyncPayloadQueueProducer).getQueueConfig();
        verify(pickupPointSyncPayloadQueueProducer, never()).produceSingle(
            any(PickupPointSyncPayload.class),
            eq(0L),
            eq(EXPECTED_ACTOR_NAME_3)
        );
    }
}
