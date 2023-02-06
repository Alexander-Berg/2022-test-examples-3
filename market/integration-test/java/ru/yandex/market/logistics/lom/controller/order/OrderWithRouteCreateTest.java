package ru.yandex.market.logistics.lom.controller.order;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.yandex.ydb.table.query.DataQueryResult;
import com.yandex.ydb.table.result.ResultSetReader;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.converter.ydb.OrderCombinedRouteHistoryConverter;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.jobs.consumer.CommitOrderConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.ConvertRouteToWaybillConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderValidationErrorPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.TestUtils;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.YdbTemplate;
import ru.yandex.market.ydb.integration.query.QFrom;
import ru.yandex.market.ydb.integration.query.QSelect;
import ru.yandex.market.ydb.integration.query.YdbSelect;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание заказа с маршрутом Комбинатора")
class OrderWithRouteCreateTest extends AbstractContextualYdbTest {
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private ConvertRouteToWaybillConsumer convertRouteToWaybillConsumer;

    @Autowired
    private CommitOrderConsumer commitOrderConsumer;

    @Autowired
    private TvmClientApi tvmClientApi;

    @Autowired
    private OrderCombinedRouteHistoryTableDescription routeHistoryTable;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription businessProcessStateStatusHistoryTable;

    @Autowired
    private OrderCombinedRouteHistoryConverter routeHistoryConverter;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable, businessProcessStateStatusHistoryTable);
    }

    @BeforeEach
    void setup() {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        clock.setFixed(Instant.parse("2019-01-01T00:00:00.00Z"), ZoneId.systemDefault());
        when(lmsClient.searchPartners(any(SearchPartnerFilter.class))).thenReturn(List.of(
            PartnerResponse.newBuilder().id(47802).partnerType(PartnerType.DROPSHIP).build(),
            PartnerResponse.newBuilder().id(100136).partnerType(PartnerType.SORTING_CENTER).build(),
            PartnerResponse.newBuilder().id(1003937).partnerType(PartnerType.DELIVERY).build()
        ));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Создать заказ с маршрутом с неправильными storage-unit'ами")
    void createOrderWithInvalidStorageUnits() throws Exception {
        doCreateOrder("controller/order/request/create_order_with_route_invalid_storage_units.json")
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "[FieldError(propertyPath=units, message=all used in boxes units must be declared)]"
            ));
    }

    @Test
    @DisplayName("Создать заказ с маршрутом с promise")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderWithPromise() throws Exception {
        createOrderAndAutoCommit(
            "controller/order/request/create_order_with_route_with_promise.json",
            "controller/order/response/create_order_with_route_with_promise_response.json"
        );
        assertYdbContainsRouteWithIds(List.of(createCombinedRoute(
            "controller/order/combined/combined_route_for_order_with_promise.json"
        )));
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of(47802L)));
    }

    @Test
    @DisplayName("Создать заказ с маршрутом и закоммитить его")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/waybill_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderWithAutoCommit() throws Exception {
        createOrderAndAutoCommit(
            "controller/order/request/create_order_with_route.json",
            "controller/order/response/create_order_with_route_with_auto_commit.json"
        );
        assertYdbContainsRouteWithIds(List.of(createCombinedRoute("controller/order/combined/combined_route.json")));
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of(47802L)));
    }

    @Test
    @DisplayName("Создать заказ с маршрутом и закоммитить его, берём поля из маршрута")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/waybill_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderWithAutoCommitWithCombinatorRouteFields() throws Exception {
        createOrderAndAutoCommit(
            "controller/order/request/create_order_with_combinator_route_fields.json",
            "controller/order/response/create_order_with_combinator_route_fields.json",
            "controller/order/response/create_order_with_route_with_auto_commit_with_combinator_route_fields.json"
        );
        assertYdbContainsRouteWithIds(List.of(
            createCombinedRoute("controller/order/combined/combined_route_with_combinator_route_fields.json")
        ));
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of(47802L)));
    }

    @Test
    @DisplayName("Создать заказ с маршрутом без tariffId в заказе и закоммитить его")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/waybill_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderWithoutTariffIdInRequest() throws Exception {
        createOrderAndAutoCommit(
            "controller/order/request/create_order_with_route_without_tariff_id.json",
            "controller/order/response/create_order_with_route_without_tariff_id.json",
            "controller/order/response/create_order_with_route_with_auto_commit_without_tariff_id.json"
        );
        assertYdbContainsRouteWithIds(List.of(createCombinedRoute("controller/order/combined/combined_route.json")));
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of(47802L)));
    }

    @Test
    @DisplayName("Создать заказ без marketId с маршрутом и закоммитить его")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/waybill_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderWithAutoCommitMarketIdIsNull() throws Exception {
        createOrderAndAutoCommit(
            "controller/order/request/create_order_with_route_market_id_is_null.json",
            "controller/order/response/create_order_with_route_without_market_id.json",
            "controller/order/response/create_order_with_route_without_market_id_after_commit.json"
        );
        assertYdbContainsRouteWithIds(List.of(createCombinedRoute("controller/order/combined/combined_route.json")));
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of(47802L)));
    }

    @Test
    @DisplayName("Создать заказ с маршрутом с вызовом курьера в HANDING и закоммитить его")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/courier_waybill_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderWithCallCourierHanding() throws Exception {
        createOrderAndAutoCommit(
            "controller/order/request/create_order_with_route_with_call_courier.json",
            "controller/order/response/create_order_with_route_with_call_courier.json"
        );

        assertYdbContainsRouteWithIds(List.of(createCombinedRoute(
            "controller/order/combined/combined_route_call_courier.json"
        )));
        OrderHistoryTestUtil.assertOrderHistoryEventCount(jdbcTemplate, 1L, 3);
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            2L,
            "controller/order/combined/order_courier_time_diff.json",
            JSONCompareMode.LENIENT
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of(47802L)));
    }

    @Test
    @DisplayName("Создать заказ с маршрутом с вызовом курьера в MOVEMENT и закоммитить его")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/courier_movement_waybill_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderWithCallCourierMovement() throws Exception {
        createOrderAndAutoCommit(
            "controller/order/request/create_order_with_route_with_call_courier_movement.json",
            "controller/order/response/create_order_with_route_with_call_courier_movement.json"
        );

        assertYdbContainsRouteWithIds(List.of(createCombinedRoute(
            "controller/order/combined/combined_route_call_courier_movement.json"
        )));
        OrderHistoryTestUtil.assertOrderHistoryEventCount(jdbcTemplate, 1L, 3);
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            2L,
            "controller/order/combined/order_courier_time_diff_movement.json",
            JSONCompareMode.LENIENT
        );
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of(47802L)));
    }

    @Test
    @DisplayName("Заказ экспресса с широкими интервалами и сдвигом времени отгрузки")
    @DatabaseSetup("/controller/commit/before/express_time_shift_variable.xml")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/courier_movement_batch_waybill_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createBatchExpressOrderWithCallCourierMovementAndShipmentTimeShift() throws Exception {
        createOrderAndAutoCommit(
            "controller/order/request/create_order_with_route_with_batch_call_courier_movement.json",
            "controller/order/response/create_order_with_route_with_batch_call_courier_movement.json"
        );

        assertYdbContainsRouteWithIds(List.of(createCombinedRoute(
            "controller/order/combined/combined_route_batch_call_courier_movement.json"
        )));
        OrderHistoryTestUtil.assertOrderHistoryEventCount(jdbcTemplate, 1L, 3);
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            2L,
            "controller/order/combined/order_courier_time_batch_diff_movement.json",
            JSONCompareMode.LENIENT
        );
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of(47802L)));
    }

    @Test
    @DisplayName("Создать заказ с маршрутом с вызовом курьера в MOVEMENT и наличием PICKUP и закоммитить его")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/courier_movement_pickup_waybill_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderWithCallCourierMovementPickup() throws Exception {
        createOrderAndAutoCommit(
            "controller/order/request/create_order_with_route_with_call_courier_movement_pickup.json",
            "controller/order/response/create_order_with_route_with_call_courier_movement_pickup.json"
        );

        assertYdbContainsRouteWithIds(List.of(createCombinedRoute(
            "controller/order/combined/combined_route_call_courier_movement_pickup.json"
        )));
        OrderHistoryTestUtil.assertOrderHistoryEventCount(jdbcTemplate, 1L, 3);
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            2L,
            "controller/order/combined/order_courier_time_diff_movement_pickup.json",
            JSONCompareMode.LENIENT
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of(47802L)));
    }

    @Test
    @DisplayName("Создать заказ с маршрутом с дропофовым returnSortingCenterId и закоммитить его")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderWithReturnSortingCenterIdAndAutoCommit() throws Exception {
        createOrderAndAutoCommit(
            "controller/order/request/create_order_with_route_with_return_center_id.json",
            "controller/order/response/create_order_with_route_with_auto_commit_with_custom_return_center_id.json"
        );

        assertYdbContainsRouteWithIds(List.of(createCombinedRoute(
            "controller/order/combined/combined_route_with_return_center_id.json"
        )));

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of(101366L, 1005526L, 47802L, 1006512L)));
    }

    @Test
    @DisplayName("Создать заказ с маршрутом и получить ошибку валидации")
    void createOrderWithCommitConstraintViolation() throws Exception {
        doCreateOrder("controller/order/request/create_order_with_route_commit_error.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/response/create_order_with_route_commit_error.json"));
    }

    @Test
    @DisplayName("Создать заказ с маршрутом, у которого вершина графа типа warehouse не является партнером со складом")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks_validation_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/business_process_state_route_does_not_match_any_pattern.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createRouteOrderInappropriateWarehousePartner() throws Exception {
        when(lmsClient.searchPartners(any(SearchPartnerFilter.class))).thenReturn(List.of(
            PartnerResponse.newBuilder().id(47802).partnerType(PartnerType.OWN_DELIVERY).build(),
            PartnerResponse.newBuilder().id(1003937).partnerType(PartnerType.DELIVERY).build()
        ));
        ResultActions result = doCreateOrder(
            "controller/order/request/create_order_with_route_inappropriate_warehouse_partner.json"
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_with_route.json",
                "created",
                "updated",
                "routeUuid"
            ));
        clock.setFixed(Instant.parse("2019-01-02T00:00:00.00Z"), ZoneId.systemDefault());
        executeConvertConsumer();
        doGetOrder(1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_with_route_that_does_not_match_any_pattern.json",
                "created",
                "updated",
                "routeUuid"
            ));

        assertYdbContainsRouteWithIds(List.of(createCombinedRoute(
            "controller/order/combined/combined_route_inappropriate_warehouse_partner.json"
        )));
        queueTaskChecker.assertQueueTaskCreated(QueueType.CONVERT_ROUTE_TO_WAYBILL, getConvertPayload());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.NOTIFY_ORDER_VALIDATION_ERROR,
            getNotifyValidationErrorPayload(getCreated(result))
        );
    }

    @Test
    @DisplayName("Нет подходящего сегмента для обработки возврата")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks_validation_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/business_process_state_route_no_relevant_partner_to_return.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void noRelevantPartnerToReturn() throws Exception {
        when(lmsClient.searchPartners(any(SearchPartnerFilter.class))).thenReturn(List.of(
            PartnerResponse.newBuilder().id(47802).partnerType(PartnerType.SUPPLIER).build(),
            PartnerResponse.newBuilder().id(1003937).partnerType(PartnerType.DELIVERY).build(),
            PartnerResponse.newBuilder().id(1003562).partnerType(PartnerType.DELIVERY).build()
        ));
        ResultActions result = doCreateOrder("controller/order/request/route_supplier_postamat.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_with_route.json",
                "created",
                "updated",
                "routeUuid"
            ));
        clock.setFixed(Instant.parse("2019-01-02T00:00:00.00Z"), ZoneId.systemDefault());
        executeConvertConsumer();
        doGetOrder(1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_with_route_no_relevant_partner_to_return.json",
                "created",
                "updated",
                "routeUuid"
            ));

        assertYdbContainsRouteWithIds(List.of(createCombinedRoute(
            "controller/order/combined/combined_route_supplier_postamat.json"
        )));
        queueTaskChecker.assertQueueTaskCreated(QueueType.CONVERT_ROUTE_TO_WAYBILL, getConvertPayload());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.NOTIFY_ORDER_VALIDATION_ERROR,
            getNotifyValidationErrorPayload(getCreated(result))
        );
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(Set.of()));
    }

    @Test
    @DisplayName("Нет активного склада для возвратного партнера")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks_validation_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/business_process_state_route_no_active_warehouses_for_return_partner.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void noActiveWarehousesForReturnPartner() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(Set.of(47802L));
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            PartnerRelationEntityDto.newBuilder()
                .fromPartnerId(47802L)
                .toPartnerId(1003937L)
                .returnPartnerId(48103L)
                .build()
        ));
        ResultActions result = doCreateOrder("controller/order/request/create_order_with_route.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_with_route.json",
                "created",
                "updated",
                "routeUuid"
            ));
        clock.setFixed(Instant.parse("2019-01-02T00:00:00.00Z"), ZoneId.systemDefault());
        executeConvertConsumer();
        doGetOrder(1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_with_route_no_active_warehouses_for_return_partner.json",
                "created",
                "updated",
                "routeUuid"
            ));

        assertYdbContainsRouteWithIds(List.of(createCombinedRoute(
            "controller/order/combined/combined_route.json"
        )));
        queueTaskChecker.assertQueueTaskCreated(QueueType.CONVERT_ROUTE_TO_WAYBILL, getConvertPayload());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.NOTIFY_ORDER_VALIDATION_ERROR,
            getNotifyValidationErrorPayload(getCreated(result))
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).searchPartners(SearchPartnerFilter.builder().setIds(Set.of(48103L)).build());
        verify(lmsClient).getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(48103L))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        );
    }

    @Test
    @DisplayName("Дубль externalId отправителя для Beru заказа")
    @DatabaseSetup("/controller/order/before/order_duplicate_check.xml")
    @DatabaseSetup(
        value = "/controller/order/before/beru_order.xml",
        type = DatabaseOperation.REFRESH
    )
    void createOrderDraftSenderExternalIdDuplicateBeru() throws Exception {
        doCreateOrder("controller/order/request/create_beru_order_route.json")
            .andExpect(status().isAlreadyReported())
            .andExpect(jsonContent(
                "controller/order/response/beru_order_duplicate_check.json",
                "created",
                "updated"
            ));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Дубль externalId отправителя для Yandex Go заказа")
    @DatabaseSetup("/controller/order/before/order_duplicate_check.xml")
    void createOrderDraftSenderExternalIdDuplicateYandexGo() throws Exception {
        doCreateOrder("controller/order/request/create_yandex_go_order_route.json")
            .andExpect(status().isAlreadyReported())
            .andExpect(jsonContent(
                "controller/order/response/yandex_go_order_duplicate_check.json",
                "created",
                "updated"
            ));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Дубль externalId отправителя для DBS заказа")
    @DatabaseSetup("/controller/order/before/order_duplicate_check.xml")
    void createOrderDraftSenderExternalIdDuplicateDBS() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_dbs_order.json")
            .andExpect(status().isAlreadyReported())
            .andExpect(jsonContent(
                "controller/order/response/dbs_order_duplicate_check.json",
                "created",
                "updated"
            ));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Дубль externalId отправителя для FaaS заказа")
    @DatabaseSetup("/controller/order/before/order_duplicate_check.xml")
    void createOrderDraftSenderExternalIdDuplicateFaaS() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_faas_order.json")
            .andExpect(status().isAlreadyReported())
            .andExpect(jsonContent(
                "controller/order/response/faas_order_duplicate_check.json",
                "created",
                "updated"
            ));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    private void createOrderAndAutoCommit(String requestPath, String afterCommitResponsePath) throws Exception {
        createOrderAndAutoCommit(
            requestPath,
            "controller/order/response/create_order_with_route.json",
            afterCommitResponsePath
        );
    }

    private void createOrderAndAutoCommit(
        String requestPath,
        String creationResponsePath,
        String afterCommitResponsePath
    ) throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(Set.of(47802L));
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            PartnerRelationEntityDto.newBuilder()
                .fromPartnerId(47802L)
                .toPartnerId(1003937L)
                .returnPartnerId(47802L)
                .build()
        ));
        doCreateOrder(requestPath)
            .andExpect(status().isOk())
            .andExpect(jsonContent(creationResponsePath, "created", "updated", "routeUuid"));
        executeConvertConsumer();
        executeCommitConsumer();
        doGetOrder(1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                afterCommitResponsePath,
                JSONCompareMode.NON_EXTENSIBLE,
                "created",
                "updated",
                "routeUuid"
            ));
    }

    @Nonnull
    private ResultActions doCreateOrder(String requestPath) throws Exception {
        return mockMvc.perform(
            post("/orders/with-route")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(TestUtils.toHttpHeaders(SERVICE_HEADERS))
                .content(extractFileContent(requestPath))
        );
    }

    @Nonnull
    private ResultActions doGetOrder(long id) throws Exception {
        return mockMvc.perform(get("/orders/{id}", id));
    }

    private void executeCommitConsumer() {
        commitOrderConsumer.execute(TaskFactory.createTask(
            queueTaskChecker.getProducedTaskPayload(QueueType.COMMIT_ORDER, OrderIdAuthorPayload.class)
        ));
    }

    private void executeConvertConsumer() {
        convertRouteToWaybillConsumer.execute(TaskFactory.createTask(
            queueTaskChecker.getProducedTaskPayload(QueueType.CONVERT_ROUTE_TO_WAYBILL, OrderIdAuthorPayload.class)
        ));
    }

    @Nonnull
    @SneakyThrows
    private Instant getCreated(ResultActions result) {
        byte[] response = result.andReturn().getResponse().getContentAsByteArray();
        OrderDto order = objectMapper.readValue(response, OrderDto.class);
        return order.getCreated();
    }

    @Nonnull
    private ExecutionQueueItemPayload getConvertPayload() {
        return new OrderIdAuthorPayload(
            "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
            1,
            new OrderHistoryEventAuthor().setTvmServiceId(222L)
        )
            .setSequenceId(1L);
    }

    @Nonnull
    private ExecutionQueueItemPayload getNotifyValidationErrorPayload(Instant created) {
        return new OrderValidationErrorPayload(
            "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1/1",
            1,
            1,
            "ext-id",
            created
        )
            .setSequenceId(2L);
    }

    private void assertYdbContainsRouteWithIds(List<CombinedRoute> routes) {
        List<JsonNode> routeNodes = routes.stream().map(CombinedRoute::getSourceRoute).collect(Collectors.toList());
        softly.assertThat(routeNodes).isEqualTo(getAllHistoryRoutesFromYdb());
    }

    @Nonnull
    private CombinedRoute createCombinedRoute(String file) throws IOException {
        return new CombinedRoute()
            .setOrderId(1L)
            .setSourceRoute(objectMapper.readTree(extractFileContent(file)));
    }

    @Nonnull
    private List<JsonNode> getAllHistoryRoutesFromYdb() {
        return ydbTemplate.selectList(
            YdbSelect.select(
                    QSelect.of(routeHistoryTable.fields())
                        .from(QFrom.table(routeHistoryTable))
                        .select()
                )
                .toQuery(),
            YdbTemplate.DEFAULT_READ,
            this::convertHistoryList
        );
    }

    @Nonnull
    private List<JsonNode> convertHistoryList(DataQueryResult dataQueryResult) {
        if (dataQueryResult.isEmpty()) {
            return List.of();
        }
        ResultSetReader resultSetReader = dataQueryResult.getResultSet(0);
        List<JsonNode> routes = new ArrayList<>(resultSetReader.getRowCount());
        while (resultSetReader.next()) {
            routes.add(routeHistoryConverter.readRoute(resultSetReader));
        }
        return routes;
    }

    @Nonnull
    private PartnerRelationFilter partnerRelationFilter(Set<Long> longs) {
        return PartnerRelationFilter.newBuilder()
            .fromPartnersIds(longs)
            .build();
    }
}
