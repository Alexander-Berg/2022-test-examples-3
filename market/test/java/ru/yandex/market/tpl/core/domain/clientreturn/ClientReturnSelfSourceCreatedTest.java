package ru.yandex.market.tpl.core.domain.clientreturn;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class ClientReturnSelfSourceCreatedTest {

    private final TestDataFactory testDataFactory;

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final Clock clock;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;

    private User user;
    private UserShift userShift;
    private LockerDeliveryTask lockerDeliveryTask;
    private RoutePoint lockerRoutePoint;
    private Order order;
    private String clientReturnBarcodeExternalCreated1;
    private String clientReturnBarcodeExternalCreated2;
    private String clientReturnBarcodeExternalCreated3;

    @BeforeEach
    void init() {
        clientReturnBarcodeExternalCreated1 = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "42";
        clientReturnBarcodeExternalCreated2 = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "43";
        clientReturnBarcodeExternalCreated3 = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "44";
        user = userHelper.findOrCreateUser(1L);
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        Instant deliveryTime = order.getDelivery().getDeliveryIntervalFrom();

        RoutePointAddress address = new RoutePointAddress("my_address", geoPoint);
        lockerDeliveryTask = (LockerDeliveryTask) commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .address(address)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .withOrderReferenceFromOrder(order, false, false)
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE,
                        GeoPoint.GEO_POINT_SCALE
                )
        );
        lockerRoutePoint = lockerDeliveryTask.getRoutePoint();
        userShift = lockerRoutePoint.getUserShift();

        userHelper.checkinAndFinishPickup(userShift);

        finishLoadingLocker();
    }

    @Test
    void whenTryReceiveUnknownClientReturn_ThenThrowException() {
        assertThrows(
                TplInvalidParameterException.class,
                this::receiveClientReturns
        );
    }

    private void finishLoadingLocker() {
        userHelper.arriveAtRoutePoint(lockerRoutePoint);

        commandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(lockerRoutePoint.getUserShift().getId(),
                        lockerRoutePoint.getId(), lockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "help me!"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of(order.getId()))
                                .build()));
    }

    private void receiveClientReturns() {
        commandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        lockerRoutePoint.getUserShift().getId(),
                        lockerRoutePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(
                                new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()),
                                new UnloadedOrder(clientReturnBarcodeExternalCreated2, null, List.of()),
                                new UnloadedOrder(clientReturnBarcodeExternalCreated3, null, List.of())
                        )
                ));
    }

}
