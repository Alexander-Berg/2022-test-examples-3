package ru.yandex.market.delivery.transport_manager.repository.mappers.movement_courier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierSendingState;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierSendingStateStatus;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/movement_courier/multiple_couriers.xml",
    "/repository/movement_courier/movement_courier_sending_status.xml"
})
class MovementCourierSendingStateMapperTest extends AbstractContextualTest {
    @Autowired
    private MovementCourierSendingStateMapper mapper;

    @Test
    @ExpectedDatabase(
        value = "/repository/movement_courier/excepted/movement_courier_sending_status_after_set_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void markWithStatus() {
        mapper.markWithStatus(
            List.of(2L, 4L),
            List.of(MovementCourierSendingStateStatus.NEW, MovementCourierSendingStateStatus.PROCESSING),
            MovementCourierSendingStateStatus.OUTDATED
        );
    }

    @Test
    void findGroupedInStatus() {
        List<MovementCourierSendingState> records = mapper.findGroupedInStatus(
            MovementCourierSendingStateStatus.NEW,
            100,
            List.of(TransportationUnitStatus.ACCEPTED)
        );

        softly.assertThat(records).hasSize(2);
        softly.assertThat(
                records.stream().
                    map(MovementCourierSendingState::getMovementCourierId)
                    .collect(Collectors.toList())
            )
            .containsExactlyInAnyOrder(4L, 4L);
    }

    @Test
    void findOne() {
        MovementCourierSendingState record = mapper.findOne(11L);
        softly.assertThat(record)
            .isEqualTo(record(11L, MovementCourierSendingStateStatus.NEW, 6L, 4L));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/movement_courier/excepted/movement_courier_sending_status_after_simple_set_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setStatus() {
        mapper.setStatus(
            List.of(1L, 11L),
            MovementCourierSendingStateStatus.SENT
        );
    }

    @Test
    @ExpectedDatabase(
        value =
            "/repository/movement_courier/excepted/movement_courier_sending_status_set_status_by_unit_and_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setStatusByCourierAndUnit() {
        mapper.setStatusByCourierAndUnit(
            2L,
            2L,
            MovementCourierSendingStateStatus.IGNORED
        );
    }

    @Test
    void getCouriersWithNewStatusAndCreatedBefore() {
        List<MovementCourierSendingState> actual =
            mapper.getCouriersWithNewStatusAndCreatedBefore(
                LocalDateTime.of(2021, 12, 1, 19, 1, 0),
                List.of(TransportationUnitStatus.ACCEPTED)
            );

        softly.assertThat(actual)
            .usingElementComparatorIgnoringFields("updated")
            .containsExactlyInAnyOrder(
                new MovementCourierSendingState(
                    5L,
                    3L,
                    2L,
                    MovementCourierSendingStateStatus.NEW,
                    LocalDateTime.of(2021, 12, 1, 18, 0),
                    LocalDateTime.of(2021, 12, 1, 18, 0)
                ),
                new MovementCourierSendingState(
                    6L,
                    3L,
                    3L,
                    MovementCourierSendingStateStatus.NEW,
                    LocalDateTime.of(2021, 12, 1, 19, 0),
                    LocalDateTime.of(2021, 12, 1, 19, 0)
                )
            );
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
