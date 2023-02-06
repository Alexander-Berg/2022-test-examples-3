package ru.yandex.market.sc.core.domain.order;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.RequestOrderId;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
@Transactional
class OrderLockRepositoryTest {

    @Autowired
    OrderLockRepository orderLockRepository;

    @Autowired
    TestFactory testFactory;

    List<OrderLike> orders;
    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        orders = List.of(
                testFactory.create(order(sortingCenter).externalId("1").build()).get(),
                testFactory.create(order(sortingCenter).externalId("2").build()).get(),
                testFactory.create(order(sortingCenter).externalId("3").build()).get()
        );
    }

    @Test
    void findAndValidateOrdersRwIds() {
        assertThat(
                orderLockRepository.findAndValidateOrdersRw(
                        sortingCenter,
                        orders.stream()
                                .map(o -> new RequestOrderId(o.getId()))
                                .toList(),
                        OrderLockRepository.NO_VALIDATION)
        ).isEqualTo(sortedOrders());
    }

    @Test
    void findAndValidateOrdersRwExternalIds() {
        assertThat(
                orderLockRepository.findAndValidateOrdersRw(
                        sortingCenter,
                        orders.stream()
                                .map(o -> new RequestOrderId(o.getExternalId()))
                                .toList(),
                        OrderLockRepository.NO_VALIDATION)
        ).isEqualTo(sortedOrders());
    }

    @Test
    @Disabled //flapping https://st.yandex-team.ru/MARKETTPLSC-3010
    void findAndValidateOrdersRwBothIds() {
        assertThat(
                orderLockRepository.findAndValidateOrdersRw(
                        sortingCenter,
                        orders.stream()
                                .map(o -> new RequestOrderId(o.getId(), o.getExternalId()))
                                .toList(),
                        OrderLockRepository.NO_VALIDATION)
        ).isEqualTo(orders);
    }

    @Test
    void findAndValidateOrdersRwMixedIds() {
        assertThat(
                orderLockRepository.findAndValidateOrdersRw(
                        sortingCenter,
                        Stream.concat(
                                orders.stream()
                                        .limit(1)
                                        .map(o -> new RequestOrderId(o.getId())),
                                orders.stream()
                                        .skip(1)
                                        .map(o -> new RequestOrderId(o.getExternalId()))
                        ).toList(),
                        OrderLockRepository.NO_VALIDATION)
        ).isEqualTo(sortedOrders());
    }

    private List<OrderLike> sortedOrders() {
        return StreamEx.of(orders).sortedBy(OrderLike::getId).toList();
    }
}
