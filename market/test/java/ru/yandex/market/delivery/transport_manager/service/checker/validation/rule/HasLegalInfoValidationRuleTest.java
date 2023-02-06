package ru.yandex.market.delivery.transport_manager.service.checker.validation.rule;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.dto.GenericPartnerId;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerMethod;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.PartnerIdType;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.delivery.transport_manager.service.external.lms.dto.method.PartnerMethod;
import ru.yandex.market.id.MarketAccount;

import static org.assertj.core.api.Assertions.assertThat;

public class HasLegalInfoValidationRuleTest {
    private final HasLegalInfoValidationRule rule = new HasLegalInfoValidationRule();
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
    void isValid_withMethods_nullLegalInfo_false() {
        TransportationPartnerMethod transportationPartnerMethod = transportationPartnerMethod(1L);

        enrichedTransportation.setEnabledMethods(Set.of(transportationPartnerMethod));

        assertThat(rule.isValid(enrichedTransportation)).isFalse();
    }

    @Test
    void isValid_withMethods_noLegalInfo_false() {
        TransportationPartnerMethod transportationPartnerMethod = transportationPartnerMethod(1L);

        enrichedTransportation.setEnabledMethods(Set.of(transportationPartnerMethod));
        MarketAccount marketAccount = MarketAccount.newBuilder().build();

        enrichedTransportation.setLegalInfos(Map.of(new GenericPartnerId(2L, PartnerIdType.LMS), marketAccount));

        assertThat(rule.isValid(enrichedTransportation)).isFalse();
    }

    @Test
    void isValid_withMethods_withLegalInfo_true() {
        TransportationPartnerMethod transportationPartnerMethod = transportationPartnerMethod(1L);

        enrichedTransportation.setEnabledMethods(Set.of(transportationPartnerMethod));
        MarketAccount marketAccount = MarketAccount.newBuilder().build();

        enrichedTransportation.setLegalInfos(Map.of(new GenericPartnerId(1L, PartnerIdType.LMS), marketAccount));

        assertThat(rule.isValid(enrichedTransportation)).isTrue();
    }

    @Test
    void checkReturnMessageForInvalidLegalInfo() {
        TransportationPartnerMethod transportationPartnerMethod = transportationPartnerMethod(1L);
        TransportationPartnerMethod transportationPartnerMethod2 = transportationPartnerMethod(2L);

        enrichedTransportation.setEnabledMethods(Set.of(transportationPartnerMethod, transportationPartnerMethod2));
        MarketAccount marketAccount = MarketAccount.newBuilder().build();

        enrichedTransportation.setLegalInfos(Map.of(new GenericPartnerId(3L, PartnerIdType.LMS), marketAccount));
        Assertions.assertEquals("Отсутствует юридическая информация по партнёрам: 1, 2",
                rule.getErrorMessage(enrichedTransportation));
    }

    @Test
    void checkReturnMessageForInvalidLegalInfoWithNullLegalInfos() {
        TransportationPartnerMethod transportationPartnerMethod = transportationPartnerMethod(1L);
        TransportationPartnerMethod transportationPartnerMethod2 = transportationPartnerMethod(2L);

        enrichedTransportation.setEnabledMethods(Set.of(transportationPartnerMethod, transportationPartnerMethod2));

        Assertions.assertEquals("Отсутствует юридическая информация по всем партнёрам",
                rule.getErrorMessage(enrichedTransportation));
    }

    private TransportationPartnerMethod transportationPartnerMethod(Long id) {
        return new TransportationPartnerMethod()
                .setPartnerId(id)
                .setMethod(PartnerMethod.PUT_MOVEMENT);
    }
}
