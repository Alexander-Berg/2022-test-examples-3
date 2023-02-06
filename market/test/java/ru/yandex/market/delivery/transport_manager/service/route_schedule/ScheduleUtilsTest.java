package ru.yandex.market.delivery.transport_manager.service.route_schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.Trip;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.service.trip.TripUtils;

public class ScheduleUtilsTest {

    @Test
    public void getLastFixedDate() {
        var firstTransportation = new Transportation();
        firstTransportation
            .setTransportationType(TransportationType.LINEHAUL)
            .setStatus(TransportationStatus.SCHEDULED);
        var secondTransportation = new Transportation();
        var outbound = new TransportationUnit();
        var timeslot = new TimeSlot();
        timeslot.setFromDate(LocalDateTime.of(2000, 3, 1, 12, 0, 0));
        outbound.setPlannedIntervalStart(LocalDateTime.of(2000, 2, 1, 12, 0, 0))
            .setBookedTimeSlot(timeslot);
        secondTransportation.setTransportationType(TransportationType.XDOC_PARTNER_SUPPLY_TO_FF)
            .setPlannedLaunchTime(LocalDateTime.of(2000, 1, 1, 12, 0, 0))
            .setOutboundUnit(outbound);
        var actual = ScheduleUtils.getLastLaunchedTripDate(
            Map.of(
                new Trip().setStartDate(LocalDate.parse("2000-02-01")),
                List.of(firstTransportation, secondTransportation)
            ),
            LocalDateTime.parse("2000-01-01T12:00:01")
        );
        Assertions.assertEquals(LocalDate.of(2000, 2, 1), actual.get());
    }

    @Test
    public void tripFixedLaunchTime() {
        var firstTransportation = new Transportation();
        firstTransportation.setTransportationType(TransportationType.LINEHAUL).setStatus(TransportationStatus.DRAFT);
        var secondTransportation = new Transportation();
        secondTransportation.setTransportationType(TransportationType.XDOC_PARTNER_SUPPLY_TO_FF)
            .setPlannedLaunchTime(LocalDateTime.of(2000, 1, 1, 12, 0, 0));
        Assertions.assertTrue(TripUtils.isTripLaunched(List.of(
            firstTransportation,
            secondTransportation
            ),
            LocalDateTime.now()
        ));
    }

    @Test
    public void tripFixedNotDraft() {
        var firstTransportation = new Transportation();
        firstTransportation.setTransportationType(TransportationType.LINEHAUL).setStatus(TransportationStatus.MOVING);
        var secondTransportation = new Transportation();
        secondTransportation.setTransportationType(TransportationType.XDOC_PARTNER_SUPPLY_TO_FF)
            .setPlannedLaunchTime(LocalDateTime.of(2100, 1, 1, 12, 0, 0));
        Assertions.assertFalse(TripUtils.isTripLaunched(List.of(
            firstTransportation,
            secondTransportation
            ),
            LocalDateTime.now()
        ));
    }

    @Test
    public void tripNotFixed() {
        var firstTransportation = new Transportation();
        firstTransportation.setTransportationType(TransportationType.LINEHAUL).setStatus(TransportationStatus.DRAFT);
        var secondTransportation = new Transportation();
        secondTransportation.setTransportationType(TransportationType.XDOC_PARTNER_SUPPLY_TO_FF)
            .setPlannedLaunchTime(LocalDateTime.of(2100, 1, 1, 12, 0, 0));
        Assertions.assertFalse(TripUtils.isTripLaunched(List.of(
            firstTransportation,
            secondTransportation
            ),
            LocalDateTime.now()
        ));
    }
}
