package ru.yandex.market.logistics.nesu.controller.order;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CancellationOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.OrderActionsDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory;
import ru.yandex.market.logistics.nesu.controller.shipment.ShipmentTestUtils;
import ru.yandex.market.logistics.nesu.dto.enums.DaasOrderStatus;
import ru.yandex.market.logistics.nesu.dto.filter.ShipmentSearchFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.objectError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.missingParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск заказов в отгрузках")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/repository/order/database_order_prepare.xml")
class OrderSearchShipmentGroupingTest extends AbstractContextualTest {
    private static final ShipmentSearchFilter FILTER = ShipmentSearchFilter.builder()
        .orderStatuses(EnumSet.of(
            DaasOrderStatus.CREATED,
            DaasOrderStatus.DELIVERY_PROCESSING_STARTED,
            DaasOrderStatus.DELIVERY_TRACK_RECEIVED,
            DaasOrderStatus.DELIVERY_LOADED,
            DaasOrderStatus.SENDER_SENT,
            DaasOrderStatus.SORTING_CENTER_PROCESSING_STARTED,
            DaasOrderStatus.SORTING_CENTER_CREATED,
            DaasOrderStatus.SORTING_CENTER_LOADED,
            DaasOrderStatus.SORTING_CENTER_TRACK_RECEIVED,
            DaasOrderStatus.GENERIC_ERROR
        ))
        .build();

    private static final ShipmentSearchFilter EMPTY_FILTER = ShipmentSearchFilter.builder().build();

    @Autowired
    private LomClient lomClient;

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DisplayName("Не указано количество заказов")
    void noOrderCount() throws Exception {
        mockMvc.perform(
            request(HttpMethod.PUT, "/back-office/orders/search/for-shipments", EMPTY_FILTER)
                .param("shopId", "1")
                .param("userId", "1")
        )
            .andExpect(status().isBadRequest())
            .andExpect(missingParameter("ordersLimit", "Integer"));
    }

