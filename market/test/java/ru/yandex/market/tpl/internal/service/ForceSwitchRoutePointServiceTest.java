package ru.yandex.market.tpl.internal.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnReadyForReceivedReasonType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.CreatedSource;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.lms.usershift.ForceSwitchRoutePointService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.internal.controller.TplIntTest;

import static org.assertj.core.api.Assertions.assertThat;

@TplIntTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ForceSwitchRoutePointServiceTest {

    private final ForceSwitchRoutePointService forceSwitchRoutePointService;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final TestUserHelper testUserHelper;
    private final ClientReturnCommandService clientReturnCommandService;
    private final ClientReturnRepository clientReturnRepository;
    private final Clock clock;
    private final UserShiftCommandService userShiftCommandService;

    @Test
    void testForceSwitchLockerDeliveryRoutePoint() {
        var user = userHelper.findOrCreateUser(1L);
        var userShift = userHelper.createEmptyShift(user, LocalDate.now(clock));

        var lockerDeliveryTask = testDataFactory.addLockerDeliveryTask(userShift.getId());
        var lockerDeliveryRoutePoint = lockerDeliveryTask.getRoutePoint();
        userHelper.checkinAndFinishPickup(userShift);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(lockerDeliveryRoutePoint);
        assertThat(lockerDeliveryRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);

        forceSwitchRoutePointService.forceSwitch(user.getUid());

        assertThat(userShift.getCurrentRoutePoint()).isNotEqualTo(lockerDeliveryRoutePoint);
        assertThat(lockerDeliveryRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.CANCELLED);
        lockerDeliveryTask.streamSubtask()
                .forEach(subtask -> assertThat(subtask.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED));
    }

    @Test
    void testForceSwitchLockerDeliveryRoutePointWithClientReturn() {
        var user = userHelper.findOrCreateUser(1L);
        var userShift = userHelper.createEmptyShift(user, LocalDate.now(clock));
        var lockerDeliveryTask = testDataFactory.addLockerDeliveryTask(userShift.getId());
        var pickupPoint = pickupPointRepository.findByIdOrThrow(lockerDeliveryTask.getPickupPointId());
        var routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();

        testUserHelper.checkinAndFinishPickup(userShift);

        var createCommand = ClientReturnCommand.Create.builder()
                .pickupPoint(pickupPoint)
                .barcode("54645646")
                .returnId("73485395")
                .createdSource(CreatedSource.SELF)
                .source(Source.SYSTEM)
                .build();

        var clientReturn = clientReturnCommandService.create(createCommand);
        var readyForReceivedCommand = ClientReturnCommand.ReadyForReceived.builder()
                .assignToCourierInRuntime(true)
                .clientReturnId(clientReturn.getId())
                .reasonType(ClientReturnReadyForReceivedReasonType.RECEIVED)
                .source(Source.SYSTEM)
                .build();

        clientReturnCommandService.readyForReceived(readyForReceivedCommand);
        testUserHelper.arriveAtRoutePoint(routePoint);
        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(userShift.getId(),
                        routePoint.getId(), lockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "Не вышло"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of())
                                .build()));

        forceSwitchRoutePointService.forceSwitch(user.getUid());

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());

        assertThat(clientReturn.getStatus()).isEqualTo(ClientReturnStatus.READY_FOR_RECEIVED);
    }

}
