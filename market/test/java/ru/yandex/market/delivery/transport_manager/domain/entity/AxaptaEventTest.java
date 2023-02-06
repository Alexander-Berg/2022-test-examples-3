package ru.yandex.market.delivery.transport_manager.domain.entity;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;

class AxaptaEventTest {
    @ParameterizedTest
    @MethodSource("getTypes")
    void type(AxaptaEvent.Type eventType, TransportationUnitType unitType, RegisterType registerType) {
        if (eventType == null) {
            Assertions.assertNull(AxaptaEvent.Type.of(unitType, registerType));
        } else {
            Assertions.assertEquals(eventType, AxaptaEvent.Type.of(unitType, registerType));
        }
    }

    private static Stream<Arguments> getTypes() {
        return Stream.of(
            Arguments.of(AxaptaEvent.Type.INBOUND_FACT, TransportationUnitType.INBOUND, RegisterType.FACT),
            Arguments.of(AxaptaEvent.Type.OUTBOUND_FACT, TransportationUnitType.OUTBOUND, RegisterType.FACT),
            Arguments.of(AxaptaEvent.Type.INBOUND_PLAN, TransportationUnitType.INBOUND, RegisterType.PLAN),
            Arguments.of(AxaptaEvent.Type.OUTBOUND_PLAN, TransportationUnitType.OUTBOUND, RegisterType.PLAN),
            Arguments.of(AxaptaEvent.Type.OUTBOUND_PREPARED, TransportationUnitType.OUTBOUND, RegisterType.PREPARED),
            Arguments.of(null, TransportationUnitType.INBOUND, RegisterType.DENIED),
            Arguments.of(null, TransportationUnitType.INBOUND, RegisterType.FACT_DELIVERED_ORDERS_RETURN),
            Arguments.of(null, TransportationUnitType.INBOUND, RegisterType.FACT_UNDELIVERED_ORDERS_RETURN),
            Arguments.of(null, TransportationUnitType.INBOUND, RegisterType.PREPARED),
            Arguments.of(null, TransportationUnitType.OUTBOUND, RegisterType.DENIED),
            Arguments.of(null, TransportationUnitType.OUTBOUND, RegisterType.FACT_DELIVERED_ORDERS_RETURN),
            Arguments.of(null, TransportationUnitType.OUTBOUND, RegisterType.FACT_UNDELIVERED_ORDERS_RETURN)
        );
    }

}
