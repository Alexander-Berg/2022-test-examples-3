package ru.yandex.market.delivery.transport_manager.domain.entity;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;

class TransportationTest {

    @Test
    void testNullTypeAndSubtype() {
        new Transportation()
            .setTransportationType(null)
            .setSubtype(null);

        new Transportation()
            .setSubtype(null)
            .setTransportationType(null);
    }

    @Test
    void testSetOkType() {
        var transportation = new Transportation()
            .setTransportationType(TransportationType.LINEHAUL)
            .setSubtype(TransportationSubtype.SUPPLEMENTARY_4);
        transportation.setSubtype(TransportationSubtype.SUPPLEMENTARY_5);

        new Transportation()
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setSubtype(TransportationSubtype.BREAK_BULK_XDOCK)
            .setTransportationType(TransportationType.XDOC_PARTNER_SUPPLY_TO_FF);
    }

    @Test
    void testSetFailedType() {
        assertThrows(() ->
            new Transportation().setSubtype(TransportationSubtype.MAIN)
        );
        assertThrows(() ->
            new Transportation()
                .setTransportationType(TransportationType.LINEHAUL)
                .setSubtype(TransportationSubtype.MAIN)
                .setTransportationType(TransportationType.ORDERS_OPERATION)
        );
        assertThrows(() ->
            new Transportation()
                .setTransportationType(TransportationType.LINEHAUL)
                .setSubtype(TransportationSubtype.MAIN)
                .setTransportationType(null)
        );
    }

    private void assertThrows(Executable executable) {
        Assertions.assertThrows(IllegalArgumentException.class, executable);
    }
}
