package ru.yandex.market.logistics.lom.controller.order.combined;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil;
import ru.yandex.market.logistics.lom.entity.OrderHistoryEvent;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.jobs.consumer.CommitOrderConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.ConvertRouteToWaybillConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderValidationErrorPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.logging.enums.CombinedRouteEventCode;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.RouteOrderRequestDto;
import ru.yandex.market.logistics.lom.repository.OrderHistoryEventRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.TestUtils;
import ru.yandex.market.logistics.lom.utils.UuidGenerator;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Оформление заказа с комбинированным маршрутом")
class OrderCommitCombinedRouteTest extends AbstractContextualYdbTest {

    private static final long FF_PARTNER_ID = 172;
    private static final long MK_PARTNER_ID = 83732L;
    private static final String ROUTE_UUID = "c3cc16b0-d081-4e10-83c7-a18827dced7a";

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
    private UuidGenerator uuidGenerator;

    @Autowired
    private OrderHistoryEventRepository orderHistoryEventRepository;

    @BeforeEach
    void setUp() {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        clock.setFixed(Instant.parse("2019-06-01T12:00:00.00Z"), ZoneOffset.UTC);
        when(uuidGenerator.randomUuid()).thenReturn(UUID.fromString(ROUTE_UUID));
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable, businessProcessStateStatusHistoryTable);
    }

    @AfterEach
    void after() {
        verifyNoMoreInteractions(lmsClient);
        verifyFirstEventHasRouteUuid();
    }

    @Test
    @DisplayName("Заказ без рутового сегмента")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks_validation_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/business_process_state_no_root_unit.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderWithCombinedRouteNoRootUnit() throws Exception {
        ResultActions result = createAndCommitOrder(order(
            "controller/commit/request/no_root_unit_order.json",
            "controller/commit/request/route_warehouse_movement.json"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value("DRAFT"));

        clock.setFixed(Instant.parse("2019-06-02T12:00:00.00Z"), ZoneOffset.UTC);
        executeConvertConsumer();

        getOrder(1)
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("validationError").value("Order 1 doesn't contain ROOT unit."));

        queueTaskChecker.assertQueueTaskCreated(QueueType.CONVERT_ROUTE_TO_WAYBILL, getConvertPayload());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.NOTIFY_ORDER_VALIDATION_ERROR,
            getNotifyValidationErrorPayload(getCreated(result))
        );
    }

    @Test
    @DisplayName("Брать идентификатор ПВЗ из последнего сегмента маршрута, а не из заказа")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_get_pickup_point_id_from_route.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void getPickupPointIdFromRouteRatherThanFromOrder() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(145L);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(145, 1005372, FF_PARTNER_ID),
            partnerRelation(145, 1003562, FF_PARTNER_ID)
        ));
        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(FF_PARTNER_ID))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(10000004403L)
                .partnerId(FF_PARTNER_ID)
                .build()
        ));
        SearchPartnerFilter searchPartnerFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID))
            .build();
        when(lmsClient.searchPartners(searchPartnerFilter)).thenReturn(List.of(
            partner(FF_PARTNER_ID, PartnerType.FULFILLMENT)
        ));

        createAndCommitOrderSuccess(
            pickupOrder("controller/commit/request/route_get_pickup_point_id_from_route.json", false)
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        verify(lmsClient).searchPartners(searchPartnerFilter);
    }

    @Test
    @DisplayName("Маршрут не подходит ни под один паттерн")
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks_validation_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/business_process_state_route_does_not_match_any_pattern_2.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void routeDoesNotMatchPattern() throws Exception {
        String errorMessage = "Route of order id = 1 does not match any pattern. " +
            "Point types [WAREHOUSE, MOVEMENT, WAREHOUSE, MOVEMENT, LINEHAUL, HANDING], " +
            "partner ids [1003937, 1003937, 1003937, 1003937, 1003937, 1003937], " +
            "partner types [DELIVERY, DELIVERY, DELIVERY, DELIVERY, DELIVERY, DELIVERY]";

        ResultActions result = createAndCommitOrder(
            courierOrder("controller/commit/request/route_does_not_match_pattern.json")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value("DRAFT"));

        clock.setFixed(Instant.parse("2019-06-02T12:00:00.00Z"), ZoneOffset.UTC);
        executeConvertConsumer();

        getOrder(1)
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("validationError").value(errorMessage));

        assertEvent(
            "ERROR",
            CombinedRouteEventCode.COMBINED_ROUTE_DOES_NOT_MATCH_ANY_PATTERN,
            errorMessage,
            "order"
        );

        queueTaskChecker.assertQueueTaskCreated(QueueType.CONVERT_ROUTE_TO_WAYBILL, getConvertPayload());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.NOTIFY_ORDER_VALIDATION_ERROR,
            getNotifyValidationErrorPayload(getCreated(result))
        );
    }

    @Test
    @DisplayName("Заказ с забором из Фулмиллмент-центра в Службу доставки")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_fulfillment_to_delivery_service.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fulfillment() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(145L);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(145, 1003937, FF_PARTNER_ID)
        ));
        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(FF_PARTNER_ID))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(10000004403L)
                .partnerId(FF_PARTNER_ID)
                .build()
        ));
        SearchPartnerFilter searchPartnerFilter2 = SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID))
            .build();
        when(lmsClient.searchPartners(searchPartnerFilter2)).thenReturn(List.of(
            partner(FF_PARTNER_ID, PartnerType.FULFILLMENT)
        ));

        // забор с Фулфиллмента 1591903800 2020-06-11T22:30:00+03:00,
        // но есть сдвиг shipment_date_offsets -1 день, поэтому 2020-06-11T22:30:00+03:00
        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_fulfillment_to_delivery_service.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        verify(lmsClient).searchPartners(searchPartnerFilter2);
    }

    @Test
    @DisplayName("Заказ с забором из Дропшипа в Службу доставки")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dropship_to_delivery_service.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void dropship() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(47802L);

        // забор от Дропшипа 1591903800 2020-06-11T22:30:00+03:00
        createAndCommitOrderSuccess(courierOrder("controller/commit/request/route_dropship_to_delivery_service.json"));

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Сегменты обогащаются типами партнеров")
    @ExpectedDatabase(
        value = "/controller/commit/after/enrich_partner_types.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void enrichPartnerTypes() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(47802L);

        // забор от Дропшипа 1591903800 2020-06-11T22:30:00+03:00
        createAndCommitOrderSuccess(courierOrder("controller/commit/request/route_dropship_to_delivery_service.json"));

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ с забором из Фулфиллмент-центра и доставкой в постамат другой Службы доставки")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_parcel_locker.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void parcelLocker() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(145L);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(145, 1003562, FF_PARTNER_ID),
            partnerRelation(145, 1005372, FF_PARTNER_ID)
        ));
        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(FF_PARTNER_ID))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(10000004403L)
                .partnerId(FF_PARTNER_ID)
                .build()
        ));
        SearchPartnerFilter searchPartnerFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID))
            .build();
        when(lmsClient.searchPartners(searchPartnerFilter)).thenReturn(List.of(
            partner(FF_PARTNER_ID, PartnerType.FULFILLMENT)
        ));

        // забор с Фулфиллмента 1594951200 2020-07-17T05:00:00+03:00
        createAndCommitOrderSuccess(
            pickupOrder("controller/commit/request/route_fulfillment_parcel_locker.json", false)
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        verify(lmsClient).searchPartners(searchPartnerFilter);
    }

    @Test
    @DisplayName("Заказ от Кроссдок-партнера через Фулфиллмент-центр и доставкой в постамат другой Службы доставки")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_from_supplier_through_fulfillment_to_parcel_locker.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fromSupplierThroughFulfillmentToParcelLocker() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(FF_PARTNER_ID);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(FF_PARTNER_ID, 107, FF_PARTNER_ID)
        ));

        // забор с Фулфиллмента 1597366800 2020-08-14T04:00:00+03:00
        createAndCommitOrderSuccess(pickupOrder(
            "controller/commit/request/route_from_supplier_through_fulfillment_to_parcel_locker.json",
            false
        ));

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ с забором из Дропшипа в Сортировочный центр")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_movement_warehouse.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void withdrawDropshipThroughSortingCenter() throws Exception {
        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(47802L, 100136L))
            .build();

        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(47802, 100136, 47802),
            partnerRelation(100136, 1003937, 48103)
        ));

        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(48103L))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(10000881933L)
                .partnerId(48103L)
                .build()
        ));
        SearchPartnerFilter searchPartnerFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(48103L))
            .build();
        when(lmsClient.searchPartners(searchPartnerFilter)).thenReturn(List.of(
            partner(48103, PartnerType.SORTING_CENTER)
        ));

        // забор от Дропшипа 1591819200 2020-06-10T23:00:00+03:00
        // забор с Сорт.центра 1591903800 2020-06-11T22:30:00+03:00
        createAndCommitOrderSuccess(courierOrder("controller/commit/request/route_movement_warehouse.json"));

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        verify(lmsClient).searchPartners(searchPartnerFilter);
    }

    @Test
    @DisplayName("Заказ с забором от Кроссдок-партнера в Фулфиллмент-центр")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_withdraw_supplier_through_fulfillment.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void withdrawSupplierThroughFulfillment() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(FF_PARTNER_ID);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(FF_PARTNER_ID, 48, FF_PARTNER_ID)
        ));

        // забор с Фулфиллмента 1596823200 2020-08-07T21:00:00+03:00
        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_withdraw_supplier_through_fulfillment.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ с самопривозом из Дропшипа в Сортировочный центр")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_warehouse_movement.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void importDropshipThroughSortingCenter() throws Exception {
        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(47802L, 100136L))
            .build();

        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(47802, 100136, 47802),
            partnerRelation(100136, 1003937, 48103)
        ));

        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(48103L))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(10000881933L)
                .partnerId(48103L)
                .build()
        ));
        SearchPartnerFilter searchPartnerFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(48103L))
            .build();
        when(lmsClient.searchPartners(searchPartnerFilter)).thenReturn(List.of(
            partner(48103, PartnerType.SORTING_CENTER)
        ));

        // самопривоз от Дропшипа 1591848000 2020-06-11T07:00:00+03:00
        // забор с Сорт.центра 1591903800 2020-06-11T22:30:00+03:00
        createAndCommitOrderSuccess(courierOrder("controller/commit/request/route_warehouse_movement.json"));

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        verify(lmsClient).searchPartners(searchPartnerFilter);
    }

    @Test
    @DisplayName("Заказ с самопривозом из Дропшипа в Службу доставки")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_import_dropship_to_delivery_service.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void importDropshipToDeliveryService() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(52906L);

        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(52906, 1003937, 48103)
        ));

        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(48103L))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(10000881933L)
                .partnerId(48103L)
                .build()
        ));
        SearchPartnerFilter searchPartnerFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(48103L))
            .build();
        when(lmsClient.searchPartners(searchPartnerFilter)).thenReturn(List.of(
            partner(48103, PartnerType.SORTING_CENTER)
        ));

        // самопривоз от Дропшипа 1613559600 2021-02-17T04:00:00+03:00
        createAndCommitOrderSuccess(
            pickupOrder("controller/commit/request/route_import_dropship_to_delivery_service.json", false)
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        verify(lmsClient).searchPartners(searchPartnerFilter);
    }

    @Test
    @DisplayName("Заказ с самопривозом из Дропшипа в Службу доставки без возвратного СЦ")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_import_dropship_to_delivery_service_without_return_sc.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void importDropshipToDeliveryServiceWithOutReturnSC() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(52906L);

        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(52906, 1003937, 52906)
        ));

        // самопривоз от Дропшипа 1613559600 2021-02-17T04:00:00+03:00
        createAndCommitOrderSuccess(
            pickupOrder("controller/commit/request/route_import_dropship_to_delivery_service.json", false)
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ с самопривозом от Кроссдок-партнера в Фулмиллмент-центр")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_import_supplier_through_fulfillment.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void importSupplierThroughFulfillment() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(FF_PARTNER_ID);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(FF_PARTNER_ID, 48, FF_PARTNER_ID)
        ));

        // забор с Фулфиллмента 1596823200 2020-08-07T21:00:00+03:00
        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_import_supplier_through_fulfillment.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ c перемещением третьим партнером")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_movement_by_third_part_partner.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void movementByThirdPartPartner() throws Exception {
        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(47802L, 100136L))
            .build();

        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(47802, 100136, 47802),
            partnerRelation(100136, 1003937, 48103)
        ));

        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(48103L))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(10000881933L)
                .partnerId(48103L)
                .build()
        ));
        SearchPartnerFilter searchPartnerFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(48103L))
            .build();
        when(lmsClient.searchPartners(searchPartnerFilter)).thenReturn(List.of(
            partner(48103, PartnerType.SORTING_CENTER)
        ));

        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_movement_by_third_part_partner.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        verify(lmsClient).searchPartners(searchPartnerFilter);
    }

    @Test
    @DisplayName("При создании заказа вылетела ошибка, таска ретраится")
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_in_invalid_status.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks_on_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void conversionRetries() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(47802L);

        when(lmsClient.searchPartnerRelation(partnerRelationFilter))
            .thenThrow(new HttpTemplateException(502, "Bad gateway"));

        RouteOrderRequestDto orderRequestDto = courierOrder(
            "controller/commit/request/route_dropship_to_delivery_service.json"
        );

        ResultActions result = verifyRouteConversionFailure(
            orderRequestDto,
            "Http request exception: status <502>, response body <Bad gateway>.",
            (attemptsCount) -> verify(lmsClient, times(attemptsCount)).searchPartnerRelation(partnerRelationFilter)
        );

        queueTaskChecker.assertQueueTaskCreated(QueueType.CONVERT_ROUTE_TO_WAYBILL, getConvertPayload());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.NOTIFY_ORDER_VALIDATION_ERROR,
            getNotifyValidationErrorPayload(getCreated(result))
        );
    }

    @Test
    @DisplayName("Заказ с консолидацией от нескольких Кроссдок-поставщиков в Фулфиллмент-центре")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_multiple_suppliers_import_to_fulfillment.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void multipleSuppliersImportToFulfillment() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(FF_PARTNER_ID);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(FF_PARTNER_ID, 126, FF_PARTNER_ID)
        ));

        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_multiple_suppliers_import_to_fulfillment.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ с доставкой по клику через Лавку")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_lavka_on_demand.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void lavkaOnDemandOrder() throws Exception {
        long pickupPointId = 10000967618L;
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(FF_PARTNER_ID);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(FF_PARTNER_ID, 239, FF_PARTNER_ID)
        ));
        when(lmsClient.getLogisticsPoint(pickupPointId)).thenReturn(
            Optional.of(LogisticsPointResponse.newBuilder().id(pickupPointId).partnerId(1005471L).build())
        );

        createAndCommitOrderSuccess(courierOrder("controller/commit/request/route_on_demand.json"));

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoint(pickupPointId);
    }

    @Test
    @DisplayName("Заказ с доставкой по клику через ПВЗ Маркета")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_market_pickup_on_demand.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void marketPickupOnDemandOrder() throws Exception {
        long pickupPointId = 10000967618L;
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(FF_PARTNER_ID);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(FF_PARTNER_ID, 239, FF_PARTNER_ID)
        ));
        when(lmsClient.getLogisticsPoint(pickupPointId)).thenReturn(
            Optional.of(LogisticsPointResponse.newBuilder().id(pickupPointId).partnerId(1005489L).build())
        );

        createAndCommitOrderSuccess(courierOrder("controller/commit/request/route_on_demand.json"));

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoint(pickupPointId);
    }

    @Test
    @DisplayName("Заказ через дропофф Почта России")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dropoff_russian_post.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void dropoffRussianPostOrder() throws Exception {
        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(53079L, 1006612L, 57916L))
            .build();
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(53079, 1006612, 53079),
            partnerRelation(1006612, 57916, 1006612),
            partnerRelation(57916, 1005492, 57916)
        ));

        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_dropoff_russian_post.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    /**
     * Создаём заказ со следующим маршрутом:
     * <ol>
     * <li>Дропшип 48620, склад 10000994291</li>
     * <li>Дропофф 1005555, точка 10000977915</li>
     * <li>СЦ 172, склад 10000004403</li>
     * <li>СД 9, везёт на свой ПВЗ 10000977916</li>
     * </ol>
     */
    @Test
    @DisplayName("Заказ через ПВЗ, который является дропоффом")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dropoff_pickup_point_no_return_sorting_center.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void dropshipThroughDropoff() throws Exception {
        throughDropoff("controller/commit/request/route_dropoff_pickup_point_no_return_sorting_center.json", false);
    }

    /**
     * Создаём заказ со следующим маршрутом:
     * <ol>
     * <li>Дбс 48620, склад 10000994291</li>
     * <li>Дропофф 1005555, точка 10000977915</li>
     * <li>СЦ 172, склад 10000004403</li>
     * <li>СД 9, везёт на свой ПВЗ 10000977916</li>
     * </ol>
     */
    @Test
    @DisplayName("Dbs-заказ через ПВЗ, который является дропоффом")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dbs_dropoff_pickup_point_without_return_sorting_center.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void dbsThroughDropoff() throws Exception {
        throughDropoff("controller/commit/request/route_dbs_dropoff.json", true);
    }

    /**
     * Создаём заказ со следующим маршрутом:
     * <ol>
     * <li>Дропшип 48620, склад 10000994291</li>
     * <li>Дропофф 1005555, точка 10000977915</li>
     * <li>СЦ 172, склад 10000004403</li>
     * <li>СД 9, везёт на свой ПВЗ 10000977916</li>
     * </ol>
     */
    @Test
    @DisplayName("Заказ через ПВЗ, который является дропоффом. Возвратный СЦ содержится в прямом маршруте.")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dropoff_pickup_point_existing_return_sorting_center.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void throughDropoffReturnSortingCenterExistsInDirectFlow() throws Exception {
        throughDropoff(
            "controller/commit/request/route_dropoff_pickup_point_return_existing_sorting_center.json",
            false
        );
    }

    /**
     * Создаём заказ со следующим маршрутом:
     * <ol>
     * <li>Дбс 48620, склад 10000994291</li>
     * <li>Дропофф 1005555, точка 10000977915</li>
     * <li>СЦ 172, склад 10000004403</li>
     * <li>СД 9, везёт на свой ПВЗ 10000977916</li>
     * </ol>
     */
    @Test
    @DisplayName("Дбс-заказ через ПВЗ, который является дропоффом. Возвратный СЦ содержится в прямом маршруте")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dbs_dropoff_pickup_point_with_return_sorting_center.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void dbsThroughDropoffReturnSortingCenterExistsInDirectFlow() throws Exception {
        throughDropoff(
            "controller/commit/request/route_dbs_dropoff_pickup_point_return_existing_sorting_center.json",
            true
        );
    }

    /**
     * Создаём заказ со следующим маршрутом:
     * <ol>
     * <li>Дропшип 48620, склад 10000994291</li>
     * <li>Дропофф 1005555, точка 10000977915</li>
     * <li>СЦ 172, склад 10000004403</li>
     * <li>СД 9, везёт на свой ПВЗ 10000977916</li>
     * </ol>
     */
    @Test
    @DisplayName("Заказ через ПВЗ, который является дропоффом. Возвратного СЦ нет в прямом маршруте.")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dropoff_pickup_point_another_return_sorting_center.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void throughDropoffNoRouteReturnSortingCenterIdInDirectFlow() throws Exception {
        throughDropoffToSamePickup(
            "controller/commit/request/route_dropoff_pickup_point_return_another_sorting_center.json",
            false
        );
    }

    /**
     * Создаём заказ со следующим маршрутом:
     * <ol>
     * <li>Дбс 48620, склад 10000994291</li>
     * <li>Дропофф 1005555, точка 10000977915</li>
     * <li>СЦ 172, склад 10000004403</li>
     * <li>СД 9, везёт на свой ПВЗ 10000977916</li>
     * </ol>
     */
    @Test
    @DisplayName("Дбс-заказ через ПВЗ, который является дропоффом. Возвратного СЦ нет в прямом маршруте.")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dbs_dropoff_pickup_point_another_return_sorting_center.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void dbsThroughDropoffNoRouteReturnSortingCenterIdInDirectFlow() throws Exception {
        throughDropoffToSamePickup(
            "controller/commit/request/route_dbs_dropoff_pickup_point_return_another_sorting_center.json",
            true
        );
    }

    /**
     * Создаём заказ со следующим маршрутом:
     * <ol>
     * <li>Дропшип 48620, склад 10000994291</li>
     * <li>Дропофф 1005555, точка 10000977915</li>
     * <li>СЦ 172, склад 10000004403</li>
     * <li>СД 9, везёт на свой ПВЗ 10000977916</li>
     * </ol>
     */
    @Test
    @DisplayName("Заказ через ПВЗ, который является дропоффом. Нет возвратного СЦ в маршруте.")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dropoff_pickup_point_no_return_sorting_center.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void throughDropoffNoRouteReturnSortingCenter() throws Exception {
        throughDropoff("controller/commit/request/route_dropoff_pickup_point_no_return_sorting_center.json", false);
    }

    /**
     * Создаём заказ со следующим маршрутом:
     * <ol>
     * <li>Дропшип 48620, склад 10000994291</li>
     * <li>Дропофф 1005555, точка 10000977915</li>
     * <li>СЦ 172, склад 10000004403</li>
     * <li>СД 1005555, везёт на свой ПВЗ 10000977915</li>
     * </ol>
     */
    @Test
    @DisplayName("Заказ через ПВЗ, который является дропоффом, с доставкой в то же ПВЗ.")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dropoff_to_same_pickup_point_use_return_sorting_center_id.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void dropshipThroughDropoffToSamePickupPoint() throws Exception {
        throughDropoffToSamePickup("controller/commit/request/route_dropoff_to_same_pickup_point.json", false);
    }

    @Test
    @DisplayName("Заказ для часовых слотов целевое решение. market_pvz - go_platform")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_market_pvz_go_platform.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void marketPvzGoPlatformForTimeIntervals() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(50422L);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(50422, 50441, 50422)
        ));

        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_market_pvz_go_platform.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ для часовых слотов целевое решение. go_platform - go_platform")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_lavka_time_intervals.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void goPlatformGoPlatformForTimeIntervals() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(50422L);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(50422, 50441, 50422)
        ));

        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_go_platform_go_platform.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ с передачей в МК средней мили интервала загрузки в ПВЗ")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_movement_to_inbound_interval_from_route.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void movementToInboundIntervalFromRoute() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(FF_PARTNER_ID);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(FF_PARTNER_ID, MK_PARTNER_ID, FF_PARTNER_ID)
        ));

        createAndCommitOrderSuccess(
            pickupOrder("controller/commit/request/combined_route_pickup_point.json", false)
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ с услугой примерки на последней миле")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_trying_service.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void tryingServiceOnLastMile() throws Exception {
        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_last_mile_trying_service.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(47802L));
    }

    @Test
    @DisplayName("Заказ с услугой вскрытия на последней миле")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_unboxing_service.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void unboxingServiceOnLastMile() throws Exception {
        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_last_mile_unboxing_service.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(47802L));
    }

    @Test
    @DisplayName("Заказ с услугой частичного возврата на сегменте склада")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_partial_return_service.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void partialReturnOnWarehouse() throws Exception {
        createAndCommitOrderSuccess(
            courierOrder("controller/commit/request/route_warehouse_partial_return_service.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(47802L));
    }

    @Test
    @DisplayName("Заказ DBS в ПВЗ")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_dbs_to_pickup.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void dbsToPickupPoint() throws Exception {
        var routeOrderRequest = pickupOrder(
            "controller/commit/request/route_dbs_to_pickup_point.json",
            false
        );
        routeOrderRequest.setPlatformClientId(PlatformClient.DBS.getId());
        createAndCommitOrderSuccess(routeOrderRequest);
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(61071L));
    }

    @Test
    @DisplayName("Заказ YANDEX_GO (доставка наружу)")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_yandex_go_external_delivery.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void yandexGoExternalDelivery() throws Exception {
        createAndCommitOrderSuccess(yandexGoOrder("controller/commit/request/external_delivery_yandex_go.json"));
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(100136L));
    }

    @Test
    @DisplayName("Заказ YANDEX_GO (доставка наружу), генерация первого сегмента по senderWarehouseId")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_yandex_go_external_delivery.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void yandexGoExternalDeliveryWithSenderWarehouseId() throws Exception {
        RouteOrderRequestDto orderRequestDto = yandexGoOrder(
            "controller/commit/request/external_delivery_yandex_go_no_fake_point.json"
        );

        long senderWarehouseId = 10000481379L;
        orderRequestDto.setSenderWarehouseId(senderWarehouseId);
        when(lmsClient.getLogisticsPoint(senderWarehouseId))
            .thenReturn(Optional.of(
                LogisticsPointResponse.newBuilder()
                    .id(senderWarehouseId)
                    .partnerId(47802L)
                    .type(PointType.WAREHOUSE)
                    .address(Address.newBuilder().build())
                    .phones(Set.of())
                    .build()
            ));

        createAndCommitOrderSuccess(orderRequestDto);
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter(100136L));
        verify(lmsClient).getLogisticsPoint(senderWarehouseId);
    }

    @Test
    @DisplayName("Заказ YANDEX_GO (доставка наружу), не найдена логточка по senderWarehouseId")
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_in_invalid_status.xml",
        assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks_on_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void yandexGoWithSenderWarehouseIdLogisticsPointNotFound() throws Exception {
        RouteOrderRequestDto orderRequestDto = yandexGoOrder(
            "controller/commit/request/external_delivery_yandex_go_no_fake_point.json"
        );

        long senderWarehouseId = 10000481379L;
        orderRequestDto.setSenderWarehouseId(senderWarehouseId);

        verifyRouteConversionFailure(
            orderRequestDto,
            "Failed to find [LOGISTICS_POINT] with id [10000481379]",
            (attemptsCount) -> verify(lmsClient, times(attemptsCount)).getLogisticsPoint(senderWarehouseId)
        );
    }

    @Test
    @DisplayName("Заказ YANDEX_GO через дропофф")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_yandex_go_through_dropoff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void yandexGoThroughDropoff() throws Exception {
        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(1005555L, 100136L))
            .build();
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of());
        createAndCommitOrderSuccess(
            yandexGoOrder("controller/commit/request/route_yandex_go_through_dropoff.json")
        );
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ YANDEX_GO через дропофф, генерация первого сегмента по senderWarehouseId")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_yandex_go_through_dropoff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void yandexGoThroughDropoffWithSenderWarehouseId() throws Exception {
        RouteOrderRequestDto orderRequestDto = yandexGoOrder(
            "controller/commit/request/route_yandex_go_through_dropoff_no_fake_point.json"
        );

        long senderWarehouseId = 10000481379L;
        orderRequestDto.setSenderWarehouseId(senderWarehouseId);
        when(lmsClient.getLogisticsPoint(senderWarehouseId))
            .thenReturn(Optional.of(
                LogisticsPointResponse.newBuilder()
                    .id(senderWarehouseId)
                    .partnerId(47802L)
                    .type(PointType.WAREHOUSE)
                    .address(Address.newBuilder().build())
                    .phones(Set.of())
                    .build()
            ));

        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(1005555L, 100136L))
            .build();
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of());

        createAndCommitOrderSuccess(orderRequestDto);

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoint(senderWarehouseId);
    }

    @Test
    @DisplayName("Заказ YANDEX_GO без senderWarehouseId через дропофф")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_yandex_go_through_dropoff_no_sender_warehouse.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void yandexGoThroughDropoffWithNoSenderWarehouseId() throws Exception {
        RouteOrderRequestDto orderRequestDto = yandexGoOrder(
            "controller/commit/request/route_yandex_go_through_dropoff_no_fake_point.json"
        );

        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(1005555L, 100136L))
            .build();
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of());

        createAndCommitOrderSuccess(orderRequestDto);

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ YANDEX_GO через дропофф с возвратом в другой СЦ")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_yandex_go_through_dropoff_return_to_another_sc.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void yandexGoThroughDropoffReturnToAnotherSc() throws Exception {
        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(1005555L, 100136L))
            .build();
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of());

        Long returnSortingCenterId = 100137L;
        SearchPartnerFilter returnPartnerRelationsFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(returnSortingCenterId))
            .build();
        when(lmsClient.searchPartners(returnPartnerRelationsFilter)).thenReturn(List.of(
            partner(returnSortingCenterId.intValue(), PartnerType.SORTING_CENTER)
        ));

        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .active(true)
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(returnSortingCenterId))
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(100500L)
                .partnerId(returnSortingCenterId)
                .build()
        ));

        createAndCommitOrderSuccess(
            yandexGoOrder("controller/commit/request/route_yandex_go_through_dropoff_return_to_another_sc.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).searchPartners(returnPartnerRelationsFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
    }

    @Test
    @DisplayName(
        "Заказ YANDEX_GO через дропофф с возвратом в другой СЦ, " +
            "генерация первого сегмента по senderWarehouseId"
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_yandex_go_through_dropoff_return_to_another_sc.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void yandexGoThroughDropoffReturnToAnotherScWithSenderWarehouseId() throws Exception {
        RouteOrderRequestDto orderRequestDto = yandexGoOrder(
            "controller/commit/request/route_yandex_go_through_dropoff_return_to_another_sc_no_fake_point.json"
        );

        long senderWarehouseId = 10000481379L;
        orderRequestDto.setSenderWarehouseId(senderWarehouseId);
        when(lmsClient.getLogisticsPoint(senderWarehouseId))
            .thenReturn(Optional.of(
                LogisticsPointResponse.newBuilder()
                    .id(senderWarehouseId)
                    .partnerId(47802L)
                    .type(PointType.WAREHOUSE)
                    .address(Address.newBuilder().build())
                    .phones(Set.of())
                    .build()
            ));

        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(1005555L, 100136L))
            .build();
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of());

        Long returnSortingCenterId = 100137L;
        SearchPartnerFilter returnPartnerRelationsFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(returnSortingCenterId))
            .build();
        when(lmsClient.searchPartners(returnPartnerRelationsFilter)).thenReturn(List.of(
            partner(returnSortingCenterId.intValue(), PartnerType.SORTING_CENTER)
        ));

        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .active(true)
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(returnSortingCenterId))
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(100500L)
                .partnerId(returnSortingCenterId)
                .build()
        ));

        createAndCommitOrderSuccess(orderRequestDto);

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).searchPartners(returnPartnerRelationsFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        verify(lmsClient).getLogisticsPoint(senderWarehouseId);
    }

    @Test
    @DisplayName("Заказ YANDEX_GO без senderWarehouseId через дропофф с возвратом в другой СЦ")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_yandex_go_dropoff_return_to_another_sc_no_sender_wh.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void yandexGoThroughDropoffReturnToAnotherScWithNoSenderWarehouseId() throws Exception {
        RouteOrderRequestDto orderRequestDto = yandexGoOrder(
            "controller/commit/request/route_yandex_go_through_dropoff_return_to_another_sc_no_fake_point.json"
        );

        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(1005555L, 100136L))
            .build();
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of());

        Long returnSortingCenterId = 100137L;
        SearchPartnerFilter returnPartnerRelationsFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(returnSortingCenterId))
            .build();
        when(lmsClient.searchPartners(returnPartnerRelationsFilter)).thenReturn(List.of(
            partner(returnSortingCenterId.intValue(), PartnerType.SORTING_CENTER)
        ));

        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .active(true)
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(returnSortingCenterId))
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(100500L)
                .partnerId(returnSortingCenterId)
                .build()
        ));

        createAndCommitOrderSuccess(orderRequestDto);

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).searchPartners(returnPartnerRelationsFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
    }

    @Test
    @DisplayName("Заказ YANDEX_GO без senderWarehouseId через дропофф с возвратом в СЦ из прямого маршрута")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_yandex_go_dropoff_return_to_sc_no_sender_warehouse.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void yandexGoThroughDropoffReturnToScWithNoSenderWarehouseId() throws Exception {
        RouteOrderRequestDto orderRequestDto = yandexGoOrder(
            "controller/commit/request/route_yandex_go_through_dropoff_return_to_sc_no_fake_point.json"
        );

        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(1005555L, 100136L))
            .build();
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of());

        createAndCommitOrderSuccess(orderRequestDto);

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Заказ FaaS с забором из Фулфиллмент-центра в Службу доставки")
    @ExpectedDatabase(
        value = "/controller/commit/after/combined_route_faas_fulfillment_to_delivery_service.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/queue_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void faas() throws Exception {
        PartnerRelationFilter partnerRelationFilter = partnerRelationFilter(145L);
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(
            partnerRelation(145, 1003937, FF_PARTNER_ID)
        ));
        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(FF_PARTNER_ID))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(10000004403L)
                .partnerId(FF_PARTNER_ID)
                .build()
        ));
        SearchPartnerFilter searchPartnerFilter2 = SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID))
            .build();
        when(lmsClient.searchPartners(searchPartnerFilter2)).thenReturn(List.of(
            partner(FF_PARTNER_ID, PartnerType.FULFILLMENT)
        ));

        createAndCommitOrderSuccess(
            faasCourierOrder("controller/commit/request/route_fulfillment_to_delivery_service.json")
        );

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        verify(lmsClient).searchPartners(searchPartnerFilter2);
    }

    @Nonnull
    private PartnerRelationFilter partnerRelationFilter(long partnerId) {
        return PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(partnerId))
            .build();
    }

    private void createAndCommitOrderSuccess(RouteOrderRequestDto request) throws Exception {
        createAndCommitOrder(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value("DRAFT"));

        executeConvertConsumer();

        commitOrderConsumer.execute(TaskFactory.createTask(
            queueTaskChecker.getProducedTaskPayload(QueueType.COMMIT_ORDER, OrderIdAuthorPayload.class)
        ));

        getOrder(1)
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value("VALIDATING"));

        assertOrderCreatedLogWritten();
    }

    private void executeConvertConsumer() {
        executeConvertConsumer(0);
    }

    private void executeConvertConsumer(int attemptsCount) {
        convertRouteToWaybillConsumer.execute(TaskFactory.createTask(
            QueueType.CONVERT_ROUTE_TO_WAYBILL,
            queueTaskChecker.getProducedTaskPayload(QueueType.CONVERT_ROUTE_TO_WAYBILL, OrderIdAuthorPayload.class),
            attemptsCount
        ));
    }

    @Nonnull
    private ResultActions createAndCommitOrder(RouteOrderRequestDto request) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, "/orders/with-route", request)
                .headers(TestUtils.toHttpHeaders(SERVICE_HEADERS))
        );
    }

    @Nonnull
    private ResultActions getOrder(long id) throws Exception {
        return mockMvc.perform(get("/orders/{id}", id));
    }

    @Nonnull
    private RouteOrderRequestDto courierOrder(String routeFilePath) {
        return order("controller/commit/request/courier_order.json", routeFilePath);
    }

    @Nonnull
    private RouteOrderRequestDto faasCourierOrder(String routeFilePath) {
        return order("controller/commit/request/faas_courier_order.json", routeFilePath);
    }

    @Nonnull
    private RouteOrderRequestDto pickupOrder(String routeFilePath, boolean isDbs) {
        return order(
            isDbs ? "controller/commit/request/pickup_dbs_order.json" : "controller/commit/request/pickup_order.json",
            routeFilePath
        );
    }

    @Nonnull
    private RouteOrderRequestDto yandexGoOrder(String routeFilePath) {
        return order("controller/commit/request/yandex_go_shop_order.json", routeFilePath);
    }

    @Nonnull
    @SneakyThrows
    private RouteOrderRequestDto order(String orderFilePath, String routeFilePath) {
        return objectMapper.readValue(
                extractFileContent(orderFilePath),
                RouteOrderRequestDto.class
            )
            .setRoute(objectMapper.readTree(extractFileContent(routeFilePath)));
    }

    @Nonnull
    private static PartnerResponse partner(long i, PartnerType supplier) {
        return PartnerResponse.newBuilder().id(i).partnerType(supplier).build();
    }

    @Nonnull
    private static PartnerRelationEntityDto partnerRelation(
        long fromPartnerId,
        long toPartnerId,
        long returnPartnerId
    ) {
        return PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(fromPartnerId)
            .toPartnerId(toPartnerId)
            .returnPartnerId(returnPartnerId)
            .build();
    }

    private void assertOrderCreatedLogWritten() {
        assertEvent(
            "INFO",
            CombinedRouteEventCode.COMBINED_ROUTE_CREATED,
            "Combined route created.",
            "order,lom_order,waybillSegment"
        );
    }

    private void assertEvent(String level, CombinedRouteEventCode event, String message, String entities) {
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=" + level
                + "\tformat=plain"
                + "\tcode=" + event.name()
                + "\tpayload=" + message
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1"
                + "\tentity_types=" + entities
            ));
    }

    @Nonnull
    @SneakyThrows
    private Instant getCreated(ResultActions result) {
        byte[] response = result.andReturn().getResponse().getContentAsByteArray();
        return objectMapper.readValue(response, OrderDto.class).getCreated();
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
            null,
            created
        )
            .setSequenceId(2L);
    }

    private void throughDropoff(String route, boolean isDbs) throws Exception {
        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(48620L, 1005555L, FF_PARTNER_ID))
            .build();
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of());

        createAndCommitOrderSuccess(pickupOrder(route, isDbs));

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    private void throughDropoffToSamePickup(String route, boolean isDbs) throws Exception {
        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(48620L, 1005555L, FF_PARTNER_ID))
            .build();
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of());

        Long returnSortingCenterId = 100136L;
        SearchPartnerFilter returnPartnerRelationsFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(returnSortingCenterId))
            .build();
        when(lmsClient.searchPartners(returnPartnerRelationsFilter)).thenReturn(List.of(
            partner(returnSortingCenterId.intValue(), PartnerType.SORTING_CENTER)
        ));

        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .active(true)
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(returnSortingCenterId))
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(100500L)
                .partnerId(returnSortingCenterId)
                .build()
        ));

        createAndCommitOrderSuccess(pickupOrder(route, isDbs));

        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).searchPartners(returnPartnerRelationsFilter);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
    }


    private void verifyFirstEventHasRouteUuid() {
        Optional<OrderHistoryEvent> optionalEvent = orderHistoryEventRepository.findById(1L);
        if (optionalEvent.isEmpty()) {
            return;
        }
        OrderHistoryEvent event = optionalEvent.get();
        softly.assertThat(event.getSnapshot().get("routeUuid").asText()).isEqualTo(ROUTE_UUID);
        String jsonNodeDiff = StreamEx.of(event.getDiff().iterator())
            .filter(jsonNode -> "/routeUuid".equals(jsonNode.get("path").asText()))
            .findFirst()
            .map(JsonNode::toString)
            .orElseThrow(IllegalStateException::new);
        JSONAssert.assertEquals(
            "{\"op\":\"replace\",\"path\":\"/routeUuid\",\"value\":\"" + ROUTE_UUID + "\",\"fromValue\":null}",
            jsonNodeDiff,
            true
        );
    }

    private ResultActions verifyRouteConversionFailure(
        RouteOrderRequestDto requestDto,
        String validationError,
        Consumer<Integer> calledOnEveryAttemptVerifier
    ) throws Exception {
        ResultActions result = createAndCommitOrder(requestDto)
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value("DRAFT"));

        int attemptsCount = (int) QueueType.CONVERT_ROUTE_TO_WAYBILL.getRetryAttemptsCount();
        for (int i = 1; i < attemptsCount; ++i) {
            clock.setFixed(Instant.parse("2019-06-01T12:00:00.00Z").plus(i, ChronoUnit.DAYS), ZoneOffset.UTC);
            executeConvertConsumer(i);
            getOrder(1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("DRAFT"));
        }

        clock.setFixed(Instant.parse("2019-06-01T12:00:00.00Z").plus(attemptsCount, ChronoUnit.DAYS), ZoneOffset.UTC);
        executeConvertConsumer(attemptsCount);
        getOrder(1)
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("validationError").value(validationError));

        calledOnEveryAttemptVerifier.accept(attemptsCount);
        return result;
    }
}
