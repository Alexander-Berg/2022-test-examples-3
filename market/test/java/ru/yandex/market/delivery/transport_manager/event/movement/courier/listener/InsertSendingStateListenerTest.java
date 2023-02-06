package ru.yandex.market.delivery.transport_manager.event.movement.courier.listener;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.event.movement.courier.MovementCourierLoadedEvent;
import ru.yandex.market.delivery.transport_manager.repository.mappers.movement_courier.MovementCourierSendingStateMapper;

class InsertSendingStateListenerTest {

    private MovementCourierSendingStateMapper sendingStateMapper;
    private InsertSendingStateListener listener;


    @BeforeEach
    void setUp() {
        sendingStateMapper = Mockito.mock(MovementCourierSendingStateMapper.class);
        listener = new InsertSendingStateListener(sendingStateMapper);
    }

    @Test
    @DisplayName("Игнорировать вставку в sending_state, если все юниты в терминальных статусах")
    void testUnitsInTerminalStatus() {
        listener.listen(eventWithTerminalUnitStatuses());
        Mockito.verifyNoInteractions(sendingStateMapper);
    }

    private MovementCourierLoadedEvent eventWithTerminalUnitStatuses() {
        return new MovementCourierLoadedEvent(
            new Movement(),
            new MovementCourier(),
            List.of(
                new Transportation()
                    .setOutboundUnit(
                        new TransportationUnit()
                            .setId(1L)
                            .setType(TransportationUnitType.OUTBOUND)
                            .setStatus(TransportationUnitStatus.PROCESSED)
                    )
                    .setInboundUnit(
                        new TransportationUnit()
                            .setId(2L)
                            .setType(TransportationUnitType.INBOUND)
                            .setStatus(TransportationUnitStatus.NEVER_SEND)
                    )
            )
        );
    }
}
