package ru.yandex.market.delivery.transport_manager.service.checker.validation.rule;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.service.checker.PartnerMethodsCheckService;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.delivery.transport_manager.service.external.lms.dto.method.PartnerMethod;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class MovingPartnerValidationRuleTest {
    private MovingPartnerValidationRule rule;
    private PartnerMethodsCheckService partnerMethodsCheckService;

    @BeforeEach
    void setUp() {
        partnerMethodsCheckService = Mockito.mock(PartnerMethodsCheckService.class);
        rule = new MovingPartnerValidationRule(partnerMethodsCheckService);
    }

    @MethodSource("validPartnerIdCombinations")
    @ParameterizedTest
    void isValid(
        Long outboundPartnerId,
        Long movingPartnerId,
        Long inboundPartnerId,
        MovementStatus movementStatus,
        MovementStatus newMovementStatus
    ) {
        Transportation transportation = new Transportation()
            .setOutboundUnit(new TransportationUnit().setPartnerId(outboundPartnerId))
            .setMovement(new Movement().setPartnerId(movingPartnerId).setStatus(movementStatus))
            .setInboundUnit(new TransportationUnit().setPartnerId(inboundPartnerId));
        EnrichedTransportation enrichedTransportation = new EnrichedTransportation()
            .setTransportation(transportation)
            .setNewMovementStatus(newMovementStatus);

        Assertions.assertTrue(rule.isValid(enrichedTransportation));
    }

    @MethodSource("trueFalse")
    @ParameterizedTest
    void isValid3pl(boolean isValid3Pl) {
        Transportation transportation = new Transportation()
            .setOutboundUnit(new TransportationUnit().setPartnerId(1L))
            .setMovement(new Movement().setPartnerId(3L).setStatus(MovementStatus.NEW))
            .setInboundUnit(new TransportationUnit().setPartnerId(2L));
        EnrichedTransportation enrichedTransportation =
            new EnrichedTransportation().setTransportation(transportation);

        when(partnerMethodsCheckService.containsActiveMethodFromGroup(
            eq(enrichedTransportation),
            eq(Set.of(PartnerMethod.PUT_MOVEMENT)),
            any()
        )).thenReturn(isValid3Pl);

        Assertions.assertEquals(isValid3Pl, rule.isValid(enrichedTransportation));

        verify(partnerMethodsCheckService).containsActiveMethodFromGroup(
            eq(enrichedTransportation),
            eq(Set.of(PartnerMethod.PUT_MOVEMENT)),
            any()
        );
        verifyNoMoreInteractions(partnerMethodsCheckService);
    }

    @MethodSource("invalidPartnerIdCombinations")
    @ParameterizedTest
    void isInvalid(
        Long outboundPartnerId,
        Long movingPartnerId,
        Long inboundPartnerId,
        MovementStatus movementStatus
    ) {
        Transportation transportation = new Transportation()
            .setOutboundUnit(new TransportationUnit().setPartnerId(outboundPartnerId))
            .setMovement(new Movement().setPartnerId(movingPartnerId).setStatus(movementStatus))
            .setInboundUnit(new TransportationUnit().setPartnerId(inboundPartnerId));
        EnrichedTransportation enrichedTransportation =
            new EnrichedTransportation().setTransportation(transportation);

        Assertions.assertFalse(rule.isValid(enrichedTransportation));
    }

    static Stream<Arguments> validPartnerIdCombinations() {
        return Stream.of(
            Arguments.of(1L, 1L, 2L, MovementStatus.NEW, null),
            Arguments.of(1L, 2L, 2L, MovementStatus.NEW, null),
            Arguments.of(1L, null, 2L, MovementStatus.DRAFT, null),
            Arguments.of(1L, null, 2L, MovementStatus.NEVER_SEND, null),
            Arguments.of(1L, null, 2L, MovementStatus.NEW, MovementStatus.NEVER_SEND)
        );
    }
    static Stream<Arguments> invalidPartnerIdCombinations() {
        return Stream.of(
            Arguments.of(1L, 3L, 2L, MovementStatus.NEW),
            Arguments.of(1L, null, 2L, MovementStatus.NEW),
            Arguments.of(null, null, 2L, MovementStatus.NEW)
        );
    }

    static Stream<Arguments> trueFalse() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }
}
