package ru.yandex.market.delivery.transport_manager.service.event.ffwf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.event.unit.status.listener.EnrichUnitExternalIdListener;
import ru.yandex.market.delivery.transport_manager.queue.task.request.external_id.RequestExternalIdQueueProducer;
import ru.yandex.market.delivery.transport_manager.util.UnitStatusReceivedEventFactory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class EnrichUnitExternalIdListenerTest {

    private static final long REQUEST_ID = 123L;

    private EnrichUnitExternalIdListener enrichUnitExternalIdListener;

    RequestExternalIdQueueProducer requestExternalIdQueueProducer;

    @BeforeEach
    public void init() {
        requestExternalIdQueueProducer = Mockito.mock(RequestExternalIdQueueProducer.class);
        enrichUnitExternalIdListener = new EnrichUnitExternalIdListener(requestExternalIdQueueProducer);
    }

    @Test
    void enrichInboundInterwareHouse() {
        Transportation transportation =
            UnitStatusReceivedEventFactory.transportation(TransportationType.INTERWAREHOUSE);
        TransportationUnit unit = UnitStatusReceivedEventFactory.unit(TransportationUnitType.INBOUND, null);
        enrichUnitExternalIdListener.enrichExternalId(
            UnitStatusReceivedEventFactory.event(transportation, unit, false)
        );
        verify(requestExternalIdQueueProducer).produce(eq(REQUEST_ID));
    }

    @Test
    void enrichOutboundInterwareHouse() {
        Transportation transportation =
            UnitStatusReceivedEventFactory.transportation(TransportationType.INTERWAREHOUSE);
        TransportationUnit unit = UnitStatusReceivedEventFactory.unit(TransportationUnitType.OUTBOUND, null);
        enrichUnitExternalIdListener.enrichExternalId(
            UnitStatusReceivedEventFactory.event(transportation, unit, false)
        );
        verify(requestExternalIdQueueProducer).produce(eq(REQUEST_ID));
    }
}
