package ru.yandex.market.logistics.lom.controller.tracker;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCode;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistics.lom.jobs.consumer.ChangeOrderRequestConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.UpdateTransferCodesSegmentConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessSegmentCheckpointsPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.service.waybill.TransferCodesService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Обработка 92 чекпоинта")
@DatabaseSetup("/controller/tracker/before/92/setup_outbound_verification.xml")
class ChangeOutboundVerificationIntegrationTest extends AbstractTrackerNotificationControllerTest {
    private static final Instant FIXED_TIME = Instant.parse("2019-06-12T00:00:00Z");

    @Autowired
    private ChangeOrderRequestConsumer changeOrderRequestConsumer;

    @Autowired
    private UpdateTransferCodesSegmentConsumer updateTransferCodesSegmentConsumer;

    @Autowired
    private TransferCodesService transferCodesService;

    @Autowired
    private DeliveryClient deliveryClient;

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        when(transferCodesService.generateCode()).thenReturn("54321");
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Обработка чекпоинта 92 от партнёра на сегмент со следующим за ним сегментом: новый флоу")
    @ExpectedDatabase(
        value = "/controller/tracker/after/92/push_update_transfer_codes_consecutive_new_flow.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processOutboundVerificationCodeUpdateRequiredConsecutiveNewFlow() {
        processOutboundVerificationCodeUpdateRequiredConsecutive(true);
    }

    @Test
    @DisplayName("Обработка чекпоинта 92 от партнёра на сегмент со следующим за ним сегментом")
    @ExpectedDatabase(
        value = "/controller/tracker/after/92/push_update_transfer_codes_consecutive.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processOutboundVerificationCodeUpdateRequiredConsecutive() {
        processOutboundVerificationCodeUpdateRequiredConsecutive(false);
    }

    @SneakyThrows
    private void processOutboundVerificationCodeUpdateRequiredConsecutive(boolean newFlowEnabled) {
        setCheckpointsProcessingFlow(newFlowEnabled);
        //По чекпоинту создается задача на его обработку
        notifyTracks(
            "controller/tracker/request/lo1_outbound_verification_code_update.json",
            "controller/tracker/response/push_101_ok.json"
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            101L
        );
        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(
            1,
            OrderDeliveryCheckpointStatus.DELIVERY_CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED,
            101
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newFlowEnabled,
            segmentCheckpointsPayload,
            orderIdDeliveryTrackPayload
        );

        ChangeOrderRequestPayload changeOrderRequestPayload =
            PayloadFactory.createChangeOrderRequestPayload(1L, "2", 1, 1);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            changeOrderRequestPayload
        );

