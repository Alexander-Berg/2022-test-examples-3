package ru.yandex.market.tpl.core.domain.transport;

import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransportTypeGenerateParamMapper {

    void mapTransportType(@MappingTarget TransportType transportType,
                          TestTransportTypeHelper.TransportTypeGenerateParam param,
                          Set<RoutingOrderTag> routingOrderTags);
}
