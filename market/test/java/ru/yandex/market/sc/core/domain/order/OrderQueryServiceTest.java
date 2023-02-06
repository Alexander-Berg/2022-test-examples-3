package ru.yandex.market.sc.core.domain.order;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.sc.core.domain.cell.CellField;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellDto;
import ru.yandex.market.sc.core.domain.cell.model.CellCargoType;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.cell.repository.CellMapper;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.courier.model.ApiCourierDto;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.ApiCellLotDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderIdsDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderListStatus;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.ApiPlaceDto;
import ru.yandex.market.sc.core.domain.order.model.ApiPlaceIdsDto;
import ru.yandex.market.sc.core.domain.order.model.OrderFFStatusRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderFFStatusResponse;
import ru.yandex.market.sc.core.domain.order.model.OrderFlowType;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.task.model.RouteTaskType;
import ru.yandex.market.sc.core.domain.warehouse.model.ApiWarehouseDto;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseProperty;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderQueryServiceTest {

    private final OrderQueryService orderQueryService;
    private final ScOrderRepository scOrderRepository;
    private final TestFactory testFactory;
    private final TransactionTemplate transactionTemplate;
    private final SortingCenterPropertySource sortingCenterPropertySource;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    Courier courier;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        courier = testFactory.storedCourier();
        testFactory.setupMockClock(clock);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, false);
    }

    @Test
    void findOrderFFStatusHistoryAndFindCurrentOrdersFFStatusReturnsSameTimestamp() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        Instant now = Instant.now(clock);
        var expectedStatus = new OrderFFStatusResponse.HistoryItem(
                now, ScOrderFFStatus.ORDER_CREATED_FF, null
        );
        assertThat(
                orderQueryService.getStatusHistory(order.getId()).getStatusHistories().values()
                        .stream().flatMap(List::stream).toList()
        ).isEqualTo(List.of(expectedStatus));
        assertThat(
                orderQueryService.getCurrentStatus(List.of(order.getId())).getStatusHistories().values()
                        .stream().flatMap(List::stream).toList()
        ).isEqualTo(List.of(expectedStatus));
    }

    @Test
    void findOrderFFStatusHistoryAndFindCurrentOrdersFFStatusReturnsSameTimestampAfterShip() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
        Instant now = Instant.now(clock);
        var expectedStatus = new OrderFFStatusResponse.HistoryItem(
                now, ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, null
        );
        assertThat(
                orderQueryService.getStatusHistory(order.getId()).getStatusHistories().values()
                        .stream()
                        .flatMap(List::stream)
                        .filter(s -> s.getFfStatus() == ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF)
                        .toList()
        ).isEqualTo(List.of(expectedStatus));
        assertThat(
                orderQueryService.getCurrentStatus(List.of(order.getId())).getStatusHistories().values()
                        .stream().flatMap(List::stream).toList()
        ).isEqualTo(List.of(expectedStatus));
    }

    @Test
    void getOrderIdsByBufferCellInRightCell() {
        var place = testFactory.createOrder(sortingCenter)
                .accept()
                .keep()
                .getPlace();

        List<ApiOrderIdsDto> orderIdDtos = getOrderIdsByCell(Objects.requireNonNull(place.getCell()).getId(), sortingCenter);

        assertThat(orderIdDtos).hasSize(1);
        assertThat(orderIdDtos.get(0).getPlaces()).hasSize(1);
        ApiPlaceIdsDto placeIdDto = orderIdDtos.get(0).getPlaces().get(0);
        assertThat(placeIdDto)
                .isEqualTo(new ApiPlaceIdsDto(
                        place.getOrderId(),
                        place.getExternalId(),
                        place.getMainPartnerCode(),
                        ApiOrderListStatus.IN_RIGHT_CELL,
                        new ApiCellDto(place.getCell())
                ));
    }

    @Test
    void getOrderWithoutTodayRouteIdsByBufferCellInRightCell() {
        var place = testFactory.createOrder(sortingCenter)
                .updateCourier(testFactory.storedCourier())
                .updateShipmentDate(LocalDate.now(clock).plusDays(2))
                .accept()
                .keep()
                .getPlace();

        List<ApiOrderIdsDto> orderIdDtos = getOrderIdsByCell(Objects.requireNonNull(place.getCell()).getId(), sortingCenter);

        assertThat(orderIdDtos).hasSize(1);
        assertThat(orderIdDtos.get(0).getPlaces()).hasSize(1);
        ApiPlaceIdsDto placeIdDto = orderIdDtos.get(0).getPlaces().get(0);

        assertThat(placeIdDto)
                .isEqualTo(new ApiPlaceIdsDto(
                        place.getOrderId(),
                        place.getExternalId(),
                        place.getMainPartnerCode(),
                        ApiOrderListStatus.IN_RIGHT_CELL,
                        new ApiCellDto(place.getCell())
                ));
    }

    @Test
    void presortedSinglePlaceOrderStatus() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, "1");
        var deliveryService = testFactory.storedDeliveryService("1");
        var order = testFactory.create(order(sortingCenter)
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT).build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(1))
                .accept().sort().get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null).getStatus())
                .isEqualTo(ApiOrderStatus.OK);
    }

    @Test
    void presortedMultiPlaceOrderStatus() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, "1");
        var deliveryService = testFactory.storedDeliveryService("1");
        var order = testFactory.create(
                        order(sortingCenter).places("p1", "p2")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build()
                )
                .updateShipmentDate(LocalDate.now(clock).plusDays(1))
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null).getStatus())
                .isEqualTo(ApiOrderStatus.OK);
    }

    @Test
    void partiallyPresortedMultiPlaceOrderStatus() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, "1");
        var order = testFactory.create(
                        order(sortingCenter).places("p1", "p2").dsType(DeliveryServiceType.TRANSIT).build()
                )
                .updateShipmentDate(LocalDate.now(clock).plusDays(1))
                .acceptPlaces("p1", "p2").sortPlaces("p1").get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), "p2").getStatus())
                .isEqualTo(ApiOrderStatus.SORT_TO_COURIER);

    }

    @Test
    void getDroppedOrderDroppedOrdersDisabled() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DROPPED_ORDERS_ENABLED, "false");
        var order = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(LocalDate.now(clock))
                .get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrderDto.getStatus()).isEqualTo(ApiOrderStatus.KEEP);
        assertThat(CollectionUtils.isEmpty(apiOrderDto.getAvailableCells())).isTrue();
    }

    @Test
    void getDroppedOrderDroppedOrdersEnabled() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DROPPED_ORDERS_ENABLED, "true");
        var cell = testFactory.storedCell(sortingCenter, "d-1", CellType.BUFFER, CellSubType.DROPPED_ORDERS);
        var order = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(LocalDate.now(clock))
                .get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrderDto.getStatus()).isEqualTo(ApiOrderStatus.KEEP);
        assertThat(CollectionUtils.isNonEmpty(apiOrderDto.getAvailableCells())).isTrue();
        assertThat(apiOrderDto.getAvailableCells().stream().anyMatch(curCell -> curCell.getId() == cell.getId())).isTrue();
    }

    /**
     * Возвратный заказ не должен проситься в ячейку выброшенных заказов
     */
    @Test
    void returnedOrderShouldNotBeSortedToDroppedOrders() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DROPPED_ORDERS_ENABLED, "true");
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        testFactory.storedCell(sortingCenter, "d-1", CellType.BUFFER, CellSubType.DROPPED_ORDERS);
        var returnedCell = testFactory.storedCell(sortingCenter, "r-1", CellType.RETURN, CellSubType.DEFAULT);
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn()
                .get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrderDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(CollectionUtils.isNonEmpty(apiOrderDto.getAvailableCells())).isTrue();
        assertThat(apiOrderDto.getAvailableCells().stream().anyMatch(curCell -> curCell.getId() == returnedCell.getId())).isTrue();
    }

    @Test
    void sortInAdvanceSort() {
        int daysInAdvance = 1;
        var deliveryService = testFactory.storedDeliveryService(
                "1",
                sortingCenter.getId(),
                false
        );
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, String.valueOf(sortingCenter.getId()));
        var order = testFactory.create(order(sortingCenter).externalId("o1")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance)).accept().get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrderDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_COURIER);
        assertThat(apiOrderDto.getPossibleOutgoingRouteDate()).isEqualTo(LocalDate.now(clock).plusDays(daysInAdvance));
        assertThat(CollectionUtils.isNonEmpty(apiOrderDto.getAvailableCells())).isTrue();
    }

    @Test
    void cantSortInAdvanceSortWhenDeliveryServiceNotSupportItOnSc() {
        int daysInAdvance = 1;
        var deliveryService = testFactory.storedDeliveryService("1");
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        var order = testFactory.create(order(sortingCenter).externalId("o1")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance)).accept().get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrderDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_COURIER);
        assertThat(apiOrderDto.getPossibleOutgoingRouteDate()).isEqualTo(LocalDate.now(clock)); //because its middl mile
        assertThat(CollectionUtils.isNonEmpty(apiOrderDto.getAvailableCells())).isTrue();
    }

    @DisplayName("Заказ не средняя миля, sortInAdvance для этой службы доставки отключена," +
            " дата доставки заказа в будущем => " +
            "Заказ идет на хранение")
    @Test
    void cantSortInAdvanceSortWhenDeliveryServiceNotSupportItOnScNotMiddleMile() {
        int daysInAdvance = 1;
        var deliveryService = testFactory.storedDeliveryService("1", true);
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        var order = testFactory.create(order(sortingCenter).externalId("o1")
                        .deliveryService(deliveryService)
                        .build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance)).accept().get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrderDto.getStatus()).isEqualTo(ApiOrderStatus.KEEP);
        assertThat(apiOrderDto.getPossibleOutgoingRouteDate()).isEqualTo(LocalDate.now(clock).plusDays(daysInAdvance));
        assertThat(CollectionUtils.isEmpty(apiOrderDto.getAvailableCells())).isTrue();
    }

    @DisplayName("Заказ не средняя миля, sortInAdvance для это службы поддерживается на этом сц, " +
            "но СЦ не поддерживает эту фичу в целом, дата доставки заказа в будущем => Заказ идет на хранение")
    @Test
    void cantSortInAdvanceSortWhenDeliveryServiceSupportItButScNotSupportedAtAllOnScNotMiddleMile() {
        int daysInAdvance = 1;
        var deliveryService = testFactory.storedDeliveryService("1");
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, String.valueOf(sortingCenter.getId()));
        var order = testFactory.create(order(sortingCenter).externalId("o1")
                        .deliveryService(deliveryService)
                        .build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance)).accept().get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrderDto.getStatus()).isEqualTo(ApiOrderStatus.KEEP);
        assertThat(apiOrderDto.getPossibleOutgoingRouteDate()).isEqualTo(LocalDate.now(clock).plusDays(daysInAdvance));
        assertThat(CollectionUtils.isEmpty(apiOrderDto.getAvailableCells())).isTrue();
    }

    @Test
    void cantSortInAdvanceRegularOrderWhenToggleOff() {
        int daysInAdvance = 1;
        var deliveryService = testFactory.storedDeliveryService("1", true);
        var order = testFactory.create(order(sortingCenter).externalId("o1")
                        .deliveryService(deliveryService)
                        .build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance)).accept().get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrderDto.getStatus()).isEqualTo(ApiOrderStatus.KEEP);
        assertThat(apiOrderDto.getPossibleOutgoingRouteDate()).isEqualTo(LocalDate.now(clock).plusDays(daysInAdvance));
        assertThat(CollectionUtils.isEmpty(apiOrderDto.getAvailableCells())).isTrue();
    }

    @Test
    void cantSortInAdvanceMiddleMileOrderWhenToggleOff() {
        int daysInAdvance = 1;
        var deliveryService = testFactory.storedDeliveryService("1", false);
        var order = testFactory.create(order(sortingCenter).externalId("o1")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance))
                .accept().get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrderDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_COURIER);
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        assertThat(route.getRouteCells()).hasSize(1);
        assertThat(route.getRouteCells(LocalDate.now(clock))).hasSize(1);
        assertThat(apiOrderDto.getPossibleOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(CollectionUtils.isNonEmpty(apiOrderDto.getAvailableCells())).isTrue();
    }

    @Test
    void sortInAdvanceKeep() {
        int daysInAdvance = 2;
        var deliveryService = testFactory.storedDeliveryService("1", false);
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, String.valueOf(sortingCenter.getId()));
        var order = testFactory.create(order(sortingCenter)
                        .externalId("o1")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build()
                )
                .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance + 1))
                .accept().get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrderDto.getStatus()).isEqualTo(ApiOrderStatus.KEEP);
        assertThat(CollectionUtils.isEmpty(apiOrderDto.getAvailableCells())).isTrue();
    }


    @Test
    void getAcceptedMiddleMileOrderWithoutShipmentDate() {
        var order = testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                .accept().get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null).getStatus())
                .isEqualTo(ApiOrderStatus.SORT_TO_COURIER);
    }

    @Test
    void getAcceptedMiddleMileOrderWithShipmentDateInFuture() {
        var order = testFactory.create(
                        order(sortingCenter)
                                .shipmentDate(LocalDate.now(clock).plusDays(1))
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build()
                )
                .accept().get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null).getStatus())
                .isEqualTo(ApiOrderStatus.SORT_TO_COURIER);
    }

    @Test
    void getAcceptedMiddleMileOrderWithShipmentDateInPast() {
        var order = testFactory.create(
                        order(sortingCenter)
                                .shipmentDate(LocalDate.now(clock).minusDays(1))
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build()
                )
                .accept().get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null).getStatus())
                .isEqualTo(ApiOrderStatus.SORT_TO_COURIER);
    }

    @Test
    void getOrder() {
        Place place = testFactory.createOrder(sortingCenter).getPlace();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, place.getExternalId(), null))
                .isEqualTo(createdOrderDto(place));
    }

    @Test
    void getOrderKeep() {
        Place place = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept().keep().getPlace();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, place.getExternalId(), null))
                .isEqualTo(createdOrderDto(place));
    }

    @Test
    void getOrderById() {
        Place place = testFactory.createOrder(sortingCenter).getPlace();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, place.getExternalId(), null))
                .isEqualTo(createdOrderDto(place));
    }

    private ApiOrderDto createdOrderDto(Place place) {
        return new ApiOrderDto(
                place.getOrderId(),
                place.getExternalId(),
                ApiOrderStatus.KEEP,
                null,
                null,
                place.isMiddleMile(),
                List.of(new ApiPlaceDto(
                        place.getMainPartnerCode(),
                        apiCellDto(place.getCell()),
                        ApiOrderStatus.KEEP,
                        place.getSortableStatus(),
                        null,
                        null,
                        null,
                        Collections.emptyList(),
                        false,
                        Collections.emptyList(),
                        place.getOutgoingRouteDate(),
                        place.isMiddleMile(),
                        place.getDeliveryService().getName()
                )),
                place.getOutgoingRouteDate(),
                place.isMiddleMile(),
                place.getDeliveryService().getName(),
                Collections.emptyList(),
                false,
                Collections.emptyList(),
                sortingCenterPropertySource.isUseZoneForBufferReturnCells(sortingCenter.getId())
        );
    }

    @Test
    void getCurrentStatus() {
        OrderLike order = testFactory.createOrder(sortingCenter).get();
        OrderFFStatusResponse response = orderQueryService.getCurrentStatus(new OrderFFStatusRequest(
                sortingCenter, List.of(new ResourceId(order.getExternalId(), String.valueOf(order.getId())))
        ));
        transactionTemplate.execute(ts -> {
            var actualOrder = scOrderRepository.findByIdOrThrow(order.getId());
            assertThat(response.getStatusHistories())
                    .isEqualTo(Map.of(
                            new ResourceId(actualOrder.getExternalId(), String.valueOf(actualOrder.getId())),
                            List.of(new OrderFFStatusResponse.HistoryItem(
                                    actualOrder.getOrderHistoryUpdated(),
                                    ScOrderFFStatus.ORDER_CREATED_FF, null
                            ))
                    ));
            return null;
        });
    }

    @Test
    void getCurrentStatus2Statuses() {
        OrderLike order = testFactory.createOrder(sortingCenter).get();
        testFactory.cancelOrder(order.getId());
        OrderFFStatusResponse response = orderQueryService.getCurrentStatus(new OrderFFStatusRequest(
                sortingCenter, List.of(new ResourceId(order.getExternalId(), String.valueOf(order.getId())))
        ));
        transactionTemplate.execute(ts -> {
            var actualOrder = scOrderRepository.findByIdOrThrow(order.getId());
            assertThat(response.getStatusHistories())
                    .isEqualTo(Map.of(
                            new ResourceId(actualOrder.getExternalId(), String.valueOf(actualOrder.getId())),
                            List.of(new OrderFFStatusResponse.HistoryItem(
                                    actualOrder.getOrderHistoryUpdated(),
                                    ScOrderFFStatus.ORDER_CANCELLED_FF, null
                            ))
                    ));
            return null;
        });
    }

    @Test
    void getStatusHistory() {
        var order = testFactory.createOrder(sortingCenter).get();
        testFactory.cancelOrder(order.getId());
        OrderFFStatusResponse response = orderQueryService.getStatusHistory(
                sortingCenter, new ResourceId(order.getExternalId(), String.valueOf(order.getId()))
        );
        transactionTemplate.execute(ts -> {
            var actualOrder = scOrderRepository.findByIdOrThrow(order.getId());
            assertThat(response.getStatusHistories())
                    .isEqualTo(Map.of(
                            new ResourceId(actualOrder.getExternalId(), String.valueOf(actualOrder.getId())),
                            List.of(
                                    new OrderFFStatusResponse.HistoryItem(
                                            actualOrder.getFfStatusHistory().get(0).getOrderUpdateTime(),
                                            ScOrderFFStatus.ORDER_CREATED_FF, null
                                    ),
                                    new OrderFFStatusResponse.HistoryItem(
                                            actualOrder.getFfStatusHistory().get(1).getOrderUpdateTime(),
                                            ScOrderFFStatus.ORDER_CANCELLED_FF, null
                                    )
                            )
                    ));
            return null;
        });
    }

    @Test
    void noRouteForKeepOrderInApi() {
        var order = testFactory.createOrder(sortingCenter).get();
        ApiOrderDto actual = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(actual.getStatus()).isEqualTo(ApiOrderStatus.KEEP);
        assertThat(actual.getRouteTo()).isNull();
    }

    @Test
    void getMultiPlaceOrder() {
        var order = testFactory.create(order(sortingCenter).places("1", "2").build()).get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null))
                .isEqualTo(apiOrderDto(order, ApiOrderStatus.KEEP,
                        List.of(
                                apiPlaceDto(testFactory.orderPlace(order, "1"), ApiOrderStatus.KEEP),
                                apiPlaceDto(testFactory.orderPlace(order, "2"), ApiOrderStatus.KEEP)
                        )));
    }

    @Test
    void getShippedMultiPlaceOrderSecondPlace() {
        var order = testFactory.createForToday(order(sortingCenter)
                        .places("1", "2")
                        .build()
                )
                .acceptPlaces().sortPlaces().ship().acceptPlaces("1").get();

        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), "1"))
                .isEqualTo(apiOrderDto(order, ApiOrderStatus.KEEP,
                        List.of(
                                apiPlaceDto(testFactory.orderPlace(order, "1"), ApiOrderStatus.KEEP),
                                apiPlaceDto(testFactory.orderPlace(order, "2"), ApiOrderStatus.KEEP)
                        )));
    }

    @Test
    void getMultiPlaceOrderForToday() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build()).get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null))
                .isEqualToIgnoringGivenFields(apiOrderDto(order, ApiOrderStatus.SORT_TO_COURIER,
                        List.of(
                                apiPlaceDto(testFactory.orderPlace(order, "1"), ApiOrderStatus.SORT_TO_COURIER),
                                apiPlaceDto(testFactory.orderPlace(order, "2"), ApiOrderStatus.SORT_TO_COURIER)
                        )), "availableCells", "yandexId");
    }

    @Test
    void getCancelledMultiPlaceOrderForToday() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .cancel().acceptPlaces("1", "2").get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null))
                .isEqualToIgnoringGivenFields(apiOrderDto(order, ApiOrderStatus.SORT_TO_WAREHOUSE,
                        List.of(
                                apiPlaceDto(testFactory.orderPlace(order, "1"), ApiOrderStatus.SORT_TO_WAREHOUSE),
                                apiPlaceDto(testFactory.orderPlace(order, "2"), ApiOrderStatus.SORT_TO_WAREHOUSE)
                        )), "availableCells", "yandexId");
    }

    @Test
    void getUseZoneForBufferReturnCells() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.USE_ZONE_FOR_BUFFER_RETURN_CELLS, "true");
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "true");

        var zone = testFactory.storedZone(sortingCenter, "zone1");
        var warehouseReturn = testFactory.storedWarehouse("warehouseReturn");
        var cellBR = testFactory.storedCell(sortingCenter, "brc", CellType.BUFFER, CellSubType.BUFFER_RETURNS,
                warehouseReturn.getYandexId(), zone);

        var order = testFactory.createForToday(order(sortingCenter)
                        .warehouseReturnId(warehouseReturn.getYandexId()).places("1", "2")
                        .build())
                .cancel().acceptPlaces("1", "2").sortPlace("1", cellBR.getId()).get();

        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), "2");
        assertThat(apiOrderDto.getAvailableCells()).containsOnly(apiCellDto(cellBR));
        assertThat(apiOrderDto.getUseZoneForBufferReturnCells()).isTrue();
    }

    private ApiOrderDto apiOrderDto(
            ScOrder order,
            ApiOrderStatus status,
            List<ApiPlaceDto> places
    ) {
        Optional<Route> routeO = testFactory.findOutgoingRoute(order);
        ApiCellDto cellTo = routeO.map(r -> apiCellDto(testFactory.determineRouteCell(r, order))).orElse(null);
        return apiOrderDto(
                order,
                status,
                places,
                cellTo == null ? List.of() : List.of(cellTo),
                false,
                Collections.emptyList());
    }

    private ApiOrderDto apiOrderDto(
            ScOrder order,
            ApiOrderStatus status,
            List<ApiPlaceDto> places,
            List<ApiCellDto> availableCells,
            boolean isLotSortAvailable,
            List<ApiCellLotDto> availableLots
    ) {
        Optional<ApiOrderDto.ApiOrderRouteDto> apiRouteO = apiOrderRouteDto(order);
        boolean direct = order.getOrderStatus().getFlowType() == OrderFlowType.DIRECT;
        return new ApiOrderDto(
                order.getId(),
                order.getExternalId(),
                status,
                apiRouteO.map(ApiOrderDto.ApiOrderRouteDto::getWarehouse).orElse(null),
                apiRouteO.orElse(null),
                order.isMiddleMile(),
                places,
                order.getOutgoingRouteDate(),
                order.isMiddleMile(),
                direct ? order.getDeliveryService().getName() : null,
                availableCells,
                isLotSortAvailable,
                availableLots,
                sortingCenterPropertySource.isUseZoneForBufferReturnCells(sortingCenter.getId())
        );
    }

    private ApiPlaceDto apiPlaceDto(
            Place place,
            ApiOrderStatus status
    ) {
        Optional<Route> routeO = testFactory.findOutgoingRoute(place);
        ApiCellDto cellTo = routeO.map(r -> apiCellDto(testFactory.determineRouteCell(r, place))).orElse(null);
        return apiPlaceDto(
                place,
                status,
                cellTo == null ? List.of() : List.of(cellTo),
                false,
                Collections.emptyList());
    }

    private ApiPlaceDto apiPlaceDto(
            Place place,
            ApiOrderStatus status,
            List<ApiCellDto> availableCells,
            boolean isLotSortAvailable,
            List<ApiCellLotDto> availableLots

    ) {
        Optional<ApiOrderDto.ApiOrderRouteDto> apiRouteO = apiOrderRouteDto(place);
        boolean direct = !place.getSortableStatus().wasCanceled();
        return new ApiPlaceDto(
                place.getMainPartnerCode(),
                apiCellDto(place.getCell()),
                status,
                place.getSortableStatus(),
                getCurrentLot(place),
                apiRouteO.map(ApiOrderDto.ApiOrderRouteDto::getWarehouse).orElse(null),
                apiRouteO.orElse(null),
                availableCells,
                isLotSortAvailable,
                availableLots,
                place.getOutgoingRouteDate(),
                place.isMiddleMile(),
                direct ? place.getDeliveryService().getName() : null
        );
    }

    @Nullable
    private ApiCellLotDto getCurrentLot(Place place) {
        if (place.getParent() == null) {
            return null;
        }
        return new ApiCellLotDto(new SortableLot(place.getLot(), place.getParent()));
    }

    private Optional<ApiOrderDto.ApiOrderRouteDto> apiOrderRouteDto(OrderLike orderOrPlace) {
        Optional<Route> warehouseRouteO = testFactory.findPossibleOutcomingWarehouseRouteByOrderId(orderOrPlace.getOrder().getId());
        Optional<Route> courierRouteO =
                testFactory.findOutgoingCourierRoute(orderOrPlace);
        return warehouseRouteO.map(route -> new ApiOrderDto.ApiOrderRouteDto(
                testFactory.getRouteIdForSortableFlow(route),
                null,
                apiWarehouseDto(Objects.requireNonNull(route.getWarehouseTo()))
        )).or(() -> courierRouteO.map(route -> new ApiOrderDto.ApiOrderRouteDto(
                testFactory.getRouteIdForSortableFlow(route),
                apiCourierDto(route.getCourierTo()),
                null
        )));
    }

    private ApiCellDto apiCellDto(Cell cell) {
        return cell == null ? null : new ApiCellDto(cell);
    }

    private ApiCourierDto apiCourierDto(Courier courier) {
        return courier == null ? null : new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(),
                courier.getDeliveryServiceId());
    }

    private ApiWarehouseDto apiWarehouseDto(Warehouse warehouse) {
        return new ApiWarehouseDto(
                warehouse.getId(), warehouse.getIncorporation(), warehouse.getYandexId(), warehouse.getType());
    }

    @Test
    void getMultiPlaceOrderSinglePlaceKeeped() {
        var order = testFactory.create(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1").keepPlaces("1").get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), "1"))
                .isEqualTo(apiOrderDto(order, ApiOrderStatus.KEEP,
                        List.of(
                                apiPlaceDto(testFactory.orderPlace(order, "1"), ApiOrderStatus.KEEP),
                                apiPlaceDto(testFactory.orderPlace(order, "2"), ApiOrderStatus.KEEP)
                        )));
    }

    @Test
    void getOrderWithAvailableLots() {
        testFactory.storedCell(sortingCenter, "rc3", CellType.RETURN, "w2");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var cell1 = testFactory.storedCell(sortingCenter, "rc1", CellType.RETURN, "w1");
        var cell2 = testFactory.storedCell(sortingCenter, "rc2", CellType.RETURN, "w1");
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2);
        var order =
                testFactory.createForToday(
                                order(sortingCenter)
                                        .dsType(DeliveryServiceType.TRANSIT)
                                        .externalId("1")
                                        .warehouseReturnId("w1")
                                        .build()
                        )
                        .accept()
                        .sort()
                        .ship()
                        .makeReturn()
                        .get();
        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrder.isLotSortAvailable()).isEqualTo(true);
        assertThat(apiOrder.getAvailableLots()).isNotNull();
        assertThat(CollectionUtils.isNonEmpty(apiOrder.getAvailableCells())).isTrue();
        assertThat(apiOrder.getAvailableCells().size()).isEqualTo(2);
        assertThat(apiOrder.getAvailableLots().size()).isEqualTo(2);
        assertThat(apiOrder.getAvailableLots().stream().map(ApiCellLotDto::getLotId))
                .containsOnly(lot1.getLotId(), lot2.getLotId());
    }


    @Test
        //https://st.yandex-team.ru/MARKETTPLSUPSC-16172
    void getOrderCanSortOrderToReturnCellIfOnePlaceInBufferedReturnAndOtherPlaceSortedDirectlyToLot() {
        testFactory.storedCell(sortingCenter, "rc3", CellType.RETURN, "w2");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "true");

        var cell1 = testFactory.storedCell(sortingCenter, "rc1", CellType.RETURN, "w1");
        var cell2 = testFactory.storedCell(sortingCenter, "rc2", CellType.RETURN, "w1");
        var cellBR = testFactory.storedCell(sortingCenter, "brc", CellType.BUFFER, CellSubType.BUFFER_RETURNS);

        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2);
        var order =
                testFactory.createForToday(
                                order(sortingCenter)
                                        .dsType(DeliveryServiceType.TRANSIT)
                                        .externalId("1")
                                        .warehouseReturnId("w1")
                                        .places("p1", "p2")
                                        .build()
                        )
                        .accept()
                        .sort()
                        .ship()
                        .makeReturn()
                        .acceptPlaces("p1", "p2")
                        .sortPlaces(cellBR.getId(), "p1")
                        .sortPlaceToLot(lot1.getLotId(), "p2")
                        .get();

        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        List<ApiCellDto> availableCells = apiOrder.getAvailableCells();

        assertThat(availableCells).hasSize(2);
        assertThat(availableCells.stream().mapToLong(ApiCellDto::getId).toArray())
                .containsExactlyInAnyOrder(cell1.getId(), cell2.getId());

    }

    @Test
    void getOrderWithLaterLot() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var order =
                testFactory.createForToday(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).places("p1",
                                "p2").build())
                        .acceptPlace("p1")
                        .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);

        var apiOrderReq1 = apiOrderDto(
                order,
                ApiOrderStatus.SORT_TO_COURIER,
                List.of(
                        apiPlaceDto(testFactory.orderPlace(order, "p1"), ApiOrderStatus.SORT_TO_COURIER, List.of(CellMapper.mapToApi(cell)), false, List.of()),
                        apiPlaceDto(testFactory.orderPlace(order, "p2"), ApiOrderStatus.SORT_TO_COURIER, List.of(CellMapper.mapToApi(cell)), false, List.of())
                ),
                List.of(CellMapper.mapToApi(cell)),
                false,
                List.of());
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null))
                .isEqualTo(apiOrderReq1);

        testFactory.sortPlace(order, "p1");

        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        testFactory.acceptPlace(order, "p2");
        order = testFactory.getOrder(order.getId());
        var apiOrderReq2 = apiOrderDto(
                order,
                ApiOrderStatus.SORT_TO_COURIER,
                List.of(
                        apiPlaceDto(testFactory.orderPlace(order, "p1"), ApiOrderStatus.OK, List.of(CellMapper.mapToApi(cell)), true, List.of(new ApiCellLotDto(lot))),
                        apiPlaceDto(testFactory.orderPlace(order, "p2"), ApiOrderStatus.SORT_TO_COURIER, List.of(CellMapper.mapToApi(cell)), true, List.of(new ApiCellLotDto(lot)))
                ),
                List.of(CellMapper.mapToApi(cell)),
                true,
                List.of(new ApiCellLotDto(lot)));
        var apiOrder2 = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), "p2");

        assertThat(apiOrder2)
                .isEqualTo(apiOrderReq2);
    }

    @Test
    void getOrderWithLotInWrongStatus() {
        var courierWithDs = testFactory.magistralCourier();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var cell = testFactory.storedMagistralCell(sortingCenter, "rc1",
                        CellSubType.DEFAULT, courierWithDs.courier().getId());
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        var lot2 = testFactory.storedLot(sortingCenter, cell, LotStatus.SHIPPED);

        var order =
                testFactory.createForToday(order(sortingCenter).externalId("1")
                                .deliveryService(courierWithDs.deliveryService())
                                .dsType(DeliveryServiceType.TRANSIT)
                                .warehouseReturnId("w1").build())
                        .accept()
                        .get();
        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrder.isLotSortAvailable()).isEqualTo(true);
        assertThat(apiOrder.getAvailableLots()).isNotNull();
        assertThat(apiOrder.getAvailableCells()).isNotNull();
        assertThat(apiOrder.getAvailableCells().size()).isEqualTo(1);
        assertThat(apiOrder.getAvailableLots().size()).isEqualTo(1);
        assertThat(apiOrder.getAvailableLots().stream().map(ApiCellLotDto::getLotId))
                .containsOnly(lot1.getLotId());
    }

    @Test
    void getOrderWithDeletedLot() {
        var courierWithDs = testFactory.magistralCourier();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var cell = testFactory.storedMagistralCell(sortingCenter, "rc1",
                CellSubType.DEFAULT, courierWithDs.courier().getId());
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell, LotStatus.PROCESSING, true);

        var order =
                testFactory.createForToday(order(sortingCenter).externalId("1")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(courierWithDs.deliveryService())
                                .warehouseReturnId("w1").build())
                        .accept()
                        .get();
        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrder.isLotSortAvailable()).isEqualTo(true);
        assertThat(apiOrder.getAvailableLots()).isNotNull();
        assertThat(apiOrder.getAvailableCells()).isNotNull();
        assertThat(apiOrder.getAvailableCells().size()).isEqualTo(1);
        assertThat(apiOrder.getAvailableLots().size()).isEqualTo(1);
        assertThat(apiOrder.getAvailableLots().stream().map(ApiCellLotDto::getLotId))
                .containsOnly(lot1.getLotId());
    }

    @Test
    void getOrderWithAvailableLotsInWrongSortingCenter() {
        testFactory.storedCell(sortingCenter, "rc3", CellType.RETURN, courier.getId());
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "false");
        var cell1 = testFactory.storedCell(sortingCenter, "rc1", CellType.RETURN, courier.getId());
        var cell2 = testFactory.storedCell(sortingCenter, "rc2", CellType.RETURN, courier.getId());
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2);

        var order =
                testFactory.createForToday(order(sortingCenter).externalId("1").dsType(DeliveryServiceType.TRANSIT).build())
                        .get();
        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrder.isLotSortAvailable()).isEqualTo(false);
        assertThat(apiOrder.getAvailableLots()).isNotNull();
        assertThat(apiOrder.getAvailableLots().isEmpty()).isEqualTo(true);
    }

    @Test
    void getOrderWithoutAvailableLots() {
        testFactory.storedCell(sortingCenter, "rc3", CellType.RETURN, "w2");
        var cell1 = testFactory.storedCell(sortingCenter, "rc1", CellType.RETURN, "w1");
        var cell2 = testFactory.storedCell(sortingCenter, "rc2", CellType.RETURN, "w2");
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2);
        var order =
                testFactory.createForToday(order(sortingCenter).externalId("1").dsType(DeliveryServiceType.TRANSIT).warehouseReturnId("w1").build())
                        .accept()
                        .sort()
                        .ship()
                        .makeReturn()
                        .accept()
                        .sort(cell1.getId())
                        .get();
        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrder.isLotSortAvailable()).isEqualTo(false);
        assertThat(apiOrder.getAvailableLots()).isNotNull();
        assertThat(apiOrder.getAvailableLots().isEmpty()).isEqualTo(true);
    }

    @Test
    void getMultiPlaceOrderSinglePlaceSortedToCourier() {
        var order =
                testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                        .acceptPlaces("1").sortPlaces("1").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), "2");

        ApiOrderDto expected = apiOrderDto(order, ApiOrderStatus.SORT_TO_COURIER,
                List.of(
                        apiPlaceDto(testFactory.orderPlace(order, "1"), ApiOrderStatus.OK),
                        apiPlaceDto(testFactory.orderPlace(order, "2"), ApiOrderStatus.SORT_TO_COURIER)
                ));

        assertThat(apiOrderDto)
                .isEqualToIgnoringGivenFields(expected, "yandexId");
    }

    @Test
    void getMultiPlaceOrderSinglePlaceSortedToCourier_middleMile() {
        var order =
                testFactory.createForToday(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).places(List.of("1",
                                "2")).build())
                        .acceptPlaces("1").sortPlaces("1").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), "2");

        ApiOrderDto expected = apiOrderDto(order, ApiOrderStatus.SORT_TO_COURIER,
                List.of(
                        apiPlaceDto(testFactory.orderPlace(order, "1"), ApiOrderStatus.OK),
                        apiPlaceDto(testFactory.orderPlace(order, "2"), ApiOrderStatus.SORT_TO_COURIER)
                ));

        assertThat(apiOrderDto)
                .isEqualToIgnoringGivenFields(expected, "yandexId");
    }

    @Test
    void getMultiPlaceOrderSinglePlaceSortedToWarehouse() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .cancel().acceptPlaces("1", "2").sortPlaces("1").get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), "2"))
                .isEqualToIgnoringGivenFields(apiOrderDto(order, ApiOrderStatus.SORT_TO_WAREHOUSE,
                        List.of(
                                apiPlaceDto(testFactory.orderPlace(order, "1"), ApiOrderStatus.OK),
                                apiPlaceDto(testFactory.orderPlace(order, "2"), ApiOrderStatus.SORT_TO_WAREHOUSE)
                        )), "availableCells", "yandexId");
    }

    @Test
    void getMultiPlaceOrderAllPlacesSortedToWarehouse() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .cancel().acceptPlaces("1", "2").sortPlaces("1", "2").get();
        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        Cell cell = testFactory.orderPlace(order, "1").getCell();
        assertThat(apiOrder)
                .isEqualToIgnoringGivenFields(apiOrderDto(order, ApiOrderStatus.OK,
                        List.of(
                                apiPlaceDto(testFactory.orderPlace(order, "1"), ApiOrderStatus.OK),
                                apiPlaceDto(testFactory.orderPlace(order, "2"), ApiOrderStatus.OK)
                        )), "availableCells", "yandexId");
        assertThat(apiOrder.getAvailableCells()).isEqualTo(List.of(apiCellDto(cell)));
    }

    @Test
    void getMultiPlaceOrderAllPlacesSortedToCourier() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrder)
                .isEqualToIgnoringGivenFields(apiOrderDto(order, ApiOrderStatus.OK,
                        List.of(
                                apiPlaceDto(testFactory.orderPlace(order, "1"), ApiOrderStatus.OK),
                                apiPlaceDto(testFactory.orderPlace(order, "2"), ApiOrderStatus.OK)
                        )), "availableCells", "yandexId");
        assertThat(apiOrder.getAvailableCells()).isEqualTo(List.of(apiCellDto(cell)));
    }

    @Test
    void getDamagedOrder() {
        var order = testFactory.createForToday(order(sortingCenter).warehouseCanProcessDamagedOrders(true).build())
                .accept().markOrderAsDamaged().get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(CollectionUtils.isNonEmpty(apiOrderDto.getAvailableCells())).isTrue();
        for (var cell : apiOrderDto.getAvailableCells()) {
            assertThat(cell.getType()).isEqualTo(CellType.RETURN);
            assertThat(cell.getSubType()).isEqualTo(CellSubType.RETURN_DAMAGED);
        }
    }

    @Test
    void getOrderIdsReturnsOrder() {
        var order = testFactory.createForToday(order(sortingCenter).externalId("o1").build()).get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<ApiOrderIdsDto> actual = orderQueryService.getRouteOrderIds(
                testFactory.getRouteIdForSortableFlow(route), null, sortingCenter, RouteTaskType.UNKNOWN
        );
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getExternalId()).isEqualTo("o1");
    }

    @Test
    void getOrderIdsByRouteReturnsShippedOrderWithShippedPlaces() {
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("o1").places("p1", "p2").build()
        ).acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        testFactory.shipOrderRoute(order);
        List<ApiOrderIdsDto> actual = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter,
                RouteTaskType.UNKNOWN);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getExternalId()).isEqualTo("o1");
        assertThat(actual.get(0).getPlaces()).hasSize(2);
        assertThat(actual.get(0).getPlaces().get(0).getExternalId()).isEqualTo("p1");
        assertThat(actual.get(0).getPlaces().get(1).getExternalId()).isEqualTo("p2");
    }

    @Test
    void getOrderIdsByCellReturnsShippedOrderWithShippedPlaces() {
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("o1").places("p1", "p2").build()
        ).acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        var cell = Objects.requireNonNull(testFactory.anyOrderPlace(order).getCell());
        testFactory.shipOrderRoute(order);
        List<ApiOrderIdsDto> actual = getOrderIdsByCell(cell.getId(), sortingCenter);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getExternalId()).isEqualTo("o1");
        assertThat(actual.get(0).getPlaces()).hasSize(2);
        assertThat(actual.get(0).getPlaces().get(0).getExternalId()).isEqualTo("p1");
        assertThat(actual.get(0).getPlaces().get(1).getExternalId()).isEqualTo("p2");
    }

    @Test
    void getOrderIdsByCellDoesNotReturnShippedOrderFromOtherCell() {
        String warehouseId = ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId();
        var damagedPlace = testFactory.createForToday(
                order(sortingCenter).externalId("damagedOrder").warehouseReturnId(warehouseId)
                        .warehouseCanProcessDamagedOrders(true).build()
        ).accept().markOrderAsDamaged().sort().getPlace();
        var clientReturn = testFactory.createForToday(
                order(sortingCenter).isClientReturn(true).externalId("clientReturn")
                        .warehouseReturnId(warehouseId).build()
        ).accept().sort().getPlace();
        var cellDamaged = Objects.requireNonNull(Objects.requireNonNull(damagedPlace).getCell());
        assertThat(cellDamaged.getSubtype()).isEqualTo(CellSubType.RETURN_DAMAGED);
        var cellClientReturn = Objects.requireNonNull(Objects.requireNonNull(clientReturn).getCell());
        assertThat(cellDamaged).isNotEqualTo(cellClientReturn);
        assertThat(cellClientReturn.getSubtype()).isEqualTo(CellSubType.CLIENT_RETURN);

        assertThat(testFactory.findOutgoingWarehouseRoute(damagedPlace).orElseThrow())
                .isEqualTo(testFactory.findOutgoingWarehouseRoute(clientReturn).orElseThrow());

        testFactory.shipOrderRoute(damagedPlace);
        testFactory.shipOrderRoute(clientReturn);

        List<ApiOrderIdsDto> actualDamaged = getOrderIdsByCell(cellDamaged.getId(), sortingCenter);
        assertThat(actualDamaged).hasSize(1);
        assertThat(actualDamaged.get(0).getExternalId()).isEqualTo("damagedOrder");

        List<ApiOrderIdsDto> actualClientReturn = getOrderIdsByCell(
                cellClientReturn.getId(), sortingCenter);
        assertThat(actualClientReturn).hasSize(1);
        assertThat(actualClientReturn.get(0).getExternalId()).isEqualTo(clientReturn.getExternalId());
    }

    @Test
    void getOrderIdsReturnsOrderByCellId() {
        var order = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = route.getCells(LocalDate.now(clock)).get(0);
        List<ApiOrderIdsDto> actual = getOrderIdsByCell(cell.getId(), sortingCenter);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getExternalId()).isEqualTo("o1");
    }

    @Test
    void getOrderIdsReturnsOrderFromOldRouteByCellId() {
        var place = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept()
                .sort()
                .getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        var cell = route.getCells(LocalDate.now(clock)).get(0);
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        List<ApiOrderIdsDto> orderIdDtos = getOrderIdsByCell(cell.getId(), sortingCenter);

        assertThat(orderIdDtos).hasSize(1);
        assertThat(orderIdDtos.get(0).getPlaces()).hasSize(1);
        ApiPlaceIdsDto placeIdDto = orderIdDtos.get(0).getPlaces().get(0);
        assertThat(placeIdDto.getStatus()).isEqualTo(ApiOrderListStatus.REMAINED_FROM_OLD_ROUTE);
    }

    @Test
    void getOrderIdsReturnsPlaces() {
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("o1").places("p1", "p2").build()
        ).get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<ApiOrderIdsDto> actual = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter,
                RouteTaskType.UNKNOWN);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(2);
        assertThat(actual.get(0).getPlaces().stream().map(ApiPlaceIdsDto::getExternalId).toList())
                .isEqualTo(List.of("p1", "p2"));
    }

    @Test
    void getOrderIdsReturnsPlacesByCellId() {
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("o1").places("p1", "p2").build()
        ).acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = route.getCells(LocalDate.now(clock)).get(0);
        List<ApiOrderIdsDto> actual = getOrderIdsByCell(cell.getId(), sortingCenter);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(2);
        assertThat(actual.get(0).getPlaces().stream().map(ApiPlaceIdsDto::getExternalId).toList())
                .isEqualTo(List.of("p1", "p2"));
    }

    @Test
    void getOrderIdsReturnsOrderWithoutCell() {
        var place = testFactory.createOrder(sortingCenter)
                .updateCourier(testFactory.defaultCourier()).updateShipmentDate(LocalDate.now(clock))
                .getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();

        List<ApiOrderIdsDto> orderIdDtos = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter,
                RouteTaskType.UNKNOWN);

        assertThat(orderIdDtos).hasSize(1);
        assertThat(orderIdDtos.get(0).getPlaces()).hasSize(1);
        ApiPlaceIdsDto placeIdDto = orderIdDtos.get(0).getPlaces().get(0);
        assertThat(placeIdDto.getCell()).isNull();
    }

    @Test
    void getAvailableCellsForOutgoingRoute() {
        testFactory.storedCell(sortingCenter, "rc3", CellType.RETURN, "w2");
        var cell1 = testFactory.storedCell(sortingCenter, "rc1", CellType.RETURN, "w1");
        var cell2 = testFactory.storedCell(sortingCenter, "rc2", CellType.RETURN, "w1");
        var order = testFactory.createForToday(order(sortingCenter).externalId("1").warehouseReturnId("w1").build())
                .accept()
                .sort()
                .ship()
                .makeReturn()
                .accept()
                .sort(cell1.getId())
                .get();
        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrder.getAvailableCells()).isNotNull();
        assertThat(apiOrder.getAvailableCells().size()).isEqualTo(2);
        assertThat(apiOrder.getAvailableCells().stream().map(ApiCellDto::getId))
                .containsOnly(cell1.getId(), cell2.getId());
    }

    @Test
    void getAvailableCellsForMultiPlaceOrder() {
        testFactory.storedCell(sortingCenter, "rc3", CellType.RETURN, "w2");
        var cell1 = testFactory.storedCell(sortingCenter, "rc1", CellType.RETURN, "w1");
        var cell2 = testFactory.storedCell(sortingCenter, "rc2", CellType.RETURN, "w1");
        var order = testFactory.createForToday(order(sortingCenter)
                        .externalId("1")
                        .places("p1", "p2")
                        .warehouseReturnId("w1")
                        .build())
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces(List.of("p1", "p2"))
                .ship()
                .makeReturn()
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlace("p1", cell1.getId())
                .sortPlace("p2", cell1.getId())
                .get();
        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(apiOrder.getAvailableCells()).isNotNull();
        assertThat(apiOrder.getAvailableCells().size()).isEqualTo(2);
        assertThat(apiOrder.getAvailableCells().stream().map(ApiCellDto::getId))
                .containsOnly(cell1.getId(), cell2.getId());
    }

    @Test
    void getAvailableCellsForMultiPlaceOrderWhenPlacesInDifferentCells() {
        var cell1 = testFactory.storedCell(sortingCenter, "rc1", CellType.RETURN, "w1");
        var cell2 = testFactory.storedCell(sortingCenter, "rc2", CellType.RETURN, "w1");
        var order = testFactory.createForToday(order(sortingCenter)
                        .externalId("1")
                        .places("p1", "p2")
                        .warehouseReturnId("w1")
                        .build())
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces(List.of("p1", "p2"))
                .ship()
                .makeReturn()
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlace("p1", cell1.getId())
                .sortPlace("p2", cell2.getId())
                .get();
        var apiOrder = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        var cells = apiOrder.getAvailableCells();
        cells = cells.stream().sorted(Comparator.comparingLong(ApiCellDto::getId)).toList();
        assertThat(cells)
                .isEqualTo(List.of(new ApiCellDto(cell1), new ApiCellDto(cell2)));
    }

    @Test
    void getOrderIdsReturnsOrderCellBuffer() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.BUFFER);
        var place = testFactory.createOrder(sortingCenter)
                .accept()
                .keep()
                .updateCourier(testFactory.defaultCourier())
                .updateShipmentDate(LocalDate.now(clock))
                .getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();

        List<ApiOrderIdsDto> orderIdDtos = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter,
                RouteTaskType.UNKNOWN);

        assertThat(orderIdDtos).hasSize(1);
        assertThat(orderIdDtos.get(0).getPlaces()).hasSize(1);
        ApiPlaceIdsDto placeIdDto = orderIdDtos.get(0).getPlaces().get(0);
        assertThat(placeIdDto.getCell()).isEqualTo(apiCellDto(cell));
    }

    @Test
    void getOrderIdsReturnsOrderInCellBufferByCellId() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.BUFFER);
        testFactory.createOrder(sortingCenter)
                .accept()
                .keep();
        List<ApiOrderIdsDto> orderIdDtos = getOrderIdsByCell(cell.getId(), sortingCenter);

        assertThat(orderIdDtos).hasSize(1);
        assertThat(orderIdDtos.get(0).getPlaces()).hasSize(1);
        ApiPlaceIdsDto placeIdDto = orderIdDtos.get(0).getPlaces().get(0);
        assertThat(placeIdDto.getCell()).isEqualTo(apiCellDto(cell));
    }

    @Test
    void getOrderIdsReturnsOrderCellCourier() {
        var zone = testFactory.storedZone(sortingCenter);
        var cell = testFactory.storedCell(sortingCenter, CellType.COURIER, zone);
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort()
                .getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();

        List<ApiOrderIdsDto> orderIdDtos = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter,
                RouteTaskType.UNKNOWN);

        assertThat(orderIdDtos).hasSize(1);
        assertThat(orderIdDtos.get(0).getPlaces()).hasSize(1);
        ApiPlaceIdsDto placeIdDto = orderIdDtos.get(0).getPlaces().get(0);
        assertThat(placeIdDto.getCell()).isEqualTo(apiCellDto(cell));
    }

    @Test
    void getOrderIdsReturnsOrderCellCourierByCellId() {
        var zone = testFactory.storedZone(sortingCenter);
        var cell = testFactory.storedCell(sortingCenter, CellType.COURIER, zone);
        testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort();

        List<ApiOrderIdsDto> orderIdDtos = getOrderIdsByCell(cell.getId(), sortingCenter);

        assertThat(orderIdDtos).hasSize(1);
        assertThat(orderIdDtos.get(0).getPlaces()).hasSize(1);
        ApiPlaceIdsDto placeIdDto = orderIdDtos.get(0).getPlaces().get(0);
        assertThat(placeIdDto.getCell()).isEqualTo(apiCellDto(cell));
    }

    @Test
    void getOrderIdsReturnsPlaceCellBuffer() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.BUFFER);
        var order = testFactory.create(order(sortingCenter, "o1").places("p1", "p2").build())
                .acceptPlaces("p1", "p2").keepPlaces("p1", "p2")
                .updateCourier(testFactory.defaultCourier()).updateShipmentDate(LocalDate.now(clock))
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();

        List<ApiOrderIdsDto> actual = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter,
                RouteTaskType.UNKNOWN);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(2);
        assertThat(getPlaceWithId(actual, "o1", "p1").getCell()).isEqualTo(apiCellDto(cell));
        assertThat(getPlaceWithId(actual, "o1", "p2").getCell()).isEqualTo(apiCellDto(cell));
    }

    @Test
    void getOrderIdsReturnsPlaceInCellBufferByCellId() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.BUFFER);
        testFactory.create(order(sortingCenter, "o1").places("p1", "p2").build())
                .acceptPlaces("p1", "p2").keepPlaces("p1", "p2")
                .get();
        List<ApiOrderIdsDto> actual = getOrderIdsByCell(cell.getId(), sortingCenter);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(2);
        assertThat(getPlaceWithId(actual, "o1", "p1").getCell()).isEqualTo(apiCellDto(cell));
        assertThat(getPlaceWithId(actual, "o1", "p2").getCell()).isEqualTo(apiCellDto(cell));
    }

    @Test
    void getOrderIdsReturnsPlaceCellCourier() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER);
        var order = testFactory.createForToday(order(sortingCenter, "o1").places("p1", "p2").build())
                .acceptPlaces("p1").sortPlaces("p1")
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<ApiOrderIdsDto> actual = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter,
                RouteTaskType.UNKNOWN);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(2);
        assertThat(getPlaceWithId(actual, "o1", "p1").getCell()).isEqualTo(apiCellDto(cell));
        assertThat(getPlaceWithId(actual, "o1", "p2").getCell()).isEqualTo(null);
    }

    @Test
    void getOrderIdsReturnsPlaceCellCourierByCellId() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER);
        testFactory.createForToday(order(sortingCenter, "o1").places("p1", "p2").build())
                .acceptPlaces("p1")
                .sortPlaces("p1");
        List<ApiOrderIdsDto> actual = getOrderIdsByCell(cell.getId(), sortingCenter);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(2);
        assertThat(getPlaceWithId(actual, "o1", "p1").getCell()).isEqualTo(apiCellDto(cell));
        assertThat(getPlaceWithId(actual, "o1", "p2").getCell()).isEqualTo(null);
    }

    @Test
    void getOrderIdsReturnsOrderStatus() {
        var orderNotOnSc = testFactory.createForToday(
                order(sortingCenter).externalId("orderNotOnSc").build())
                .getPlace();
        testFactory.createForToday(
                order(sortingCenter).externalId("orderOnScWithoutCell").build())
                .accept();
        testFactory.create(
                order(sortingCenter).externalId("orderShouldBeResorted").build())
                .accept()
                .keep()
                .updateCourier(testFactory.defaultCourier())
                .updateShipmentDate(LocalDate.now(clock));
        testFactory.createForToday(
                order(sortingCenter).externalId("orderInRightCell").build())
                .accept()
                .sort();
        var route = testFactory.findOutgoingCourierRoute(orderNotOnSc).orElseThrow();

        List<ApiOrderIdsDto> actual = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter,
                RouteTaskType.UNKNOWN);

        assertThat(actual).hasSize(4);
        assertThat(getPlaceWithId(actual, "orderNotOnSc").getStatus())
                .isEqualTo(ApiOrderListStatus.NOT_ACCEPTED_AT_SORTING_CENTER);
        assertThat(getPlaceWithId(actual, "orderOnScWithoutCell").getStatus())
                .isEqualTo(ApiOrderListStatus.ACCEPTED_AT_SORTING_CENTER);
        assertThat(getPlaceWithId(actual, "orderShouldBeResorted").getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);
        assertThat(getPlaceWithId(actual, "orderInRightCell").getStatus())
                .isEqualTo(ApiOrderListStatus.IN_RIGHT_CELL);
    }

    @Test
    void getOrderIdsReturnsOrderStatusByCellId() {
        var placeNotOnSc = testFactory.createForToday(
                order(sortingCenter).externalId("orderNotOnSc").build())
                .getPlace();
        testFactory.createForToday(
                order(sortingCenter).externalId("orderOnScWithoutCell").build())
                .accept();
        testFactory.create(
                order(sortingCenter).externalId("orderShouldBeResorted").build())
                .accept()
                .keep()
                .updateCourier(testFactory.defaultCourier())
                .updateShipmentDate(LocalDate.now(clock));
        testFactory.createForToday(
                order(sortingCenter).externalId("orderInRightCell").build())
                .accept()
                .sort();
        var route = testFactory.findOutgoingCourierRoute(placeNotOnSc).orElseThrow();
        var cell = route.getCells(LocalDate.now(clock)).get(0);

        List<ApiOrderIdsDto> actual = getOrderIdsByCell(cell.getId(), sortingCenter);

        assertThat(actual).hasSize(4);
        assertThat(getPlaceWithId(actual, "orderNotOnSc").getStatus())
                .isEqualTo(ApiOrderListStatus.NOT_ACCEPTED_AT_SORTING_CENTER);
        assertThat(getPlaceWithId(actual, "orderOnScWithoutCell").getStatus())
                .isEqualTo(ApiOrderListStatus.ACCEPTED_AT_SORTING_CENTER);
        assertThat(getPlaceWithId(actual, "orderShouldBeResorted").getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);
        assertThat(getPlaceWithId(actual, "orderInRightCell").getStatus())
                .isEqualTo(ApiOrderListStatus.IN_RIGHT_CELL);
    }

    @Test
    void getOrderIdsReturnsPlaceStatus() {
        var order = testFactory.createForToday(
                        order(sortingCenter, "o1").places(
                                List.of("placeNotOnSc", "placeOnScWithoutCell", "placeInRightCell")
                        ).build()
                )
                .acceptPlaces("placeOnScWithoutCell", "placeInRightCell")
                .sortPlaces("placeInRightCell")
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<ApiOrderIdsDto> actual = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter,
                RouteTaskType.UNKNOWN);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(3);
        assertThat(getPlaceWithId(actual, "o1", "placeNotOnSc").getStatus())
                .isEqualTo(ApiOrderListStatus.NOT_ACCEPTED_AT_SORTING_CENTER);
        assertThat(getPlaceWithId(actual, "o1", "placeOnScWithoutCell").getStatus())
                .isEqualTo(ApiOrderListStatus.ACCEPTED_AT_SORTING_CENTER);
        assertThat(getPlaceWithId(actual, "o1", "placeInRightCell").getStatus())
                .isEqualTo(ApiOrderListStatus.IN_RIGHT_CELL);
    }

    @Test
    void getOrderIdsReturnsPlaceStatusByCellId() {
        var order = testFactory.createForToday(
                        order(sortingCenter, "o1").places(
                                List.of("placeNotOnSc", "placeOnScWithoutCell", "placeInRightCell")
                        ).build()
                )
                .acceptPlaces("placeOnScWithoutCell", "placeInRightCell")
                .sortPlaces("placeInRightCell")
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = route.getCells(LocalDate.now(clock)).get(0);
        List<ApiOrderIdsDto> actualOrders = getOrderIdsByCell(cell.getId(), sortingCenter);
        assertThat(actualOrders).hasSize(1);
        assertThat(actualOrders.get(0).getPlaces()).hasSize(3);
        assertThat(getPlaceWithId(actualOrders, "o1", "placeNotOnSc").getStatus())
                .isEqualTo(ApiOrderListStatus.NOT_ACCEPTED_AT_SORTING_CENTER);
        assertThat(getPlaceWithId(actualOrders, "o1", "placeOnScWithoutCell").getStatus())
                .isEqualTo(ApiOrderListStatus.ACCEPTED_AT_SORTING_CENTER);
        assertThat(getPlaceWithId(actualOrders, "o1", "placeInRightCell").getStatus())
                .isEqualTo(ApiOrderListStatus.IN_RIGHT_CELL);
    }

    @Test
    void getOrderIdsReturnsPlaceStatusWrongCell() {
        var order = testFactory.create(
                        order(sortingCenter, "o1").places("placeShouldBeResorted1", "placeShouldBeResorted2").build()
                )
                .acceptPlaces("placeShouldBeResorted1", "placeShouldBeResorted2")
                .keepPlaces("placeShouldBeResorted1", "placeShouldBeResorted2")
                .updateCourier(testFactory.defaultCourier()).updateShipmentDate(LocalDate.now(clock))
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<ApiOrderIdsDto> actual = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter,
                RouteTaskType.UNKNOWN);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(2);
        assertThat(getPlaceWithId(actual, "o1", "placeShouldBeResorted1").getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);
    }

    @Test
    void getOrderIdsReturnsPlaceStatusWrongCellByCellId() {
        var order = testFactory.create(
                        order(sortingCenter, "o1").places("placeShouldBeResorted1", "placeShouldBeResorted2").build()
                )
                .acceptPlaces("placeShouldBeResorted1", "placeShouldBeResorted2")
                .keepPlaces("placeShouldBeResorted1", "placeShouldBeResorted2")
                .updateCourier(testFactory.defaultCourier()).updateShipmentDate(LocalDate.now(clock))
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = route.getCells(LocalDate.now(clock)).get(0);
        List<ApiOrderIdsDto> actual = getOrderIdsByCell(cell.getId(), sortingCenter);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(2);
        assertThat(getPlaceWithId(actual, "o1", "placeShouldBeResorted1").getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);
    }

    @Test
    void keptOrderShouldBeResorted() {
        var place = testFactory.createOrder(sortingCenter)
                .accept()
                .keep()
                .updateCourier(testFactory.defaultCourier()).updateShipmentDate(LocalDate.now(clock))
                .getPlace();
        var cell = Objects.requireNonNull(place.getCell());

        List<ApiOrderIdsDto> actual = getOrderIdsByCell(cell.getId(), sortingCenter);

        assertThat(actual).hasSize(1);
        assertThat(getPlaceWithId(actual, place.getExternalId()).getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);
    }

    @Test
    void orderRemainFromOldRoute() {
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort()
                .getPlace();
        var cell = Objects.requireNonNull(place.getCell());
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        List<ApiOrderIdsDto> actual = getOrderIdsByCell(cell.getId(), sortingCenter);

        assertThat(actual).hasSize(1);
        assertThat(getPlaceWithId(actual, place.getExternalId()).getStatus())
                .isEqualTo(ApiOrderListStatus.REMAINED_FROM_OLD_ROUTE);
    }

    @Test
    void placeRemainFromOldRoute() {
        assertThatThrownBy(() -> testFactory.createForToday(
                        order(sortingCenter).places("placeRemainFromOldRoute", "notAcceptedPlace").build()
                )
                .acceptPlaces("placeRemainFromOldRoute")
                .sortPlace("placeRemainFromOldRoute")
                .makeReturn() // нельзя возвратить здесь https://st.yandex-team.ru/MARKETTPLSC-4142

                .get());
    }


    public void setUpScProperty(SortingCenter sc) {
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        var whShop = testFactory.storedWarehouse("whShop-1", WarehouseType.SHOP);

        // склад с включенное поддержкой сортировки через аддресное хранение
        var whWarehouse = testFactory.storedWarehouse("whWarehouse-1", WarehouseType.SORTING_CENTER);
        testFactory.setWarehouseProperty(String.valueOf(whWarehouse.getYandexId()),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS,
                "true");
    }

    private static Set<String> getWarehouseReturnId() {
        return Set.of("whShop-1", "whWarehouse-1");
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getOrderIdsBufferReturnsByCellId(String whReturnYandexId) {
        setUpScProperty(sortingCenter);

        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1")
                .get();

        var order = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell.getId(), "o2p1")
                .get();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();
        List<ApiOrderIdsDto> actual = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), bufferCell.getId(),
                sortingCenter, RouteTaskType.BUFFER_RETURN);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(1);
        assertThat(getPlaceWithId(actual, "o1", "o1p2").getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);
        List<ApiOrderIdsDto> actual2 = orderQueryService.getRouteOrderIds(
                testFactory.getRoutable(route2).getId(), bufferCell.getId(),
                sortingCenter, RouteTaskType.BUFFER_RETURN);
        assertThat(actual2).hasSize(0);
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getOrderIdsBufferReturnsByCellIdInDifferentCells(String whReturnYandexId) {
        setUpScProperty(sortingCenter);

        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var bufferCell2 = testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS);

        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1")
                .get();

        var order = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell2.getId(), "o2p1", "o2p2")
                .get();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").keepPlaces(bufferCell.getId(), "o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();

        List<ApiOrderIdsDto> actual = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), bufferCell.getId(),
                sortingCenter, RouteTaskType.BUFFER_RETURN);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getPlaces()).hasSize(1);
        assertThat(getPlaceWithId(actual, "o1", "o1p2").getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);

        List<ApiOrderIdsDto> actual2 = orderQueryService.getRouteOrderIds(testFactory.getRouteIdForSortableFlow(route), bufferCell2.getId(),
                sortingCenter, RouteTaskType.BUFFER_RETURN);
        assertThat(actual2).hasSize(1);
        assertThat(actual2.get(0).getPlaces()).hasSize(2);
        assertThat(getPlaceWithId(actual2, "o2", "o2p1").getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);
        assertThat(getPlaceWithId(actual2, "o2", "o2p2").getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);
        List<ApiOrderIdsDto> actual3 = orderQueryService.getRouteOrderIds(testFactory.getRoutable(route2).getId(),
                bufferCell.getId(),
                sortingCenter, RouteTaskType.BUFFER_RETURN);
        assertThat(actual3).hasSize(1);
        assertThat(actual3.get(0).getPlaces()).hasSize(2);
        assertThat(getPlaceWithId(actual3, "o5", "o5p1").getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);
        assertThat(getPlaceWithId(actual3, "o5", "o5p2").getStatus())
                .isEqualTo(ApiOrderListStatus.SHOULD_BE_RESORTED);
    }

    private ApiPlaceIdsDto getPlaceWithId(List<ApiOrderIdsDto> orders, String externalId) {
        return getPlaceWithId(orders, externalId, externalId);
    }

    private ApiPlaceIdsDto getPlaceWithId(List<ApiOrderIdsDto> orders, String externalId, String externalPlaceId) {
        ApiOrderIdsDto order = orders.stream()
                .filter(o -> Objects.equals(externalId, o.getExternalId()))
                .findAny()
                .orElseThrow();
        return order.getPlaces().stream()
                .filter(p -> Objects.equals(externalPlaceId, p.getExternalId()))
                .findAny().orElseThrow();
    }

    private List<ApiOrderIdsDto> getOrderIdsByCell(long cellId, SortingCenter sortingCenter) {
        return orderQueryService.getRouteOrderIds(null, cellId, sortingCenter, RouteTaskType.UNKNOWN);
    }

    @Test
    void checkCargoTypeForBufferReturns() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        testFactory.storedWarehouse("whShop-1", WarehouseType.SHOP);
        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS)
                .cargoType(CellCargoType.NONE);
        var bufferCell = testFactory.storedCell(sortingCenter, "br1", cellFieldBuilder);
        testFactory.createOrder(order(sortingCenter, "o1")
                        .warehouseReturnId("whShop-1").places("p1", "p2").build())
                .acceptPlaces("p1")
                .cancel()
                .get();

        ApiOrderDto result = orderQueryService.getOrderForApi(sortingCenter, "o1", "p1");
        assertThat(result.getAvailableCells())
                .filteredOn(cell -> cell.getId() == bufferCell.getId())
                .allMatch(cell -> cell.getCargoType() == CellCargoType.NONE);
    }
}
