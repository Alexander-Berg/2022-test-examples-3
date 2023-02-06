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
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест заключается в том, что если возврат, назначенный на определенного курьера,
 * заберет совсем другой курьер
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class CourierGetClientReturnAssignedToAnotherCourier {
    private static final String CLIENT_RETURN_EXTERNAL_ID_1 = "EXTERNAL_RETURN_ID_1";
    private final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";
    private final String EXTERNAL_ORDER_ID_2 = "EXTERNAL_ORDER_ID_2";
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
    private final OrderRepository orderRepository;
    private final LockerDeliveryTaskRepository lockerDeliveryTaskRepository;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;

    private User userWhoGetClientReturn;
    private LockerDeliveryTask lockerDeliveryTaskUserWhoGetClientReturn;

    private User userWhichAssignedClientReturn;
    private LockerDeliveryTask lockerDeliveryTaskUserWhichAssignedClientReturn;

    private String clientReturnBarcodeExternalCreated1;

    @BeforeEach
    void init() {
        clientReturnBarcodeExternalCreated1 = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));

        userWhichAssignedClientReturn = testUserHelper.findOrCreateUser(2L);
        lockerDeliveryTaskUserWhichAssignedClientReturn =
                setupCourier(userWhichAssignedClientReturn, EXTERNAL_ORDER_ID_1, pickupPoint);

        ClientReturnCreateDto clientReturnCreateDto = new ClientReturnCreateDto();
        clientReturnCreateDto.setReturnId(CLIENT_RETURN_EXTERNAL_ID_1);
        clientReturnCreateDto.setBarcode(clientReturnBarcodeExternalCreated1);
        clientReturnCreateDto.setPickupPointId(pickupPoint.getId());
        clientReturnCreateDto.setLogisticPointId(pickupPoint.getLogisticPointId());
        clientReturnService.create(clientReturnCreateDto);

        clientReturnService.receiveOnPvz(CLIENT_RETURN_EXTERNAL_ID_1);

        userWhoGetClientReturn = testUserHelper.findOrCreateUser(1L);

        lockerDeliveryTaskUserWhoGetClientReturn =
                setupCourier(userWhoGetClientReturn, EXTERNAL_ORDER_ID_2, pickupPoint);
        finishLoadingLocker(lockerDeliveryTaskUserWhoGetClientReturn, userWhoGetClientReturn, EXTERNAL_ORDER_ID_2);
    }

    @Test
    void courierGetClientReturnAssignedToAnotherCourier() {
        Set<UnloadedOrder> unloadedOrders = Set.of(
                new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()));

        receiveClientReturns(unloadedOrders, lockerDeliveryTaskUserWhoGetClientReturn, userWhoGetClientReturn);

        assertThat(lockerDeliveryTaskUserWhoGetClientReturn.streamClientReturnSubtasks().collect(Collectors.toList()))
                .hasSize(1);
        lockerDeliveryTaskUserWhoGetClientReturn.streamClientReturnSubtasks()
                .forEach(lockerSubtask ->
                        assertThat(lockerSubtask.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED));
        ClientReturn clientReturn =
                clientReturnRepository.findByBarcode(clientReturnBarcodeExternalCreated1).get();

        assertThat(clientReturn.getStatus()).isEqualTo(ClientReturnStatus.RECEIVED);

        lockerDeliveryTaskUserWhichAssignedClientReturn =
                lockerDeliveryTaskRepository.findByIdOrThrow(lockerDeliveryTaskUserWhichAssignedClientReturn.getId());

        assertThat(lockerDeliveryTaskUserWhichAssignedClientReturn.streamClientReturnSubtasks()
                .collect(Collectors.toList())).hasSize(0);


        assertThat(lockerDeliveryTaskUserWhichAssignedClientReturn.streamSubtask().collect(Collectors.toList()))
                .hasSize(1);
    }

    private LockerDeliveryTask setupCourier(User user, String externalOrderId, PickupPoint pickupPoint) {
        var shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        var userShift = userShiftRepository
                .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
        var geoPoint = GeoPointGenerator.generateLonLat();

        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId(externalOrderId)
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

        var routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        var lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks()).hasSize(1);
        return lockerDeliveryTask;
    }

    private void finishLoadingLocker(LockerDeliveryTask lockerDeliveryTask, User user, String externalOrderId) {
        RoutePoint routePoint = lockerDeliveryTask.getRoutePoint();
        testUserHelper.arriveAtRoutePoint(routePoint);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "help me!"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of(orderRepository.findByExternalOrderId(externalOrderId).get().getId()))
                                .build()));
    }

    private void receiveClientReturns(Set<UnloadedOrder> unloadedOrders,
                                      LockerDeliveryTask lockerDeliveryTask,
                                      User user) {
        RoutePoint routePoint = lockerDeliveryTask.getRoutePoint();

        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        unloadedOrders
                ));
    }
}
