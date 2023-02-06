package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.task.LockerSubtaskType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.core.domain.base.DomainEvents;
import ru.yandex.market.tpl.core.domain.pickup.LockerUnloadOrderResult;
import ru.yandex.market.tpl.core.domain.pickup.LockerUnloadScanSummary;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.events.dropoff.DropOffCargoAcceptedEvent;
import ru.yandex.market.tpl.core.domain.usershift.events.dropoff.DropoffDirectMovementDeliveringEvent;

import static org.assertj.core.api.Assertions.assertThat;

class FinishUnloadingLockerHandlerTest {

    public static final long EXPECTED_MOVEMENT_ID = 11L;

    @Test
    void handler_directDropoff() {
        //given
        var expectedFinish = Instant.now();
        var drSummary1 = new LockerUnloadScanSummary.DropoffCargo(1L, "barcode1", LockerUnloadOrderResult.OK);
        var drSummary2 = new LockerUnloadScanSummary.DropoffCargo(2L, "barcode2", LockerUnloadOrderResult.OK);

        var lockerUnloadScanSummary = new LockerUnloadScanSummary(Map.of(), Map.of(), Set.of(),
                Set.of(drSummary1, drSummary2));
        var lockerDeliveryTask = buildLockerDeliveryTask();

        //when
        var events = FinishUnloadingLockerHandler.builder()
                .finishedAt(expectedFinish)
                .lockerUnloadScanSummary(lockerUnloadScanSummary)
                .build()
                .handler(lockerDeliveryTask);

        //then
        Set<Long> acceptedDropoffIds = collectAcceptedDropoffIds(events);
        assertThat(acceptedDropoffIds).containsOnly(drSummary1.getDropoffCargoId(), drSummary2.getDropoffCargoId());

        assertThat(lockerDeliveryTask.getSubtasks()).hasSize(2);
        for (var stl : lockerDeliveryTask.getSubtasks()) {
            assertThat(stl.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
            assertThat(stl.getType()).isEqualTo(LockerSubtaskType.DROPOFF);
            assertThat(stl.getLockerSubtaskDropOff()).isNotNull();
            assertThat(stl.getLockerSubtaskDropOff().getMovementId()).isEqualTo(EXPECTED_MOVEMENT_ID);
        }

        Set<Long> subtaskDropoffIds = collectSubtaskDropoffIds(lockerDeliveryTask);
        assertThat(subtaskDropoffIds).containsOnly(drSummary1.getDropoffCargoId(), drSummary2.getDropoffCargoId());

        assertThat(events.stream().anyMatch(DropoffDirectMovementDeliveringEvent.class::isInstance)).isTrue();
    }

    @Test
    void handler_directDropoff_whenEmpty() {
        //given
        var expectedFinish = Instant.now();

        var lockerUnloadScanSummary = new LockerUnloadScanSummary(Map.of(), Map.of(), Set.of(),
                Set.of());
        var lockerDeliveryTask = buildLockerDeliveryTask();

        //when
        var events = FinishUnloadingLockerHandler.builder()
                .finishedAt(expectedFinish)
                .lockerUnloadScanSummary(lockerUnloadScanSummary)
                .build()
                .handler(lockerDeliveryTask);

        //then
        Set<Long> acceptedDropoffIds = collectAcceptedDropoffIds(events);
        assertThat(acceptedDropoffIds).isEmpty();

        assertThat(lockerDeliveryTask.getSubtasks()).hasSize(1);
        for (var stl : lockerDeliveryTask.getSubtasks()) {
            assertThat(stl.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
            assertThat(stl.getType()).isEqualTo(LockerSubtaskType.DROPOFF);
            assertThat(stl.getLockerSubtaskDropOff()).isNotNull();
            assertThat(stl.getLockerSubtaskDropOff().getMovementId()).isEqualTo(EXPECTED_MOVEMENT_ID);
        }
    }

    private Set<Long> collectAcceptedDropoffIds(DomainEvents events) {
        return events.stream()
                .filter(DropOffCargoAcceptedEvent.class::isInstance)
                .map(DropOffCargoAcceptedEvent.class::cast)
                .map(DropOffCargoAcceptedEvent::getDropOffCargoId)
                .collect(Collectors.toSet());
    }

    private Set<Long> collectSubtaskDropoffIds(LockerDeliveryTask lockerDeliveryTask) {
        return lockerDeliveryTask.getSubtasks()
                .stream()
                .map(LockerSubtask::getLockerSubtaskDropOff)
                .map(LockerSubtaskDropOff::getDropoffCargoId)
                .collect(Collectors.toSet());
    }

    private LockerDeliveryTask buildLockerDeliveryTask() {
        var lockerDeliveryTask = new LockerDeliveryTask();
        var routePoint = new RoutePoint();
        routePoint.setUserShift(new UserShift());
        lockerDeliveryTask.setRoutePoint(routePoint);
        lockerDeliveryTask.addSubtaskForDropOff(CargoReference.builder()
                .isReturn(false)
                .movementId(EXPECTED_MOVEMENT_ID)
                .build());
        return lockerDeliveryTask;
    }
}
