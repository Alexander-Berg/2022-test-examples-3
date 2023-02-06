package ru.yandex.market.tpl.core.domain.transport;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.routing.tag.RoutingOrderTagType;
import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.routing.custom.transport.CustomRoutingTransportConfig;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTagRepository;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;
import ru.yandex.market.tpl.core.service.user.transport.TransportTypeRepository;

@RequiredArgsConstructor

@Component
public class TestTransportTypeHelper {

    private final RoutingOrderTagRepository routingOrderTagRepository;
    private final TransportTypeRepository transportTypeRepository;
    private final TransportTypeGenerateParamMapper mapper;

    public TransportType createTransportType(TransportTypeGenerateParam transportTypeGenerateParam) {
        TransportType transportType = new TransportType();

        Set<RoutingOrderTag> routingOrderTags = StreamEx.of(transportTypeGenerateParam.getTransportTags())
                .map(tagName -> new RoutingOrderTag(
                        tagName,
                        tagName,
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        RoutingOrderTagType.ORDER_TYPE,
                        Set.of()
                ))
                .toSet();

        routingOrderTagRepository.saveAll(routingOrderTags);

        mapper.mapTransportType(transportType, transportTypeGenerateParam, routingOrderTags);
        return transportTypeRepository.save(transportType);
    }

    @Getter
    @Builder(toBuilder = true)
    public static class TransportTypeGenerateParam {
        @Builder.Default
        private final String name = "Машинка";

        @Builder.Default
        private final BigDecimal capacity = BigDecimal.TEN;

        @Builder.Default
        private final List<String> transportTags = List.of();

        @Builder.Default
        private final int routingPriority = 100;

        @Builder.Default
        private final RoutingVehicleType routingVehicleType = RoutingVehicleType.COMMON;

        @Builder.Default
        private final int palletsCapacity = 0;

        private final Company company;

        private final CustomRoutingTransportConfig customRoutingTransportConfig = null;
    }

}
