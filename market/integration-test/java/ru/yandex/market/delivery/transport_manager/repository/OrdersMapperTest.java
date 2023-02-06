package ru.yandex.market.delivery.transport_manager.repository;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Order;
import ru.yandex.market.delivery.transport_manager.domain.enums.OrderStatus;
import ru.yandex.market.delivery.transport_manager.repository.mappers.OrdersMapper;

class OrdersMapperTest extends AbstractContextualTest {

    private static final Order ORDER = new Order()
        .setBarcode("2000001")
        .setSenderId(300L)
        .setSenderMarketId(30L)
        .setStatus(OrderStatus.PROCESSING)
        .setReturnSortingCenterId(999L)
        .setLogbrokerId(5001L);

    @Autowired
    private OrdersMapper ordersMapper;

    @Test
    @ExpectedDatabase(
        value = "/repository/order/order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void persist() {
        ordersMapper.insert(ORDER);
    }

    @Test
    @DatabaseSetup("/repository/order/order.xml")
    void getById() {
        Order order = ordersMapper.getById(1L);
        assertThatModelEquals(ORDER, order);
    }

    @Test
    @DatabaseSetup("/repository/order/existing_order.xml")
    @ExpectedDatabase(
        value = "/repository/order/empty_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteOrders() {
        ordersMapper.deleteOrders(Set.of(1L));
    }
}
