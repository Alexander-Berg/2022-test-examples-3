package ru.yandex.market.delivery.transport_manager.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.RecordApplicationEvents;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.admin.dto.CourierDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.MovementConfirmationDto;
import ru.yandex.market.delivery.transport_manager.event.movement.courier.MovementCourierLoadedEvent;
import ru.yandex.market.delivery.transport_manager.event.movement.status.MovementStatusReceivedEvent;
import ru.yandex.market.delivery.transport_manager.service.core.TmEventPublisher;
import ru.yandex.market.delivery.transport_manager.service.movement.MovementAdminService;
import ru.yandex.market.logistics.front.library.dto.FormattedTextObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RecordApplicationEvents
public class MovementAdminServiceTest extends AbstractContextualTest {

    @Autowired
    MovementAdminService movementAdminService;
    @Autowired
    TmEventPublisher publisher;

    @BeforeEach
    void setUp() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 14, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
    }

    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
        "/repository/movement_courier/multiple_couriers_without_id.xml"
    })
    @Test
    void assign() {
        movementAdminService.save(new CourierDto(
            -1L,
            "some title",
            new FormattedTextObject("2;1"),
            new FormattedTextObject(""),
            "Иван",
            "Иванович",
            "Курьерский",
            "+1800228899",
            "большой газотурбинный грузовик",
            "Р173НО199"
        ));
        verify(publisher, times(2)).publishEvent(any(MovementStatusReceivedEvent.class));
        verify(publisher, times(2)).publishEvent(any(MovementCourierLoadedEvent.class));
    }

    @Test
    @Disabled
    void confirm() {
        movementAdminService.confirm(new MovementConfirmationDto(
            -1L,
            "some title",
            new FormattedTextObject("")
        ));
//        verify(publisher).publishEvent(
//            new MovementStatusReceivedEvent(
//                this,
//                new Movement().setId(2L),
//                List.of(new Transportation().setId(1L)),
//                MovementStatus.NEW,
//                MovementStatus.CONFIRMED
//            )
//        );

    }
}
