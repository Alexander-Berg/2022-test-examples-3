package ru.yandex.market.logistics.management.queue.producer;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.queue.model.BuildWarehouseSegmentsPayload;
import ru.yandex.market.logistics.management.repository.LogisticsPointSegmentRepository;
import ru.yandex.market.logistics.management.util.DbQueueActorChecker;
import ru.yandex.money.common.dbqueue.settings.QueueConfig;
import ru.yandex.money.common.dbqueue.settings.QueueId;
import ru.yandex.money.common.dbqueue.settings.QueueLocation;
import ru.yandex.money.common.dbqueue.settings.QueueSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@DisplayName("BuildWarehouse Queue Test")
public class BuildWarehouseSegmentsProducerTest extends AbstractContextualAspectValidationTest {

    private static final long LOGISTICS_POINT_ID = 1L;
    private static final String EXPECTED_ACTOR_NAME = "BUILD_WAREHOUSE_1";
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
    private ArgumentCaptor<BuildWarehouseSegmentsPayload> buildWarehousePayloadArgumentCaptor;

    @Mock
    private DbQueueActorChecker dbQueueActorChecker;

    @Mock
    private LogisticsPointSegmentRepository repository;

    private BuildWarehouseSegmentsProducer buildWarehouseSegmentsProducer;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        this.buildWarehouseSegmentsProducer = mock(
            BuildWarehouseSegmentsProducer.class,
            withSettings().useConstructor(dbQueueActorChecker, repository, featureProperties)
        );

        when(buildWarehouseSegmentsProducer.getRequestId()).thenReturn(REQUEST_ID);
        when(buildWarehouseSegmentsProducer.getQueueConfig()).thenReturn(QUEUE_CONFIG);
        doCallRealMethod().when(buildWarehouseSegmentsProducer).produceTask(anyLong());
    }

    @Test
    @DisplayName("Тестирование успешной отправки сообщения в очередь")
    void testSuccessfulSending() {
        // Action
        transactionTemplate.execute(status -> {
            buildWarehouseSegmentsProducer.produceTask(LOGISTICS_POINT_ID);
            return null;
        });

        // Assertion
        verify(buildWarehouseSegmentsProducer).getRequestId();
        verify(buildWarehouseSegmentsProducer).produceSingle(
            buildWarehousePayloadArgumentCaptor.capture(),
            eq(1L),
            eq(EXPECTED_ACTOR_NAME)
        );
        verifyNoMoreInteractions(dbQueueActorChecker);

        BuildWarehouseSegmentsPayload payload = buildWarehousePayloadArgumentCaptor.getValue();
        assertThat(payload.getLogisticsPointId()).isEqualTo(LOGISTICS_POINT_ID);
        assertThat(payload.getRequestId()).isEqualTo(REQUEST_ID);
    }

    @Test
    @DisplayName("Тестирование успешной отправки сообщения в очередь с включенным флагом дедупликации")
    void testSuccessfulSendingWithEnabledDeduplication() {
        featureProperties.setDeduplicateBuildWarehouseTasks(true);
        // Setup
        when(dbQueueActorChecker.taskExists(QUEUE_LOCATION, EXPECTED_ACTOR_NAME)).thenReturn(false);

        // Action
        transactionTemplate.execute(status -> {
            buildWarehouseSegmentsProducer.produceTask(LOGISTICS_POINT_ID);
            return null;
        });

        // Assertion
        verify(buildWarehouseSegmentsProducer).getQueueConfig();
        verify(buildWarehouseSegmentsProducer).getRequestId();
        verify(buildWarehouseSegmentsProducer).produceSingle(
            buildWarehousePayloadArgumentCaptor.capture(),
            eq(1L),
            eq(EXPECTED_ACTOR_NAME)
        );
        verify(dbQueueActorChecker).taskExists(eq(QUEUE_LOCATION), eq(EXPECTED_ACTOR_NAME));

        BuildWarehouseSegmentsPayload payload = buildWarehousePayloadArgumentCaptor.getValue();
        assertThat(payload.getLogisticsPointId()).isEqualTo(LOGISTICS_POINT_ID);
        assertThat(payload.getRequestId()).isEqualTo(REQUEST_ID);
        featureProperties.setDeduplicateBuildWarehouseTasks(false);
    }
}
