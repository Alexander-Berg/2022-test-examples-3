package ru.yandex.market.delivery.transport_manager.service.movement.courier;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierSendingStateStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.courier.RefreshCourierProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.movement_courier.MovementCourierSendingStateMapper;

class MovementCourierRefreshTaskCreatorTest extends AbstractContextualTest {
    @Autowired
    private MovementCourierSendingStateMapper movementCourierSendingStateMapper;

    @Autowired
    private MovementCourierSendingStateMapper sendingStateMapper;

    @Autowired
    private RefreshCourierProducer refreshCourierProducer;

    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
        "/repository/movement_courier/multiple_couriers.xml",
        "/repository/movement_courier/movement_courier_sending_status.xml"

    })
    @Test
    void lookup() {
        MovementCourierRefreshTaskCreator creator = new MovementCourierRefreshTaskCreator(
            movementCourierSendingStateMapper, refreshCourierProducer
        );

        creator.lookup();
        ArgumentCaptor<Long> argumentCaptor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(refreshCourierProducer, Mockito.times(2)).enqueue(argumentCaptor.capture());

        List<Long> movementIds = argumentCaptor.getAllValues();

        softly.assertThat(movementIds).containsExactlyInAnyOrder(7L, 8L);
        softly.assertThat(sendingStateMapper.findOne(7L).getStatus())
            .isEqualTo(MovementCourierSendingStateStatus.PROCESSING);
        softly.assertThat(sendingStateMapper.findOne(8L).getStatus())
            .isEqualTo(MovementCourierSendingStateStatus.PROCESSING);
        // Unsupported status: can't send courier info
        softly.assertThat(sendingStateMapper.findOne(11L).getStatus())
            .isEqualTo(MovementCourierSendingStateStatus.NEW);
        softly.assertThat(sendingStateMapper.findOne(12L).getStatus())
            .isEqualTo(MovementCourierSendingStateStatus.NEW);
    }
}
