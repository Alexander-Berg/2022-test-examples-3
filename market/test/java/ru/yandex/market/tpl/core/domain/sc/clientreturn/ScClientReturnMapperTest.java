package ru.yandex.market.tpl.core.domain.sc.clientreturn;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.CreatedSource;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.sc.clientreturn.model.ScClientReturn;
import ru.yandex.market.tpl.core.domain.sc.clientreturn.model.ScCourier;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class ScClientReturnMapperTest extends TplAbstractTest {

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final ClientReturnCommandService clientReturnCommandService;
    private final TransactionTemplate transactionTemplate;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;

    private final ScClientReturnMapper scClientReturnMapper;

    private User user;
    private Shift shift;

    private Order order;

    private RoutePoint routePoint;
    private LockerDeliveryTask lockerDeliveryTask;
    private String clientReturnBarcodeExternalCreated1;

    @BeforeEach
    void init() {
        transactionTemplate.execute(ts -> {
            clientReturnBarcodeExternalCreated1 = barcodePrefixRepository.findBarcodePrefixByName(
                    "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
            user = testUserHelper.findOrCreateUser(1L);
            shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
            Long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            PickupPoint pickupPoint =
                    pickupPointRepository.save(testDataFactory.createPickupPoint(PartnerSubType.LOCKER
                            , 1L, 1L));
            GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

            order = orderGenerateService.createOrder(
                    OrderGenerateService.OrderGenerateParam.builder()
                            .deliveryDate(LocalDate.now(clock))
                            .pickupPoint(pickupPoint)
                            .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                    .geoPoint(geoPoint)
                                    .build())
                            .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                            .build());
            clientReturnCommandService.create(ClientReturnCommand.Create.builder()
                    .barcode(clientReturnBarcodeExternalCreated1)
                    .returnId(clientReturnBarcodeExternalCreated1)
                    .pickupPoint(pickupPoint)
                    .createdSource(CreatedSource.EXTERNAL)
                    .source(Source.SYSTEM)
                    .build());

            userShiftReassignManager.assign(userShift, order);
            testUserHelper.checkinAndFinishPickup(userShift);
            routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
            lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            return null;
        });
    }

    @Test
    void mapReturnTest() {
        testUserHelper.arriveAtRoutePoint(routePoint);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "help me!"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of(order.getId()))
                                .build()));


        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()))
                ));

        ScClientReturn scClientReturn = scClientReturnMapper.mapClientReturn(clientReturnBarcodeExternalCreated1,
                userShiftRepository.findCurrentShift(user).get().getId()
        );

        assertThat(scClientReturn.getBarcode()).isEqualTo(clientReturnBarcodeExternalCreated1);
        assertThat(scClientReturn.getReturnDate()).isEqualTo(shift.getShiftDate());
        assertThat(scClientReturn.getSortingCenterId()).isEqualTo(shift.getSortingCenter().getId());
        assertThat(scClientReturn.getDeliveryPartnerId())
                .isEqualTo(shift.getSortingCenter().getDeliveryServices().get(0).getId());
        assertThat(scClientReturn.getToken()).isEqualTo(shift.getSortingCenter().getToken());
        ScCourier courier = scClientReturn.getCourier();
        assertThat(courier.getId()).isEqualTo(user.getId());
        assertThat(courier.getUid()).isEqualTo(user.getUid());
        assertThat(courier.getName()).isEqualTo(user.getName());
        assertThat(courier.getCarNumber()).isEqualTo(user.getVehicleNumber());
        assertThat(courier.getCarDescription()).isEqualTo(user.getVehicleColor());
        assertThat(courier.getCompanyName()).isEqualTo(user.getCompany().getName());
    }

    @Test
    void mapExtraditionTest() {
        testUserHelper.arriveAtRoutePoint(routePoint);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "help me!"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of(order.getId()))
                                .build()));


        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()))
                ));

        ScClientReturn scClientReturn = scClientReturnMapper.mapExtradition(clientReturnBarcodeExternalCreated1,
                userShiftRepository.findCurrentShift(user).get().getId()
        );

        assertThat(scClientReturn.getBarcode()).isEqualTo(clientReturnBarcodeExternalCreated1);
        assertThat(scClientReturn.getReturnDate()).isEqualTo(shift.getShiftDate());
        assertThat(scClientReturn.getSortingCenterId()).isEqualTo(shift.getSortingCenter().getId());
        assertThat(scClientReturn.getDeliveryPartnerId())
                .isEqualTo(shift.getSortingCenter().getDeliveryServices().get(0).getId());
        assertThat(scClientReturn.getToken()).isEqualTo(shift.getSortingCenter().getToken());
        ScCourier courier = scClientReturn.getCourier();
        assertThat(courier.getId()).isEqualTo(user.getId());
        assertThat(courier.getUid()).isEqualTo(user.getUid());
        assertThat(courier.getName()).isEqualTo(user.getName());
        assertThat(courier.getCarNumber()).isEqualTo(user.getVehicleNumber());
        assertThat(courier.getCarDescription()).isEqualTo(user.getVehicleColor());
        assertThat(courier.getCompanyName()).isEqualTo(user.getCompany().getName());
    }
}
