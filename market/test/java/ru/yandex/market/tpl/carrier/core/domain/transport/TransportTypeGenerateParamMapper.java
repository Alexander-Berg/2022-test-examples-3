package ru.yandex.market.tpl.carrier.core.domain.transport;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportType;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransportTypeGenerateParamMapper {

    void mapTransportType(@MappingTarget TransportType transportType,
                          TestTransportTypeHelper.TransportTypeGenerateParam param);
}
