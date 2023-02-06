package ru.yandex.market.logistics.lom.service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.admin.dto.ChangeOrderReturnSegmentDto;
import ru.yandex.market.logistics.lom.controller.order.OrderHistoryTestUtil;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.OrderReturn;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.embedded.Sender;
import ru.yandex.market.logistics.lom.entity.enums.OrderReturnStatus;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.jobs.consumer.MultipleChangeOrderReturnSegmentConsumer;
import ru.yandex.market.logistics.lom.jobs.model.MultipleChangeOrderReturnSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.OrderRepository;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.lom.service.process.BusinessProcessStateService;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentMetaInfoValueDto;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.ActivityStatus;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Обработка задачи изменения возвратных сегментов заказов")
@DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-task.xml")
class MultipleChangeOrderReturnSegmentProcessorTest extends AbstractContextualTest {

    private static final Set<PartnerResponse> LMS_PARTNERS = Set.of(
        PartnerResponse.newBuilder().id(73).marketId(73000L).partnerType(PartnerType.SORTING_CENTER).build(),
        PartnerResponse.newBuilder()
            .id(74)
            .marketId(74000L)
            .partnerType(PartnerType.SORTING_CENTER)
            .name("SC_PEK")
            .readableName("СЦ ПЭК Бутово")
            .billingClientId(10101010L)
            .params(List.of(new PartnerExternalParam("RECIPIENT_UID_ENABLED", "description", "yes")))
            .status(PartnerStatus.ACTIVE)
            .build(),
        PartnerResponse.newBuilder()
            .id(76).marketId(76000L)
            .partnerType(PartnerType.SORTING_CENTER)
            .status(PartnerStatus.ACTIVE)
            .build(),
        PartnerResponse.newBuilder()
            .id(77)
            .marketId(77000L)
            .partnerType(PartnerType.FULFILLMENT)
            .status(PartnerStatus.ACTIVE)
            .build(),
        PartnerResponse.newBuilder()
            .id(78)
            .marketId(78000L)
            .partnerType(PartnerType.DELIVERY)
            .name("COOL_DROPOFF")
            .readableName("Дропофф для крутых")
            .billingClientId(10101010L)
            .params(List.of(new PartnerExternalParam("RECIPIENT_UID_ENABLED", "description", "yes")))
            .status(PartnerStatus.ACTIVE)
            .build(),
        PartnerResponse.newBuilder()
            .id(79)
            .marketId(79000L)
            .partnerType(PartnerType.SORTING_CENTER)
            .status(PartnerStatus.INACTIVE)
            .build()
    );

    private static final Set<LogisticsPointResponse> PARTNER_WAREHOUSES = Set.of(
        warehouse(73L),
        warehouse(74L),
        warehouse(77L),
        pickupPoint(78L),
        pickupPoint(10000001178L, 78L),
        inactivePickupPoint(10000001278L, 78L),
        pickupPoint(10000001378L, 78L),
        pickupPoint(10000001478L, 78L)
    );

    private static final Set<LogisticSegmentDto> POINT_SEGMENTS = Set.of(
        pickupPointWarehouseSegment(10000001078L, activeService()),
        pickupPointWarehouseSegment(10000001178L, inactiveService()),
        pickupPointWarehouseSegment(10000001278L, activeService()),
        pickupPointWarehouseSegment(10000001478L, activeServiceWithReturnSortingCenterId())
    );

