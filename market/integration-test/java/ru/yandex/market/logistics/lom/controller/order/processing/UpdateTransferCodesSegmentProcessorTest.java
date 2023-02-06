package ru.yandex.market.logistics.lom.controller.order.processing;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCode;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.UpdateTransferCodesRequestStatusUpdateConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.UpdateTransferCodesSegmentConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.service.waybill.TransferCodesService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обработчик задач очереди PROCESS_WAYBILL_SEGMENT_UPDATE_TRANSFER_CODES")
@DatabaseSetup({
    "/controller/order/update_transfer_codes/before/order.xml",
    "/controller/order/update_transfer_codes/before/waybill_segment.xml",
    "/controller/order/update_transfer_codes/before/change_order_request_processing.xml",
    "/controller/order/update_transfer_codes/before/change_order_segment_request_processing.xml",
    "/controller/order/update_transfer_codes/before/process_waybill_segment_update_order_items_instances.xml",
})
class UpdateTransferCodesSegmentProcessorTest extends AbstractContextualTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD_LAST =
        PayloadFactory.createChangeOrderSegmentRequestPayload(
            1101,
            "1002"
        );
    private static final Task<ChangeOrderSegmentRequestPayload> TASK_LAST =
        TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD_LAST);

    private static final ChangeOrderSegmentRequestPayload PAYLOAD_CONSECUTIVE =
        PayloadFactory.createChangeOrderSegmentRequestPayload(
            1102,
            "1003"
        );
    private static final Task<ChangeOrderSegmentRequestPayload> TASK_CONSECUTIVE =
        TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD_CONSECUTIVE);

    @Autowired
    private UpdateTransferCodesSegmentConsumer updateTransferCodesSegmentConsumer;

    @Autowired
    private UpdateTransferCodesRequestStatusUpdateConsumer updateTransferCodesRequestStatusUpdateConsumer;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private TransferCodesService transferCodesService;

    @BeforeEach
    void setUp() {
        when(transferCodesService.generateCode()).thenReturn("54321");
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @DisplayName("У заказа есть заявка на отмену - бизнес-процесс переведется в статус UNPROCESSED")
    @DatabaseSetup("/controller/order/update_transfer_codes/before/cancellation_order_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_transfer_codes/after/segment_unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderHasCancellationRequestTaskWillUnprocessed() {
        updateTransferCodesSegmentConsumer.execute(TASK_LAST);
    }

    @Test
    @DisplayName("Успешная обработка задачи для последнего сегмента")
    @ExpectedDatabase(
        value = "/controller/order/update_transfer_codes/after/segment_success_last.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessingLast() {
        updateTransferCodesSegmentConsumer.execute(TASK_LAST);

        verify(deliveryClient).updateOrderTransferCodes(
            eq(ResourceId.builder().setYandexId("1001").setPartnerId("test-external-id-4").build()),
            eq(new OrderTransferCodes.OrderTransferCodesBuilder()
                .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("12345").build())
                .build()
            ),
            eq(new Partner(1000004L)),
            eq(new ClientRequestMeta("1002"))
        );
    }

    @Test
    @DisplayName("Успешная обработка задачи для сегмента со следующим сегментом")
    @ExpectedDatabase(
        value = "/controller/order/update_transfer_codes/after/segment_success_consecutive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessingConsecutive() {
        updateTransferCodesSegmentConsumer.execute(TASK_CONSECUTIVE);

        verify(deliveryClient).updateOrderTransferCodes(
            eq(ResourceId.builder().setYandexId("1001").setPartnerId("test-external-id-3").build()),
            eq(new OrderTransferCodes.OrderTransferCodesBuilder()
                .setInbound(
                    new OrderTransferCode.OrderTransferCodeBuilder()
                        .setVerification("12345")
                        .setElectronicAcceptanceCertificate("asd123")
                        .build()
                )
                .setOutbound(
                    new OrderTransferCode.OrderTransferCodeBuilder()
                        .setElectronicAcceptanceCertificate("qwe456")
                        .build()
                )
                .build()
            ),
            eq(new Partner(1000003L)),
            eq(new ClientRequestMeta("1003"))
        );
    }

    @Test
    @DisplayName("Неуспешный асинхронный ответ")
    @ExpectedDatabase(
        value = "/controller/order/update_transfer_codes/after/segment_error_response.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void errorResponseFromLgw() {
        updateTransferCodesSegmentConsumer.execute(TASK_LAST);
        verify(deliveryClient).updateOrderTransferCodes(any(), any(), any(), any());
        callAsyncResponseMethod("/orders/ds/updateTransferCodes/error");
        processUpdateChangeRequestStatus();
    }

    @Test
    @DisplayName("Успешный флоу")
    @ExpectedDatabase(
        value = "/controller/order/update_transfer_codes/after/segment_success_flow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successFlow() {
        updateTransferCodesSegmentConsumer.execute(TASK_LAST);
        verify(deliveryClient).updateOrderTransferCodes(any(), any(), any(), any());
        callAsyncResponseMethod("/orders/ds/updateTransferCodes/success");
        processUpdateChangeRequestStatus();
    }

    private void callAsyncResponseMethod(String path) throws Exception {
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
        mockMvc.perform(
            put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/order/update_transfer_codes/request/update_transfer_codes.json"
                ))
        )
            .andExpect(status().isOk());
    }

    private void processUpdateChangeRequestStatus() {
        updateTransferCodesRequestStatusUpdateConsumer.execute(
            TaskFactory.createTask(
                QueueType.UPDATE_TRANSFER_CODES_REQUEST_STATUS_UPDATE,
                PayloadFactory.createChangeOrderSegmentRequestPayload(
                    1101,
                    "1"
                )
            )
        );
    }
}
