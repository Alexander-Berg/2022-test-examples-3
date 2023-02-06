package ru.yandex.market.delivery.transport_manager.event.les;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderRoute;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.event.les.order.OrderBoundEvent;
import ru.yandex.market.delivery.transport_manager.event.les.order.listener.OrderBindEventListener;
import ru.yandex.market.delivery.transport_manager.repository.mappers.OrderRouteMapper;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;

@DatabaseSetup("/repository/order_route/orders.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class OrderBindEventListenerTest extends AbstractContextualTest {
    @Autowired
    private OrderBindEventListener orderBindEventListener;

    @Autowired
    private OrderRouteMapper orderRouteMapper;

    @Autowired
    private TmPropertyService propertyService;

    @Test
    @DatabaseSetup("/repository/order_route/routes.xml")
    @ExpectedDatabase(
        value = "/les/order_event.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_LES_EVENT_SENDING)).thenReturn(true);
        List<OrderRoute> routes = orderRouteMapper.getByIds(Set.of(1L, 2L));
        OrderBoundEvent event = new OrderBoundEvent(this, routes, 1L);
        orderBindEventListener.onApplicationEvent(event);
    }
}