        //При обработке задачи на изменение кодов создаются посегментные задачи
        Task<ChangeOrderRequestPayload> task =
            TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, changeOrderRequestPayload);
        changeOrderRequestConsumer.execute(task);

        ChangeOrderSegmentRequestPayload changeOrderSegmentRequestPayload1 =
            PayloadFactory.createChangeOrderSegmentRequestPayload(1, "4", 1, 1, 1);
        ChangeOrderSegmentRequestPayload changeOrderSegmentRequestPayload2 =
            PayloadFactory.createChangeOrderSegmentRequestPayload(2, "5", 1, 1, 2);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_TRANSFER_CODES,
            changeOrderSegmentRequestPayload1
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_TRANSFER_CODES,
            changeOrderSegmentRequestPayload2
        );

        //Корректная обработка посегментных задач
        updateTransferCodesSegmentConsumer.execute(
            TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, changeOrderSegmentRequestPayload1)
        );
        updateTransferCodesSegmentConsumer.execute(
            TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, changeOrderSegmentRequestPayload2)
        );

        verify(deliveryClient).updateOrderTransferCodes(
            eq(ResourceId.builder().setYandexId("LO1").setPartnerId("test-external-id-2").build()),
            eq(new OrderTransferCodes.OrderTransferCodesBuilder()
                .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("54321").build())
                .build()),
            eq(new Partner(48L)),
            any()
        );
        verify(deliveryClient).updateOrderTransferCodes(
            eq(ResourceId.builder().setYandexId("LO1").setPartnerId("test-external-id-3").build()),
            eq(new OrderTransferCodes.OrderTransferCodesBuilder()
                .setInbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("54321").build())
                .build()),
            eq(new Partner(49L)),
            any()
        );
    }

    @Test
    @DisplayName("Обработка чекпоинта 92 от партнёра на последний сегмент")
    @ExpectedDatabase(
        value = "/controller/tracker/after/92/push_update_transfer_codes_last.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processOutboundVerificationCodeUpdateRequiredLast() {
        processOutboundVerificationCodeUpdateRequiredLast(false);
    }

    @Test
    @DisplayName("Обработка чекпоинта 92 от партнёра на последний сегмент: новый флоу")
    @ExpectedDatabase(
        value = "/controller/tracker/after/92/push_update_transfer_codes_last_new_flow.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processOutboundVerificationCodeUpdateRequiredLastNewFlow() {
        processOutboundVerificationCodeUpdateRequiredLast(true);
    }

    @SneakyThrows
    private void processOutboundVerificationCodeUpdateRequiredLast(boolean newFlowEnabled) {
        setCheckpointsProcessingFlow(newFlowEnabled);
        //По чекпоинту создается задача на его обработку
        notifyTracks(
            "controller/tracker/request/lo1_outbound_verification_code_update_last.json",
            "controller/tracker/response/push_104_ok.json"
        );
        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(
            1,
            OrderDeliveryCheckpointStatus.DELIVERY_CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED,
            104
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            5L,
            104L
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newFlowEnabled,
            segmentCheckpointsPayload,
            orderIdDeliveryTrackPayload
        );

        ChangeOrderRequestPayload changeOrderRequestPayload =
            PayloadFactory.createChangeOrderRequestPayload(1L, "2", 1, 1);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            changeOrderRequestPayload
        );

        //При обработке задачи на изменение кодов создаются посегментные задачи
        Task<ChangeOrderRequestPayload> task =
            TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, changeOrderRequestPayload);
        changeOrderRequestConsumer.execute(task);

        ChangeOrderSegmentRequestPayload changeOrderSegmentRequestPayload =
            PayloadFactory.createChangeOrderSegmentRequestPayload(1, "4", 1, 1, 1);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_TRANSFER_CODES,
            changeOrderSegmentRequestPayload
        );

        //Корректная обработка посегментных задач
        updateTransferCodesSegmentConsumer.execute(
            TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, changeOrderSegmentRequestPayload)
        );

        verify(deliveryClient).updateOrderTransferCodes(
            eq(ResourceId.builder().setYandexId("LO1").setPartnerId("test-external-id-5").build()),
            eq(new OrderTransferCodes.OrderTransferCodesBuilder()
                .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("54321").build())
                .build()),
            eq(new Partner(50L)),
            any()
        );
    }

    @Test
    @DisplayName("Обработка чекпоинта 92 от партнёра на сегмент со следующим за ним сегментом")
    void outboundVerificationCodeNotProcessedForDropoff() {
        outboundVerificationCodeNotProcessedForDropoff(false);
    }

    @Test
    @DisplayName("Обработка чекпоинта 92 от партнёра на сегмент со следующим за ним сегментом: новый флоу")
    void outboundVerificationCodeNotProcessedForDropoffNewFlow() {
        outboundVerificationCodeNotProcessedForDropoff(true);
    }

    @SneakyThrows
    private void outboundVerificationCodeNotProcessedForDropoff(boolean newFlowEnabled) {
        setCheckpointsProcessingFlow(newFlowEnabled);
        //По чекпоинту создается задача на его обработку
        notifyTracks(
            "controller/tracker/request/92/lo1_outbound_verification_code_update_dropoff_segment.json",
            "controller/tracker/response/push_103_ok.json"
        );
        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = getOrderIdDeliveryTrackPayload(
            1,
            OrderDeliveryCheckpointStatus.DELIVERY_CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED,
            103
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            4L,
            103L
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newFlowEnabled,
            segmentCheckpointsPayload,
            orderIdDeliveryTrackPayload
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CHANGE_ORDER_REQUEST);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_TRANSFER_CODES);
        verifyZeroInteractions(deliveryClient);
    }
}
