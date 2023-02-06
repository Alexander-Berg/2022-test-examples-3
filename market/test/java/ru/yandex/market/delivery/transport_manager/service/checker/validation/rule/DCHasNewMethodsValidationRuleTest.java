package ru.yandex.market.delivery.transport_manager.service.checker.validation.rule;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerMethod;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.delivery.transport_manager.service.external.lms.dto.method.PartnerMethod;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DCHasNewMethodsValidationRuleTest {

    private final DCHasNewMethodsValidationRule dCHasNewMethodsValidationRule = new DCHasNewMethodsValidationRule();

    private final EnrichedTransportation enrichedTransportation =
        new EnrichedTransportation().setTransportation(
            new Transportation()
                .setOutboundUnit(new TransportationUnit().setPartnerId(2L))
                .setInboundUnit(new TransportationUnit().setPartnerId(1L))
        );

    @Test
    public void assertEachPartnerSupportsMethods() {
        enrichedTransportation.setEnabledMethods(
            Set.of(
                method(PartnerMethod.PUT_INBOUND, 1L),
                method(PartnerMethod.GET_INBOUND, 1L),
                method(PartnerMethod.PUT_INBOUND_REGISTRY, 1L),
                method(PartnerMethod.GET_INBOUND_STATUS, 1L),
                method(PartnerMethod.GET_INBOUND_STATUS_HISTORY, 1L),
                method(PartnerMethod.PUT_OUTBOUND, 2L),
                method(PartnerMethod.GET_OUTBOUND, 2L),
                method(PartnerMethod.PUT_OUTBOUND_REGISTRY, 2L),
                method(PartnerMethod.GET_OUTBOUND_STATUS, 2L),
                method(PartnerMethod.GET_OUTBOUND_STATUS_HISTORY, 2L)
            )
        );
        enrichedTransportation.setPartnerInfos(
            Map.of(
                1L, PartnerResponse.newBuilder().partnerType(PartnerType.DISTRIBUTION_CENTER)
                    .id(1L)
                    .build(),
                2L, PartnerResponse.newBuilder().partnerType(PartnerType.DISTRIBUTION_CENTER)
                    .id(2L)
                    .build()
            )
        );

        assertTrue(dCHasNewMethodsValidationRule.isValid(enrichedTransportation));
        assertEquals(dCHasNewMethodsValidationRule.getErrorMessage(enrichedTransportation), "");
    }

    @Test
    public void assertInboundDcDoesNotSupportMethods() {
        enrichedTransportation.setEnabledMethods(
            Set.of(
                method(PartnerMethod.PUT_INBOUND, 1L),
                method(PartnerMethod.PUT_INBOUND_REGISTRY, 1L),
                method(PartnerMethod.PUT_OUTBOUND, 2L),
                method(PartnerMethod.GET_OUTBOUND, 2L),
                method(PartnerMethod.PUT_OUTBOUND_REGISTRY, 2L),
                method(PartnerMethod.GET_OUTBOUND_STATUS, 2L),
                method(PartnerMethod.GET_OUTBOUND_STATUS_HISTORY, 2L)
            )
        );
        enrichedTransportation.setPartnerInfos(
            Map.of(
                1L, PartnerResponse.newBuilder().partnerType(PartnerType.DISTRIBUTION_CENTER)
                    .id(1L)
                    .build(),
                2L, PartnerResponse.newBuilder().partnerType(PartnerType.DISTRIBUTION_CENTER)
                    .id(2L)
                    .build()
            )
        );
        var errorMessage = "Принимающий РЦ не поддерживает методы: getInbound, getInboundStatus, " +
            "getInboundStatusHistory\n";

        assertFalse(dCHasNewMethodsValidationRule.isValid(enrichedTransportation));
        assertEquals(dCHasNewMethodsValidationRule.getErrorMessage(enrichedTransportation), errorMessage);
    }

    @Test
    public void assertOutboundDcDoesNotSupportMethods() {
        enrichedTransportation.setEnabledMethods(
            Set.of(
                method(PartnerMethod.PUT_INBOUND, 1L),
                method(PartnerMethod.GET_INBOUND, 1L),
                method(PartnerMethod.PUT_INBOUND_REGISTRY, 1L),
                method(PartnerMethod.GET_INBOUND_STATUS, 1L),
                method(PartnerMethod.GET_INBOUND_STATUS_HISTORY, 1L),
                method(PartnerMethod.PUT_OUTBOUND, 2L)
            )
        );
        enrichedTransportation.setPartnerInfos(
            Map.of(
                1L, PartnerResponse.newBuilder().partnerType(PartnerType.DISTRIBUTION_CENTER)
                    .id(1L)
                    .build(),
                2L, PartnerResponse.newBuilder().partnerType(PartnerType.DISTRIBUTION_CENTER)
                    .id(2L)
                    .build()
            )
        );
        var errorMessage = "Отгружающий РЦ не поддерживает методы: getOutbound, getOutboundStatus, " +
            "getOutboundStatusHistory, putOutboundRegistry";

        assertFalse(dCHasNewMethodsValidationRule.isValid(enrichedTransportation));
        assertEquals(dCHasNewMethodsValidationRule.getErrorMessage(enrichedTransportation), errorMessage);
    }

    @Test
    public void assertInboundUnitSupportsMethodsWithFfOutbound() {
        enrichedTransportation.setEnabledMethods(
            Set.of(
                method(PartnerMethod.PUT_INBOUND, 1L),
                method(PartnerMethod.GET_INBOUND, 1L),
                method(PartnerMethod.PUT_INBOUND_REGISTRY, 1L),
                method(PartnerMethod.GET_INBOUND_STATUS, 1L),
                method(PartnerMethod.GET_INBOUND_STATUS_HISTORY, 1L)
            )
        );
        enrichedTransportation.setPartnerInfos(
            Map.of(
                1L, PartnerResponse.newBuilder().partnerType(PartnerType.DISTRIBUTION_CENTER)
                    .id(1L)
                    .build(),
                2L, PartnerResponse.newBuilder().partnerType(PartnerType.FULFILLMENT)
                    .id(2L)
                    .build()
            )
        );

        assertTrue(dCHasNewMethodsValidationRule.isValid(enrichedTransportation));
        assertEquals(dCHasNewMethodsValidationRule.getErrorMessage(enrichedTransportation), "");
    }

    @Test
    public void assertOutboundUnitSupportsMethodsWithFfOutbound() {
        enrichedTransportation.setEnabledMethods(
            Set.of(
                method(PartnerMethod.PUT_OUTBOUND, 2L),
                method(PartnerMethod.GET_OUTBOUND, 2L),
                method(PartnerMethod.PUT_OUTBOUND_REGISTRY, 2L),
                method(PartnerMethod.GET_OUTBOUND_STATUS, 2L),
                method(PartnerMethod.GET_OUTBOUND_STATUS_HISTORY, 2L)
            )
        );
        enrichedTransportation.setPartnerInfos(
            Map.of(
                1L, PartnerResponse.newBuilder().partnerType(PartnerType.SUPPLIER)
                    .id(1L)
                    .build(),
                2L, PartnerResponse.newBuilder().partnerType(PartnerType.DISTRIBUTION_CENTER)
                    .id(2L)
                    .build()
            )
        );

        assertTrue(dCHasNewMethodsValidationRule.isValid(enrichedTransportation));
        assertEquals(dCHasNewMethodsValidationRule.getErrorMessage(enrichedTransportation), "");
    }

    @Test
    public void assertUnitIsNotDistributionCenter() {
        enrichedTransportation.setPartnerInfos(
            Map.of(
                1L, PartnerResponse.newBuilder().partnerType(PartnerType.SUPPLIER)
                    .id(1L)
                    .build(),
                2L, PartnerResponse.newBuilder().partnerType(PartnerType.FULFILLMENT)
                    .id(2L)
                    .build()
            )
        );
        assertTrue(dCHasNewMethodsValidationRule.isValid(enrichedTransportation));
        assertEquals(dCHasNewMethodsValidationRule.getErrorMessage(enrichedTransportation), "");
    }

    private TransportationPartnerMethod method(PartnerMethod method, Long partnerId) {
        return new TransportationPartnerMethod().setMethod(method).setPartnerId(partnerId);
    }
}
