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

@DisplayName("Обработка чекпоинта  70 заказ вернулся на склад")
class ProduceExportOrderReturningEventTest extends AbstractTrackerNotificationControllerTest {
    private static final Instant FIXED_TIME = Instant.parse("2019-06-12T00:00:00Z");

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Таска не создается: BERU заказ")
    @DatabaseSetup("/controller/tracker/before/order_pvz.xml")
    void beruOrderReturningEventNotProduced(boolean newCheckpointsFlowEnabled) {
        notifyOrderReturning(false, newCheckpointsFlowEnabled);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Таска не создается: не подходящий подтип партнера")
    @DatabaseSetup("/controller/tracker/before/order_pvz_yado_without_subtype.xml")
    void emptySubtypeOrderDeliveredEventNotProduced(boolean newCheckpointsFlowEnabled) {
        notifyOrderReturning(false, newCheckpointsFlowEnabled);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Таска создается: YADO заказ")
    @DatabaseSetup("/controller/tracker/before/order_pvz_yado.xml")
    void yadoOrderReturningEventProduced(boolean newCheckpointsFlowEnabled) {
        notifyOrderReturning(true, newCheckpointsFlowEnabled);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Таска создается: GO заказ")
    @DatabaseSetup("/controller/tracker/before/order_pvz_with_code_go.xml")
    void goOrderReturningEventProduced(boolean newCheckpointsFlowEnabled) {
        notifyOrderReturning(true, newCheckpointsFlowEnabled);
    }

    @SneakyThrows
    void notifyOrderReturning(boolean lesExportTaskCreated, boolean newCheckpointsFlowEnabled) {
        setCheckpointsProcessingFlow(newCheckpointsFlowEnabled);
        notifyTracks(
            "controller/tracker/request/lo1_order_ds_return_arrived.json",
            "controller/tracker/response/push_101_ok.json"
        );
        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(
            1,
            OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY,
            101
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            101L
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlowEnabled,
            segmentCheckpointsPayload,
            orderIdDeliveryTrackPayload
        );

        if (lesExportTaskCreated) {
            queueTaskChecker.assertQueueTaskCreated(
                QueueType.EXPORT_ORDER_RETURNING,
                PayloadFactory.lesOrderEventPayload(
                    1,
                    1,
                    "3",
                    1,
                    2
                )
            );
        } else {
            queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_RETURNING);
        }
    }
}
