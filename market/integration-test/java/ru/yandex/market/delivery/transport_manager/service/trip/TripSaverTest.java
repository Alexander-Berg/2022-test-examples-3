package ru.yandex.market.delivery.transport_manager.service.trip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip.TripCreateEntity;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.Tag;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.Trip;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.event.status_change.TransportationStatusChangeEvent;
import ru.yandex.market.delivery.transport_manager.service.core.TmEventPublisher;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DatabaseSetup({
    "/repository/route/full_routes.xml",
    "/repository/route_schedule/full_schedules.xml",
})
class TripSaverTest extends AbstractContextualTest {

    @Autowired
    private TripSaver tripSaver;
    @Autowired
    private RegisterService registerService;
    @Autowired
    private TmEventPublisher tmEventPublisher;

    @Test
    void insertEmpty() {
        tripSaver.insertAll(List.of());
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/trip/after/trips_and_transportations.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void insertFull() {
        tripSaver.insertAll(tripsForInsert());
        verify(tmEventPublisher, times(3)).publishEvent(any(TransportationStatusChangeEvent.class));
    }

    @Test
    void insertOrdersOperation() {
        tripSaver.insertAll(Set.of(simpleEntity(TransportationType.ORDERS_OPERATION)));
        softly.assertThat(registerService.getByTransportationIds(Set.of(1L))).hasSize(2);
    }

    private List<TripCreateEntity> tripsForInsert() {
        return List.of(simpleEntity(TransportationType.LINEHAUL), twoTransportationsEntity());
    }

    private TripCreateEntity simpleEntity(TransportationType type) {
        var outbound = unit(TransportationUnitType.OUTBOUND, 1L, 10L, "2021-11-26T10:30:00", "2021-11-26T11:00:00");
        var movement = movement(MovementStatus.NEW, null, 1L, 15);
        var inbound = unit(TransportationUnitType.INBOUND, 2L, 20L, "2021-11-26T15:30:00", "2021-11-26T16:00:00");
        var transportation = transportation(type, outbound, movement, inbound, "2021-11-25T21:00:00");
        return new TripCreateEntity(
            new Trip().setRouteScheduleId(100L).setStartDate(LocalDate.parse("2021-11-26")),
            List.of(transportation),
            Map.of(0, outbound, 1, inbound),
            null
        );
    }

    private TripCreateEntity twoTransportationsEntity() {
        var outbound = unit(TransportationUnitType.OUTBOUND, 1L, 10L, "2021-11-26T10:00:00", "2021-11-26T11:00:00");
        var movement = movement(MovementStatus.DRAFT, 112L, null, null);
        var inbound = unit(TransportationUnitType.INBOUND, 3L, 30L, "2021-11-26T15:00:00", "2021-11-25T17:00:00");
        var transportation = transportation(outbound, movement, inbound, "2021-11-25T21:10:00");

        var outbound2 = unit(TransportationUnitType.OUTBOUND, 2L, 20L, "2021-11-26T12:00:00", "2021-11-26T13:00:00");
        var movement2 = movement(MovementStatus.NEW, null, 1L, 33);
        var inbound2 = unit(TransportationUnitType.INBOUND, 3L, 30L, "2021-11-26T15:00:00", "2021-11-26T17:00:00");
        var transportation2 = transportation(outbound2, movement2, inbound2, "2021-11-25T21:20:00");
        return new TripCreateEntity(
            new Trip().setRouteScheduleId(102L).setStartDate(LocalDate.parse("2021-11-26")),
            List.of(transportation, transportation2),
            Map.of(0, outbound, 1, outbound2, 2, inbound2, 3, inbound),
            List.of(tag())
        );
    }

    private TransportationUnit unit(TransportationUnitType type, long partnerId, long pointId, String from, String to) {
        return new TransportationUnit()
            .setPartnerId(partnerId)
            .setLogisticPointId(pointId)
            .setStatus(TransportationUnitStatus.NEW)
            .setType(type)
            .setPlannedIntervalStart(LocalDateTime.parse(from))
            .setPlannedIntervalEnd(LocalDateTime.parse(to));
    }

    private Movement movement(MovementStatus status, Long transportId, Long partnerId, Integer pallets) {
        return new Movement()
            .setStatus(status)
            .setPlannedTransportId(transportId)
            .setPartnerId(partnerId)
            .setMaxPallet(pallets);
    }

    private Transportation transportation(
        TransportationUnit outbound,
        Movement movement,
        TransportationUnit inbound,
        String planned
    ) {
        return transportation(TransportationType.LINEHAUL, outbound, movement, inbound, planned);
    }

    private Transportation transportation(
        TransportationType type,
        TransportationUnit outbound,
        Movement movement,
        TransportationUnit inbound,
        String planned
    ) {
        return new Transportation()
            .setStatus(TransportationStatus.DRAFT)
            .setTransportationType(type)
            .setOutboundUnit(outbound)
            .setMovement(movement)
            .setInboundUnit(inbound)
            .setPlannedLaunchTime(LocalDateTime.parse(planned))
            .setHash("hash")
            .setRegular(true)
            .setTransportationSource(TransportationSource.TM_GENERATED);
    }

    private Tag tag() {
        return new Tag()
            .setTransportationId(1L)
            .setCode(TagCode.IS_CONFIRMED)
            .setValue("false");
    }
}
