package ru.yandex.market.sc.core.domain.order;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.registry.OrderForRegistry;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
class OrderNonBlockingQueryServiceTest {

    @Autowired
    OrderNonBlockingQueryService orderNonBlockingQueryService;
    @Autowired
    TestFactory testFactory;
    @Autowired
    TransactionTemplate transactionTemplate;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;

    DeliveryService deliveryService;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        deliveryService = testFactory.storedDeliveryService();
        testFactory.setupMockClock(clock);
    }

    @Test
    void attemptToGetOldRoute() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        var route = testFactory.findOutgoingCourierRoute(
                testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow().allowReading();
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));
        try {
            transactionTemplate.execute(ts -> {
                assertThat(catchThrowable(
                        () -> orderNonBlockingQueryService.getPlainOrdersByRoutable(
                                LocalDate.now(clock), sortingCenter, route)
                )).isInstanceOf(TplInvalidActionException.class);
                return null;
            });
        } catch (UnexpectedRollbackException ignored) {
        }
    }

    @Test
    void getOrdersForRegistry() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build()).accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build()).accept().sort().get();
        var orders = List.of(order1, order2);

        List<OrderForRegistry> ordersForRegistry = transactionTemplate.execute(
                t -> orderNonBlockingQueryService.getOrdersForRegistryOnlyShippedPlaces(new ArrayList<>(orders)));
        AtomicInteger count = new AtomicInteger(0);
        assertThat(ordersForRegistry).containsExactlyInAnyOrder(
                orders.stream()
                        .sorted(Comparator.comparingLong(OrderLike::getId))
                        .map(o -> new OrderForRegistry(
                                count.incrementAndGet(),
                                o.getExternalId(),
                                o.getAssessedCost().doubleValue(),
                                "0/1"
                        )).toArray(OrderForRegistry[]::new));
    }

    @Test
    void getOrdersForRegistryAllPlacesShipped() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1")
                .places("1", "2")
                .build())
                .acceptPlaces().sortPlaces().ship().get();
        var order2 =
                testFactory.createForToday(order(sortingCenter, "2").build()).accept().sort().get();
        var orders = List.of(order1, order2);

        List<OrderForRegistry> ordersForRegistry = transactionTemplate.execute(
                t -> orderNonBlockingQueryService.getOrdersForRegistryOnlyShippedPlaces(new ArrayList<>(orders)));
        AtomicInteger count = new AtomicInteger(0);
        assertThat(ordersForRegistry).containsExactlyInAnyOrder(
                orders.stream()
                        .sorted(Comparator.comparingLong(OrderLike::getId))
                        .map(o -> new OrderForRegistry(
                                count.incrementAndGet(),
                                o.getExternalId(),
                                o.getAssessedCost().doubleValue(),
                                o.hasMoreThanOnePlace()
                                        ? "2/2"
                                        : "0/1"
                        )).toArray(OrderForRegistry[]::new));
    }

    @Test
    void getOrderForRegistryNotAllPlacesAreShipped() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("1", "2").build()
        )
                .cancel().acceptPlaces("1", "2").sortPlaces("1", "2").shipPlace("1").get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build())
                .cancel().accept().sort().get();
        var orders = List.of(order1, order2);

        List<OrderForRegistry> ordersForRegistry = transactionTemplate.execute(
                t -> orderNonBlockingQueryService.getOrdersForRegistryOnlyShippedPlaces(new ArrayList<>(orders)));
        AtomicInteger count = new AtomicInteger(0);


        List<ScOrder> scOrders = orders.stream()
                .sorted(Comparator.comparingLong(OrderLike::getId)).toList();
        OrderForRegistry[] orderForRegistries = new OrderForRegistry[scOrders.size()];
        for (int i = 0; i < orderForRegistries.length; i++) {
            var o = scOrders.get(i);
            orderForRegistries[i] = new OrderForRegistry(
                    count.incrementAndGet(),
                    o.getExternalId(),
                    o.getAssessedCost().doubleValue(),
                    o.hasMoreThanOnePlace()
                            ? "1/2"
                            : "0/1"
            );

        }

        assertThat(ordersForRegistry).containsExactlyInAnyOrder(orderForRegistries);
    }

}
