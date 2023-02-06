package ru.yandex.market.logistics.lom.controller.tracker;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Тест чекпоинтов средней мили OnDemand")
class TrackerNotificationControllerOnDemandTest extends AbstractTrackerNotificationControllerTest {

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Чекпоинт 49 не процессится для ПВЗ сегмента")
    @DatabaseSetup("/controller/tracker/before/setup_ondemand.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_ondemand_transmitted_to_recipient_not_processed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void transitToRecipientNotProcessedForPickupSegment(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_transmitted_to_recipient.json",
            "controller/tracker/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT, 2, newCheckpointsFlow);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Чекпоинт 50 не процессится для ПВЗ сегмента")
    @DatabaseSetup("/controller/tracker/before/setup_ondemand.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_ondemand_delivery_delivered_not_processed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deliveryDeliveredNotProcessedForPickupSegment(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_delivered.json",
            "controller/tracker/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2, newCheckpointsFlow);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Обработка чекпоинта 49 от партнёра")
    @DatabaseSetup("/controller/tracker/before/setup_ondemand_partner.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_ondemand_transmitted_to_recipient.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deliveryTransmittedToRecipientForLastMile(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_transmitted_to_recipient.json",
            "controller/tracker/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT, 3, newCheckpointsFlow);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Обработка чекпоинта 50 от партнёра")
    @DatabaseSetup("/controller/tracker/before/setup_ondemand_partner.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_ondemand_delivery_delivered.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deliveryDeliveredForLastMile(boolean newCheckpointsFlow) throws Exception {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo1_order_delivered.json",
            "controller/tracker/response/push_101_ok.json"
        );
        assertAndExecuteTask(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 3, newCheckpointsFlow);
    }

    private void assertAndExecuteTask(
        OrderDeliveryCheckpointStatus checkpointStatus,
        long waybillSegmentId,
        boolean newCheckpointsFlow
    ) {
        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(
            1,
            checkpointStatus,
            101
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(waybillSegmentId, 101L),
            orderIdDeliveryTrackPayload
        );
    }
}
