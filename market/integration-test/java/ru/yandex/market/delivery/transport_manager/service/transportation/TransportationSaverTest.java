package ru.yandex.market.delivery.transport_manager.service.transportation;

import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.transportation.UpdatedTransportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.Tag;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DatabaseSetup({
    "/repository/route/full_routes.xml",
    "/repository/route_schedule/full_schedules.xml",
    "/repository/trip/before/trip_update.xml"
})
class TransportationSaverTest extends AbstractContextualTest {

    @Autowired
    private TransportationSaver transportationSaver;

    @Test
    @ExpectedDatabase(
        value = "/repository/trip/after/trip_update.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateAll() {
        transportationSaver.updateAll(List.of(updatedTransportation()));
    }

    private UpdatedTransportation updatedTransportation() {
        return new UpdatedTransportation(
            transportation(),
            TransportationStatus.DRAFT,
            TransportationUnitStatus.DO_NOT_NEED_TO_SEND,
            MovementStatus.DRAFT,
            TransportationUnitStatus.ERROR,
            List.of(tag())
        );
    }

    private Tag tag() {
        return new Tag()
            .setTransportationId(1L)
            .setCode(TagCode.TOTAL_COST)
            .setValue("777");
    }

    private Transportation transportation() {
        return new Transportation()
            .setId(1L)
            .setStatus(TransportationStatus.NEW) // updated
            .setTransportationType(TransportationType.LINEHAUL)
            .setOutboundUnit(
                new TransportationUnit()
                    .setId(1L)
                    .setStatus(TransportationUnitStatus.NEW)
                    .setType(TransportationUnitType.OUTBOUND)
                    .setPartnerId(1L)
                    .setLogisticPointId(10L)
                    .setPlannedIntervalStart(LocalDateTime.parse("2021-11-26T13:00:00")) // updated
                    .setPlannedIntervalEnd(LocalDateTime.parse("2021-11-26T14:00:00")) // updated
            )
            .setMovement(
                new Movement()
                    .setId(1L) // updated
                    .setStatus(MovementStatus.NEW) // updated
                    .setPartnerId(100L)
                    .setMaxPallet(33) // updated
                    .setPlannedTransportId(null) // updated
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setId(2L)
                    .setStatus(TransportationUnitStatus.NEW)
                    .setType(TransportationUnitType.INBOUND)
                    .setPartnerId(2L)
                    .setLogisticPointId(20L)
                    .setPlannedIntervalStart(LocalDateTime.parse("2021-11-26T15:00:00")) // updated
                    .setPlannedIntervalEnd(LocalDateTime.parse("2021-11-26T16:00:00")) // updated
            )
            .setRegular(false) // updated
            .setDeleted(false)
            .setTransportationSource(TransportationSource.TM_GENERATED)
            .setPlannedLaunchTime(LocalDateTime.parse("2021-11-25T21:00:00"))
            .setHash("newHash"); // updated
    }
}
