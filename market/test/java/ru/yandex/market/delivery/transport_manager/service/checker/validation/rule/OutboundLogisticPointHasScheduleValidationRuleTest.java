package ru.yandex.market.delivery.transport_manager.service.checker.validation.rule;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OutboundLogisticPointHasScheduleValidationRuleTest {
    private final OutboundLogisticPointHasScheduleValidationRule rule
        = new OutboundLogisticPointHasScheduleValidationRule();
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
    void isValid_withoutSchedule_false() {
        enrichedTransportation.setOutboundLogisticPoint(mock(LogisticsPointResponse.class));
        assertThat(rule.isValid(enrichedTransportation)).isFalse();
    }

    @Test
    void isValid_emptySchedule_false() {
        LogisticsPointResponse logisticsPointResponse = LogisticsPointResponse.newBuilder()
            .id(1L)
            .partnerId(1L)
            .externalId("a")
            .active(true)
            .schedule(Set.of())
            .build();
        enrichedTransportation.setOutboundLogisticPoint(logisticsPointResponse);
        assertThat(rule.isValid(enrichedTransportation)).isFalse();
    }

    @Test
    void isValid_nonEmptySchedule_true() {
        LogisticsPointResponse logisticsPointResponse = LogisticsPointResponse.newBuilder()
            .id(1L)
            .partnerId(1L)
            .externalId("a")
            .active(true)
            .schedule(Set.of(mock(ScheduleDayResponse.class)))
            .build();
        enrichedTransportation.setOutboundLogisticPoint(logisticsPointResponse);
        assertThat(rule.isValid(enrichedTransportation)).isTrue();
    }
}
