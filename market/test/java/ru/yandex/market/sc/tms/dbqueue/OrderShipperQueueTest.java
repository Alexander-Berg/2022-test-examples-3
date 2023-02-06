package ru.yandex.market.sc.tms.dbqueue;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BiFunction;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.dbqueue.ScQueueType;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTmsTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderShipperQueueTest {

    private final TestFactory testFactory;
    private final DbQueueTestUtil dbQueueTestUtil;

    SortingCenter acceptSortingCenter;
    SortingCenter sortingCenterAnarhiya;
    User userBezdelnik;
    User userAcceptSc;

    @MockBean
    Clock clock;

    @BeforeEach
    void init() {
        sortingCenterAnarhiya = testFactory.storedSortingCenter(1000L);
        userBezdelnik = testFactory.storedUser(sortingCenterAnarhiya, 100L);
        acceptSortingCenter = testFactory.storedSortingCenter(2000L);
        userAcceptSc = testFactory.storedUser(acceptSortingCenter, 200L);
        testFactory.setupMockClock(clock);
    }

    @Nested
    @DisplayName("Одноместный заказ")
    class SingleOrder {
        BiFunction<SortingCenter, String, TestFactory.TestOrderBuilder> singleOrder =
                (sortingCenter, externalOrderId) -> testFactory.createForToday(
                        order(sortingCenter, externalOrderId)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build());

        @Test
        @DisplayName("Отгрузка заказа в статусе 'Принят'")
        void shipSinglePlaceOrderAccepted() {
            var unshippedOrder = singleOrder.apply(sortingCenterAnarhiya, "o1").accept(userBezdelnik).get();
            var acceptingOrder = singleOrder.apply(acceptSortingCenter, "o1").accept(userAcceptSc).get();

            assertThat(dbQueueTestUtil.getTasks(ScQueueType.ORDER_SHIPPER)).hasSize(1);
            dbQueueTestUtil.executeSingleQueueItem(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        }

        @Test
        @DisplayName("Отгрузка заказа в статусе 'Отсортирован'")
        void shipSinglePlaceOrderSorted() {
            var unshippedOrder = singleOrder.apply(sortingCenterAnarhiya, "o1")
                    .accept(userBezdelnik).sort().get();
            var acceptingOrder = singleOrder.apply(acceptSortingCenter, "o1")
                    .accept(userAcceptSc).get();

            assertThat(dbQueueTestUtil.getTasks(ScQueueType.ORDER_SHIPPER)).hasSize(1);
            dbQueueTestUtil.executeSingleQueueItem(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        }

        @Test
        @DisplayName("Не отгружаем заказ в статусе 'Создан'")
        void dontShipSinglePlaceOrderCreated() {
            var unshippedOrder = singleOrder.apply(sortingCenterAnarhiya, "o1").get();
            var acceptingOrder = singleOrder.apply(acceptSortingCenter, "o1").accept(userAcceptSc).get();

            assertThat(dbQueueTestUtil.getTasks(ScQueueType.ORDER_SHIPPER)).hasSize(0);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        }

    }

    @Nested
    @DisplayName("Многоместный заказ")
    class MultiplaceOrder {
        BiFunction<SortingCenter, String, TestFactory.TestOrderBuilder> multiplaceOrder =
                (sortingCenter, externalId) -> testFactory.createForToday(
                        order(sortingCenter, externalId)
                                .places("1", "2")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build());

        @Test
        @DisplayName("Отгрузка одной посылки (все посылки отсортированы)")
        void shipPartialMultiplace() {
            var unshippedOrder = multiplaceOrder.apply(sortingCenterAnarhiya, "o3")
                    .acceptPlaces().sortPlaces().get();
            var acceptingOrder = multiplaceOrder.apply(acceptSortingCenter, "o3").get();
            testFactory.acceptPlace(acceptingOrder, "1", userAcceptSc);

            dbQueueTestUtil.executeSingleQueueItem(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(unshippedOrder, "1").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(testFactory.orderPlace(unshippedOrder, "2").getStatus()).isEqualTo(PlaceStatus.SORTED);

            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(acceptingOrder, "1").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(testFactory.orderPlace(acceptingOrder, "2").getStatus()).isEqualTo(PlaceStatus.CREATED);
        }

        @Test
        @DisplayName("Отгрузка всех посылок (все посылки отсортированы)")
        void shipFullMultiplace() {
            var unshippedOrder = multiplaceOrder.apply(sortingCenterAnarhiya, "o4")
                    .acceptPlaces().sortPlaces().get();
            var acceptingOrder = multiplaceOrder.apply(acceptSortingCenter, "o4").get();
            testFactory.acceptPlace(acceptingOrder, "1", userAcceptSc);
            testFactory.acceptPlace(acceptingOrder, "2", userAcceptSc);

            assertThat(dbQueueTestUtil.getTasks(ScQueueType.ORDER_SHIPPER)).hasSize(2);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
            assertThat(testFactory.orderPlace(unshippedOrder, "1").getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(testFactory.orderPlace(unshippedOrder, "2").getStatus()).isEqualTo(PlaceStatus.SHIPPED);

            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(acceptingOrder, "1").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(testFactory.orderPlace(acceptingOrder, "2").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        }

        @Test
        @DisplayName("Последовательная отгрузка посылок")
        void shipSequentiallyFullMultiplace() {
            var unshippedOrder = multiplaceOrder.apply(sortingCenterAnarhiya, "o5")
                    .acceptPlaces().sortPlaces().get();
            var acceptingOrder = multiplaceOrder.apply(acceptSortingCenter, "o5").get();

            testFactory.acceptPlace(acceptingOrder, "1", userAcceptSc);
            dbQueueTestUtil.executeSingleQueueItem(ScQueueType.ORDER_SHIPPER);
            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(unshippedOrder, "1").getStatus())
                    .isEqualTo(PlaceStatus.SHIPPED);
            assertThat(testFactory.orderPlace(unshippedOrder, "2").getStatus())
                    .isEqualTo(PlaceStatus.SORTED);

            testFactory.acceptPlace(acceptingOrder, "2", userAcceptSc);
            dbQueueTestUtil.executeSingleQueueItem(ScQueueType.ORDER_SHIPPER);
            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ORDER_SHIPPED_TO_SO_FF);
            assertThat(testFactory.orderPlace(unshippedOrder, "1").getStatus())
                    .isEqualTo(PlaceStatus.SHIPPED);
            assertThat(testFactory.orderPlace(unshippedOrder, "2").getStatus())
                    .isEqualTo(PlaceStatus.SHIPPED);

            long numberOfShippedRouteFinishPlaceRecordsForPlace = testFactory.orderPlaces(unshippedOrder).stream()
                    .map(Place::getId)
                    .map(testFactory::findRouteFinishPlacesByPlaceId)
                    .flatMap(List::stream)
                    .filter(rfp -> rfp.getFinishedPlaceStatus().equals(PlaceStatus.SHIPPED))
                    .count();

            assertThat(numberOfShippedRouteFinishPlaceRecordsForPlace).isEqualTo(2);
        }

        @Test
        @DisplayName("Отгрузка посылки в статусе 'Принято'")
        void shipPlaceInAcceptedState() {
            var unshippedOrder = multiplaceOrder.apply(sortingCenterAnarhiya, "o5")
                    .acceptPlaces("1").get();
            var acceptingOrder = multiplaceOrder.apply(acceptSortingCenter, "o5").get();
            testFactory.acceptPlace(acceptingOrder, "1", userAcceptSc);

            dbQueueTestUtil.executeSingleQueueItem(ScQueueType.ORDER_SHIPPER);
            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(unshippedOrder, "1").getStatus())
                    .isEqualTo(PlaceStatus.SHIPPED);
            assertThat(testFactory.orderPlace(unshippedOrder, "2").getStatus())
                    .isEqualTo(PlaceStatus.CREATED);
        }

        @Test
        @DisplayName("Отгрузка посылки в статусе 'Отсортировано'")
        void shipPlaceInSortedState() {
            var unshippedOrder = multiplaceOrder.apply(sortingCenterAnarhiya, "o5")
                    .acceptPlaces("1").sortPlaces("1").get();
            var acceptingOrder = multiplaceOrder.apply(acceptSortingCenter, "o5").get();
            testFactory.acceptPlace(acceptingOrder, "1", userAcceptSc);

            dbQueueTestUtil.executeSingleQueueItem(ScQueueType.ORDER_SHIPPER);
            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(unshippedOrder, "1").getStatus())
                    .isEqualTo(PlaceStatus.SHIPPED);
            assertThat(testFactory.orderPlace(unshippedOrder, "2").getStatus())
                    .isEqualTo(PlaceStatus.CREATED);
            long numberOfShippedRouteFinishPlaceRecordsForPlace = testFactory.findRouteFinishPlacesByPlaceId(
                            testFactory.orderPlaces(unshippedOrder)
                                    .stream().filter(p -> p.getMainPartnerCode().equals("1")).findFirst().get()
                                    .getId()).stream()
                    .filter(rfp -> rfp.getFinishedPlaceStatus().equals(PlaceStatus.SHIPPED))
                    .count();
            assertThat(numberOfShippedRouteFinishPlaceRecordsForPlace).isEqualTo(1);
        }

    }

    @Nested
    @DisplayName("Возвратный поток")
    class ReturnFlow {

        BiFunction<SortingCenter, String, TestFactory.TestOrderBuilder> singleOrder =
                (sortingCenter, externalOrderId) -> testFactory.createForToday(
                        order(sortingCenter, externalOrderId)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build());

        BiFunction<SortingCenter, String, TestFactory.TestOrderBuilder> multiplaceOrder =
                (sortingCenter, externalId) -> testFactory.createForToday(
                        order(sortingCenter, externalId)
                                .places("1", "2")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build());

        @Test
        @DisplayName("Отгрузка заказа в статусе 'Готов к отгрузке' по событию с возвратного потока")
        void shipSinglePlaceOrderAcceptedByEventFromReturnFlow() {
            var unshippedOrder = singleOrder.apply(sortingCenterAnarhiya, "o1")
                    .accept(userBezdelnik).sort().get();
            var acceptingOrder = singleOrder.apply(acceptSortingCenter, "o1")
                    .cancel().accept(userAcceptSc).get();

            assertThat(dbQueueTestUtil.getTasks(ScQueueType.ORDER_SHIPPER)).hasSize(1);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);

            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        }

        @Test
        @DisplayName("Не применяем автоматическую отгрузку заказам, статус которых последним менял не кладовщик")
        void doNotShipOrderLastOperationWasNotPerformedByStockman() {
            var unshippedOrder = singleOrder.apply(sortingCenterAnarhiya, "o1")
                    .accept(userBezdelnik).get();

            testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.MINUTES));
            testFactory.cancelOrder(unshippedOrder);

            var orderStatusHistoryItems = testFactory.findOrderStatusHistoryItems(unshippedOrder.getId());
            assertThat(orderStatusHistoryItems).isNotEmpty();
            var anyHistoryItem = orderStatusHistoryItems.get(0);
            var notMatchingUpdateTimeItemNumber = orderStatusHistoryItems.stream()
                    .filter(hi -> hi.getOrderUpdateTime() != anyHistoryItem.getOrderUpdateTime())
                    .count();
            assertThat(notMatchingUpdateTimeItemNumber).isNotZero();
            var acceptingOrder = singleOrder.apply(acceptSortingCenter, "o1")
                    .cancel().accept(userAcceptSc).get();

            assertThat(dbQueueTestUtil.getTasks(ScQueueType.ORDER_SHIPPER)).hasSize(0);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        }

        @Test
        @DisplayName("Отгрузка заказа в статусе 'Готов к отгрузке' по событию с возвратного потока без маршрута")
        void shipOrderWithoutRouteByEventFromReturnFlow() {
            var unshippedOrder = singleOrder.apply(sortingCenterAnarhiya, "o1")
                    .accept(userBezdelnik).sort().get();

            testFactory.setupMockClock(clock, clock.instant().plus(3, ChronoUnit.DAYS));

            var acceptingOrder = singleOrder.apply(acceptSortingCenter, "o1")
                    .cancel().accept(userAcceptSc).get();

            assertThat(dbQueueTestUtil.getTasks(ScQueueType.ORDER_SHIPPER)).hasSize(1);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);

            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        }

        @Test
        @DisplayName("Отгрузка заказа в возвратном статусе '170' по событию с возвратного потока")
        void shipSinglePlaceOrder170AcceptedByEventFromReturnFlow() {
            var unshippedOrder = singleOrder.apply(sortingCenterAnarhiya, "o1")
                    .cancel().accept(userBezdelnik).get();
            var acceptingOrder = singleOrder.apply(acceptSortingCenter, "o1")
                    .cancel().accept(userAcceptSc).get();

            assertThat(dbQueueTestUtil.getTasks(ScQueueType.ORDER_SHIPPER)).hasSize(1);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        }

        @Test
        @DisplayName("Отгрузка заказа в возвратном статусе '170' по событию с возвратного потока без маршрута")
        void shipSinglePlaceOrder170AcceptedByEventFromReturnFlowWithoutRoute() {
            var unshippedOrder = singleOrder.apply(sortingCenterAnarhiya, "o1")
                    .cancel().accept(userBezdelnik).get();

            testFactory.setupMockClock(clock, clock.instant().plus(3, ChronoUnit.DAYS));

            var acceptingOrder = singleOrder.apply(acceptSortingCenter, "o1")
                    .cancel().accept(userAcceptSc).get();

            assertThat(dbQueueTestUtil.getTasks(ScQueueType.ORDER_SHIPPER)).hasSize(1);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        }

        @Test
        @DisplayName("Отгрузка заказа в возвратном статусе '175' по событию с возвратного потока")
        void shipSinglePlaceOrder175AcceptedByEventFromReturnFlow() {
            var unshippedOrder = singleOrder.apply(sortingCenterAnarhiya, "o1")
                    .cancel().accept(userBezdelnik).sort().get();
            var acceptingOrder = singleOrder.apply(acceptSortingCenter, "o1")
                    .cancel().accept(userAcceptSc).get();

            assertThat(dbQueueTestUtil.getTasks(ScQueueType.ORDER_SHIPPER)).hasSize(1);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        }

        @Test
        @DisplayName("Отгрузка одной посылки на возвратном потоке (все посылки приняты)")
        void shipAcceptedOneByOneMultiplace() {
            var unshippedOrder = multiplaceOrder.apply(sortingCenterAnarhiya, "o3")
                    .cancel().acceptPlaces().get();
            var acceptingOrder = multiplaceOrder.apply(acceptSortingCenter, "o3").cancel().get();

            testFactory.acceptPlace(acceptingOrder, "1", userAcceptSc);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(unshippedOrder, "1").getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.orderPlace(unshippedOrder, "2").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);

            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(acceptingOrder, "1").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(testFactory.orderPlace(acceptingOrder, "2").getStatus()).isEqualTo(PlaceStatus.CREATED);

            testFactory.acceptPlace(acceptingOrder, "2", userAcceptSc);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
            assertThat(testFactory.orderPlace(unshippedOrder, "1").getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.orderPlace(unshippedOrder, "2").getStatus()).isEqualTo(PlaceStatus.RETURNED);

            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(acceptingOrder, "1").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(testFactory.orderPlace(acceptingOrder, "2").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        }

        @Test
        @DisplayName("Отгрузка одной посылки на возвратном потоке (все посылки отсортированы)")
        void shipSortedOneByOneMultiplace() {
            var unshippedOrder = multiplaceOrder.apply(sortingCenterAnarhiya, "o3")
                    .cancel().acceptPlaces().sortPlaces().get();
            var acceptingOrder = multiplaceOrder.apply(acceptSortingCenter, "o3").cancel().get();

            testFactory.acceptPlace(acceptingOrder, "1", userAcceptSc);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
            assertThat(testFactory.orderPlace(unshippedOrder, "1").getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.orderPlace(unshippedOrder, "2").getStatus()).isEqualTo(PlaceStatus.SORTED);

            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(acceptingOrder, "1").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(testFactory.orderPlace(acceptingOrder, "2").getStatus()).isEqualTo(PlaceStatus.CREATED);

            testFactory.acceptPlace(acceptingOrder, "2", userAcceptSc);
            dbQueueTestUtil.executeAllQueueItems(ScQueueType.ORDER_SHIPPER);

            assertThat(testFactory.getOrder(unshippedOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
            assertThat(testFactory.orderPlace(unshippedOrder, "1").getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(testFactory.orderPlace(unshippedOrder, "2").getStatus()).isEqualTo(PlaceStatus.RETURNED);

            assertThat(testFactory.getOrder(acceptingOrder.getId()).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
            assertThat(testFactory.orderPlace(acceptingOrder, "1").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(testFactory.orderPlace(acceptingOrder, "2").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        }

    }
}
