package ru.yandex.market.logistics.lom.controller.order.processing;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Car;
import ru.yandex.market.logistic.gateway.common.model.common.Courier;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCode;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.Person;
import ru.yandex.market.logistic.gateway.common.model.common.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.UpdateCourierRequestStatusUpdateConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.UpdateCourierSegmentConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обработчик задач очереди PROCESS_WAYBILL_SEGMENT_UPDATE_COURIER")
@DatabaseSetup({
    "/controller/order/update_courier/before/order.xml",
    "/controller/order/update_courier/before/waybill_segment.xml",
    "/controller/order/update_courier/before/change_order_request_processing.xml",
    "/controller/order/update_courier/before/change_order_segment_request_processing.xml",
    "/controller/order/update_courier/before/process_waybill_segment_update_courier.xml",
})
class UpdateCourierSegmentProcessorTest extends AbstractContextualTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD_DS =
        PayloadFactory.createChangeOrderSegmentRequestPayload(
            1102,
            "1003"
        );
    private static final Task<ChangeOrderSegmentRequestPayload> TASK_DS =
        TaskFactory.createTask(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_COURIER, PAYLOAD_DS);

    private static final ChangeOrderSegmentRequestPayload PAYLOAD_FF =
        PayloadFactory.createChangeOrderSegmentRequestPayload(
            1101,
            "1002"
        );
    private static final Task<ChangeOrderSegmentRequestPayload> TASK_FF =
        TaskFactory.createTask(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_COURIER, PAYLOAD_FF);

    @Autowired
    private UpdateCourierSegmentConsumer updateCourierSegmentConsumer;

    @Autowired
    private UpdateCourierRequestStatusUpdateConsumer updateCourierRequestStatusUpdateConsumer;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Test
    @DisplayName("Заказ в статусе возврата — курьер обновляется")
    @DatabaseSetup("/controller/order/update_courier/before/cancellation_order_request.xml")
    @DatabaseSetup(
        value = {
            "/controller/order/update_courier/before/order_returning.xml",
            "/controller/order/update_courier/after/waybill_segments_updated.xml",
        },
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/segment_success_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderHasCancellationRequestTaskWillUnprocessed() {
        updateCourierSegmentConsumer.execute(TASK_FF);
        verifyUpdateCourierFF();
    }

    @Test
    @DisplayName("Успешная обработка задачи для delivery сегмента")
    @DatabaseSetup(
        value = "/controller/order/update_courier/after/waybill_segments_updated.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/segment_success_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessingDS() {
        updateCourierSegmentConsumer.execute(TASK_DS);
        verifyUpdateCourierDS();
    }

    @Test
    @DisplayName("Успешная обработка задачи для delivery сегмента")
    @DatabaseSetup(
        value = "/controller/order/update_courier/after/waybill_segments_updated_without_courier.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/segment_success_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessingDSWithoutCourier() {
        updateCourierSegmentConsumer.execute(TASK_DS);
        verify(deliveryClient).updateCourier(
            ResourceId.builder().setYandexId("1001").setPartnerId("test-external-id-3").build(),
            new Partner(1000003L),
            Courier.builder().build(),
            null,
            getInboundCodes(),
            new ClientRequestMeta("1003")
        );
    }

    @Test
    @DisplayName("Успешная обработка задачи для fulfillment сегмента")
    @DatabaseSetup(
        value = "/controller/order/update_courier/after/waybill_segments_updated.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/segment_success_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessingFF() {
        updateCourierSegmentConsumer.execute(TASK_FF);
        verifyUpdateCourierFF();
    }

    @Test
    @DisplayName("Успешная обработка задачи для fulfillment сегмента - нет курьера")
    @DatabaseSetup(
        value = "/controller/order/update_courier/after/waybill_segments_updated_without_courier.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/segment_success_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessingFFWithoutCourier() {
        updateCourierSegmentConsumer.execute(TASK_FF);
        verify(fulfillmentClient).updateCourier(
            ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.builder()
                .setYandexId("1001")
                .setPartnerId("test-external-id-1")
                .build(),
            new Partner(1000001L),
            null,
            Courier.builder().build(),
            getOutboundCodes(),
            new ClientRequestMeta("1002")
        );
    }

    @Test
    @DisplayName("Оба сегмента обновлены неуспешно")
    @DatabaseSetup(
        value = "/controller/order/update_courier/after/waybill_segments_updated.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/segment_both_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void errorProcessingBoth() {
        updateCourierSegmentConsumer.execute(TASK_DS);
        verifyUpdateCourierDS();
        updateCourierSegmentConsumer.execute(TASK_FF);
        verifyUpdateCourierFF();

        callAsyncResponseMethod(
            "/orders/ds/updateCourier/error",
            "controller/order/update_courier/request/update_courier_error_ds.json"
        );
        callAsyncResponseMethod(
            "/orders/ff/updateCourier/error",
            "controller/order/update_courier/request/update_courier_error_ff.json"
        );
        processUpdateChangeRequestStatus(1102, "1");
        processUpdateChangeRequestStatus(1101, "2");
    }

    @Test
    @DisplayName("Оба сегмента обновлены успешно")
    @DatabaseSetup(
        value = "/controller/order/update_courier/after/waybill_segments_updated.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/segment_both_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessingBoth() {
        updateCourierSegmentConsumer.execute(TASK_DS);
        verifyUpdateCourierDS();
        updateCourierSegmentConsumer.execute(TASK_FF);
        verifyUpdateCourierFF();

        callAsyncResponseMethod(
            "/orders/ds/updateCourier/success",
            "controller/order/update_courier/request/update_courier_success_ds.json"
        );
        callAsyncResponseMethod(
            "/orders/ff/updateCourier/success",
            "controller/order/update_courier/request/update_courier_success_ff.json"
        );
        processUpdateChangeRequestStatus(1102, "1");
        processUpdateChangeRequestStatus(1101, "2");
    }

    @Test
    @DisplayName("Один сегмент обновлен успешно, один неуспешно")
    @DatabaseSetup(
        value = "/controller/order/update_courier/after/waybill_segments_updated.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/segment_success_and_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void oneSuccessOneError() {
        updateCourierSegmentConsumer.execute(TASK_DS);
        verifyUpdateCourierDS();
        updateCourierSegmentConsumer.execute(TASK_FF);
        verifyUpdateCourierFF();

        callAsyncResponseMethod(
            "/orders/ds/updateCourier/success",
            "controller/order/update_courier/request/update_courier_success_ds.json"
        );
        callAsyncResponseMethod(
            "/orders/ff/updateCourier/error",
            "controller/order/update_courier/request/update_courier_error_ff.json"
        );
        processUpdateChangeRequestStatus(1102, "1");
        processUpdateChangeRequestStatus(1101, "2");
    }

    @SneakyThrows
    private void verifyUpdateCourierDS() {
        verify(deliveryClient).updateCourier(
            ResourceId.builder().setYandexId("1001").setPartnerId("test-external-id-3").build(),
            new Partner(1000003L),
            getCourier(),
            null,
            getInboundCodes(),
            new ClientRequestMeta("1003")
        );
    }

    @SneakyThrows
    private void verifyUpdateCourierFF() {
        verify(fulfillmentClient).updateCourier(
            ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.builder()
                .setYandexId("1001")
                .setPartnerId("test-external-id-1")
                .build(),
            new Partner(1000001L),
            null,
            getCourier(),
            getOutboundCodes(),
            new ClientRequestMeta("1002")
        );
    }

    @Nonnull
    private Courier getCourier() {
        return Courier.builder()
            .setPersons(List.of(Person.builder("Иван").setSurname("Иванов").setPatronymic("Иванович").build()))
            .setCar(
                Car.builder("AA111AA")
                    .setDescription("Семиместная тойота")
                    .setColor("перламутровый")
                    .setModel("BMW X5")
                    .build()
            )
            .setPhone(Phone.builder("+88005553535").setAdditional("123").build())
            .setUrl("https://go.yandex/route/6ea161f870ba6574d3bd9bdd19e1e9d8?lang=ru")
            .build();
    }

    @Nonnull
    private OrderTransferCodes getInboundCodes() {
        return new OrderTransferCodes.OrderTransferCodesBuilder()
            .setInbound(new OrderTransferCode.OrderTransferCodeBuilder()
                .setElectronicAcceptanceCertificate("asd123")
                .build()
            )
            .build();
    }

    @Nonnull
    private OrderTransferCodes getOutboundCodes() {
        return new OrderTransferCodes.OrderTransferCodesBuilder()
            .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder()
                .setElectronicAcceptanceCertificate("qwe456")
                .build()
            )
            .build();
    }

    private void processUpdateChangeRequestStatus(int changeOrderSegmentRequestId, String sequenceId) {
        updateCourierRequestStatusUpdateConsumer.execute(
            TaskFactory.createTask(
                QueueType.UPDATE_COURIER_REQUEST_STATUS_UPDATE,
                PayloadFactory.createChangeOrderSegmentRequestPayload(
                    changeOrderSegmentRequestId,
                    sequenceId
                )
            )
        );
    }

    @SneakyThrows
    private void callAsyncResponseMethod(String path, String requestPath) {
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
        mockMvc.perform(
                put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent(requestPath))
            )
            .andExpect(status().isOk())
            .andExpect(noContent());
    }
}
