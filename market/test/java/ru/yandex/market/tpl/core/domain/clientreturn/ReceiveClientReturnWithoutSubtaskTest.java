package ru.yandex.market.tpl.core.domain.clientreturn;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnCreateDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
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

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.RECEIVED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class ReceiveClientReturnWithoutSubtaskTest {
    private static final String CLIENT_RETURN_EXTERNAL_ID_1 = "EXTERNAL_RETURN_ID_1";

    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;

    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final ClientReturnService clientReturnService;
    private final ClientReturnRepository clientReturnRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;

    private User user;
    private Shift shift;
    private UserShift userShift;
    private Order order;
    private LockerDeliveryTask lockerDeliveryTask;
    private RoutePoint routePoint;
    private String clientReturnBarcodeExternalCreated1;

    @BeforeEach
    void init() {
        clientReturnBarcodeExternalCreated1 = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShift = userShiftRepository
                .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        ClientReturnCreateDto clientReturnCreateDto = new ClientReturnCreateDto();
        clientReturnCreateDto.setReturnId(CLIENT_RETURN_EXTERNAL_ID_1);
        clientReturnCreateDto.setBarcode(clientReturnBarcodeExternalCreated1);
        clientReturnCreateDto.setPickupPointId(pickupPoint.getId());
        clientReturnCreateDto.setLogisticPointId(pickupPoint.getLogisticPointId());
        clientReturnService.create(clientReturnCreateDto);

        clientReturnService.receiveOnPvz(CLIENT_RETURN_EXTERNAL_ID_1);

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("111")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .pickupPoint(pickupPoint)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        userShiftReassignManager.assign(userShift, order);
        testUserHelper.checkinAndFinishPickup(userShift);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks()).hasSize(1);
    }

    @Test
    void receiveClientReturnWithoutSubtaskTest() {
        finishLoadingLocker();
        //receive
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()));
        receiveClientReturns(unloadedOrders);

        assertThat(lockerDeliveryTask.streamClientReturnSubtasks().collect(Collectors.toList()))
                .hasSize(1);
        lockerDeliveryTask.streamClientReturnSubtasks()
                .forEach(lockerSubtask ->
                        assertThat(lockerSubtask.getStatus())
                                .isEqualTo(LockerDeliverySubtaskStatus.FINISHED)
                );
        ClientReturn clientReturn = clientReturnRepository.findByBarcode(clientReturnBarcodeExternalCreated1).get();
        assertThat(clientReturn.getStatus()).isEqualTo(RECEIVED);
    }

    private void finishLoadingLocker() {
        testUserHelper.arriveAtRoutePoint(routePoint);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "help me!"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of(order.getId()))
                                .build()));
    }

    private void receiveClientReturns(Set<UnloadedOrder> unloadedOrders) {
        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        unloadedOrders
                ));
    }
}

