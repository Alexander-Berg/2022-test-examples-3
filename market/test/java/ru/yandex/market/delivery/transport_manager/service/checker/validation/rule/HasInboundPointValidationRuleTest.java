package ru.yandex.market.delivery.transport_manager.service.checker.validation.rule;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerMethod;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.service.checker.PartnerMethodsCheckService;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.delivery.transport_manager.service.external.lms.dto.method.PartnerMethod;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HasInboundPointValidationRuleTest {
    private final HasInboundPointValidationRule rule = new HasInboundPointValidationRule(
        new PartnerMethodsCheckService(null, null)
    );
    private final EnrichedTransportation enrichedTransportation =
        new EnrichedTransportation().setTransportation(
            new Transportation()
                .setOutboundUnit(new TransportationUnit().setPartnerId(1L))
                .setMovement(new Movement().setPartnerId(1L))
                .setInboundUnit(new TransportationUnit().setPartnerId(2L))
        );

    @Test
    void isValid_noMethods_true() {
        assertThat(rule.isValid(enrichedTransportation)).isTrue();
    }

    @Test
    void isValid_notMatchingMethods_true() {
        enrichedTransportation.setEnabledMethods(Collections.emptySet());
        assertThat(rule.isValid(enrichedTransportation)).isTrue();
    }

    @Test
    void isValid_neededMethods_noPoint_false() {
        TransportationPartnerMethod transportationPartnerMethod = new TransportationPartnerMethod()
                .setMethod(PartnerMethod.PUT_INBOUND)
                .setPartnerId(1L);
        enrichedTransportation.setEnabledMethods(Set.of(transportationPartnerMethod));

        assertThat(rule.isValid(enrichedTransportation)).isFalse();
    }

    @Test
    void isValid_neededMethods_neededPoint_true() {
        TransportationPartnerMethod transportationPartnerMethod = new TransportationPartnerMethod()
                .setMethod(PartnerMethod.PUT_INBOUND)
                .setPartnerId(1L);
        enrichedTransportation.setEnabledMethods(Set.of(transportationPartnerMethod));

        enrichedTransportation.setInboundLogisticPoint(mock(LogisticsPointResponse.class));

        assertThat(rule.isValid(enrichedTransportation)).isTrue();
    }
}
