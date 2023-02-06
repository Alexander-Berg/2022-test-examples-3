package ru.yandex.market.sc.internal.controller.lms;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.exception.ScInvalidTransitionException;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.controller.dto.OrderWithReturnWarehousePartnerId;
import ru.yandex.market.sc.internal.controller.dto.OrdersToShipDto;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbIntTest
class LmsOrderServiceTest {

    @Autowired
    LmsOrderService lmsOrderService;
    @Autowired
    TestFactory testFactory;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    CellRepository cellRepository;
    @Autowired
    ScanService scanService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    Clock clock;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 123L);
    }

    @Test
    void changeWarehouseReturn() {
        String returnSortingCenterId = "550";
        var returnSc = testFactory.storedSortingCenter(Long.parseLong(returnSortingCenterId));
        var warehouseReturn = testFactory.storedWarehouse(returnSc.getYandexId());
        var place =
                testFactory.createOrderForToday(sortingCenter)
                        .accept()
                        .sort()
                        .ship()
                        .makeReturn()
                        .accept()
                        .sort()
                        .getPlace();
        lmsOrderService.changeWarehouseReturn(List.of(new OrderWithReturnWarehousePartnerId(
                String.valueOf(place.getOrderId()),
                returnSortingCenterId, null)
        ));
        place = testFactory.getPlace(place.getId());
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(place.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(place.getCell()).isNull();
        assertThat(place.getLot()).isNull();
        var createdCells =
                cellRepository.findAllBySortingCenterAndTypeAndSubtypeAndDeletedAndWarehouseYandexIdOrderByScNumberAscIdDesc(
                        place.getSortingCenter(),
                        CellType.RETURN,
                        CellSubType.DEFAULT,
                        false,
                        returnSc.getYandexId()
                );
        assertThat(createdCells).hasSize(1);
        assertThat(createdCells.get(0).getScNumber()).isEqualTo("RETURN SORTING_CENTER [456439232550]");

        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(place))
                .orElseThrow();
        String externalId = place.getExternalId();

        transactionTemplate.execute(t -> {
            var routable = testFactory.getRoutable(route);
            var cellsOnRoute = routable.getCells();
            assertThat(cellsOnRoute).hasSize(1);
            assertThat(cellsOnRoute.get(0)).isEqualTo(createdCells.get(0));
            assertThat(route.getWarehouseTo()).isEqualTo(warehouseReturn);

            var apiOrder = scanService.getOrder(externalId, null, null, new ScContext(user));
            assertThat(apiOrder.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
            return null;
        });
    }

    @Test
    void changeShopWarehouseReturn() {
        String shopWhId = "123123";
        var warehouseReturn = testFactory.storedWarehouse(shopWhId, WarehouseType.SHOP);
        var place =
                testFactory.createOrderForToday(sortingCenter)
                        .accept()
                        .sort()
                        .ship()
                        .makeReturn()
                        .accept()
                        .sort()
                        .getPlace();
        lmsOrderService.changeWarehouseReturn(List.of(new OrderWithReturnWarehousePartnerId(
                String.valueOf(place.getOrderId()),
                null,
                shopWhId)
        ));
        place = testFactory.getPlace(place.getId());
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(place.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(place.getCell()).isNull();
        assertThat(place.getLot()).isNull();
        var createdCells =
                cellRepository.findAllBySortingCenterAndTypeAndSubtypeAndDeletedAndWarehouseYandexIdOrderByScNumberAscIdDesc(
                        place.getSortingCenter(),
                        CellType.RETURN,
                        CellSubType.DEFAULT,
                        false,
                        shopWhId
                );
        assertThat(createdCells).hasSize(1);
        assertThat(createdCells.get(0).getScNumber()).isEqualTo("RETURN SHOP [123123]");
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();
        String externalId = place.getExternalId();
        transactionTemplate.execute(t -> {
            var routable = testFactory.getRoutable(route);
            var cellsOnRoute = routable.getCells();
            assertThat(cellsOnRoute).hasSize(1);
            assertThat(cellsOnRoute.get(0)).isEqualTo(createdCells.get(0));
            assertThat(route.getWarehouseTo()).isEqualTo(warehouseReturn);

            var apiOrder = scanService.getOrder(externalId, null, null, new ScContext(user));
            assertThat(apiOrder.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
            return null;
        });
    }

    @Test
    void returnToBufferNullsPlacesLot() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").build()
                )
                .acceptPlaces("p1", "p2")
                .sortPlaces("p1", "p2")
                .sortPlaceToLot("SC_LOT_1", SortableType.PALLET, "p1", "p2")
                .get();
        assertThat(testFactory.orderPlace(order, "p1").getParent()).isNotNull();
        assertThat(testFactory.orderPlace(order, "p2").getParent()).isNotNull();
        lmsOrderService.returnToBuffer(List.of(order.getId()));
        assertThat(testFactory.orderPlace(order, "p1").getParent()).isNull();
        assertThat(testFactory.orderPlace(order, "p2").getParent()).isNull();
    }

    private static byte[] getCsvToCourierWithCourier(List<OrderLike> ordersToCourier) {
        return (HEADER_TO_COURIER_WITH_COURIER + ordersToCourier.stream()
                .map(order -> String.format("%s,%d,%d", order.getExternalId(),
                        order.getSortingCenter().getId(), Objects.requireNonNull(order.getCourier()).getId()))
                .collect(Collectors.joining("\n"))).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] getCsvToCourierWithAndWithoutCourier(
            List<OrderLike> ordersWithCourier,
            List<OrderLike> ordersWithoutCourier
    ) {
        return (HEADER_TO_COURIER_WITH_COURIER + Stream.concat(
                        ordersWithCourier.stream()
                                .map(order -> String.format("%s,%d,%d", order.getExternalId(),
                                        order.getSortingCenter().getId(),
                                        Objects.requireNonNull(order.getCourier()).getId())),
                        ordersWithoutCourier.stream()
                                .map(order -> String.format("%s,%d,", order.getExternalId(),
                                        order.getSortingCenter().getId()))
                )
                .collect(Collectors.joining("\n"))).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void shipToCourierSinglePlace() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        lmsOrderService.shipToCourier(List.of(order.getId()));
        assertShippedToCourier(order.getId());
    }

    @Test
    void shipToCourierWithoutCell() {
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().get();
        testFactory.shipOrderRouteAndDisableCellDistribution(order1);
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build()).get();
        lmsOrderService.shipToCourier(List.of(order2.getId()));
        assertShippedToCourier(order2.getId());
    }


    @Test
    void shipToCourier() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).get();
        lmsOrderService.shipToCourier(List.of(order.getId()));
        assertShippedToCourier(order.getId());
    }

    @Test
    void shipToCourierAccepted() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1").get();
        lmsOrderService.shipToCourier(List.of(order.getId()));
        assertShippedToCourier(order.getId());
    }

    @Test
    void shipToCourierHalfSorted() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1").sortPlaces("1").get();
        lmsOrderService.shipToCourier(List.of(order.getId()));
        assertShippedToCourier(order.getId());
    }

    @Test
    void shipToCourierSorted() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1", "2").sortPlaces("1", "2").get();
        lmsOrderService.shipToCourier(List.of(order.getId()));
        assertShippedToCourier(order.getId());
    }

    @Test
    void shipToCourierToReturn() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).cancel().acceptPlaces("1", "2").makeReturn().get();
        assertThatThrownBy(() -> lmsOrderService.shipToCourier(List.of(order.getId())))
                .hasMessageContaining("Заказ из возвратного потока нельзя перевести на прямой поток.");
    }

    @Test
    void shipToCourierNotTodaysOrder() {
        var order = testFactory.create(
                order(sortingCenter).places("1", "2").build()
        )
                .updateCourier(testFactory.defaultCourier())
                .get();
        lmsOrderService.shipToCourier(List.of(order.getId()));
        assertShippedToCourier(order.getId());
    }

    @Test
    void shipToCourierKeepedOrder() {
        var order = testFactory.create(
                order(sortingCenter).places("1", "2").build()
        ).updateCourier(testFactory.defaultCourier()).acceptPlaces("1", "2").keepPlaces("1").get();
        lmsOrderService.shipToCourier(List.of(order.getId()));
        assertShippedToCourier(order.getId());
    }

    private void assertShippedToCourier(long orderId) {
        var order = scOrderRepository.findByIdOrThrow(orderId);
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        List<Place> places = testFactory.orderPlaces(order);
        for (Place place : places) {
            assertThat(place.getCell()).isNull();
            assertThat(place.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        }
    }

    private static byte[] getCsvToCourierWithoutCourier(List<OrderLike> ordersToCourier) {
        return (HEADER_TO_COURIER_WITHOUT_COURIER + ordersToCourier.stream()
                .map(order -> String.format("%s,%d", order.getExternalId(),
                        order.getSortingCenter().getId()))
                .collect(Collectors.joining("\n"))).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void returnToWarehouse() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).cancel().get();
        lmsOrderService.returnToWarehouse(List.of(order.getId()));
        assertReturnedToWarehouse(order.getId());
    }

    @Test
    void returnAcceptedToWarehouse() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1", "2").get();
        assertThatThrownBy(() -> lmsOrderService.returnToWarehouse(List.of(order.getId())))
                .isInstanceOf(ScInvalidTransitionException.class)
                .hasMessageContaining("Не отгруженный заказ прямого потока, нельзя перевести на возвратный поток.");
    }

    @Test
    void returnSortedToWarehouse() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).acceptPlaces("1", "2").sortPlaces("1", "2").get();
        assertThatThrownBy(() -> lmsOrderService.returnToWarehouse(List.of(order.getId())))
                .isInstanceOf(ScInvalidTransitionException.class)
                .hasMessageContaining("Не отгруженный заказ прямого потока, нельзя перевести на возвратный поток.");
    }

    @Test
    void returnCanceledToWarehouse() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).cancel().acceptPlaces("1", "2").sortPlaces("1", "2").get();
        lmsOrderService.returnToWarehouse(List.of(order.getId()));
        assertReturnedToWarehouse(order.getId());
    }

    private void assertReturnedToWarehouse(long orderId) {
        var order = scOrderRepository.findByIdOrThrow(orderId);

        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

        List<Place> places = testFactory.orderPlaces(order);
        for (Place place : places) {
            assertThat(place.getCell()).isNull();
            assertThat(place.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        }
    }

    @Test
    void singleShipToCourier() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).get();
        var ordersToShip = new OrdersToShipDto(getCsvToCourierWithCourier(List.of(order)), null, null);
        lmsOrderService.modifyOrdersByExternalId(ordersToShip);

        assertShippedToCourier(order.getId());
    }

    @Test
    void multipleShipToCourierWithCourier() {
        var orders = generateListOrders(50, 0, false);
        var ordersToShip = new OrdersToShipDto(getCsvToCourierWithCourier(orders), null, null);
        lmsOrderService.modifyOrdersByExternalId(ordersToShip);

        orders.forEach(order -> assertShippedToCourier(order.getId()));
    }

    @Test
    void multipleShipToCourierWithoutCourier() {
        var orders = generateListOrders(50, 0, false);
        var ordersToShip = new OrdersToShipDto(getCsvToCourierWithoutCourier(orders), null, null);
        lmsOrderService.modifyOrdersByExternalId(ordersToShip);

        orders.forEach(order -> assertShippedToCourier(order.getId()));
    }

    @Test
    void multipleShipToCourierWithAndWithoutCourier() {
        var orders1 = generateListOrders(50, 0, false);
        var orders2 = generateListOrders(50, 0, false);
        var ordersToShip = new OrdersToShipDto(
                getCsvToCourierWithAndWithoutCourier(orders1, orders2), null, null);
        lmsOrderService.modifyOrdersByExternalId(ordersToShip);

        orders1.forEach(order -> assertShippedToCourier(order.getId()));
        orders2.forEach(order -> assertShippedToCourier(order.getId()));
    }

    @Test
    void singleReturnToWarehouse() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").build()
        ).cancel().get();
        var ordersToShip = new OrdersToShipDto(null, getCsvToWarehouse(List.of(order)), null);
        lmsOrderService.modifyOrdersByExternalId(ordersToShip);

        assertReturnedToWarehouse(order.getId());
    }

    @Test
    void multipleReturnToWarehouse() {
        var orders = generateListOrders(50, 0, true);
        var ordersToShip = new OrdersToShipDto(null, getCsvToWarehouse(orders), null);
        lmsOrderService.modifyOrdersByExternalId(ordersToShip);

        orders.forEach(order -> assertReturnedToWarehouse(order.getId()));
    }

    @Test
    void multipleShipAndReturn() {
        var ordersToCourier = generateListOrders(25, 0, false);
        var ordersToWarehouse = generateListOrders(25, 25, true);
        var ordersToShip = new OrdersToShipDto(getCsvToCourierWithCourier(ordersToCourier),
                getCsvToWarehouse(ordersToWarehouse), null);
        lmsOrderService.modifyOrdersByExternalId(ordersToShip);

        ordersToCourier.forEach(order -> assertShippedToCourier(order.getId()));
        ordersToWarehouse.forEach(order -> assertReturnedToWarehouse(order.getId()));
    }

    private static byte[] getCsvToWarehouse(List<OrderLike> ordersToWarehouse) {
        return (headerToWarehouse + ordersToWarehouse.stream()
                .map(order -> String.format("%s,%d", order.getExternalId(), order.getSortingCenter().getId()))
                .collect(Collectors.joining("\n"))).getBytes(StandardCharsets.UTF_8);
    }

    private final static String HEADER_TO_COURIER_WITH_COURIER = "id,scId,courierId\n";

    @Test
    void returnToBufferReturnOrder() {
        var place =
                testFactory.createOrderForToday(sortingCenter)
                        .cancel()
                        .accept()
                        .sort()
                        .getPlace();
        assertThat(place.getCell()).isNotNull();
        assertThat(place.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        lmsOrderService.returnToBuffer(List.of(place.getOrderId()));

        place = testFactory.updated(place);
        assertThat(place.getCell()).isNull();
        assertThat(place.getOrderStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void returnToBufferOrder() {
        var place =
                testFactory.createOrderForToday(sortingCenter)
                        .accept()
                        .sort()
                        .getPlace();
        assertThat(place.getCell()).isNotNull();
        assertThat(place.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        lmsOrderService.returnToBuffer(List.of(place.getOrderId()));

        place = testFactory.updated(place);
        assertThat(place.getCell()).isNull();
        assertThat(place.getOrderStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    private final static String HEADER_TO_COURIER_WITHOUT_COURIER = "id,scId\n";

    private final static String headerToWarehouse = "id,scId\n";

    private List<OrderLike> generateListOrders(int count, int start, boolean returnFlow) {
        var orders = new ArrayList<OrderLike>();
        for (int i = start; i < start + count; i++) {
            TestFactory.TestOrderBuilder orderBuilder = testFactory.createForToday(
                    order(sortingCenter)
                            .externalId(Integer.toString(i))
                            .places(Integer.toString(i), Integer.toString(i + 1)).build()
            );
            if (returnFlow) {
                orderBuilder = orderBuilder.cancel();
            }
            orders.add(orderBuilder.get());
        }

        return orders;
    }
}
