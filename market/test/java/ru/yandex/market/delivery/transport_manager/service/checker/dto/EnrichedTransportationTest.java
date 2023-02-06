package ru.yandex.market.delivery.transport_manager.service.checker.dto;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;

import static org.assertj.core.api.Assertions.assertThat;

class EnrichedTransportationTest {

    @Test
    void setNewUnitStatus() {
        EnrichedTransportation t = new EnrichedTransportation();
        t.setNewUnitStatus(TransportationUnitType.OUTBOUND, TransportationUnitStatus.SENT);
        t.setNewUnitStatus(TransportationUnitType.INBOUND, TransportationUnitStatus.ACCEPTED);

        assertThat(t.getNewOutboundStatus()).isEqualTo(TransportationUnitStatus.SENT);
        assertThat(t.getNewInboundStatus()).isEqualTo(TransportationUnitStatus.ACCEPTED);
    }

    @Test
    void getNewUnitStatus() {
        EnrichedTransportation t = new EnrichedTransportation();
        t.setNewOutboundStatus(TransportationUnitStatus.PROCESSED);
        t.setNewInboundStatus(TransportationUnitStatus.DO_NOT_NEED_TO_SEND);

        assertThat(t.getNewUnitStatus(TransportationUnitType.OUTBOUND))
            .isEqualTo(TransportationUnitStatus.PROCESSED);
        assertThat(t.getNewUnitStatus(TransportationUnitType.INBOUND))
            .isEqualTo(TransportationUnitStatus.DO_NOT_NEED_TO_SEND);
    }
}