    @Autowired
    private MultipleChangeOrderReturnSegmentConsumer consumer;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MarketIdService marketIdService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BusinessProcessStateService businessProcessStateService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, marketIdService, mdsS3Client);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    void validation(
        String displayName,
        Order order,
        Set<Long> returnSortingCenterIds,
        Set<Long> returnWarehouseIds,
        Set<Long> marketIds,
        List<ChangeOrderReturnSegmentDto> changeOrderReturnSegmentDtos,
        String errorMessage
    ) throws Exception {
        orderRepository.save(order);
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(changeOrderReturnSegmentDtos);

        try (
            var ignored1 = mockLmsSearchPartners(returnSortingCenterIds);
            var ignored2 = mockLmsGetWarehouses(returnSortingCenterIds, returnWarehouseIds);
            var ignored3 = mockMarketIdServiceFindAccountById(marketIds)
        ) {
            consumer.execute(task);
        }

        softly.assertThat(businessProcessStateService.getBusinessProcessState(1001L).getComment())
            .isEqualTo("Some orders were not updated. Errors by types: {" + errorMessage + "}");
        OrderHistoryTestUtil.assertOrderHistoryEventCount(jdbcTemplate, 1, 0);
    }

    @Nonnull
    private static Stream<Arguments> validation() throws Exception {
        return Stream.of(
            Arguments.of(
                "Заказа по указанному id не существует",
                order(73),
                Set.of(74L),
                null,
                Set.of(74000L),
                List.of(createChangeOrderReturnSegment(74L, 0L, null)),
                "ORDER_NOT_FOUND=[(0,null)]"
            ),
            Arguments.of(
                "Заказа по указанному barcode не существует",
                order(73),
                Set.of(74L),
                null,
                Set.of(74000L),
                List.of(createChangeOrderReturnSegment(74L, null, "1000")),
                "ORDER_NOT_FOUND=[(null,1000)]"
            ),
            Arguments.of(
                "Для одного заказа указано несколько разных возвратных партнёров",
                order(73),
                Set.of(73L, 74L),
                null,
                Set.of(73000L, 74000L),
                List.of(
                    createChangeOrderReturnSegment(73L, 1L, null),
                    createChangeOrderReturnSegment(74L, null, "1001")
                ),
                "MULTIPLE_PARTNER_IDS_FOR_SAME_ORDER=[(1,1001)]"
            ),
            Arguments.of(
                "Заказ в неподходящем статусе",
                order(73, OrderStatus.DELIVERED)
                    .setWaybill(List.of(fulfillment(), sortingCenter(), courier())),
                Set.of(74L),
                null,
                Set.of(74000L),
                List.of(createChangeOrderReturnSegment(74L, 1L, "1001")),
                "INAPPROPRIATE_ORDER_STATUS=[(1,1001)]"
            ),
            Arguments.of(
                "Невыкуп обрабатывается в LRM",
                orderInLrm(73),
                Set.of(74L),
                null,
                Set.of(74000L),
                List.of(createChangeOrderReturnSegment(74L, 1L, "1001")),
                "CANCELLED_ORDER_ALREADY_BEING_PROCESSED_BY_LRM=[(1,1001)]"
            ),
            Arguments.of(
                "Партнёра с указанным id не существует",
                order(73),
                Set.of(75L),
                null,
                null,
                List.of(createChangeOrderReturnSegment(75L, 1L, "1001")),
                "PARTNER_NOT_FOUND=[(1,1001)]"
            ),
            Arguments.of(
                "У партнёра нет склада",
                order(73),
                Set.of(76L),
                null,
                Set.of(76000L),
                List.of(createChangeOrderReturnSegment(76L, 1L, "1001")),
                "PARTNER_WAREHOUSE_NOT_FOUND=[(1,1001)]"
            ),
            Arguments.of(
                "Неподходящий тип партнёра",
                order(73),
                Set.of(77L),
                null,
                Set.of(77000L),
                List.of(createChangeOrderReturnSegment(77L, 1L, "1001")),
                "INAPPROPRIATE_PARTNER_TYPE=[(1,1001)]"
            ),
            Arguments.of(
                "Отсутствует аккаунт MarketId по идентификатору marketId",
                order(73),
                Set.of(74L),
                null,
                Set.of(),
                List.of(createChangeOrderReturnSegment(74L, 1L, "1001")),
                "WAREHOUSE_MARKET_ACCOUNT_NOT_FOUND=[(1,1001)]"
            ),
            Arguments.of(
                "Отсутствует сегмент для партнера order.returnSortingCenterId",
                order(76)
                    .setWaybill(List.of(fulfillment(), sortingCenter(), courier())),
                Set.of(74L),
                null,
                Set.of(74000L),
                List.of(createChangeOrderReturnSegment(74L, 1L, "1001")),
                "RETURN_WAYBILL_SEGMENT_NOT_FOUND=[(1,1001)]"
            ),
            Arguments.of(
                "У партнера нет склада, указанного в запросе",
                order(73),
                Set.of(78L),
                Set.of(123456L),
                Set.of(78000L),
                List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 123456L)),
                "PARTNER_WAREHOUSE_NOT_FOUND=[(1,1001)]"
            ),
            Arguments.of(
                "Партнер в неподходящем статусе",
                order(73),
                Set.of(79L),
                null,
                Set.of(79000L),
                List.of(createChangeOrderReturnSegment(79L, 1L, "1001")),
                "PARTNER_NOT_FOUND=[(1,1001)]"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса со складом дропоффа")
    void validationDropoff(
        String displayName,
        Order order,
        Set<Long> partnerIds,
        Set<Long> pickupPointsId,
        Set<Long> marketIds,
        List<ChangeOrderReturnSegmentDto> changeOrderReturnSegmentDtos,
        String errorMessage
    ) throws Exception {
        orderRepository.save(order);
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(changeOrderReturnSegmentDtos);

        try (
            var ignored1 = mockLmsSearchPartners(partnerIds);
            var ignored2 = mockLmsGetPickupPointsByPointIds(pickupPointsId, partnerIds);
            var ignored3 = mockMarketIdServiceFindAccountById(marketIds)
        ) {
            consumer.execute(task);
        }

        softly.assertThat(businessProcessStateService.getBusinessProcessState(1001L).getComment())
            .isEqualTo("Some orders were not updated. Errors by types: {" + errorMessage + "}");
        OrderHistoryTestUtil.assertOrderHistoryEventCount(jdbcTemplate, 1, 0);
    }

    @Nonnull
    private static Stream<Arguments> validationDropoff() throws Exception {
        return Stream.of(
            Arguments.of(
                "Для партнера дропоффа в запросе не указан конкретный склад",
                order(73),
                Set.of(78L),
                null,
                Set.of(78000L),
                List.of(createChangeOrderReturnSegment(78L, 1L, "1001")),
                "DROPOFF_WAREHOUSE_NOT_SPECIFIED=[(1,1001)]"
            ),
            Arguments.of(
                "Для партнера дропоффа указан склад с неактивным сегментом WAREHOUSE",
                order(73),
                Set.of(78L),
                Set.of(10000001178L),
                Set.of(78000L),
                List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 10000001178L)),
                "PICKUP_POINT_NOT_VALID_FOR_RETURN=[(1,1001)]"
            ),
            Arguments.of(
                "Для партнера дропоффа указан неактивный склад",
                order(73),
                Set.of(78L),
                Set.of(10000001278L),
                Set.of(78000L),
                List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 10000001278L)),
                "PARTNER_WAREHOUSE_NOT_FOUND=[(1,1001)]"
            ),
            Arguments.of(
                "Для партнера дропоффа указан склад без сегмента WAREHOUSE",
                order(73),
                Set.of(78L),
                Set.of(10000001378L),
                Set.of(78000L),
                List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 10000001378L)),
                "PICKUP_POINT_NOT_VALID_FOR_RETURN=[(1,1001)]"
            ),
            Arguments.of(
                "Для партнера дропоффа указан склад, для которого в сегменте указан RETURN_SORTING_CENTER_ID",
                order(73),
                Set.of(78L),
                Set.of(10000001478L),
                Set.of(78000L),
                List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 10000001478L)),
                "PICKUP_POINT_NOT_VALID_FOR_RETURN=[(1,1001)]"
            )
        );
    }

    @Test
    @DisplayName("Успешное обновление существующего возвратного сегмента")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-update.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segments-orders-update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSegment() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegment(74L, 1L, "1001"))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(74L));
            var ignored2 = mockLmsGetLogisticsPointsByPartnerIds(Set.of(74L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(74000L))
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/update_event_1.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Успешное обновление существующего возвратного сегмента для возвращённого заказа")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-update.xml")
    @DatabaseSetup(
        value = "/controller/admin/order/before/order-is-returned.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segments-returned-orders-update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSegmentForReturnedOrder() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegment(74L, 1L, "1001"))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(74L));
            var ignored2 = mockLmsGetLogisticsPointsByPartnerIds(Set.of(74L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(74000L))
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/update_event_1_for_returned_order.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Успешное обновление существующего возвратного сегмента для возвращённого заказа")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-update.xml")
    @DatabaseSetup(
        value = "/controller/admin/order/before/order-is-lost.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segments-lost-orders-update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSegmentForLostOrder() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegment(74L, 1L, "1001"))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(74L));
            var ignored2 = mockLmsGetLogisticsPointsByPartnerIds(Set.of(74L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(74000L))
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/update_event_1_for_lost_order.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Успешное обновление существующего возвратного сегмента с указанием склада")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-update.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segments-orders-update-with-warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSegmentWithSpecificWarehouse() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegmentWithWarehouse(74L, 1L, "1001", 10000001074L))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(74L));
            var ignored2 = mockLmsGetWarehousesByPointIds(Set.of(10000001074L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(74000L));
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/update_event_with_warehouse.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Успешное обновление существующего возвратного сегмента с указанием склада дропоффа")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-update.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/"
            + "process-change-order-return-segments-orders-update-with-warehouse-dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSegmentWithSpecificDropoffWarehouse() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 10000001078L))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(78L));
            var ignored2 = mockLmsGetPickupPointsByPointIds(Set.of(10000001078L), Set.of(78L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(78000L));
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/update_event_with_pickup.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Успешное создание нового возвратного сегмента")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-create.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segments-orders-create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSegment() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegment(74L, 1L, "1001"))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(74L));
            var ignored2 = mockLmsGetLogisticsPointsByPartnerIds(Set.of(74L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(74000L))
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/update_event_2.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Успешное создание нового возвратного сегмента с указанием склада")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-create.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segments-orders-create-with-warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSegmentWithSpecificWarehouse() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegmentWithWarehouse(74L, 1L, "1001", 10000001074L))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(74L));
            var ignored2 = mockLmsGetWarehousesByPointIds(Set.of(10000001074L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(74000L))
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/create_event_with_warehouse.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Успешное создание нового возвратного сегмента с указанием склада дропоффа")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-create.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/"
            + "process-change-order-return-segments-orders-create-with-warehouse-dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSegmentWithSpecificDropoffWarehouse() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 10000001078L))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(78L));
            var ignored2 = mockLmsGetPickupPointsByPointIds(Set.of(10000001078L), Set.of(78L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(78000L))
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/create_event_with_pickup.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Возвратный сегмент на новый склад уже существует, новый сегмент не создаем")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segments-orders-segment-existed.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segments-orders-segment-existed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnSegmentToSameDropoffExisted() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 10000001078L))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(78L));
            var ignored2 = mockLmsGetPickupPointsByPointIds(Set.of(10000001078L), Set.of(78L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(78000L))
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/create_event_with_pickup.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Существующий сегмент для склада имеет тип не SORTING_CENTER, создаем новый сегмент")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-not-sc.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segments-orders-create-exists-not-sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createReturnSegmentExistedSegmentNotSc() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 10000001078L))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(78L));
            var ignored2 = mockLmsGetPickupPointsByPointIds(Set.of(10000001078L), Set.of(78L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(78000L))
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/create_event_not_sc.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Существует возвратный сегмент на другой склад того же партнера, создаем новый")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-another-warehouse.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/"
            + "process-change-order-return-segments-orders-create-exists-to-another-warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createReturnSegmentExistedSegmentToAnotherWarehouse() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 10000001078L))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(78L));
            var ignored2 = mockLmsGetPickupPointsByPointIds(Set.of(10000001078L), Set.of(78L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(78000L))
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/create_event_another_warehouse.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Существует сегмент без warehouseLocation, создаем новый")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-empty-warehouse.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segments-orders-create-empty-warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createReturnSegmentExistedSegmentWithoutWarehouseLocation() throws Exception {
        Task<MultipleChangeOrderReturnSegmentPayload> task = createTask(
            List.of(createChangeOrderReturnSegmentWithWarehouse(78L, 1L, "1001", 10000001078L))
        );

        try (
            var ignored1 = mockLmsSearchPartners(Set.of(78L));
            var ignored2 = mockLmsGetPickupPointsByPointIds(Set.of(10000001078L), Set.of(78L));
            var ignored3 = mockMarketIdServiceFindAccountById(Set.of(78000L))
        ) {
            consumer.execute(task);
        }

        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/admin/order/snapshot/create_event_empty_warehouse.json",
            "created",
            "updated"
        );
    }

    @Nonnull
    private AutoCloseable mockLmsSearchPartners(Set<Long> partnerIds) {
        SearchPartnerFilter filter = SearchPartnerFilter.builder().setIds(partnerIds).build();
        when(lmsClient.searchPartners(refEq(filter))).thenReturn(
            LMS_PARTNERS.stream()
                .filter(partner -> partnerIds.contains(partner.getId()))
                .collect(Collectors.toList())
        );
        return () -> verify(lmsClient).searchPartners(refEq(filter));
    }

    @Nonnull
    private AutoCloseable mockLmsGetWarehouses(Set<Long> partnerIds, Set<Long> pointIds) {
        return pointIds == null
            ? mockLmsGetLogisticsPointsByPartnerIds(partnerIds)
            : mockLmsGetWarehousesByPointIds(pointIds);
    }

    @Nonnull
    private AutoCloseable mockLmsGetLogisticsPointsByPartnerIds(Set<Long> partnerIds) {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .partnerIds(partnerIds)
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(refEq(filter))).thenReturn(
            PARTNER_WAREHOUSES.stream()
                .filter(warehouse -> partnerIds.contains(warehouse.getPartnerId()))
                .collect(Collectors.toList())
        );
        return () -> verify(lmsClient).getLogisticsPoints(refEq(filter));
    }

    @Nonnull
    private AutoCloseable mockLmsGetWarehousesByPointIds(Set<Long> pointIds) {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .ids(pointIds)
            .build();
        when(lmsClient.getLogisticsPoints(refEq(filter))).thenReturn(
            PARTNER_WAREHOUSES.stream()
                .filter(warehouse -> pointIds.contains(warehouse.getId()))
                .filter(warehouse -> warehouse.getType() == PointType.WAREHOUSE)
                .collect(Collectors.toList())
        );
        return () -> verify(lmsClient).getLogisticsPoints(refEq(filter));
    }

    @Nonnull
    private AutoCloseable mockLmsGetPickupPointsByPointIds(Set<Long> pointIds, Set<Long> partnerIds) {
        if (pointIds == null) {
            return () -> verify(lmsClient).getLogisticsPoints(refEq(
                LogisticsPointFilter.newBuilder()
                    .partnerIds(partnerIds)
                    .type(PointType.WAREHOUSE)
                    .active(true)
                    .build()
            ));
        }
        LogisticsPointFilter pointsFilter = LogisticsPointFilter.newBuilder()
            .ids(pointIds)
            .build();
        LogisticSegmentFilter segmentsFilter = LogisticSegmentFilter.builder()
            .setLogisticsPointIds(pointIds)
            .setTypes(Set.of(LogisticSegmentType.WAREHOUSE))
            .build();
        when(lmsClient.getLogisticsPoints(refEq(pointsFilter))).thenReturn(
            PARTNER_WAREHOUSES.stream()
                .filter(warehouse -> pointIds.contains(warehouse.getId()))
                .filter(warehouse -> warehouse.getType() == PointType.PICKUP_POINT)
                .collect(Collectors.toList())
        );
        when(lmsClient.searchLogisticSegments(refEq(segmentsFilter))).thenReturn(
            POINT_SEGMENTS.stream()
                .filter(segment -> pointIds.contains(segment.getLogisticsPointId()))
                .collect(Collectors.toList())
        );
        return () -> {
            verify(lmsClient).getLogisticsPoints(refEq(pointsFilter));
            verify(lmsClient).searchLogisticSegments(refEq(segmentsFilter));
        };
    }

    @Nonnull
    private AutoCloseable mockMarketIdServiceFindAccountById(Set<Long> marketIds) {
        if (marketIds == null) {
            return () -> {
            };
        }
        if (marketIds.size() == 0) {
            when(marketIdService.findAccountById(any(Long.class))).thenReturn(Optional.empty());
            return () -> verify(marketIdService).findAccountById(any(Long.class));
        }
        marketIds.forEach(id -> when(marketIdService.findAccountById(id)).thenReturn(
                Optional.of(
                    MarketAccount.newBuilder()
                        .setMarketId(id)
                        .setLegalInfo(LegalInfo.newBuilder().setLegalName("legal-name").build())
                        .build()
                )
            )
        );
        return () -> marketIds.forEach(id -> verify(marketIdService).findAccountById(id));
    }

    @Nonnull
    private static Task<MultipleChangeOrderReturnSegmentPayload> createTask(List<ChangeOrderReturnSegmentDto> changes) {
        MultipleChangeOrderReturnSegmentPayload payload = PayloadFactory.multipleChangeOrderReturnSegmentPayload(
            changes,
            new OrderHistoryEventAuthor().setTvmServiceId(1010L).setYandexUid(BigDecimal.TEN),
            "1001",
            1L
        );

        return TaskFactory.createTask(QueueType.MULTIPLE_CHANGE_ORDER_RETURN_SEGMENT, payload);
    }

    @Nonnull
    private static ChangeOrderReturnSegmentDto createChangeOrderReturnSegment(
        Long partnerId,
        Long lomOrderId,
        String barcode
    ) {
        ChangeOrderReturnSegmentDto changeOrderReturnSegment = new ChangeOrderReturnSegmentDto();
        changeOrderReturnSegment.setOrderId(lomOrderId);
        changeOrderReturnSegment.setBarcode(barcode);
        changeOrderReturnSegment.setPartnerId(partnerId);
        return changeOrderReturnSegment;
    }

    @Nonnull
    private static ChangeOrderReturnSegmentDto createChangeOrderReturnSegmentWithWarehouse(
        Long partnerId,
        Long lomOrderId,
        String barcode,
        Long warehouseId
    ) {
        return createChangeOrderReturnSegment(partnerId, lomOrderId, barcode).setWarehouseId(warehouseId);
    }

    @Nonnull
    private static Order order(long returnSortingCenterId) throws Exception {
        return order(returnSortingCenterId, OrderStatus.PROCESSING);
    }

    @Nonnull
    private static Order orderInLrm(long returnSortingCenterId) throws Exception {
        Order order = order(returnSortingCenterId);
        order.getOrderReturns().add(
            new OrderReturn()
                .setOrder(order)
                .setReturnId(300L)
                .setReturnStatus(OrderReturnStatus.CREATED)
        );
        return order;
    }

    @Nonnull
    private static Order order(long returnSortingCenterId, OrderStatus orderStatus) throws Exception {
        Order order = new Order()
            .setBarcode("1001")
            .setPlatformClient(PlatformClient.BERU)
            .setSender(new Sender().setId(1L))
            .setReturnSortingCenterId(returnSortingCenterId);
        Field statusField = order.getClass().getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(order, orderStatus);
        return order;
    }

    @Nonnull
    private static WaybillSegment sortingCenter() {
        return new WaybillSegment()
            .setExternalId("1024")
            .setSegmentType(SegmentType.SORTING_CENTER)
            .setPartnerType(ru.yandex.market.logistics.lom.entity.enums.PartnerType.SORTING_CENTER)
            .setPartnerId(73L);
    }

    @Nonnull
    private static WaybillSegment courier() {
        return new WaybillSegment()
            .setExternalId("1023")
            .setSegmentType(SegmentType.COURIER)
            .setPartnerType(ru.yandex.market.logistics.lom.entity.enums.PartnerType.DELIVERY)
            .setPartnerId(1003937L);
    }

    @Nonnull
    private static WaybillSegment fulfillment() {
        return new WaybillSegment()
            .setExternalId("1022")
            .setSegmentType(SegmentType.FULFILLMENT)
            .setPartnerType(ru.yandex.market.logistics.lom.entity.enums.PartnerType.DROPSHIP)
            .setPartnerId(48150L)
            .setRequisiteId(2000L);
    }

    @Nonnull
    private static LogisticsPointResponse warehouse(long partnerId) {
        return logisticsPoint(10000001000L + partnerId, partnerId, PointType.WAREHOUSE).build();
    }

    @Nonnull
    private static LogisticsPointResponse pickupPoint(long partnerId) {
        return pickupPoint(10000001000L + partnerId, partnerId);
    }

    @Nonnull
    private static LogisticsPointResponse pickupPoint(Long pointId, long partnerId) {
        return logisticsPoint(pointId, partnerId, PointType.PICKUP_POINT).active(true).build();
    }

    @Nonnull
    private static LogisticsPointResponse inactivePickupPoint(Long pointId, long partnerId) {
        return logisticsPoint(pointId, partnerId, PointType.PICKUP_POINT).active(false).build();
    }

    @Nonnull
    private static LogisticsPointResponse.LogisticsPointResponseBuilder logisticsPoint(
        long pointId,
        long partnerId,
        PointType pointType
    ) {
        return LmsFactory.createLogisticsPointResponse(
            pointId,
            partnerId,
            "warehouse-" + partnerId,
            pointType
        );
    }

    @Nonnull
    private static LogisticSegmentDto pickupPointWarehouseSegment(
        long pointId,
        List<LogisticSegmentServiceDto> services
    ) {
        return new LogisticSegmentDto()
            .setId(1L)
            .setType(LogisticSegmentType.WAREHOUSE)
            .setLogisticsPointId(pointId)
            .setServices(services);
    }

    @Nonnull
    private static List<LogisticSegmentServiceDto> activeService() {
        return List.of(
            LogisticSegmentServiceDto.builder()
                .setStatus(ActivityStatus.ACTIVE)
                .build()
        );
    }

    @Nonnull
    private static List<LogisticSegmentServiceDto> activeServiceWithReturnSortingCenterId() {
        return List.of(
            LogisticSegmentServiceDto.builder()
                .setStatus(ActivityStatus.INACTIVE)
                .setMeta(List.of(LogisticSegmentMetaInfoValueDto.builder()
                    .setKey("RETURN_SORTING_CENTER_ID")
                    .setValue("1234567")
                    .build()
                ))
                .build()
        );
    }

    @Nonnull
    private static List<LogisticSegmentServiceDto> inactiveService() {
        return List.of(
            LogisticSegmentServiceDto.builder()
                .setStatus(ActivityStatus.INACTIVE)
                .build()
        );
    }
}
