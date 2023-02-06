package ru.yandex.market.delivery.transport_manager.service.movement.courier;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierSendingStateStatus;
import ru.yandex.market.delivery.transport_manager.repository.mappers.movement_courier.MovementCourierSendingStateMapper;
import ru.yandex.market.delivery.transport_manager.service.transportation_unit.TransportationUnitUpdater;

class MovementCourierRefresherTest extends AbstractContextualTest {
    @Autowired
    private TransportationUnitUpdater transportationUnitUpdater;

    @Autowired
    private MovementCourierSendingStateMapper sendingStateMapper;

    private MovementCourierRefresher refresher;

    @BeforeEach
    void setUp() {
        refresher = new MovementCourierRefresher(
            sendingStateMapper,
            transportationUnitUpdater
        );
    }

    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
        "/repository/movement_courier/multiple_couriers.xml",
        "/repository/movement_courier/movement_courier_sending_status.xml"
    })
    @Test
    void refresh() {
        refresher.refresh(11L);

        ArgumentCaptor<Long> argumentCaptor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(transportationUnitUpdater).update(argumentCaptor.capture());

        Long unitId = argumentCaptor.getAllValues().get(0);

        softly.assertThat(unitId).isEqualTo(4L);
    }

    @DatabaseSetup({
        "/repository/transportation/cancelled_transportation.xml",
    })
    @Test
    void refreshCancelledTransportation() {
        refresher.refresh(1L);

        softly.assertThat(sendingStateMapper.findOne(1L).getStatus())
            .isEqualTo(MovementCourierSendingStateStatus.NEW);
    }
}
