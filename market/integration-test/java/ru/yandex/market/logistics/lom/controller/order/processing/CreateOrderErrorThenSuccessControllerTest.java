package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.CancellationOrderRequest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.enums.ApiType;
import ru.yandex.market.logistics.lom.entity.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.jobs.consumer.order.create.FulfillmentCreateOrderExternalConsumer;
import ru.yandex.market.logistics.lom.jobs.model.CreateOrderErrorPayload;
import ru.yandex.market.logistics.lom.jobs.model.CreateOrderSuccessPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessWaybillService;
import ru.yandex.market.logistics.lom.jobs.processor.order.processing.ProcessCreateOrderAsyncErrorResultService;
import ru.yandex.market.logistics.lom.jobs.processor.order.processing.ProcessCreateOrderAsyncSuccessResultService;
import ru.yandex.market.logistics.lom.model.async.CreateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.CreateOrderSuccessDto;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.controller.order.OrderTestUtil.asyncOrderCreate;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

/**
 * Тест покрывает кейсы, когда LGW не смог создать в СД/СЦ один или несколько сегментов заказа и вернул ошибку.
 * Затем, после устранения проблем в СД/СЦ таски в LOM были перевыставлены.
 */
@DisplayName("Пересоздание сегментов заказа")
public class CreateOrderErrorThenSuccessControllerTest extends AbstractContextualTest {

    @Autowired
    private ProcessCreateOrderAsyncSuccessResultService processCreateOrderAsyncSuccessResultService;

    @Autowired
    private ProcessCreateOrderAsyncErrorResultService processCreateOrderAsyncErrorResultService;

    @Autowired
    private ProcessWaybillService processWaybillService;

    @Autowired
    private FulfillmentCreateOrderExternalConsumer fulfillmentCreateOrderExternalConsumer;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-06-12T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        when(lmsClient.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnersIds(Set.of(1L))
                .toPartnersIds(Set.of(2L))
                .build(),
            new PageRequest(0, 1)
        ))
            .thenReturn(new PageResult<PartnerRelationEntityDto>().setData(
                List.of(PartnerRelationEntityDto.newBuilder()
                    .cutoffs(Set.of(CutoffResponse.newBuilder().cutoffTime(LocalTime.of(18, 0)).build()))
                    .build())
                )
            );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(fulfillmentClient);
    }

    @Test
    @DisplayName("Заказ переведен в успешный статус после успешного пересоздания сегмента")
    @DatabaseSetup("/controller/order/processing/create/error_then_success/before/order_create_error.xml")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error_then_success/after/order_create_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderErrorThenSuccess() throws Exception {
        processWaybillService.processPayload(PayloadFactory.createWaybillSegmentPayload(1L, 1L, 1L));

        OrderIdWaybillSegmentPayload ffCreateOrderPayload = PayloadFactory.createWaybillSegmentPayload(1L, 1, "1", 1L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            ffCreateOrderPayload
        );

        fulfillmentCreateOrderExternalConsumer.execute(TaskFactory.createTask(
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            ffCreateOrderPayload,
            1
        ));

        clock.setFixed(Instant.parse("2019-06-12T01:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        //В BaseQueueConsumer чистится контекст
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));

        asyncOrderCreate(
            mockMvc,
            "ff/createError",
            "controller/order/processing/create/error_then_success/request/order_create_error.json"
        )
            .andExpect(status().isOk());

        CreateOrderErrorPayload fulfillmentCreateOrderErrorPayload = PayloadFactory.createOrderErrorPayload(
            ApiType.FULFILLMENT,
            1L,
            1L,
            1L,
            new CreateOrderErrorDto(1L, null, null, "LOinttest-1", 1010L, false),
            "2",
            1L
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_ERROR_RESULT,
            fulfillmentCreateOrderErrorPayload
        );
        processCreateOrderAsyncErrorResultService.processPayload(fulfillmentCreateOrderErrorPayload);

        clock.setFixed(Instant.parse("2019-06-12T02:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        // После пересоздания сегмента

        fulfillmentCreateOrderExternalConsumer.execute(TaskFactory.createTask(
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            PayloadFactory.createWaybillSegmentPayload(1L, 1L, "1", 3L),
            1
        ));

        verify(fulfillmentClient, times(2)).createOrder(any(), any(), any(), any());

        clock.setFixed(Instant.parse("2019-06-12T03:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        //В BaseQueueConsumer чистится контекст
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));

        asyncOrderCreate(
            mockMvc,
            "ff/createSuccess",
            "controller/order/processing/create/error_then_success/request/order_create_success.json"
        )
            .andExpect(status().isOk());

        CreateOrderSuccessPayload fulfullmentCreateOrderSuccessPayload = PayloadFactory.createOrderSuccessPayload(
            ApiType.FULFILLMENT,
            1L,
            1L,
            1L,
            new CreateOrderSuccessDto("test-external-id-2", 1L, "LOinttest-1", 1010L),
            "4",
            1L
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_SUCCESS_RESULT,
            fulfullmentCreateOrderSuccessPayload
        );
        processCreateOrderAsyncSuccessResultService.processPayload(fulfullmentCreateOrderSuccessPayload);
    }

    @Test
    @DisplayName("В момент пересоздания сегмента заказ в статусе Отменён")
    @DatabaseSetup("/controller/order/processing/create/error_then_success/before/order_create_error.xml")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error_then_success/after/order_create_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void recreateCancelledOrder() throws Exception {
        processWaybillService.processPayload(PayloadFactory.createWaybillSegmentPayload(1L, 1L, 1L));

        OrderIdWaybillSegmentPayload ffCreateOrderPayload = PayloadFactory.createWaybillSegmentPayload(1L, 1, "1", 1L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            ffCreateOrderPayload
        );

        fulfillmentCreateOrderExternalConsumer.execute(TaskFactory.createTask(
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            ffCreateOrderPayload,
            1
        ));

        verify(fulfillmentClient).createOrder(any(), any(), any(), any());

        clock.setFixed(Instant.parse("2019-06-12T01:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        //В BaseQueueConsumer чистится контекст
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));

        asyncOrderCreate(
            mockMvc,
            "ff/createError",
            "controller/order/processing/create/error_then_success/request/order_create_error.json"
        )
            .andExpect(status().isOk());

        CreateOrderErrorPayload fulfillmentCreateOrderErrorPayload = PayloadFactory.createOrderErrorPayload(
            ApiType.FULFILLMENT,
            1L,
            1L,
            1L,
            new CreateOrderErrorDto(1L, null, null, "LOinttest-1", 1010L, false),
            "2",
            1L
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_ERROR_RESULT,
            fulfillmentCreateOrderErrorPayload
        );
        processCreateOrderAsyncErrorResultService.processPayload(fulfillmentCreateOrderErrorPayload);

        clock.setFixed(Instant.parse("2019-06-12T02:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        // Отменяем заказ
        transactionTemplate.execute(status -> {
            Order order = orderService.findById(1L);
            new CancellationOrderRequest()
                .setOrder(order)
                .setStatus(CancellationOrderStatus.SUCCESS);
            order.setStatus(OrderStatus.CANCELLED, clock);
            return null;
        });

        // После пересоздания сегмента не происходит создания в службе, так как заказ отменён

        fulfillmentCreateOrderExternalConsumer.execute(TaskFactory.createTask(
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            PayloadFactory.createWaybillSegmentPayload(1L, 1L, "1", 3L),
            1
        ));
    }
}
