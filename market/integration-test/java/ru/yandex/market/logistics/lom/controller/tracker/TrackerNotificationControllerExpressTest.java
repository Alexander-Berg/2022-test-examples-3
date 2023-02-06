package ru.yandex.market.logistics.lom.controller.tracker;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Тест чекпоинтов средней мили экспресса")
class TrackerNotificationControllerExpressTest extends AbstractTrackerNotificationControllerTest {

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Чекпоинт 49 не процессится для MOVEMENT сегменгта экспресса в пвз")
    @DatabaseSetup("/controller/tracker/before/setup_express_pickup.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_express_transmitted_to_recipient_not_processed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void transitToRecipientNotProcessedForExpressPickup(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_transmitted_to_recipient.json",
            "controller/tracker/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT, newCheckpointsFlow);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Чекпоинт 50 не процессится для MOVEMENT сегменгта экспресса в пвз")
    @DatabaseSetup("/controller/tracker/before/setup_express_pickup.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_express_delivery_delivered_not_processed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deliveryDeliveredNotProcessedForExpressPickup(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_delivered.json",
            "controller/tracker/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, newCheckpointsFlow);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Обработка чекпоинта 49 от партнёра для экспресса")
    @DatabaseSetup("/controller/tracker/before/setup_express.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_express_transmitted_to_recipient.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deliveryTransmittedToRecipientForExpress(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_transmitted_to_recipient.json",
            "controller/tracker/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT, newCheckpointsFlow);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Обработка чекпоинта 50 от партнёра для экспресса")
    @DatabaseSetup("/controller/tracker/before/setup_express.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_express_delivery_delivered.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deliveryDeliveredForExpress(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_delivered.json",
            "controller/tracker/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, newCheckpointsFlow);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Обработка чекпоинта 49 от партнёра для не экспресс MOVEMENT")
    @DatabaseSetup("/controller/tracker/before/setup_express.xml")
    @DatabaseSetup(
        value = "/controller/tracker/before/setup_drop_tags.xml",
        type = DatabaseOperation.DELETE_ALL
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_express_transmitted_to_recipient.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deliveryTransmittedToRecipientForMovement(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_transmitted_to_recipient.json",
            "controller/tracker/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT, newCheckpointsFlow);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Обработка чекпоинта 50 от партнёра для не экспресс MOVEMENT")
    @DatabaseSetup("/controller/tracker/before/setup_express.xml")
    @DatabaseSetup(
        value = "/controller/tracker/before/setup_drop_tags.xml",
        type = DatabaseOperation.DELETE_ALL
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_express_delivery_delivered.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deliveryDeliveredForMovement(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_delivered.json",
            "controller/tracker/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, newCheckpointsFlow);
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
