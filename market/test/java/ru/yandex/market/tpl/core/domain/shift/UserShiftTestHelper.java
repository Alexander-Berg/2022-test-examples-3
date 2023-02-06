package ru.yandex.market.tpl.core.domain.shift;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.usershift.ArriveAtRoutePointService;

@Component
@RequiredArgsConstructor
@Transactional
public class UserShiftTestHelper {

    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final ArriveAtRoutePointService arriveAtRoutePointService;

    public long start(UserShiftCommand.Create createCommand) {
        var userShift = createUserShift(createCommand);
        userShiftCommandService.checkin(userShift.getUser(), new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(userShift.getUser(), new UserShiftCommand.Start(userShift.getId()));
        return userShift.getId();
    }

    public UserShift createUserShift(UserShiftCommand.Create createCommand) {
        long userShiftId = userShiftCommandService.createUserShift(createCommand);
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        userShiftCommandService.switchActiveUserShift(userShift.getUser(), userShiftId);
        return userShift;
    }

    public void arriveAtRoutePoint(UserShift userShift) {
        arriveAtRoutePointService.arrivedAtRoutePoint(
                userShift.getCurrentRoutePoint().getId(),
                new LocationDto(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "myDevice",
                        userShift.getId()
                ),
                userShift.getUser()
        );
    }

    public void startOrderPickup(UserShift userShift, OrderPickupTask pickupTask) {
        userShiftCommandService.startOrderPickup(userShift.getUser(), new UserShiftCommand.StartScan(
                userShift.getId(),
                pickupTask.getRoutePoint().getId(),
                pickupTask.getId()
        ));
    }

    public void createTransferAct(
            UserShift userShift,
            OrderPickupTask pickupTask,
            List<Long> scannedOrdersIds,
            List<Long> skippedOrdersIds
    ) {
        userShiftCommandService.pickupOrders(userShift.getUser(), new UserShiftCommand.FinishScan(
                userShift.getId(),
                pickupTask.getRoutePoint().getId(),
                pickupTask.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(scannedOrdersIds)
                        .skippedOrders(skippedOrdersIds)
                        .build()
        ));
    }

    public void waitForTransferActSignature(
            UserShift userShift,
            OrderPickupTask pickupTask,
            long transferActId,
            String transferActExternalId
    ) {
        userShiftCommandService.waitForTransferActSignature(
                userShift.getUser(),
                new UserShiftCommand.WaitForTransferActSignature(
                        userShift.getId(),
                        pickupTask.getRoutePoint().getId(),
                        pickupTask.getId(),
                        transferActId,
                        transferActExternalId
                )
        );
    }

    public OrderPickupTask getOrderPickupTask(UserShift userShift) {
        return userShift.streamPickupTasks().findFirst()
                .orElseThrow(() -> new RuntimeException("OrderPickupTask not found"));
    }

}
