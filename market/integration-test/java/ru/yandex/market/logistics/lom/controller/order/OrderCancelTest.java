package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.entity.ChangeOrderRequest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Sender;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.OrderRepository;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_ID;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_AND_SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_UID;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderCancellationRequestIdPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class OrderCancelTest extends AbstractContextualTest {

    @Autowired
    private TvmClientApi tvmClientApi;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-05-02T22:00:11Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void close() {
        featureProperties.setCancellationWithLrmAllEnabled(false);
        featureProperties.setUseNewFlowForExpressCancellation(false);
    }

    @Test
    @DisplayName("Отмена заказа в DRAFT (с вейбилом)")
    @DatabaseSetup("/controller/order/cancel/before/draft_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb_draft.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void draftOrderCancel() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Отмена заказа в DRAFT (с вейбилом), с LRM")
    @DatabaseSetup("/controller/order/cancel/before/draft_cancel_lrm.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb_lrm.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void draftOrderCancelLrm() throws Exception {
        featureProperties.setCancellationWithLrmAllEnabled(true);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_CANCELLED);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.SEND_CANCELLATION_TO_LRM,
            PayloadFactory.createOrderIdPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Отмена экспресс-заказа в DRAFT (с вейбилом), с LRM")
    @DatabaseSetup("/controller/order/cancel/before/draft_express_cancel_lrm.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb_lrm.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void draftExpressOrderCancelLrm() throws Exception {
        featureProperties.setUseNewFlowForExpressCancellation(true);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_CANCELLED);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.SEND_CANCELLATION_TO_LRM,
            PayloadFactory.createOrderIdPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Отмена заказа в DRAFT (с вейбилом), с LRM, существует активный возврат, не создаем новый")
    @DatabaseSetup("/controller/order/cancel/before/draft_cancel_lrm.xml")
    @DatabaseSetup("/controller/order/cancel/before/order_return.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb_lrm.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void draftOrderCancelLrmActiveReturnExists() throws Exception {
        featureProperties.setCancellationWithLrmAllEnabled(true);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Отмена экспресс-заказа в DRAFT (с вейбилом), с LRM, существует активный возврат, не создаем новый")
    @DatabaseSetup("/controller/order/cancel/before/draft_express_cancel_lrm.xml")
    @DatabaseSetup("/controller/order/cancel/before/order_return.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb_lrm.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void draftExpressOrderCancelLrmActiveReturnExists() throws Exception {
        featureProperties.setUseNewFlowForExpressCancellation(true);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Отмена заказа в DRAFT (с вейбилом), с LRM, нет активных возвратов, создаем новый")
    @DatabaseSetup("/controller/order/cancel/before/draft_cancel_lrm.xml")
    @DatabaseSetup("/controller/order/cancel/before/order_return_cancelled.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb_lrm.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void draftOrderCancelLrmActiveReturnNotExists() throws Exception {
        featureProperties.setCancellationWithLrmAllEnabled(true);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_CANCELLED);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.SEND_CANCELLATION_TO_LRM,
            PayloadFactory.createOrderIdPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Отмена экспресс-заказа в DRAFT (с вейбилом), с LRM, нет активных возвратов, создаем новый")
    @DatabaseSetup("/controller/order/cancel/before/draft_express_cancel_lrm.xml")
    @DatabaseSetup("/controller/order/cancel/before/order_return_cancelled.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb_lrm.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void draftOrderExpressCancelLrmActiveReturnNotExists() throws Exception {
        featureProperties.setUseNewFlowForExpressCancellation(true);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_CANCELLED);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.SEND_CANCELLATION_TO_LRM,
            PayloadFactory.createOrderIdPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Отмена заказа в DRAFT (с BillingEntity)")
    @DatabaseSetup("/controller/order/cancel/before/draft_with_billing_entity_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_with_billing_entity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void draftOrderWithBillingEntityCancel() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Отмена заказа в статусе VALIDATING")
    @DatabaseSetup("/controller/order/cancel/before/draft_cancel.xml")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_order_set_status_validating.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb_validating.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validatingOrderCancel() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Отмена заказа в статусе ENQUEUED")
    @DatabaseSetup("/controller/order/cancel/before/draft_cancel.xml")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_order_set_status_enqueued.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel_wb_enqueued.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void enqueuedOrderCancel() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(
                1,
                1,
                "1",
                1
            )
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Отмена заказа при ошибке валидации")
    @DatabaseSetup("/controller/order/cancel/before/validation_error_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/success_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validationErrorOrderCancel() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_success.json", "created", "updated"));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Отмена отменённого заказа")
    @DatabaseSetup("/controller/order/cancel/before/cancelled_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/before/cancelled_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancelCancelledOrder() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS,
            createOrderCancellationRequestIdPayload(1L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Отмена отменённого заказа c активной заявкой")
    @DatabaseSetup("/controller/order/cancel/before/cancelled_with_request_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/before/cancelled_with_request_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancelCancelledWithRequestOrder() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isConflict())
            .andExpect(errorMessage("Active cancellation request restriction. Order id 1."));
    }

    @Test
    @DisplayName("Отмена завершённого заказа")
    @DatabaseSetup("/controller/order/cancel/before/finished_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/before/finished_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void finishedOrderCancel() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonContent("controller/order/cancel/response/finished_cancel.json"));
    }

    @Test
    @DisplayName("Отмена заказа при ошибке обработки заказа")
    @DatabaseSetup("/controller/order/cancel/before/processing_error_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/before/processing_error_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processingErrorOrderCancel() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS,
            createOrderCancellationRequestIdPayload(1L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Отмена заказа, находящегося в обработке")
    @DatabaseSetup("/controller/order/cancel/before/processing_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processingOrderCancel() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS,
            createOrderCancellationRequestIdPayload(1L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Отмена заказа, находящегося в обработке, с указанием причины отмены")
    @DatabaseSetup("/controller/order/cancel/before/processing_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_cancel_with_reason.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processingOrderCancelWithReason() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID, "controller/order/cancel/request/cancel_with_reason.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/cancel/response/cancel_order_created_with_reason.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS,
            createOrderCancellationRequestIdPayload(1L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Отмена заказа с заявкой о товаре, не указанном в поставке")
    @DatabaseSetup("/controller/order/cancel/before/processing_cancel.xml")
    @DatabaseSetup("/controller/order/cancel/before/order_item_is_not_supplied.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_cancel_with_item_is_not_supplied.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelOrderWithOrderItemIsNotSuppliedRequest() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS,
            createOrderCancellationRequestIdPayload(1L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Отмена заказа с обработанной заявкой о товаре, не указанном в поставке")
    @DatabaseSetup("/controller/order/cancel/before/processing_cancel.xml")
    @DatabaseSetup("/controller/order/cancel/before/order_item_is_not_supplied_succeed.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancelOrderWithOrderItemIsNotSuppliedRequestInFinalStatus() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS,
            createOrderCancellationRequestIdPayload(1L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Отмена заказа с юзер-тикетом в хедере и проверить что проставился автор изменений")
    @DatabaseSetup("/controller/order/cancel/before/processing_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processingOrderCancelWithUserTicket() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID, USER_HEADERS)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, null);
    }

    @Test
    @DisplayName("Отмена заказа с сервис-тикетом в хедере и проверить что проставился автор изменений")
    @DatabaseSetup("/controller/order/cancel/before/processing_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processingOrderCancelWithServiceTicket() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID, SERVICE_HEADERS)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, SERVICE_ID);
    }

    @Test
    @DisplayName("Отмена заказа с юзер-тикетом и сервис-тикетом в хедере и проверить что проставился автор изменений")
    @DatabaseSetup("/controller/order/cancel/before/processing_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processingOrderCancelWithUserAndServiceTicket() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID, USER_AND_SERVICE_HEADERS)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, SERVICE_ID);
    }

    @Test
    @DisplayName("Заявка на отмену существует")
    @DatabaseSetup("/controller/order/cancel/after/processing_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processingOrderCancelTwice() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isConflict())
            .andExpect(errorMessage("Active cancellation request restriction. Order id 1."));
    }

    @Test
    @DisplayName("Заявка на отмену существует, но она неактивна")
    @DatabaseSetup("/controller/order/cancel/before/cancel_retry.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancel_retry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processingOrderCancelRetry() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));
    }

    @Test
    @DisplayName("Отмена несуществующего заказа")
    void cancelNonExistOrder() throws Exception {
        OrderTestUtil.cancelOrder(mockMvc, 2L)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/cancel/response/order_not_found.json"));
    }

    @DisplayName(
        "Всякий обрабатываемый запрос на изменение заказа переходит в статус ORDER_CANCELLED при отмене заказа"
    )
    @EnumSource(
        value = ChangeOrderRequestType.class,
        names = {"UPDATE_COURIER", "UPDATE_PLACES"},
        mode = EnumSource.Mode.EXCLUDE
    )
    @ParameterizedTest
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_change_request_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancelOrderWithActiveChangeOrderRequest(ChangeOrderRequestType changeOrderRequestType) throws Exception {
        Order order = validOrder();
        order.setChangeOrderRequests(Set.of(
            new ChangeOrderRequest()
                .setRequestType(changeOrderRequestType)
                .setStatus(ChangeOrderRequestStatus.CREATED)
                .setOrder(order)
        ));
        orderRepository.save(order);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));
    }

    @Test
    @DisplayName("Запрос на изменение заказа, обновляющий курьера, не отклоняется при отмене заказа")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_change_request_retained.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancelOrderWithActiveChangeOrderRequestUpdateCourier() throws Exception {
        Order order = validOrder();
        order.setChangeOrderRequests(Set.of(
            new ChangeOrderRequest()
                .setRequestType(ChangeOrderRequestType.UPDATE_COURIER)
                .setStatus(ChangeOrderRequestStatus.PROCESSING)
                .setOrder(order)
        ));
        orderRepository.save(order);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));
    }

    @Test
    @DisplayName("Запрос на изменение заказа, обновляющий коробки, не отклоняется при отмене заказа")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processing_change_request_retained.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancelOrderWithActiveChangeOrderRequestUpdatePlaces() throws Exception {
        Order order = validOrder();
        order.setChangeOrderRequests(Set.of(
            new ChangeOrderRequest()
                .setRequestType(ChangeOrderRequestType.UPDATE_PLACES)
                .setStatus(ChangeOrderRequestStatus.PROCESSING)
                .setOrder(order)
        ));
        orderRepository.save(order);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));
    }

    @DisplayName("Всякий обработанный запрос на изменение заказа остаётся в терминальном статусе")
    @EnumSource(ChangeOrderRequestType.class)
    @ParameterizedTest
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/processed_change_request_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancelOrderWithProcessedChangeOrderRequest(ChangeOrderRequestType changeOrderRequestType) throws Exception {
        Order order = validOrder();
        order.setChangeOrderRequests(Set.of(
            new ChangeOrderRequest()
                .setRequestType(changeOrderRequestType)
                .setStatus(ChangeOrderRequestStatus.SUCCESS)
                .setOrder(order)
        ));
        orderRepository.save(order);
        OrderTestUtil.cancelOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/response/cancel_order_created.json", "created", "updated"));
    }

    @Nonnull
    private Order validOrder() {
        return new Order()
            .setPlatformClient(PlatformClient.YANDEX_DELIVERY)
            .setStatus(OrderStatus.PROCESSING, clock)
            .setSender(new Sender().setId(1L))
            .setWaybill(List.of(new WaybillSegment().setSegmentType(SegmentType.COURIER)));
    }

    private void tasksNotCreated() {
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_CANCELLED);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }
}
