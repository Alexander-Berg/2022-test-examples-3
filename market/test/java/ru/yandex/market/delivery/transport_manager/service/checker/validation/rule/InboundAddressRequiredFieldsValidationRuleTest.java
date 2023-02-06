package ru.yandex.market.delivery.transport_manager.service.checker.validation.rule;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InboundAddressRequiredFieldsValidationRuleTest {
    private final InboundAddressRequiredFieldsValidationRule rule = new InboundAddressRequiredFieldsValidationRule();

    private final EnrichedTransportation enrichedTransportation =
        new EnrichedTransportation().setTransportation(
            new Transportation()
                .setOutboundUnit(new TransportationUnit().setPartnerId(1L))
                .setMovement(new Movement().setPartnerId(1L))
                .setInboundUnit(new TransportationUnit().setPartnerId(2L))
        );

    @Test
    void isValid_noLogPoints_true() {
        assertThat(rule.isValid(enrichedTransportation)).isTrue();
    }

    @Test
    void isValid_lp_withoutAddress_false() {
        enrichedTransportation.setInboundLogisticPoint(mock(LogisticsPointResponse.class));
        assertThat(rule.isValid(enrichedTransportation)).isFalse();
    }

    @Test
    void isValid_lp_withAddress_noFields_false() {
        Address address = Address.newBuilder()
            .locationId(1)
            .build();
        LogisticsPointResponse logisticsPointResponse = LogisticsPointResponse.newBuilder()
            .id(1L)
            .partnerId(1L)
            .externalId("a")
            .address(address)
            .active(true)
            .build();
        enrichedTransportation.setInboundLogisticPoint(logisticsPointResponse);
        assertThat(rule.isValid(enrichedTransportation)).isFalse();
    }

    @Test
    void isValid_lp_withAddress_withFields_true() {
        Address address = Address.newBuilder()
            .locationId(1)
            .settlement("Settelment")
            .region("Region")
            .build();
        LogisticsPointResponse logisticsPointResponse = LogisticsPointResponse.newBuilder()
            .id(1L)
            .partnerId(1L)
            .externalId("a")
            .address(address)
            .active(true)
            .build();
        enrichedTransportation.setInboundLogisticPoint(logisticsPointResponse);
        assertThat(rule.isValid(enrichedTransportation)).isTrue();
    }
}
