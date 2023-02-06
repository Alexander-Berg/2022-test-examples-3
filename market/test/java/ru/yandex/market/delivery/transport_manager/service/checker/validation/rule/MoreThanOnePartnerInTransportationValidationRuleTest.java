package ru.yandex.market.delivery.transport_manager.service.checker.validation.rule;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;

import static org.assertj.core.api.Assertions.assertThat;

public class MoreThanOnePartnerInTransportationValidationRuleTest {
    private final MoreThanOnePartnerInTransportationValidationRule rule =
        new MoreThanOnePartnerInTransportationValidationRule();

    @Test
    void isValid_onePartner_false() {
        EnrichedTransportation enrichedTransportation =
            new EnrichedTransportation().setTransportation(
                new Transportation()
                    .setOutboundUnit(new TransportationUnit().setPartnerId(1L))
                    .setMovement(new Movement().setPartnerId(1L))
                    .setInboundUnit(new TransportationUnit().setPartnerId(1L))
            );
        assertThat(rule.isValid(enrichedTransportation)).isFalse();
    }

    @Test
    void isValid_twoPartners_true() {
        EnrichedTransportation enrichedTransportation =
            new EnrichedTransportation().setTransportation(
                new Transportation()
                    .setOutboundUnit(new TransportationUnit().setPartnerId(1L))
                    .setMovement(new Movement().setPartnerId(1L))
                    .setInboundUnit(new TransportationUnit().setPartnerId(2L))
            );
        assertThat(rule.isValid(enrichedTransportation)).isTrue();
    }
}
