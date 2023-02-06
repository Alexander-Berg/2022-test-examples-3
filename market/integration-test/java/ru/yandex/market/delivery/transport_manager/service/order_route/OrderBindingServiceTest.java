package ru.yandex.market.delivery.transport_manager.service.order_route;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderBindingType;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.event.les.order.listener.OrderBindEventListener;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.order.OrderBindingService;

@DatabaseSetup({
    "/repository/order_route/transportations.xml",
    "/repository/order_route/orders.xml"
})
public class OrderBindingServiceTest extends AbstractContextualTest {
    @Autowired
    private OrderBindingService orderBindingService;

    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private OrderBindEventListener eventListener;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2021-11-21T16:59:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup("/repository/order_route/routes_for_binding.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after/after_binding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testBinding() {
        orderBindingService.bindOrders(Set.of(1L, 2L, 3L, 4L), OrderBindingType.ON_ROUTE_CREATION, false);
        Mockito.verify(eventListener, Mockito.times(3)).onApplicationEvent(Mockito.any());
    }

    @Test
    @DatabaseSetup("/repository/order_route/routes_for_binding.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after/after_binding_with_gap.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testBindingWithGap() {
        orderBindingService.bindOrders(Set.of(4L), OrderBindingType.TMS_TASK, false);
    }

    @Test
    @DatabaseSetup("/repository/order_route/after/after_binding.xml")
    @DatabaseSetup("/repository/order_route/register_relations.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after/after_rebinding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testRebinding() {
        orderBindingService.bindOrders(Set.of(1L, 2L), OrderBindingType.ON_ROUTE_UPDATE, true);
        Mockito.verify(eventListener, Mockito.times(1)).onApplicationEvent(Mockito.any());
    }

    @Test
    @DatabaseSetup(
        value = "/repository/order_route/rebind_in_sent_status.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/order_route/after/rebind_in_sent_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testRebindingInSentStatus() {
        orderBindingService.bindOrders(Set.of(1L), OrderBindingType.ON_ROUTE_UPDATE, true);
        Mockito.verify(eventListener, Mockito.times(1)).onApplicationEvent(Mockito.any());
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/order_route/routes_for_transportation.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after/after_transportation_binding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testBindAllMatchingToTransportation() {
        Transportation transportation = transportationMapper.getById(1L);
        orderBindingService.bindAllMatchingToTransportation(
            transportation,
            OrderBindingType.ON_TRANSPORTATION_CREATION
        );
        Mockito.verify(eventListener, Mockito.times(1)).onApplicationEvent(Mockito.any());
    }
}
