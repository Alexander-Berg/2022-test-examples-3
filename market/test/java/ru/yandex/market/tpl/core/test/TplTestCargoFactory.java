package ru.yandex.market.tpl.core.test;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;

@Component
@RequiredArgsConstructor
public class TplTestCargoFactory {

    private final DropoffCargoCommandService dropoffCargoCommandService;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final Clock clock;


    public DropoffCargo createCargo(String barcode, String logisticPointIdTo) {
        return createCargo(barcode, logisticPointIdTo, "Test");
    }

    public DropoffCargo createCargoDirect(String barcode, String logisticPointIdFrom) {
        return createCargo(barcode, "Test", logisticPointIdFrom);
    }

    public DropoffCargo createCargo(String barcode, String logisticPointIdTo, String logisticPointIdFrom) {
        return dropoffCargoCommandService.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(barcode)
                        .logisticPointIdFrom(logisticPointIdFrom)
                        .logisticPointIdTo(logisticPointIdTo)
                        .build()
        );
    }

    public void initPickupCargoFlow(CargoPickupContext pickupContext, ShiftContext shiftContext) {
        long userShiftId = shiftContext.getUserShiftId();
        User user = shiftContext.getUser();

        transactionTemplate.execute(tt -> {
            var orderPickupRoutePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamPickupRoutePoints().findFirst().orElseThrow();
            userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShiftId));
            userShiftCommandService.arriveAtRoutePoint(
                    user,
                    new UserShiftCommand.ArriveAtRoutePoint(
                            userShiftId,
                            orderPickupRoutePoint.getId(),
                            userShiftCommandDataHelper.getLocationDto(userShiftId)
                    ));

            var task = orderPickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
            userShiftCommandService.startOrderPickup(
                    user,
                    new UserShiftCommand.StartScan(
                            userShiftId,
                            orderPickupRoutePoint.getId(),
                            task.getId()
                    ));

            var pickupOrdersCommand = new UserShiftCommand.FinishScan(
                    userShiftId,
                    orderPickupRoutePoint.getId(),
                    task.getId(),
                    ScanRequest.builder()
                            .successfullyScannedOrders(pickupContext.getSuccessfullyScannedOrderIds())
                            .successfullyScannedDropoffCargos(pickupContext.getSuccessfullyScannedDropoffIds())
                            .skippedDropoffCargos(pickupContext.getSkippedScanned())
                            .finishedAt(Instant.now(clock))
                            .build()
            );
            userShiftCommandService.pickupOrders(user, pickupOrdersCommand);
            userShiftCommandService.finishLoading(
                    user,
                    new UserShiftCommand.FinishLoading(
                            userShiftId,
                            orderPickupRoutePoint.getId(),
                            task.getId()));

            return null;
        });
    }

    public void finishUpload(Collection<DropoffCargo> dropoffCargos, ShiftContext shiftContext) {
        long userShiftId = shiftContext.getUserShiftId();
        User user = shiftContext.getUser();

        transactionTemplate.execute(tt -> {
            var lockerDeliveryRoutePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShiftId));
            userShiftCommandService.arriveAtRoutePoint(
                    user,
                    new UserShiftCommand.ArriveAtRoutePoint(
                            userShiftId,
                            lockerDeliveryRoutePoint.getId(),
                            userShiftCommandDataHelper.getLocationDto(userShiftId)
                    ));

            var task = lockerDeliveryRoutePoint.streamLockerDeliveryTasks().findFirst().orElseThrow();

            task.finishLoadingLocker(
                    Instant.now(),
                    null,
                    ScanRequest.builder()
                            .successfullyScannedOrders(List.of())
                            .build()
            );

            Set<UnloadedOrder> unloadedOrders = dropoffCargos.stream()
                    .map(dc -> new UnloadedOrder(dc.getBarcode(), null, List.of(dc.getBarcode())))
                    .collect(Collectors.toSet());

            userShiftCommandService.finishUnloadingLocker(user,
                    new UserShiftCommand.FinishUnloadingLocker(
                            userShiftId,
                            lockerDeliveryRoutePoint.getId(),
                            task.getId(),
                            unloadedOrders
                    )
            );
            return null;
        });

    }


    @Value(staticConstructor = "of")
    public static class ShiftContext {
        private User user;
        private Long userShiftId;
    }

    @Value(staticConstructor = "of")
    public static class CargoPickupContext {
        private List<Long> successfullyScannedOrderIds;
        private Set<Long> successfullyScannedDropoffIds;
        private Set<Long> skippedScanned;
    }
}
