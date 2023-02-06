package ru.yandex.market.logistics.lom.controller.tracker;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Тест обработки 50го чп в контексте отмены возвратов (невыкупов)")
class TrackerNotificationControllerOrderReturnTest extends AbstractTrackerNotificationControllerTest {

    @Test
    @DisplayName("Обработка чекпоинта возвратного потока от СЦ")
    @DatabaseSetup("/controller/tracker/before/order_return.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/before/order_return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processSortingCenterReturnFlowCheckpoint() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo1_order_sc_returned.json",
            "controller/tracker/response/push_102_ok.json"
        );

        assertAndExecuteTask(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_RETURNED, 2, 102, false);

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CANCEL_ORDER_RETURNS);
    }

    @Test
    @DisplayName(
        "Обработка чекпоинта возвратного потока от СЦ: таска не создается, так как возвратные чп не обрабатываются"
    )
    @DatabaseSetup("/controller/tracker/before/order_return.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/before/order_return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processSortingCenterReturnFlowCheckpointNewFlow() throws Exception {
        setCheckpointsProcessingFlow(true);
        notifyTracks(
            "controller/tracker/request/lo1_order_sc_returned.json",
            "controller/tracker/response/push_102_ok.json"
        );

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Обработка чекпоинта прямого потока от СЦ")
    @DatabaseSetup("/controller/tracker/before/order_return.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/order_return_sc_out.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processSortingCenterDirectFlowCheckpoint(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_sc_shipped.json",
            "controller/tracker/response/push_102_ok.json"
        );

        assertAndExecuteTask(OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED, 2, 102, newCheckpointsFlow);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CANCEL_ORDER_RETURNS);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Обработка чекпоинта 50 от партнёра")
    @DatabaseSetup("/controller/tracker/before/order_return.xml")
    void processDeliveryDeliveredStatus(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_delivered.json",
            "controller/tracker/response/push_101_ok.json"
        );

        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 3, 101, newCheckpointsFlow);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CANCEL_ORDER_RETURNS,
            PayloadFactory.createOrderIdPayload(1L, 1, 1).setSequenceId(2L)
        );
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Обработка чекпоинта 50 от партнёра, нет активных возвратов, не ставим в очередь отмену возвратов")
    @DatabaseSetup("/controller/tracker/before/order_return.xml")
    @DatabaseSetup(
        value = "/controller/tracker/before/order_return_cancelled.xml",
        type = DatabaseOperation.UPDATE
    )
    void processDeliveryDeliveredStatusActiveReturnsNotExist(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_delivered.json",
            "controller/tracker/response/push_101_ok.json"
        );

        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 3, 101, newCheckpointsFlow);

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CANCEL_ORDER_RETURNS);
    }

    private void assertAndExecuteTask(
        OrderDeliveryCheckpointStatus checkpointStatus,
        long waybillSegmentId,
        long trackId,
        boolean newCheckpointsFlow
    ) {
        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(
            1,
            checkpointStatus,
            trackId
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(waybillSegmentId, trackId),
            orderIdDeliveryTrackPayload
        );
    }
}
