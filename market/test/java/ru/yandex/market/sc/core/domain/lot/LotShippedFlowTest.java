package ru.yandex.market.sc.core.domain.lot;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * Процесс отгрузки лотов (паллет)
 * по прямому/возвратному потоку.
 */
@EmbeddedDbTest
@DisplayName("Процесс сортировки/отгрузки возвратов на склад лотами")
public class LotShippedFlowTest {

    @Autowired
    private TestFactory testFactory;
    @Autowired
    private TransactionTemplate transactionTemplate;

    SortingCenter sortingCenter;
    TestFactory.CourierWithDs magistralCourierWithDs;

    private static final String LOT_EXTERNAL_ID = "SC_LOT_100000";

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        magistralCourierWithDs = testFactory.magistralCourier();
    }

    @Nested
    @DisplayName("Прямой поток|Средняя миля")
    class DirectFlowMiddleMile {
        Supplier<TestFactory.TestOrderBuilder> multiplaceOrder = () -> testFactory.createForToday(
                order(sortingCenter, UUID.randomUUID().toString())
                        .places("1", "2", "3")
                        .deliveryService(magistralCourierWithDs.deliveryService())
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build());

        @Test
        @DisplayName("[Полный многоместный] Отгрузка лота")
        void shipRouteForLots1() {
            var order = multiplaceOrder.get()
                    .acceptPlaces()
                    .sortPlaces()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1", "2", "3")
                    .prepareToShipLot(1)
                    .get();

            var lotId = testFactory.orderPlace(order, "1").getLotId().orElseThrow();
            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);

            var places = testFactory.orderPlaces(order);
            assertThat(places).allMatch(p -> p.getStatus() == PlaceStatus.SHIPPED);

            var lot = testFactory.getLot(lotId);
            assertThat(lot.getOptLotStatus()).isEmpty();
            assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        }

        @Test
        @DisplayName("[Неполный многоместный] Все заказы приняты / 2 заказа отсортировано")
        void shipRouteForLots2() {
            var order = multiplaceOrder.get()
                    .acceptPlaces()
                    .sortPlaces()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1", "2")
                    .prepareToShipLot(1)
                    .get();

            var lotId = testFactory.orderPlace(order, "1").getLotId().orElseThrow();
            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);

            var places = testFactory.orderPlaces(order);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(places.get(2).getStatus()).isEqualTo(PlaceStatus.SORTED);

            var lot = testFactory.getLot(lotId);
            assertThat(lot.getOptLotStatus()).isEmpty();
            assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 заказа принято / 2 заказа отсортировано")
        void shipRouteForLots3() {
            var cell = testFactory.storedMagistralCell(sortingCenter, magistralCourierWithDs.courier().getId());
            var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = multiplaceOrder.get()
                    .acceptPlaces("1", "2")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1", "2")
                    .prepareToShipLot(1)
                    .get();

            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);

            var places = testFactory.orderPlaces(order);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(places.get(2).getStatus()).isEqualTo(PlaceStatus.CREATED);

            lot = testFactory.getLot(lot.getLotId());
            assertThat(lot.getOptLotStatus()).isEmpty();
            assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 заказа принято / 1 заказ отсортирован")
        void shipRouteForLots4() {
            var cell = testFactory.storedMagistralCell(sortingCenter, magistralCourierWithDs.courier().getId());
            var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = multiplaceOrder.get()
                    .acceptPlaces("1", "2").sortPlaces("1")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "2")
                    .prepareToShipLot(1)
                    .get();

            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);

            var places = testFactory.orderPlaces(order);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SORTED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(places.get(2).getStatus()).isEqualTo(PlaceStatus.CREATED);

            lot = testFactory.getLot(lot.getLotId());
            assertThat(lot.getOptLotStatus()).isEmpty();
            assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        }
    }

    private void assertShipSingleReturnOrderMiddleMile(Long placeId, Long routeId, Long lotId) {
        var place = testFactory.getPlace(placeId);
        assertThat(place.isMiddleMile()).isTrue();
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place.getCell()).isNull();
        assertThat(place.getParent()).isNull();

        transactionTemplate.execute( t -> {
                    var route = testFactory.getRoutable(routeId, sortingCenter);
//        var route = testFactory.getRoute(routeId);
                    var routeFinishOrders = route.getAllRouteFinishOrders();
                    assertThat(routeFinishOrders).hasSize(1);
                    assertThat(routeFinishOrders.get(0).getStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
                    return  null;
                });
        var lot = testFactory.getLot(lotId);
        assertThat(lot.getOptLotStatus()).isEmpty();
        assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
    }

    @Nested
    @DisplayName("Возвратный поток (возврат заказа)|Средняя миля")
    class ReturnOrderMiddleMile {
        Supplier<TestFactory.TestOrderBuilder> returnMultiplaceOrderMiddleMile = () -> testFactory.createForToday(
                        order(sortingCenter, UUID.randomUUID().toString())
                                .places("1", "2", "3")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .acceptPlaces().sortPlaces().ship()
                .makeReturn();

        Supplier<TestFactory.TestOrderBuilder> returnSingleOrderMiddleMile = () -> testFactory.createForToday(
                        order(sortingCenter, UUID.randomUUID().toString())
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept().sort().ship()
                .makeReturn();

        @Test
        @DisplayName("[Одноместный заказа] Отсортирован в ячейку / отсортирован в лот")
        void shipLotsWithSingleOrderMiddleMile1() {
            var place = returnSingleOrderMiddleMile.get()
                    .accept().sort()
                    .sortToLot(LOT_EXTERNAL_ID, SortableType.PALLET)
                    .prepareToShipLot(1)
                    .getPlace();

            var lot = Objects.requireNonNull(place.getLot());
            var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertShipSingleReturnOrderMiddleMile(place.getId(), testFactory.getRouteIdForSortableFlow(route), lot.getId());
        }

        @Test
        @DisplayName("[Одноместный заказа] Отсортирован напрямую в лот")
        void shipLotsWithSingleOrderMiddleMile2() {
            var place = returnSingleOrderMiddleMile.get()
                    .accept()
                    .enableSortMiddleMileToLot()
                    .sortToLot(LOT_EXTERNAL_ID, SortableType.PALLET)
                    .prepareToShipLot(1)
                    .getPlace();

            var lot =  Objects.requireNonNull(place.getLot());
            var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertShipSingleReturnOrderMiddleMile(place.getId(), testFactory.getRouteIdForSortableFlow(route), lot.getId());
        }

        @Test
        @DisplayName("[Полный многоместный заказ] Отсортированы в ячейку / отсортированы в лот")
        void shipLotsWithFullMultiplaceOrderMiddleMile1() {
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces().sortPlaces()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1", "2", "3")
                    .prepareToShipLot(1)
                    .getPlaces();

            Place place1 = order.get("1");
            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            var lotId = place1.getLotId().orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM,
                    testFactory.getRouteIdForSortableFlow(route), lotId);
            assertThat(testFactory.updated(order.get("1")).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(order.get("2")).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(order.get("3")).getStatus()).isEqualTo(PlaceStatus.RETURNED);
        }

        @Test
        @DisplayName("[Полный многоместный заказ] Отсортированы в ячейку / отсортированы в разные лоты")
        void shipLotsWithFullMultiplaceOrderMiddleMile2() {
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces().sortPlaces()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1")
                    .prepareToShipLot(1)
                    .sortPlaceToLot("SC_LOT_100001", SortableType.PALLET, "2")
                    .prepareToShipLot(2)
                    .sortPlaceToLot("SC_LOT_100002", SortableType.PALLET, "3")
                    .prepareToShipLot(3)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            var lotId1 = place1.getLotId().orElseThrow();
            var lotId2 = place2.getLotId().orElseThrow();
            var lotId3 = place3.getLotId().orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM,
                    testFactory.getRouteIdForSortableFlow(route), lotId1, lotId2, lotId3);
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.RETURNED);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 посылки приняты / отсортированы напрямую в один лот")
        void shipLotsWithPartialMultiplaceOrderMiddleMile1() {
            var cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN);
            var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1", "2")
                    .prepareToShipLot(1)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN,
                    testFactory.getRouteIdForSortableFlow(route), lot.getLotId());
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 посылки приняты / отсортированы в ячейку / " +
                "отсортированы в один лот")
        void shipLotsWithPartialMultiplaceOrderMiddleMile2() {
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2")
                    .sortPlaces("1", "2")
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1", "2")
                    .prepareToShipLot(1)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var lotId = place1.getLotId().orElseThrow();
            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN,
                    testFactory.getRouteIdForSortableFlow(route), lotId);
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 посылки приняты / 1 отсортирована в ячейку / 1 отсортирована в лот")
        void shipLotsWithPartialMultiplaceOrderMiddleMile3() {
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2")
                    .sortPlaces("1")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "2")
                    .prepareToShipLot(1)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var lotId = place2.getLotId().orElseThrow();
            var route = testFactory.findOutgoingWarehouseRoute(place2).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNotNull();
            assertShipMultiplaceReturnOrderMiddleMile(place2.getId(),
                    ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, testFactory.getRouteIdForSortableFlow(route), lotId);
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.SORTED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 посылки приняты / отсортированы напрямую в разные лоты")
        void shipLotsWithPartialMultiplaceOrderMiddleMile4() {
            var cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN);
            var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1")
                    .prepareToShipLot(1)
                    .sortPlaceToLot("SC_LOT_100001", SortableType.PALLET, "2")
                    .prepareToShipLot(2)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN,
                    testFactory.getRouteIdForSortableFlow(route), lot1.getLotId(), lot2.getLotId());
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 посылки приняты / отсортированы в ячейку / " +
                "отсортированы в разные лоты")
        void shipLotsWithPartialMultiplaceOrderMiddleMile5() {
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2")
                    .sortPlaces("1", "2")
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1")
                    .prepareToShipLot(1)
                    .sortPlaceToLot("SC_LOT_100001", SortableType.PALLET, "2")
                    .prepareToShipLot(2)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var lotId1 = place1.getLotId().orElseThrow();
            var lotId2 = place2.getLotId().orElseThrow();
            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN,
                    testFactory.getRouteIdForSortableFlow(route), lotId1, lotId2);
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        }

        @Test
        @DisplayName("[Неполный многоместный] полностью принят / 2 посылки отсортированы напрямую в один лот")
        void shipLotsWithPartialMultiplaceOrderMiddleMile6() {
            var cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN);
            var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2", "3")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1", "2")
                    .prepareToShipLot(1)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE,
                    testFactory.getRouteIdForSortableFlow(route), lot.getLotId());
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        }

        @Test
        @DisplayName("[Неполный многоместный] полностью принят / " +
                "2 посылки отсортированы напрямую в разные лоты")
        void shipLotsWithPartialMultiplaceOrderMiddleMile7() {
            var cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN);
            var l1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var l2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2", "3")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1")
                    .prepareToShipLot(1)
                    .sortPlaceToLot("SC_LOT_100001", SortableType.PALLET, "2")
                    .prepareToShipLot(2)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE,
                    testFactory.getRouteIdForSortableFlow(route), l1.getLotId(), l2.getLotId());
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        }

        @Test
        @DisplayName("[Неполный многоместный] полностью принят / 2 отсортированы в ячейку / " +
                "2 отсортированы в один лот")
        void shipLotsWithPartialMultiplaceOrderMiddleMile8() {
            var cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN);
            var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2", "3")
                    .sortPlaces("1", "2")
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1", "2")
                    .prepareToShipLot(1)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE,
                    testFactory.getRouteIdForSortableFlow(route), lot.getLotId());
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        }

        @Test
        @DisplayName("[Неполный многоместный] полностью принят / 2 отсортированы в ячейку / " +
                "1 отсортирован напрямую в лот")
        void shipLotsWithPartialMultiplaceOrderMiddleMile9() {
            var cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN);
            var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2", "3")
                    .sortPlaces("1", "2")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1")
                    .prepareToShipLot(1)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place2).getCell()).isNotNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(),
                    ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, testFactory.getRouteIdForSortableFlow(route), lot.getLotId());
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.SORTED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        }

        @Test
        @DisplayName("[Неполный многоместный] полностью принят / полностью отсортирован в ячейку / " +
                "2 отсортированы в разные лоты")
        void shipLotsWithPartialMultiplaceOrderMiddleMile10() {
            var cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN);
            var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces()
                    .sortPlaces()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1")
                    .prepareToShipLot(1)
                    .sortPlaceToLot("SC_LOT_100001", SortableType.PALLET, "2")
                    .prepareToShipLot(2)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place3).getCell()).isNotNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(),
                    ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, testFactory.getRouteIdForSortableFlow(route), lot1.getLotId(),
                    lot2.getLotId());
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.SORTED);
        }

        @Test
        @DisplayName("[Неполный многоместный] полностью принят / 1 отсортирована в ячейку / " +
                "1 отсортирована в лот")
        void shipLotsWithPartialMultiplaceOrderMiddleMile11() {
            var cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN);
            var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = returnMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2", "3")
                    .sortPlaces("1")
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1")
                    .prepareToShipLot(1)
                    .getPlaces();
            Place place1 = order.get("1");
            Place place2 = order.get("2");
            Place place3 = order.get("3");

            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertThat(testFactory.updated(place1).getCell()).isNull();
            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE,
                    testFactory.getRouteIdForSortableFlow(route), lot.getLotId());
            assertThat(testFactory.updated(place1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.updated(place2).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(testFactory.updated(place3).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("Возвратный поток (отмена заказа)|Средняя миля")
    class CancelOrderMiddleMile {

        Supplier<TestFactory.TestOrderBuilder> cancelMultiplaceOrderMiddleMile = () -> testFactory.createForToday(
                        order(sortingCenter, UUID.randomUUID().toString())
                                .places("1", "2", "3")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .cancel();

        @Test
        @DisplayName("[Неполный многоместный] полностью принят / 2 посылки отсортированы напрямую в один лот")
        void shipLotsWithPartialMultiplaceOrderMiddleMile22() {
            var cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN);
            var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = cancelMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2", "3")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1", "2")
                    .prepareToShipLot(1)
                    .getPlaces();
            Place place2 = order.get("2");

            var route = testFactory.findOutgoingWarehouseRoute(place2).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertShipMultiplaceReturnOrderMiddleMile(place2.getId(), ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE,
                    testFactory.getRouteIdForSortableFlow(route), lot.getLotId());
        }

        @Test
        @DisplayName("[Неполный многоместный] полностью принят / 2 посылки отсортированы напрямую  в разные лоты")
        void shipLotsWithPartialMultiplaceOrderMiddleMile23() {
            var cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN);
            var l1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var l2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
            var order = cancelMultiplaceOrderMiddleMile.get()
                    .acceptPlaces("1", "2", "3")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1")
                    .prepareToShipLot(1)
                    .sortPlaceToLot("SC_LOT_100001", SortableType.PALLET, "2")
                    .prepareToShipLot(2)
                    .getPlaces();
            Place place1 = order.get("1");

            var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            assertShipMultiplaceReturnOrderMiddleMile(place1.getId(), ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE,
                    testFactory.getRouteIdForSortableFlow(route), l1.getLotId(), l2.getLotId());
        }
    }

    @Nested
    @DisplayName("Возвратный поток (возврат заказа)|Последняя миля")
    class ReturnOrderLastMile {
        Supplier<TestFactory.TestOrderBuilder> returnSingleOrderLastMile = () -> testFactory.createForToday(
                        order(sortingCenter, "o1")
                                .dsType(DeliveryServiceType.LAST_MILE_COURIER)
                                .build())
                .accept().sort().ship()
                .makeReturn();

        Supplier<TestFactory.TestOrderBuilder> returnMultiplaceOrderLastMile = () -> testFactory.createForToday(
                        order(sortingCenter, "o1")
                                .places("1", "2", "3")
                                .dsType(DeliveryServiceType.LAST_MILE_COURIER)
                                .build()
                )
                .acceptPlaces().sortPlaces().ship()
                .makeReturn();

        @Test
        @DisplayName("[Одноместный заказ] Отсортирован в ячейку / отсортирован в лот")
        void shipLotsWithSingleOrderLastMile() {
            var order = returnSingleOrderLastMile.get()
                    .accept()
                    .sort()
                    .sortToLot(LOT_EXTERNAL_ID, SortableType.PALLET)
                    .prepareToShipLot(1)
                    .get();

            var lot =  Objects.requireNonNull(testFactory.orderPlace(order).getLot());
            var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            order = testFactory.getOrder(order.getId());
            assertThat(order.isMiddleMile()).isFalse();
            assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
            var sortableLot = testFactory.getLot(lot.getId());
            assertThat(sortableLot.getOptLotStatus()).isEmpty();
            assertThat(sortableLot.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
            transactionTemplate.execute( t ->
            assertThat(testFactory.getRoutable(route).getAllRouteFinishOrders()).hasSize(1));
//            assertThat(testFactory.getRoute(testFactory.getRouteIdForSortableFlow(route)).getAllRouteFinishOrders()).hasSize(1);
        }

        @Test
        @DisplayName("[Полный многоместный заказ] Отсортированы в ячейку / отсортированы в один лот")
        void shipLotsWithFullMultiplaceOrderLastMile() {
            var o1 = returnMultiplaceOrderLastMile.get()
                    .acceptPlaces()
                    .sortPlaces()
                    .sortPlaceToLot(LOT_EXTERNAL_ID, SortableType.PALLET, "1", "2", "3")
                    .prepareToShipLot(1)
                    .get();

            var route = testFactory.findOutgoingWarehouseRoute(o1).orElseThrow();
            var lotId = testFactory.orderPlace(o1, "1").getLotId().orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            var order = testFactory.getOrder(o1.getId());
            assertThat(order.isMiddleMile()).isFalse();
            assertThat(order.getPlaceCount()).isEqualTo(3);
            assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
            assertThat(testFactory.orderPlace(order, "1").getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.orderPlace(order, "2").getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.orderPlace(order, "3").getStatus()).isEqualTo(PlaceStatus.RETURNED);

            var lot = testFactory.getLot(lotId);
            assertThat(lot.getOptLotStatus()).isEmpty();
            assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);

            transactionTemplate.execute( t -> {
                var routable = testFactory.getRoutable(route);
//            route = testFactory.getRoute(testFactory.getRouteIdForSortableFlow(route));
                assertThat(routable.getAllRouteFinishOrders()).hasSize(1);
                return null;
            });
        }
    }

    private void assertShipMultiplaceReturnOrderMiddleMile(Long placeId, ScOrderFFStatus orderStatus, Long routeId,
                                                           Long... lotIds) {
        var place = testFactory.getPlace(placeId);
        assertThat(place.isMiddleMile()).isTrue();
        assertThat(place.getFfStatus()).isEqualTo(orderStatus);
        assertThat(place.getParent()).isNull();
        transactionTemplate.execute( t -> {
            var route = testFactory.getRoutable(routeId, sortingCenter);
    //        var route = testFactory.getRoute(routeId);
            var routeFinishOrders = route.getAllRouteFinishOrders();
            assertThat(routeFinishOrders).hasSize(1);
            assertThat(routeFinishOrders.get(0).getStatus()).isEqualTo(orderStatus);
            return null;
        });
        Arrays.stream(lotIds).forEach(id -> {
            var lot = testFactory.getLot(id);
            assertThat(lot.getOptLotStatus()).isEmpty();
            assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        });

    }
}
