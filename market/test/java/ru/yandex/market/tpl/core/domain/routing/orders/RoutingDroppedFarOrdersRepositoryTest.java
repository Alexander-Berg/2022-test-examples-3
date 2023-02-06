package ru.yandex.market.tpl.core.domain.routing.orders;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class RoutingDroppedFarOrdersRepositoryTest {
    private final RoutingDroppedFarOrdersRepository routingDroppedFarOrdersRepository;

    @Test
    public void testSaveFarOrders() {
        var orderIds = List.of(1L, 2L, 3L);
        var routingDroppedFarOrders = new RoutingDroppedFarOrders();
        routingDroppedFarOrders.setOrders(orderIds);
        routingDroppedFarOrders.setRouteDate(LocalDate.now(DateTimeUtil.DEFAULT_ZONE_ID));
        routingDroppedFarOrders.setProfileType(RoutingProfileType.GROUP);
        routingDroppedFarOrders.setDeliveryServiceId(1L);

        routingDroppedFarOrdersRepository.save(routingDroppedFarOrders);

        var savedEntity = routingDroppedFarOrdersRepository.findById(routingDroppedFarOrders.getId()).orElseThrow();
        assertThat(savedEntity.getOrders()).containsAll(orderIds);
        assertThat(savedEntity.getDeliveryServiceId()).isEqualTo(routingDroppedFarOrders.getDeliveryServiceId());
        assertThat(savedEntity.getRouteDate()).isEqualTo(routingDroppedFarOrders.getRouteDate());
        assertThat(savedEntity.getProfileType()).isEqualTo(routingDroppedFarOrders.getProfileType());
    }
}
