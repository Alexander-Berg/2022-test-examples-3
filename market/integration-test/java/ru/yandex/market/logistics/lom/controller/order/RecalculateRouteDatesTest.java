package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.persistence.OptimisticLockException;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.dto.RecalculateRouteDatesRequestDto;
import ru.yandex.market.logistics.lom.service.order.OrderService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/order/before/order_with_combined_route.xml")
@DisplayName("Пересчёт дат по сегментам заказа в Комбинаторе")
class RecalculateRouteDatesTest extends AbstractContextualTest {

    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @Autowired
    private FeatureProperties featureProperties;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    public void setUp() {
        clock.setFixed(Instant.parse("2022-03-10T15:00:00.00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    public void tearDown() {
        featureProperties.setEnableLastSegmentRouteRecalculation(false);
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_without_status_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех")
    void success() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates_without_segment_status.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/recalculate_route_dates_without_segment_status.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_without_status_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех при исключении")
    void successWithRetry(String displayName, Exception exception) throws Exception {
        doThrow(exception)
            .doCallRealMethod()
            .when(orderService).createRecalculateRouteDatesChangeRequest(any(), any());
        performRequest("controller/order/request/recalculate_route_dates_without_segment_status.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/recalculate_route_dates_without_segment_status.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Nonnull
    private static Stream<Arguments> successWithRetry() {
        return Stream.of(
            Arguments.of(
                "ObjectOptimisticLockingFailureException",
                new ObjectOptimisticLockingFailureException(
                    "Row was updated or deleted by another transaction",
                    new StaleObjectStateException("Order", 1L)
                )
            ),
            Arguments.of(
                "OptimisticLockException",
                new OptimisticLockException()
            ),
            Arguments.of(
                "StaleStateException",
                new StaleStateException("Row was updated or deleted by another transaction")
            )
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_without_notify_user.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех без указания notifyUser")
    void successWithoutNotifyUser() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates_without_notify_user.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/recalculate_route_dates_without_notify_user.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_without_status_created_2_waybill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех - заявка для заказа есть, но для сегмента нет")
    void successWithRequestForOrder() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates_2_waybill.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/recalculate_route_dates_2.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех со статусом сегмента")
    void successWithStatus() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/recalculate_route_dates.json", "created", "updated"));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DatabaseSetup(
        value = "/controller/order/before/inactive_cancellation_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех с неактивной заявкой на отмену заказа")
    void successOrderHasInactiveCancellationRequest() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/recalculate_route_dates.json", "created", "updated"));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/cp_49_not_last_segment.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех с 49 на первом сегменте последней мили")
    void successOrderHas49CheckpointInNotLastSegment() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/recalculate_route_dates.json", "created", "updated"));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка - сегмент с данным id не существует")
    void failRecalculateRouteDatesNonExistentSegmentId() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates_non_existent_segment_id.json")
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAYBILL_SEGMENT] with id [10]"));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup("/controller/order/before/change_order_request_recalculate_route_dates.xml")
    @ExpectedDatabase(
        value = "/controller/order/before/change_order_request_recalculate_route_dates.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка - заявка на пересчёт уже существует")
    void failRecalculateRouteDatesChangeOrderRequestAlreadyExists() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isConflict())
            .andExpect(errorMessage(
                "Active Change Request with type = RECALCULATE_ROUTE_DATES is already exists "
                    + "for order 1002-LOinttest-2 and segment 1"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/order_lost_status.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка - заказ не в статусе PROCESSING")
    void failOrderNotProcessing() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for order 1001 in status LOST"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/cancellation_request.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка - есть заявки на отмену заказа")
    void failOrderHasCancellationRequest() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for cancelled order 1001"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/cp_45.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/order/before/post_order.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка - есть 45 чп для почты")
    void failOrderHas45CheckpointPost() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for order 1001. "
                    + "TRANSIT_PICKUP checkpoint already received"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/cp_45.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/order/before/pickup_order.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка - есть 45 чп для пвз")
    void failOrderHas45CheckpointPickup() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for order 1001. "
                    + "TRANSIT_PICKUP checkpoint already received"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/cp_49.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка - есть 49 чп для курьера")
    void failOrderHas49Checkpoint() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for order 1001. "
                    + "Order already transmitted to recipient"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/cp_50.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка - есть 50 чп для курьера")
    void failOrderHas50Checkpoint() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for order 1001. "
                    + "Order already transmitted to recipient"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/cp_110_before.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка - следующему сегменту пришел 110 раньше")
    void failNextSegmentHas110CheckpointBefore() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for order 1001. "
                    + "Next segment 2 after segment 1 received IN checkpoint on 2020-11-01T12:00:00Z"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/cp_110_after.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_without_status_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех - следующему сегменту пришел 110 позже")
    void failNextSegmentHas110CheckpointAfter() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates_without_segment_status.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/recalculate_route_dates_without_segment_status.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка - опоздавший IN на DELIVERY сегменте после не DROPSHIP сегмента")
    void failLateInOnDeliveryAfterNonDropship() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates_2_waybill_in.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for order 1001. "
                    + "Late IN on DELIVERY partner segment after FULFILLMENT partner segment"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/update_order_dropship.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_without_status_created_2_waybill_in.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех - опоздавший IN на DELIVERY сегменте после DROPSHIP сегмента")
    void successLateInOnDeliveryAfterDropship() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates_2_waybill_in.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/recalculate_route_dates_without_segment_status_segment_2.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/update_order_yandex_go_shop.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_without_status_created_2_waybill_in.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех - опоздавший IN на DELIVERY сегменте после YANDEX_GO_SHOP сегмента")
    void successLateInOnDeliveryAfterYandexGoShop() throws Exception {
        performRequest("controller/order/request/recalculate_route_dates_2_waybill_in.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/recalculate_route_dates_without_segment_status_segment_2.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DisplayName("Валидация тела запроса RecalculateRouteDatesRequestDto")
    void requestDtoValidation() throws Exception {
        mockMvc
            .perform(request(
                HttpMethod.PUT,
                "/orders/recalculateRouteDates",
                RecalculateRouteDatesRequestDto.builder().build()
            ))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Following validation errors occurred:\n" +
                    "Field: 'segmentId', message: 'must not be null'\n" +
                    "Field: 'segmentStatus', message: 'must not be null'\n" +
                    "Field: 'startDateTime', message: 'must not be null'"
            ));
    }
    @Test
    @DisplayName("preDeliveryRddRecalculation, IN - последний чекпоинт")
    @DatabaseSetup(value = "/controller/order/before/cp_110_first_segment.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/pre_delivery_success_last_in.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void preDeliverySuccessLastIn() throws Exception {
        performPreDeliveryRequest("controller/order/request/pre_delivery_success.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/pre_delivery_success_last_in.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DisplayName("preDeliveryRddRecalculation, IN - последний чекпоинт текущее время -1ч меньше времени чп")
    @DatabaseSetup(value = "/controller/order/before/cp_110_first_segment.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/pre_delivery_success_last_in_current_time_lt_110_date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void preDeliverySuccessLastInCurrentTimeLessThan110Time() throws Exception {
        clock.setFixed(Instant.parse("2020-11-01T12:10:00.00Z"), ZoneId.systemDefault());
        performPreDeliveryRequest("controller/order/request/pre_delivery_success.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/pre_delivery_success_last_in_current_time_lt_110_date.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DisplayName("preDeliveryRddRecalculation, OUT - последний чекпоинт")
    @DatabaseSetup(value = "/controller/order/before/cp_50_second_segment.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/pre_delivery_success_last_out.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void preDeliverySuccess() throws Exception {
        performPreDeliveryRequest("controller/order/request/pre_delivery_success.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/pre_delivery_success_last_out.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DisplayName("preDeliveryRddRecalculation нет ни одного in или out")
    @ExpectedDatabase(
        value = "/controller/order/after/pre_delivery_success_no_in_out.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void preDeliverySuccessNoInOut() throws Exception {
        performPreDeliveryRequest("controller/order/request/pre_delivery_success.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/pre_delivery_success_no_in_out.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DisplayName("preDeliveryRddRecalculation успех если последний чп - IN на DELIVERY сегменте")
    @DatabaseSetup(value = "/controller/order/before/cp_110_before.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/pre_delivery_in_on_delivery_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void preDeliveryInOnDeliverySuccess() throws Exception {
        performPreDeliveryRequest("controller/order/request/pre_delivery_success.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/pre_delivery_in_on_delivery_success.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DisplayName("preDeliveryRddRecalculation успех, заказ с МК сегментом, но заказ еще не доехал до него")
    @DatabaseSetup(
        value = {
            "/controller/order/before/market_courier_and_pickup_segments.xml",
            "/controller/order/before/cp_50_second_segment.xml"
        },
        type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/order/after/pre_delivery_success_last_out.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void preDeliverySuccessOrderWithMcSegment() throws Exception {
        performPreDeliveryRequest("controller/order/request/pre_delivery_success.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/pre_delivery_success_last_out.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/cp_110_last_segment.xml", type = DatabaseOperation.INSERT)
    @DisplayName("Ошибка - пересчет на последнем сегменте с отключенной настройкой")
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failCorOnLastSegment() throws Exception {
        performPreDeliveryRequest("controller/order/request/pre_delivery_success.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for order 1001."
                    + " Order already on last segment."
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(value = "/controller/order/before/cp_110_last_segment.xml", type = DatabaseOperation.INSERT)
    @DisplayName("Успешный пересчет на последнем сегменте с включенной настройкой")
    @ExpectedDatabase(
        value = "/controller/order/after/pre_delivery_in_on_last_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successCorOnLastSegment() throws Exception {
        featureProperties.setEnableLastSegmentRouteRecalculation(true);
        performPreDeliveryRequest("controller/order/request/pre_delivery_success.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/pre_delivery_in_on_last_success.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/controller/order/before/market_courier_and_pickup_segments.xml",
            "/controller/order/before/cp_110_market_courier_segment.xml"
        },
        type = DatabaseOperation.INSERT)
    @DisplayName("Ошибка - заказ уже доехал до маркет курьерки")
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failOrderOnMc() throws Exception {
        performPreDeliveryRequest("controller/order/request/pre_delivery_success.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for order 1001."
                    + " Mc segment has IN status."
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(
        value = {
            "/controller/order/before/market_courier_and_pickup_segments.xml",
            "/controller/order/before/cp_50_third_segment.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/order/before/update_order_sc_and_sc_mk_segments.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Ошибка - заказ уже отгружен с СЦ перед СЦ МК")
    @ExpectedDatabase(
        value = "/controller/order/after/recalculate_route_dates_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failOrderOnMcOrderHasOutOnScBeforeScMk() throws Exception {
        performPreDeliveryRequest("controller/order/request/pre_delivery_success.json")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to create RECALCULATE_ROUTE_DATES change order request for order 1001."
                    + " Sc segment before sc mk has OUT status."
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Nonnull
    private ResultActions performRequest(@Nonnull String expectedRequestBodyFilePath) throws Exception {
        return mockMvc.perform(
            put("/orders/recalculateRouteDates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(expectedRequestBodyFilePath))
        );
    }

    @Nonnull
    private ResultActions performPreDeliveryRequest(@Nonnull String expectedRequestBodyFilePath) throws Exception {
        return mockMvc.perform(
            put("/orders/preDeliveryRddRecalculation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(expectedRequestBodyFilePath))
        );
    }
}
