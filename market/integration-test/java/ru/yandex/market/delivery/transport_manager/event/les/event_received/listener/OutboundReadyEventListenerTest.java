package ru.yandex.market.delivery.transport_manager.event.les.event_received.listener;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.FetchRegisterProducer;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.health.event.TrnEventWriter;
import ru.yandex.market.logistics.les.ScOutboundReadyEvent;
import ru.yandex.market.logistics.les.sc.OutboundType;

@DatabaseSetup({
    "/repository/facade/trn/transportation_to_request_register.xml",
})
class OutboundReadyEventListenerTest extends AbstractContextualTest {
    @Autowired
    private OutboundReadyEventListener outboundReadyEventListener;

    @Autowired
    private FetchRegisterProducer fetchRegisterProducer;

    @Autowired
    private TmPropertyService propertyService;

    @Autowired
    private TrnEventWriter trnEventWriter;

    @Test
    void listen() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_SC_OUTBOUND_READY_LES_EVENT_REGISTER_FETCHING))
            .thenReturn(true);
        outboundReadyEventListener.listen(event());

        Mockito.verify(fetchRegisterProducer).produce(
            2L,
            false
        );
        Mockito.verify(trnEventWriter).statusReceived(2L);
    }

    private ScOutboundReadyEvent event() {
        return new ScOutboundReadyEvent(
            5L,
            "ololo",
            "TMU2",
            OutboundType.DS_SC
        );
    }
}
