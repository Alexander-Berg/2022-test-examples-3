package ru.yandex.market.tpl.carrier.core.domain.transport;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.EcologicalClass;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportType;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportTypeRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportTypeSource;

@RequiredArgsConstructor

@Component
public class TestTransportTypeHelper {

    private final TransportTypeRepository transportTypeRepository;
    private final TransportTypeGenerateParamMapper mapper;

    public TransportType createTransportType(TransportTypeGenerateParam transportTypeGenerateParam) {
        TransportType transportType = new TransportType();

        mapper.mapTransportType(transportType, transportTypeGenerateParam);
        return transportTypeRepository.save(transportType);
    }

    @Value
    @Builder(toBuilder = true)
    public static class TransportTypeGenerateParam {
        @Builder.Default
        String name = "Машинка";

        @Builder.Default
        BigDecimal capacity = BigDecimal.TEN;

        @Builder.Default
        List<String> transportTags = List.of();

        @Builder.Default
        int routingPriority = 100;

        @Builder.Default
        int palletsCapacity = 0;

        @Builder.Default
        BigDecimal grossWeightTons = new BigDecimal("3.0");

        @Builder.Default
        BigDecimal maxLoadOnAxleTons = new BigDecimal("1.0");

        @Builder.Default
        BigDecimal maxWeightTons = new BigDecimal("4.5");

        @Builder.Default
        BigDecimal heightMeters = new BigDecimal("4.12");

        @Builder.Default
        BigDecimal widthMeters = new BigDecimal("2.34");

        @Builder.Default
        BigDecimal lengthMeters = new BigDecimal("6.78");

        @Builder.Default
        EcologicalClass ecologicalClass = EcologicalClass.EURO5;

        @Builder.Default
        TransportTypeSource source = TransportTypeSource.CARRIER;

        private final Company company;
    }

}
