package ru.yandex.market.delivery.transport_manager.repository.mappers.movement_courier;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierSendingState;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierSendingStateStatus;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/movement_courier/multiple_couriers.xml"
})
class MovementCourierSendingStateMapperInsertionTest extends AbstractContextualTest {
    @Autowired
    private MovementCourierSendingStateMapper mapper;

    @Test
    void insert() {
        mapper.insert(
            List.of(3L, 4L),
            1L,
            MovementCourierSendingStateStatus.NEW
        );

        softly.assertThat(mapper.findOne(1L))
            .isEqualTo(record(1L, MovementCourierSendingStateStatus.NEW, 1L, 3L));

        softly.assertThat(mapper.findOne(2L))
            .isEqualTo(record(2L, MovementCourierSendingStateStatus.NEW, 1L, 4L));
    }

    private static MovementCourierSendingState record(
        Long id,
        MovementCourierSendingStateStatus status,
        Long courierId,
        Long unitId
    ) {
        return new MovementCourierSendingState()
            .setId(id)
            .setStatus(status)
            .setMovementCourierId(courierId)
            .setTransportationUnitId(unitId);
    }
}
