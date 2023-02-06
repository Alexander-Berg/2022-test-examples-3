package ru.yandex.market.logistics.lom.controller.tracker;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Тест чекпоинтов последней мили DBS")
class TrackerNotificationControllerDbsTest extends AbstractTrackerNotificationControllerTest {

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Успешная обработка чекпоинта прямого потока")
    @DatabaseSetup("/controller/tracker/dbs/before/setup_no_order_return.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/dbs/after/direct_status.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successDirectCheckpoint(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/dbs/request/order_delivery_loaded.json",
            "controller/tracker/dbs/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_LOADED, newCheckpointsFlow);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Успешная обработка чекпоинта возвратного потока. По заказу не заведен возврат в LRM")
    @DatabaseSetup("/controller/tracker/dbs/before/setup_no_order_return.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/dbs/after/return_status.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successReturnCheckpointNoOrderReturn(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/dbs/request/order_return_preparing.json",
            "controller/tracker/dbs/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.RETURN_PREPARING, newCheckpointsFlow);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_RETURN_PREPARING,
            PayloadFactory.createWaybillSegmentPayload(1L, 2L, "2", 1, 1)
        );
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Успешная обработка чекпоинта возвратного потока. По заказу заведен возврат в LRM, пушим статус")
    @DatabaseSetup("/controller/tracker/dbs/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/dbs/after/return_status.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successReturnCheckpoint(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/dbs/request/order_return_preparing.json",
            "controller/tracker/dbs/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.RETURN_PREPARING, newCheckpointsFlow);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PUSH_CANCELLATION_RETURN_DELIVERY_SERVICE_STATUSES,
            PayloadFactory.createOrderIdPayload(1L, "2", 1, 1)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_RETURN_PREPARING,
            PayloadFactory.createWaybillSegmentPayload(1L, 2L, "3", 1, 2)
        );
    }

    private void assertAndExecuteTask(
        OrderDeliveryCheckpointStatus checkpointStatus,
        boolean newCheckpointsFlow
    ) {
        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(
            1,
            checkpointStatus,
            101
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(2L, 101L),
            orderIdDeliveryTrackPayload
        );
    }
}
