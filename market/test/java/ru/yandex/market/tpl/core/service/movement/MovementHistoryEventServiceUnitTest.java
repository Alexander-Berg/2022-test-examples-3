package ru.yandex.market.tpl.core.service.movement;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.base.TplEventPublisher;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoFlowStatus;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementCargoDeliveredEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementHistoryEventServiceUnitTest {
    public static final long EXISTED_MOVEMENT_ID = 1L;
    public static final long EXISTED_CARGO_ID = 2L;
    public static final String CARGO_BARCODE = "barcode";
    @Mock
    private TplEventPublisher eventPublisher;
    @Mock
    private MovementRepository movementRepository;
    @Mock
    private DropoffCargoRepository dropoffCargoRepository;
    @InjectMocks
    private MovementHistoryEventService service;

    @Captor
    private ArgumentCaptor<MovementCargoDeliveredEvent> captorEvent;

    @Test
    void updateContext_whenPrevEmpty_and_Cancel() {
        //given
        when(dropoffCargoRepository.findById(eq(EXISTED_CARGO_ID)))
                .thenReturn(Optional.of(buildCargo()));

        Movement movement = buildMovement(EXISTED_MOVEMENT_ID);
        when(movementRepository.findById(eq(EXISTED_MOVEMENT_ID)))
                .thenReturn(Optional.of(movement));

        //when
        service.publishEventWhenCargoDelivered(EXISTED_MOVEMENT_ID, EXISTED_CARGO_ID, DropoffCargoFlowStatus.CANCELLED);

        //then
        verify(eventPublisher).publishEvent(captorEvent.capture());
        MovementCargoDeliveredEvent capturedEvent = captorEvent.getValue();
        assertNotNull(capturedEvent);
        assertEquals(EXISTED_MOVEMENT_ID, capturedEvent.getEntityId());
        assertEquals(movement, capturedEvent.getAggregate());
        assertEquals(MovementHistoryEventService.FAIL_DELIVERED_EVENT_DESCRIPTION + CARGO_BARCODE,
                capturedEvent.getContext());

    }

    @Test
    void updateContext_whenPrevEmpty_andSuccess() {
        //given
        when(dropoffCargoRepository.findById(eq(EXISTED_CARGO_ID)))
                .thenReturn(Optional.of(buildCargo()));

        Movement movement = buildMovement(EXISTED_MOVEMENT_ID);
        when(movementRepository.findById(eq(EXISTED_MOVEMENT_ID)))
                .thenReturn(Optional.of(movement));

        //when
        service.publishEventWhenCargoDelivered(EXISTED_MOVEMENT_ID, EXISTED_CARGO_ID,
                DropoffCargoFlowStatus.DELIVERED_TO_LOGISTIC_POINT);

        //then
        verify(eventPublisher).publishEvent(captorEvent.capture());
        MovementCargoDeliveredEvent capturedEvent = captorEvent.getValue();
        assertNotNull(capturedEvent);
        assertEquals(EXISTED_MOVEMENT_ID, capturedEvent.getEntityId());
        assertEquals(movement, capturedEvent.getAggregate());
        assertEquals(MovementHistoryEventService.SUCCESS_DELIVERED_EVENT_DESCRIPTION + CARGO_BARCODE,
                capturedEvent.getContext());
    }

    private Movement buildMovement(long id) {
        Movement movement = new Movement();
        movement.setId(id);
        return movement;
    }

    private DropoffCargo buildCargo() {
        DropoffCargo dropoffCargo = new DropoffCargo();
        dropoffCargo.setBarcode(CARGO_BARCODE);
        return dropoffCargo;
    }
}
