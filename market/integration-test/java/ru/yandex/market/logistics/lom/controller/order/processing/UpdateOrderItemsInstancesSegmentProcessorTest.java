package ru.yandex.market.logistics.lom.controller.order.processing;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.ItemInstances;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.UnitId;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.UpdateOrderItemsInstancesSegmentConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.UpdateOrderUpdatingRequestConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обработчик задач очереди PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS_INSTANCES")
@DatabaseSetup({
    "/controller/order/update_items_instances/before/order.xml",
    "/controller/order/update_items_instances/before/waybill_segment.xml",
    "/controller/order/update_items_instances/before/order_item_with_instances.xml",
    "/controller/order/update_items_instances/before/change_order_request_processing.xml",
    "/controller/order/update_items_instances/before/change_order_segment_request_processing.xml",
    "/controller/order/update_items_instances/before/process_waybill_segment_update_order_items_instances.xml",
})
class UpdateOrderItemsInstancesSegmentProcessorTest extends AbstractContextualTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD =
        PayloadFactory.createChangeOrderSegmentRequestPayload(
            1101,
            "1002"
        );

    private static final Task<ChangeOrderSegmentRequestPayload> TASK =
        TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD);

    @Autowired
    private UpdateOrderItemsInstancesSegmentConsumer updateOrderItemsInstancesSegmentConsumer;

    @Autowired
    private UpdateOrderUpdatingRequestConsumer updateOrderUpdatingRequestConsumer;

    @Autowired
    private DeliveryClient deliveryClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @DisplayName("У заказа есть заявка на отмену - бизнес-процесс переведется в статус UNPROCESSED")
    @DatabaseSetup("/controller/order/update_items_instances/before/cancellation_order_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/segment_unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderHasCancellationRequestTaskWillUnprocessed() {
        updateOrderItemsInstancesSegmentConsumer.execute(TASK);
    }

    @Test
    @DisplayName("Успешная обработка задачи")
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/segment_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessing() {
        updateOrderItemsInstancesSegmentConsumer.execute(TASK);

        ArgumentCaptor<List<ItemInstances>> captor = ArgumentCaptor.forClass(List.class);
        verify(deliveryClient).updateItemsInstances(
            eq(ResourceId.builder().setYandexId("1001").setPartnerId("test-external-id-4").build()),
            captor.capture(),
            eq(new Partner(1000004L)),
            eq(new ClientRequestMeta("1002"))
        );
        softly.assertThat(captor.getValue()).containsExactlyInAnyOrder(
            new ItemInstances.ItemInstancesBuilder()
                .setUnitId(new UnitId(null, 1L, "test-item-article"))
                .setInstances(List.of(Map.of("SN", "SC02DX3V9Q6LD")))
                .build(),
            new ItemInstances.ItemInstancesBuilder()
                .setUnitId(new UnitId(null, 1L, "test-item-article"))
                .setInstances(List.of(Map.of("SN", "SC02DX3V9Q6LD-1")))
                .build()
        );
    }

    @Test
    @DisplayName("Успешный асинхронный ответ")
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/segment_success_response.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successResponseFromLgw() {
        updateOrderItemsInstancesSegmentConsumer.execute(TASK);
        verify(deliveryClient).updateItemsInstances(any(), any(), any(), any());
        callAsyncResponseMethod("/orders/ds/update-items-instances/success");
    }

    @Test
    @DisplayName("Неуспешный асинхронный ответ")
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/segment_error_response.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void errorResponseFromLgw() {
        updateOrderItemsInstancesSegmentConsumer.execute(TASK);
        verify(deliveryClient).updateItemsInstances(any(), any(), any(), any());
        callAsyncResponseMethod("/orders/ds/update-items-instances/error");
    }

    @Test
    @DisplayName("Успешный флоу")
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/segment_success_flow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successFlow() {
        processSuccessFlow();
    }

    @Test
    @DisplayName("Успешный флоу без учета заявок на неактивных сегментах")
    @DatabaseSetup(
        value = {
            "/controller/order/update_items_instances/before/waybill_segment_inactive.xml",
            "/controller/order/update_items_instances/before/change_order_segment_request_processing_inactive.xml"
        },
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/segment_success_flow_disregard_inactive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successFlowDisregardInactiveSegments() {
        processSuccessFlow();
    }

    @SneakyThrows
    private void processSuccessFlow() {
        updateOrderItemsInstancesSegmentConsumer.execute(TASK);
        verify(deliveryClient).updateItemsInstances(any(), any(), any(), any());
        callAsyncResponseMethod("/orders/ds/update-items-instances/success");
        updateOrderUpdatingRequestConsumer.execute(
            TaskFactory.createTask(
                QueueType.UPDATE_ORDER_ITEMS_REQUEST_STATUS_UPDATE,
                PayloadFactory.createChangeOrderSegmentRequestPayload(
                    1101,
                    "1"
                )
            )
        );
    }

    private void callAsyncResponseMethod(String path) throws Exception {
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
        mockMvc.perform(
            put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/order/update_items_instances/request/update_items_instances.json"
                ))
        )
            .andExpect(status().isOk());
    }
}
