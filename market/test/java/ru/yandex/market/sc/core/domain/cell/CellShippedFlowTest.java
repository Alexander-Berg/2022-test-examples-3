package ru.yandex.market.sc.core.domain.cell;

import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.RouteFacade;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * Процесс отгрузки ячейки для
 * прямого/возвратного потока.
 */
@EmbeddedDbTest
@DisplayName("Процесс отгрузки ячейки")
public class CellShippedFlowTest {

    @Autowired
    private TestFactory testFactory;
    @Autowired
    private RouteFacade routeFacade;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 16L);
    }

    @Nested
    @DisplayName("Прямой поток|Средняя миля")
    class DirectFlowMultiplaceOrderMiddleMile {
        Supplier<TestFactory.TestOrderBuilder> multiplaceOrder = () -> testFactory.createForToday(
                order(sortingCenter, UUID.randomUUID().toString())
                        .places("1", "2", "3")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build());

        @Test
        @DisplayName("[Полный многоместный] Отгрузка ячейки")
        void shipRouteByCell1() {
            var order = multiplaceOrder.get()
                    .acceptPlaces()
                    .sortPlaces()
                    .get();

            Place place = testFactory.anyOrderPlace(order);

            var route = testFactory.findOutgoingRoute(place).orElseThrow();
            Long cellId = place.getCellId().orElseThrow();
            FinishRouteRequestDto request = FinishRouteRequestDto.builder()
                    .cellId(cellId).build();
            routeFacade.finishOutgoingRoute(testFactory.getRouteIdForSortableFlow(route), request, new ScContext(user));

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);

            var places = testFactory.orderPlaces(order);
            assertThat(places).allMatch(p -> p.getSortableStatus() == SortableStatus.SHIPPED_DIRECT);
        }

        @Test
        @DisplayName("[Неполный многоместный] Все заказы приняты / 2 заказа отсортировано")
        void shipRouteByCell2() {
            var order = multiplaceOrder.get()
                    .acceptPlaces()
                    .sortPlaces("1", "2")
                    .get();

            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            Long cellId = testFactory.orderPlace(order, "1").getCellId().orElseThrow();
            FinishRouteRequestDto request = FinishRouteRequestDto.builder()
                    .cellId(cellId).build();
            routeFacade.finishOutgoingRoute(testFactory.getRouteIdForSortableFlow(route), request, new ScContext(user));

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);

            var places = testFactory.orderPlaces(order);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(places.get(2).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 заказа принято / 2 заказа отсортировано")
        void shipRouteByCell3() {
            var order = multiplaceOrder.get()
                    .acceptPlaces("1", "2")
                    .sortPlaces("1", "2")
                    .get();

            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            Long cellId = testFactory.orderPlace(order, "1").getCellId().orElseThrow();
            FinishRouteRequestDto request = FinishRouteRequestDto.builder()
                    .cellId(cellId).build();
            routeFacade.finishOutgoingRoute(testFactory.getRouteIdForSortableFlow(route), request, new ScContext(user));

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);

            var places = testFactory.orderPlaces(order);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(places.get(2).getStatus()).isEqualTo(PlaceStatus.CREATED);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 заказа принято / 1 заказ отсортирован")
        void shipRouteByCell4() {
            var order = multiplaceOrder.get()
                    .acceptPlaces("1", "2")
                    .sortPlaces("1")
                    .get();

            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            Long cellId = testFactory.orderPlace(order, "1").getCellId().orElseThrow();
            FinishRouteRequestDto request = FinishRouteRequestDto.builder()
                    .cellId(cellId).build();
            routeFacade.finishOutgoingRoute(testFactory.getRouteIdForSortableFlow(route), request, new ScContext(user));

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);

            var places = testFactory.orderPlaces(order);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(places.get(2).getStatus()).isEqualTo(PlaceStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("Возвратный поток|Средняя миля")
    class ReturnFlowMultiplaceOrderMiddleMile {
        Supplier<TestFactory.TestOrderBuilder> returnMultiplaceOrder = () -> testFactory.createForToday(
                        order(sortingCenter, UUID.randomUUID().toString())
                                .places("1", "2", "3")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .acceptPlaces().sortPlaces().ship()
                .makeReturn();

        @Test
        @DisplayName("[Полный многоместный] Отгрузка ячейки")
        void shipRouteByCell1() {
            var order = returnMultiplaceOrder.get()
                    .acceptPlaces()
                    .sortPlaces()
                    .get();

            Place place = testFactory.anyOrderPlace(order);

            var route = testFactory.findOutgoingRoute(place).orElseThrow();
            Long cellId = place.getCellId().orElseThrow();
            FinishRouteRequestDto request = FinishRouteRequestDto.builder()
                    .cellId(cellId).build();
            routeFacade.finishOutgoingRoute(testFactory.getRouteIdForSortableFlow(route), request, new ScContext(user));

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

            var places = testFactory.orderPlaces(order);
            assertThat(places).allMatch(p -> p.getSortableStatus() == SortableStatus.SHIPPED_RETURN);
        }

        @Test
        @DisplayName("[Неполный многоместный] Все заказы приняты / 2 заказа отсортировано")
        void shipRouteByCell2() {
            var order = returnMultiplaceOrder.get()
                    .acceptPlaces()
                    .sortPlaces("1", "2")
                    .get();

            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            Long cellId = testFactory.orderPlace(order, "1").getCellId().orElseThrow();
            FinishRouteRequestDto request = FinishRouteRequestDto.builder()
                    .cellId(cellId).build();
            routeFacade.finishOutgoingRoute(testFactory.getRouteIdForSortableFlow(route), request, new ScContext(user));

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

            var places = testFactory.orderPlaces(order);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(places.get(2).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 заказа принято / 2 заказа отсортировано")
        void shipRouteByCell3() {
            var order = returnMultiplaceOrder.get()
                    .acceptPlaces("1", "2")
                    .sortPlaces("1", "2")
                    .get();

            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            Long cellId = testFactory.orderPlace(order, "1").getCellId().orElseThrow();
            FinishRouteRequestDto request = FinishRouteRequestDto.builder()
                    .cellId(cellId).build();
            routeFacade.finishOutgoingRoute(testFactory.getRouteIdForSortableFlow(route), request, new ScContext(user));

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);

            var places = testFactory.orderPlaces(order);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(places.get(2).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        }

        @Test
        @DisplayName("[Неполный многоместный] 2 заказа принято / 1 заказ отсортирован")
        void shipRouteByCell4() {
            var order = returnMultiplaceOrder.get()
                    .acceptPlaces("1", "2")
                    .sortPlaces("1")
                    .get();

            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            Long cellId = testFactory.orderPlace(order, "1").getCellId().orElseThrow();
            FinishRouteRequestDto request = FinishRouteRequestDto.builder()
                    .cellId(cellId).build();
            routeFacade.finishOutgoingRoute(testFactory.getRouteIdForSortableFlow(route), request, new ScContext(user));

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

            var places = testFactory.orderPlaces(order);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(places.get(2).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        }
    }
}
