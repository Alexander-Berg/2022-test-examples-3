package ru.yandex.market.logistics.lom.controller.tracker;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessSegmentCheckpointsPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Обработка 45 чекпоинта")
class TrackerNotificationArrivedPickupPointTest extends AbstractTrackerNotificationControllerTest {
    private static final Instant FIXED_TIME = Instant.parse("2019-06-12T00:00:00Z");

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Обработка 45 чекпоинта от ПВЗ для заказа ЯДо - требуется код")
    @DatabaseSetup("/controller/tracker/before/order_pvz_with_code_yado.xml")
    void yadoOrderPvzWithCodeArrivedHandler(boolean newCheckpointsFlowEnabled) {
        pvzWithCodeArrivedHandler(newCheckpointsFlowEnabled);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Обработка 45 чекпоинта от ПВЗ для заказа GO - требуется код")
    @DatabaseSetup("/controller/tracker/before/order_pvz_with_code_go.xml")
    void GoOrderPvzWithCodeArrivedHandler(boolean newCheckpointsFlowEnabled) {
        pvzWithCodeArrivedHandler(newCheckpointsFlowEnabled);
    }

    void pvzWithCodeArrivedHandler(boolean newCheckpointsFlowEnabled) {
        processDeliveryArrivedPickupPoint(newCheckpointsFlowEnabled);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_ARRIVED_PICKUP_POINT,
            PayloadFactory.lesOrderArrivedPickupPointEventPayload(
                1,
                1,
                Instant.ofEpochMilli(1565098800000L),
                "2",
                1,
                1
            )
        );
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Обработка 45 чекпоинта от ПВЗ для заказа ЯДо - код не требуется")
    @DatabaseSetup("/controller/tracker/before/order_pvz_yado.xml")
    void yadoOrderPvzWithoutCodeArrivedHandler(boolean newCheckpointsFlowEnabled) {
        processDeliveryArrivedPickupPoint(newCheckpointsFlowEnabled);

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_ARRIVED_PICKUP_POINT);
    }

    @SneakyThrows
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Обработка 45 чекпоинта от ПВЗ")
    @DatabaseSetup("/controller/tracker/before/order_pvz.xml")
    void orderPvzArrivedHandler(boolean newCheckpointsFlowEnabled) {
        processDeliveryArrivedPickupPoint(newCheckpointsFlowEnabled);

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_ARRIVED_PICKUP_POINT);
    }

    @SneakyThrows
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Обработка 45 чекпоинта от Постамата для заказа ЯДо")
    @DatabaseSetup("/controller/tracker/before/order_locker_yado.xml")
    void yadoOrderLockerArrivedHandler(boolean newCheckpointsFlowEnabled) {
        setCheckpointsProcessingFlow(newCheckpointsFlowEnabled);
        processDeliveryArrivedPickupPoint(newCheckpointsFlowEnabled);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_ARRIVED_PICKUP_POINT);
    }

    @SneakyThrows
    private void processDeliveryArrivedPickupPoint(boolean newCheckpointsFlowEnabled) {
        setCheckpointsProcessingFlow(newCheckpointsFlowEnabled);
        notifyTracks(
            "controller/tracker/request/lo1_order_arrived_pickup_point.json",
            "controller/tracker/response/push_102_ok.json"
        );
        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(
            1,
            OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT,
            102
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            3L,
            102L
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlowEnabled,
            segmentCheckpointsPayload,
            orderIdDeliveryTrackPayload
        );
    }
}