    @Test
    @DisplayName("Отрицательное количество заказов")
    void negativeOrderCount() throws Exception {
        mockSearchOrdersForShipments(0L, 1L, EMPTY_FILTER)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(objectError(
                "ordersLimit",
                "must be greater than or equal to 1",
                "Min",
                Map.of("value", 1)
            )));
    }

    @Test
    @DisplayName("Не найден магазин")
    void shopNotFound() throws Exception {
        mockSearchOrdersForShipments(1, 42L, FILTER)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [42]"));
    }

    @Test
    @DisplayName("Поиск отгрузок вернул пустой список")
    void emptyShipments() throws Exception {
        List<ShipmentSearchDto> result = List.of();
        when(lomClient.searchShipments(any(), any()))
            .thenReturn(PageResult.of(result, 0, 0, 10));

        mockSearchOrdersForShipments(1, 1L, FILTER)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/empty_page.json"));
    }

    @Test
    @DisplayName("Поиск заказов вернул пустой список")
    void ordersSearchEmptyList() throws Exception {
        List<ShipmentSearchDto> result = List.of(ShipmentTestUtils.createShipment(1L, 12L, null));
        ShipmentTestUtils.mockGetPartnersByIds(Set.of(12L), lmsClient);
        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(1L, 2L), lmsClient);
        mockSearchShipments(result, Set.of(1L, 25L), 1);

        mockSearchOrdersForShipments(1L, 1L, FILTER)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/empty_shipment_result.json"));
    }

    @Test
    @DisplayName("Успешный сценарий - 3 отгрузки по 2 заказа")
    void searchByShipmentsTest() throws Exception {
        List<ShipmentSearchDto> result = List.of(
            ShipmentTestUtils.createShipment(
                1L,
                12L,
                null,
                PageResult.of(
                    List.of(
                        OrderDtoFactory.createLomOrder(BigDecimal.valueOf(234)).setId(1L)
                            .setCancellationOrderRequests(createCancellationRequests(1L, 2L, 3L))
                            .setAvailableActions(createAvailableActions(true)),
                        OrderDtoFactory.createLomOrder(BigDecimal.valueOf(234)).setId(2L)
                            .setCancellationOrderRequests(createCancellationRequests(4L, 6L, 5L))
                            .setAvailableActions(createAvailableActions(false))
                    ),
                    13,
                    0,
                    2
                ),
                new BigDecimal(468)
            ),
            ShipmentTestUtils.createShipment(
                2L,
                14L,
                null,
                PageResult.of(
                    List.of(
                        OrderDtoFactory.createLomOrder(BigDecimal.valueOf(345)).setId(3L)
                            .setCancellationOrderRequests(createCancellationRequests(8L, 7L, 9L))
                            .setAvailableActions(createAvailableActions(null)),
                        OrderDtoFactory.createLomOrder(BigDecimal.valueOf(234)).setId(4L)
                            .setCancellationOrderRequests(createCancellationRequests(11L, 12L, 10L))
                            .setAvailableActions(createAvailableActions(true))
                    ),
                    800,
                    0,
                    2
                ),
                new BigDecimal(579)
            ),
            ShipmentTestUtils.createShipment(
                3L,
                16L,
                null,
                PageResult.of(
                    List.of(
                        OrderDtoFactory.createLomOrder(null).setId(5L)
                            .setCancellationOrderRequests(createCancellationRequests(15L, 13L, 14L))
                            .setAvailableActions(createAvailableActions(false)),
                        OrderDtoFactory.createLomOrder(BigDecimal.valueOf(234)).setId(6L)
                            .setCancellationOrderRequests(createCancellationRequests(18L, 17L, 16L))
                            .setAvailableActions(createAvailableActions(null))
                    ),
                    813,
                    0,
                    2
                ),
                new BigDecimal(234)
            )
        );

        ShipmentTestUtils.mockGetPartnersByIds(Set.of(12L, 14L, 16L), lmsClient);
        ShipmentTestUtils.mockGetPartnersById(5L, lmsClient);
        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(1L, 2L), lmsClient);
        ShipmentTestUtils.mockGetLogisticsPoints(Set.of(3L, 4L, 101L), lmsClient, null);
        mockSearchShipments(result, Set.of(1L, 25L), 2);

        mockSearchOrdersForShipments(2L, 1L, FILTER)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/orders_by_shipments.json"));
    }

    @Test
    @DisplayName("В фильтре отгрузок не указаны статусы заказов")
    void noOrderStatusInShipmentsFilter() throws Exception {
        mockSearchOrdersForShipments(1L, 1L, EMPTY_FILTER)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Shipment filter must contain at least one order status"));
    }

    private List<CancellationOrderRequestDto> createCancellationRequests(long id1, long id2, long id3) {
        return List.of(
            CancellationOrderRequestDto.builder()
                .id(id1)
                .status(CancellationOrderStatus.PROCESSING)
                .build(),
            CancellationOrderRequestDto.builder()
                .id(id2)
                .status(CancellationOrderStatus.MANUALLY_CONFIRMED)
                .build(),
            CancellationOrderRequestDto.builder()
                .id(id3)
                .status(CancellationOrderStatus.FAIL)
                .build()
        );
    }

    private OrderActionsDto createAvailableActions(Boolean cancel) {
        return OrderActionsDto.builder().cancel(cancel).build();
    }

    private void mockSearchShipments(List<ShipmentSearchDto> result, Set<Long> senderIds, int ordersPageSize) {
        when(lomClient.searchShipments(
            safeRefEq(
                ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter.builder()
                    .orderStatuses(Set.of(OrderStatus.ENQUEUED, OrderStatus.PROCESSING_ERROR))
                    .segmentStatuses(Map.of(
                        PartnerType.OWN_DELIVERY,
                        Set.of(SegmentStatus.INFO_RECEIVED),
                        PartnerType.DELIVERY,
                        Set.of(
                            SegmentStatus.STARTED,
                            SegmentStatus.PENDING,
                            SegmentStatus.INFO_RECEIVED,
                            SegmentStatus.TRACK_RECEIVED
                        ),
                        PartnerType.SORTING_CENTER,
                        Set.of(
                            SegmentStatus.STARTED,
                            SegmentStatus.PENDING,
                            SegmentStatus.INFO_RECEIVED,
                            SegmentStatus.TRACK_RECEIVED
                        )
                    ))
                    .marketIdFrom(201L)
                    .senderIds(senderIds)
                    .ordersPageSize(ordersPageSize)
                    .build()),
            safeRefEq(new Pageable(0, 10, null))
        ))
            .thenReturn(createPageResult(result));
    }

    @Nonnull
    private ResultActions mockSearchOrdersForShipments(
        long ordersLimit,
        long shopId,
        ShipmentSearchFilter filter
    ) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.PUT, "/back-office/orders/search/for-shipments", filter)
                .param("shopId", String.valueOf(shopId))
                .param("userId", "1")
                .param("ordersLimit", String.valueOf(ordersLimit))
        );
    }

    @Nonnull
    private PageResult<ShipmentSearchDto> createPageResult(List<ShipmentSearchDto> result) {
        return new PageResult<ShipmentSearchDto>()
            .setData(result)
            .setSize(10)
            .setTotalElements(result.size())
            .setTotalPages(1);
    }
}
