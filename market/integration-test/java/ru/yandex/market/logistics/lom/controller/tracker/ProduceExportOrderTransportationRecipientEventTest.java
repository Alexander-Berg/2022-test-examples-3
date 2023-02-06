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

@DisplayName("Обработка 48 чп заказ на доставке клиенту")
class ProduceExportOrderTransportationRecipientEventTest extends AbstractTrackerNotificationControllerTest {
    private static final Instant FIXED_TIME = Instant.parse("2019-06-12T00:00:00Z");

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Таска не создается: BERU заказ")
    @DatabaseSetup("/controller/tracker/before/order_pvz.xml")
    void beruOrderTransportationRecipientEventNotProduced(boolean newCheckpointsFlowEnabled) {
        notifyTracksAndExecuteTask(newCheckpointsFlowEnabled);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_TRANSPORTATION_RECIPIENT);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Таска не создается: не подходящий подтип партнера")
    @DatabaseSetup("/controller/tracker/before/order_pvz_yado.xml")
    void emptySubtypeOrderTransportationRecipientEventNotProduced(boolean newCheckpointsFlowEnabled) {
        notifyTracksAndExecuteTask(newCheckpointsFlowEnabled);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_TRANSPORTATION_RECIPIENT);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Таска создается")
    @DatabaseSetup("/controller/tracker/before/order_market_courier_yado.xml")
    void beruOrderTransportationRecipientEventProduced(boolean newCheckpointsFlowEnabled) {
        notifyTracksAndExecuteTask(newCheckpointsFlowEnabled);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_TRANSPORTATION_RECIPIENT,
            PayloadFactory.lesOrderEventPayload(
                1,
                1,
                "2",
                1,
                1
            )
        );
    }

    @SneakyThrows
    private void notifyTracksAndExecuteTask(boolean newCheckpointsFlowEnabled) {
        setCheckpointsProcessingFlow(newCheckpointsFlowEnabled);
        notifyTracks(
            "controller/tracker/request/lo1_order_ds_transportation_recipient.json",
            "controller/tracker/response/push_101_ok.json"
        );
        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(
            1,
            OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT,
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
    }
}
