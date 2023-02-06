package ru.yandex.travel.orders.repository;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.entities.OrdersUpdatesQueueStateChange;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class OrdersUpdatesQueueStateChangeRepositoryTest {
    @Autowired
    private OrdersUpdatesQueueStateChangeRepository ordersUpdatesQueueStateChangeRepository;

    @Test
    public void testGetOrdersWaitingStateRefresh() {
        assertThat(ordersUpdatesQueueStateChangeRepository.getOrdersWaitingStateRefresh(
                ordersUpdatesQueueStateChangeRepository.NO_EXCLUDE_IDS,
                10
        )).isEmpty();

        var orderId = createStateChange().getOrderId();

        assertThat(ordersUpdatesQueueStateChangeRepository.getOrdersWaitingStateRefresh(
                ordersUpdatesQueueStateChangeRepository.NO_EXCLUDE_IDS,
                10
        )).isEqualTo(List.of(orderId));
    }

    @Test
    public void testCountOrdersWaitingStateRefresh() {
        assertThat(ordersUpdatesQueueStateChangeRepository.countOrdersWaitingStateRefresh(
                ordersUpdatesQueueStateChangeRepository.NO_EXCLUDE_IDS
        )).isZero();

        var orderId = createStateChange().getOrderId();
        assertThat(ordersUpdatesQueueStateChangeRepository.countOrdersWaitingStateRefresh(
                ordersUpdatesQueueStateChangeRepository.NO_EXCLUDE_IDS
        )).isOne();

        createStateChange(orderId);
        assertThat(ordersUpdatesQueueStateChangeRepository.countOrdersWaitingStateRefresh(
                ordersUpdatesQueueStateChangeRepository.NO_EXCLUDE_IDS
        )).isOne();

        createStateChange();
        assertThat(ordersUpdatesQueueStateChangeRepository.countOrdersWaitingStateRefresh(
                ordersUpdatesQueueStateChangeRepository.NO_EXCLUDE_IDS
        )).isEqualTo(2);
    }

    @Test
    public void testFindMostRecentStateChangeForOrder() {
        var uuid1 = UUID.randomUUID();
        assertThat(ordersUpdatesQueueStateChangeRepository.findMostRecentStateChangeForOrder(uuid1)).isNull();

        var stateChangeForOrder1 = createStateChange(uuid1);
        var actual = ordersUpdatesQueueStateChangeRepository.findMostRecentStateChangeForOrder(uuid1);
        assertThat(actual).isEqualTo(stateChangeForOrder1);

        createStateChange();
        actual = ordersUpdatesQueueStateChangeRepository.findMostRecentStateChangeForOrder(uuid1);
        assertThat(actual).isEqualTo(stateChangeForOrder1);

        stateChangeForOrder1 = createStateChange(uuid1);
        actual = ordersUpdatesQueueStateChangeRepository.findMostRecentStateChangeForOrder(uuid1);
        assertThat(actual).isEqualTo(stateChangeForOrder1);
    }

    @Test
    public void testCleanupByOrderIdAndIdLessThanEqual() {
        var orderId1 = createStateChange().getOrderId();
        createStateChange(orderId1);
        createStateChange(orderId1);

        var orderId2 = createStateChange().getOrderId();
        createStateChange(orderId2);


        assertThat(ordersUpdatesQueueStateChangeRepository.count()).isEqualTo(5);

        ordersUpdatesQueueStateChangeRepository.cleanupByOrderIdAndIdLessThanEqual(orderId1, 0L);

        assertThat(ordersUpdatesQueueStateChangeRepository.count()).isEqualTo(5);

        ordersUpdatesQueueStateChangeRepository.cleanupByOrderIdAndIdLessThanEqual(
                orderId1,
                ordersUpdatesQueueStateChangeRepository.findMostRecentStateChangeForOrder(orderId1).getId()
        );

        assertThat(ordersUpdatesQueueStateChangeRepository.count()).isEqualTo(2);

        ordersUpdatesQueueStateChangeRepository.cleanupByOrderIdAndIdLessThanEqual(
                orderId2,
                ordersUpdatesQueueStateChangeRepository.findMostRecentStateChangeForOrder(orderId2).getId()
        );

        assertThat(ordersUpdatesQueueStateChangeRepository.count()).isEqualTo(0);
    }

    private OrdersUpdatesQueueStateChange createStateChange() {
        var uuid = UUID.randomUUID();
        var stateChange = OrdersUpdatesQueueStateChange.createOrdersUpdatesQueueStateChange(uuid);
        ordersUpdatesQueueStateChangeRepository.save(stateChange);
        return stateChange;
    }

    private OrdersUpdatesQueueStateChange createStateChange(UUID uuid) {
        var stateChange = OrdersUpdatesQueueStateChange.createOrdersUpdatesQueueStateChange(uuid);
        ordersUpdatesQueueStateChangeRepository.save(stateChange);
        return stateChange;
    }
}
