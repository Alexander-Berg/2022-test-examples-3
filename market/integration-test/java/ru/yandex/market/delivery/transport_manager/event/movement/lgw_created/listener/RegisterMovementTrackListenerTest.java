package ru.yandex.market.delivery.transport_manager.event.movement.lgw_created.listener;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType;
import ru.yandex.market.delivery.transport_manager.event.movement.lgw_created.MovementLgwCreatedEvent;
import ru.yandex.market.delivery.transport_manager.queue.task.tracker.register.RegisterTrackApiType;
import ru.yandex.market.delivery.transport_manager.queue.task.tracker.register.RegisterTrackProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.MovementMapper;

@DatabaseSetup("/repository/movement/movement_test.xml")
class RegisterMovementTrackListenerTest extends AbstractContextualTest {
    @Autowired
    private RegisterMovementTrackListener listener;

    @Autowired
    private MovementMapper movementMapper;

    @Autowired
    private RegisterTrackProducer registerTrackProducer;

    @Test
    void listen() {
        listener.listen(getEvent());

        Mockito.verify(registerTrackProducer).produce(
            1L,
            EntityType.MOVEMENT,
            "movement1",
            156L,
            RegisterTrackApiType.DS
        );
    }

    @NotNull
    private MovementLgwCreatedEvent getEvent() {
        return new MovementLgwCreatedEvent(
            this,
            movementMapper.getById(1L),
            "movement1"
        );
    }
}
