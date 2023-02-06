package ru.yandex.market.delivery.transport_manager.event.unit;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType;
import ru.yandex.market.delivery.transport_manager.event.unit.status.UnitStatusReceivedEvent;
import ru.yandex.market.delivery.transport_manager.event.unit.status.listener.CreateUnitTrackListener;
import ru.yandex.market.delivery.transport_manager.queue.task.tracker.register.RegisterTrackApiType;
import ru.yandex.market.delivery.transport_manager.queue.task.tracker.register.RegisterTrackProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;


@DatabaseSetup({
        "/repository/event/transportation_with_deps.xml",
        "/repository/event/partner_methods.xml"
})
public class CreateUnitTrackListenerTest extends AbstractContextualTest {

    @Autowired
    private CreateUnitTrackListener createUnitTrackListener;

    @Autowired
    private RegisterTrackProducer registerTrackProducer;

    @Autowired
    private TransportationMapper transportationMapper;

    @Test
    void listenInbound() {
        var transportation = transportationMapper.getById(1L);
        createUnitTrackListener.listen(getEvent(transportation, transportation.getInboundUnit()));
        Mockito.verify(registerTrackProducer).produce(
            3L,
            EntityType.INBOUND,
            "ff",
            6L,
            RegisterTrackApiType.FF
        );
    }

    @Test
    void listenOutbound() {
        var transportation = transportationMapper.getById(1L);
        createUnitTrackListener.listen(getEvent(transportation, transportation.getOutboundUnit()));
        Mockito.verify(registerTrackProducer).produce(
                2L,
                EntityType.OUTBOUND,
                "ff",
                5L,
                RegisterTrackApiType.FF
        );
    }

    @NotNull
    private UnitStatusReceivedEvent getEvent(Transportation transportation, TransportationUnit transportationUnit) {
        return new UnitStatusReceivedEvent(
            this,
                transportationUnit,
                transportation,
                TransportationUnitStatus.SENT,
                TransportationUnitStatus.ACCEPTED,
                null,
                true
        );
    }
}
