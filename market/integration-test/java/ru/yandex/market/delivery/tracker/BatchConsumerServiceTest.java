package ru.yandex.market.delivery.tracker;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.dao.repository.DeliveryTrackDao;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.service.logger.BatchConsumerDelayTskvLogger;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackQueueProducer;
import ru.yandex.market.delivery.tracker.service.tracking.batching.consumer.BatchConsumerService;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.MovementStatus;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.Status;
import ru.yandex.market.logistic.gateway.common.model.common.StatusCode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchConsumerServiceTest extends AbstractContextualTest {

    private static final long TRACK_ID = 1;

    @Autowired
    private BatchConsumerService batchConsumerService;

    @Autowired
    private FulfillmentClient lgwClient;

    @Autowired
    private DeliveryTrackDao deliveryTrackDao;

    @Autowired
    private PushTrackQueueProducer pushTrackQueueProducer;

    @Autowired
    private BatchConsumerDelayTskvLogger delayLogger;

    /**
     * Тест проверяет, что после успешной обработки батча он помечается как обработанный.
     */
    @Test
    @DatabaseSetup("/database/states/batches/process/before_batch_status_is_processed_after_processing.xml")
    @ExpectedDatabase(
        value = "/database/expected/batches/process/after_batch_status_is_processed_after_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testBatchStatusIsProcessedAfterProcessing() {
        batchConsumerService.processFreeSourceBatchByPriority(DeliveryService.DEFAULT_PRIORITY);
    }

    /**
     * Тест проверяет, что при неуспехе обработки батча он помечается как обработанный.
     */
    @Test
    @DatabaseSetup(
        "/database/states/batches/process/before_batch_status_is_processed_when_request_meta_was_not_present.xml"
    )
    @ExpectedDatabase(
        value = "/database/expected/batches/process" +
            "/after_batch_status_is_processed_when_request_meta_was_not_present.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testBatchStatusIsProcessedWhenRequestMetaWasNotPresent() {
        batchConsumerService.processFreeSourceBatchByPriority(DeliveryService.DEFAULT_PRIORITY);
    }

    /**
     * Тест проверяет, что батч не обрабатывается при несоответствии приоритету
     */
    @Test
    @DatabaseSetup(
        "/database/states/batches/process/before_batch_status_is_not_processed_after_processing_different_priority.xml"
    )
    @ExpectedDatabase(
        value = "/database/expected/batches/process" +
            "/after_batch_status_is_not_processed_after_processing_different_priority.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testBatchStatusIsNotProcessedAfterProcessingDifferentPriority() {
        batchConsumerService.processFreeSourceBatchByPriority(DeliveryService.DEFAULT_PRIORITY);
    }


    /**
     * Тест проверяет, что полученные данные после обработки батча сохранены.
     */
    @Test
    @DatabaseSetup("/database/states/batches/process/before_received_data_is_saved_after_batch_processing.xml")
    @ExpectedDatabase(
        value = "/database/expected/batches/process/after_received_data_is_saved_after_batch_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testReceivedDataIsSavedAfterBatchProcessing() {
        OrderStatusHistory response = createOrderStatusHistory();

        when(lgwClient.getOrderHistory(any(ResourceId.class), any(Partner.class))).thenReturn(response);

        batchConsumerService.processFreeSourceBatchByPriority(DeliveryService.DEFAULT_PRIORITY);

        DeliveryTrackMeta trackMeta = deliveryTrackDao.getDeliveryTrackMeta(TRACK_ID);
        List<DeliveryTrackCheckpoint> checkpoints = deliveryTrackDao.getDeliveryTrackCheckpoints(TRACK_ID);

        assertions().assertThat(checkpoints)
            .as("Asserting the checkpoints list size")
            .hasSize(1);
        assertions().assertThat(checkpoints.get(0).getAcquiredByTrackerDate())
            .as("Asserting the acquired by tracker timestamps are equal")
            .isEqualTo(trackMeta.getLastCheckpointAcquiredDate());
        verify(pushTrackQueueProducer).enqueue(TRACK_ID);
    }

    /**
     * Тест проверяет, что полученные данные после обработки батча сохранены.
     */
    @Test
    @DatabaseSetup("/database/states/batches/process/"
        + "before_received_data_is_saved_after_batch_processing_for_deleted_track.xml")
    @ExpectedDatabase(
        value = "/database/expected/batches/process/"
            + "after_received_data_is_saved_after_batch_processing_for_deleted_track.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testReceivedDataIsSavedAfterBatchProcessingForDeletedTrack() {
        OrderStatusHistory response = createOrderStatusHistory();

        when(lgwClient.getOrderHistory(any(ResourceId.class), any(Partner.class))).thenReturn(response);

        batchConsumerService.processFreeSourceBatchByPriority(DeliveryService.DEFAULT_PRIORITY);

        DeliveryTrackMeta trackMeta = deliveryTrackDao.getDeliveryTrackMeta(TRACK_ID);
        List<DeliveryTrackCheckpoint> checkpoints = deliveryTrackDao.getDeliveryTrackCheckpoints(TRACK_ID);

        assertions().assertThat(checkpoints)
            .as("Asserting the checkpoints list size")
            .hasSize(1);
        assertions().assertThat(checkpoints.get(0).getAcquiredByTrackerDate())
            .as("Asserting the acquired by tracker timestamps are equal")
            .isEqualTo(trackMeta.getLastCheckpointAcquiredDate());
        verify(pushTrackQueueProducer).enqueue(TRACK_ID);
    }

    @Test
    @DatabaseSetup("/database/states/batches/process/process_batch_with_track_request_type_order_status.xml")
    @ExpectedDatabase(
        value = "/database/expected/batches/process/process_batch_with_track_request_type_order_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processBatchWithTrackRequestTypeOrderStatus() {
        OrderStatusHistory response = createOrderStatusHistory();

        when(lgwClient.getOrdersStatus(anyList(), any(Partner.class))).thenReturn(List.of(response));

        batchConsumerService.processFreeSourceBatchByPriority(DeliveryService.DEFAULT_PRIORITY);
    }

    private OrderStatusHistory createOrderStatusHistory() {
        return new OrderStatusHistory(
            List.of(
                new OrderStatus(
                    OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE,
                    new DateTime("2018-12-24T12:00:00+03:00"),
                    "Order arrived to pickup point")
            ),
            ResourceId.builder()
                .setYandexId("ORDER_1")
                .setPartnerId("ORDER_1")
                .build()
        );
    }

    private MovementStatus createMovementStatus() {
        return new MovementStatus(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setYandexId("MOVEMENT_1")
                .setPartnerId("MOVEMENT_1")
                .build(),
            new Status(
                StatusCode.COURIER_FOUND,
                new ru.yandex.market.logistic.gateway.common.model.utils.DateTime("2018-12-24T12:00:00+03:00"),
                "Courier is here, bro"
            )
        );
    }
}
