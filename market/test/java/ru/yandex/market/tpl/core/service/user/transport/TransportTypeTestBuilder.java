package ru.yandex.market.tpl.core.service.user.transport;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import lombok.Builder;

import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTag;

@Builder
public class TransportTypeTestBuilder {

    @Builder.Default
    private String name = "test-transport";
    @Builder.Default
    private BigDecimal capacity = BigDecimal.TEN;
    @Builder.Default
    private Set<RoutingOrderTag> routingOrderTags = new HashSet<>();
    @Builder.Default
    private Integer routingPriority = 1;
    @Builder.Default
    private RoutingVehicleType routingVehicleType = RoutingVehicleType.COMMON;
    @Builder.Default
    private Integer palletsCapacity = 1;

    public TransportType get() {
        TransportType transportType = new TransportType();
        transportType.setName(name);
        transportType.setCapacity(capacity);
        transportType.setRoutingOrderTags(routingOrderTags);
        transportType.setRoutingPriority(routingPriority);
        transportType.setRoutingVehicleType(routingVehicleType);
        transportType.setPalletsCapacity(palletsCapacity);
        return transportType;
    }
}
