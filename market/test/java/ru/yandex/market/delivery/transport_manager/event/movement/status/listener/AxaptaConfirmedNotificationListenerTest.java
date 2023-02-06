package ru.yandex.market.delivery.transport_manager.event.movement.status.listener;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.domain.entity.AxaptaEvent;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.event.movement.status.MovementStatusReceivedEvent;
import ru.yandex.market.delivery.transport_manager.service.AxaptaStatusEventService;
import ru.yandex.market.delivery.transport_manager.service.movement.MovementStatusService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AxaptaConfirmedNotificationListenerTest {

    private AxaptaConfirmedNotificationListener listener;
    private MovementStatusService movementStatusService;
    private AxaptaStatusEventService axaptaStatusEventService;

    @BeforeEach
    void setUp() {
        movementStatusService = Mockito.mock(MovementStatusService.class);
        axaptaStatusEventService = Mockito.mock(AxaptaStatusEventService.class);
        listener = new AxaptaConfirmedNotificationListener(
            movementStatusService,
            axaptaStatusEventService
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(movementStatusService, axaptaStatusEventService);
    }

    @Test
    void testOk() {
        when(axaptaStatusEventService.exists(eq(AxaptaEvent.Type.NEW_TRANSPORTATION), eq(1L)))
            .thenReturn(false);

        Movement movement = new Movement();
        Transportation transportation = new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT);

        listener.listen(new MovementStatusReceivedEvent(
            movement,
            movement,
            List.of(transportation),
            MovementStatus.PARTNER_CREATED,
            MovementStatus.CONFIRMED,
            LocalDateTime.MAX,
            false
        ));

        verify(axaptaStatusEventService).exists(eq(AxaptaEvent.Type.NEW_TRANSPORTATION), eq(1L));
        verify(axaptaStatusEventService).createNewTransportationEvent(transportation);
    }

    @Test
    void testWrongTransportationType() {
        when(axaptaStatusEventService.exists(eq(AxaptaEvent.Type.NEW_TRANSPORTATION), eq(1L)))
            .thenReturn(false);

        Movement movement = new Movement();
        Transportation transportation = new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.INTERWAREHOUSE);

        listener.listen(new MovementStatusReceivedEvent(
            movement,
            movement,
            List.of(transportation),
            MovementStatus.PARTNER_CREATED,
            MovementStatus.CONFIRMED,
            LocalDateTime.MAX,
            false
        ));
    }

    @Test
    void testSkipWrongStatus() {
        Movement movement = new Movement();
        Transportation transportation = new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT);

        listener.listen(new MovementStatusReceivedEvent(
            movement,
            movement,
            List.of(transportation),
            MovementStatus.PARTNER_CREATED,
            MovementStatus.CANCELLED,
            LocalDateTime.MAX,
            false
        ));
    }

    @Test
    void testSkipAlreadyConfirmed() {
        Movement movement = new Movement();
        Transportation transportation = new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT);

        listener.listen(new MovementStatusReceivedEvent(
            movement,
            movement,
            List.of(transportation),
            MovementStatus.PARTNER_CREATED,
            MovementStatus.CONFIRMED,
            LocalDateTime.MAX,
            true
        ));
    }

    @Test
    void testEventExists() {
        when(axaptaStatusEventService.exists(eq(AxaptaEvent.Type.NEW_TRANSPORTATION), eq(1L)))
            .thenReturn(true);

        Movement movement = new Movement();
        Transportation transportation = new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT);

        listener.listen(new MovementStatusReceivedEvent(
            movement,
            movement,
            List.of(transportation),
            MovementStatus.PARTNER_CREATED,
            MovementStatus.CONFIRMED,
            LocalDateTime.MAX,
            false
        ));

        verify(axaptaStatusEventService).exists(eq(AxaptaEvent.Type.NEW_TRANSPORTATION), eq(1L));
    }
}
