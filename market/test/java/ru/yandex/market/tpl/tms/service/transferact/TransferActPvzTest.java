package ru.yandex.market.tpl.tms.service.transferact;


import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.transferact.client.model.TransferActCallbackDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferCreateRequestDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferStatus;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderPlace;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrder;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrderRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.transferact.TransferActSourceType;
import ru.yandex.market.tpl.core.domain.transferact.dbqueue.CreateTransferActPvzPayload;
import ru.yandex.market.tpl.core.domain.transferact.dbqueue.CreateTransferActPvzService;
import ru.yandex.market.tpl.core.domain.transferact.dbqueue.TransferActCallbackPayload;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.demo.ManualService;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus.FINISHED;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus.ORDERS_LOADED;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus.UNLOADING_WAITING_SIGN;

@RequiredArgsConstructor
public class TransferActPvzTest extends TplTmsAbstractTest {
    private static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";
    private static final String EXTERNAL_ORDER_ID_2 = "EXTERNAL_ORDER_ID_2";
    public static final String RETURN_EXTERNAL_ORDER_ID_1 = "RETURN_EXTERNAL_ORDER_ID_1";
    public static final String UNKNOWN_ORDER_ID = "UNKNOWN_ORDER_ID";


    private final TestUserHelper testUserHelper;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final OrderGenerateService orderGenerateService;
    private final ScManager scManager;
    private final ScOrderRepository scOrderRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftCommandService commandService;
    private final TransactionTemplate tt;
    private final ManualService manualService;
    private final LockerDeliveryService lockerDeliveryService;
    private final OrderManager orderManager;
    private final TransferActCallbackProcessingService transferActCallbackProcessingService;
    private final TransferApi transferApi;
    private final CreateTransferActPvzService createTransferActPvzService;

    private User user;
    private Shift shift;
    private UserShift userShift;
    private Order order1;
    private Order order2;
    private Order orderToReturn;
    private LockerDeliveryTask lockerDeliveryTask;
    private RoutePoint routePoint;


    void init() {
        tt.execute(a -> {
            user = testUserHelper.findOrCreateUser(1L);
            shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                    sortingCenterService.findSortCenterForDs(239).getId());
            var userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
            userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            PickupPoint pickupPoint = pickupPointRepository.save(
                    testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L));
            GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

            order1 = getPickupOrder(EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint,
                    OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
            order2 = getPickupOrder(EXTERNAL_ORDER_ID_2, pickupPoint, geoPoint,
                    OrderFlowStatus.SORTING_CENTER_PREPARED, 1);

            orderToReturn = getPickupOrder(RETURN_EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint,
                    OrderFlowStatus.DELIVERED_TO_PICKUP_POINT, 1);

            userShiftReassignManager.assign(userShift, order1);
            userShiftReassignManager.assign(userShift, order2);

            userShift.streamDeliveryRoutePoints()
                    .flatMap(RoutePoint::streamLockerDeliveryTasks)
                    .findFirst()
                    .ifPresent(t -> t.setTransferActEnabled(true));
            routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();

            testUserHelper.checkinAndFinishPickup(userShift);
            lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            return 0;
        });

    }

    @Test
    void shouldUnload() {
        tt.execute(a -> {
            init();
            TransferDto transferDto = new TransferDto();
            transferDto.setId("123");
            ArgumentCaptor<TransferCreateRequestDto> requestCaptor =
                    ArgumentCaptor.forClass(TransferCreateRequestDto.class);

            when(transferApi.transferPut(requestCaptor.capture())).thenReturn(transferDto);

            orderManager.cancelOrder(order1);

            testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());

            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order2.getId()))
                            .build()));


            manualService.signedByPvz(userShift.getId(), lockerDeliveryTask.getId(),
                    List.of(EXTERNAL_ORDER_ID_2));

            commandService.signLoading(new UserShiftCommand.SignLoading(
                    userShift.getId(),
                    routePoint.getId(),
                    lockerDeliveryTask.getId(),
                    Map.of(),
                    user
            ));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(ORDERS_LOADED);

            commandService.finishUnloadingLocker(user,
                    new UserShiftCommand.FinishUnloadingLocker(
                            routePoint.getUserShift().getId(),
                            routePoint.getId(),
                            lockerDeliveryTask.getId(),
                            Set.of(
                                    new UnloadedOrder(order1.getExternalOrderId(), null, List.of()),
                                    new UnloadedOrder(orderToReturn.getExternalOrderId(), null,
                                            getPlaceBarcodes(orderToReturn)),
                                    new UnloadedOrder(UNKNOWN_ORDER_ID, null, null))
                    ));

            var createTransferActPvzPayload = new CreateTransferActPvzPayload(
                    "requestId",
                    lockerDeliveryTask.getId(),
                    Set.of(order1.getExternalOrderId(), orderToReturn.getExternalOrderId()),
                    Set.of()
            );
            createTransferActPvzService.processPayload(createTransferActPvzPayload);

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(UNLOADING_WAITING_SIGN);
            var ta = lockerDeliveryTask.getTransferActs().stream()
                    .filter(transferAct -> transferAct.getSourceType() == TransferActSourceType.PVZ_RETURN)
                    .findFirst().get();
            transferActCallbackProcessingService.processPayload(new TransferActCallbackPayload("REQUEST_ID",
                    getTransferActCallbackDto(ta.getExternalId(), TransferStatus.CLOSED)));


            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(FINISHED);


            return 0;
        });

    }

    @NotNull
    private TransferActCallbackDto getTransferActCallbackDto(
            String id,
            TransferStatus status
    ) {
        TransferActCallbackDto transferActCallback = new TransferActCallbackDto();
        TransferDto transfer = new TransferDto();
        transfer.setId(id);
        transfer.setStatus(status);
        transferActCallback.setTransfer(transfer);
        return transferActCallback;
    }


    private List<String> getPlaceBarcodes(Order order) {
        return StreamEx.of(order.getPlaces())
                .map(OrderPlace::getBarcode)
                .filter(Objects::nonNull)
                .map(OrderPlaceBarcode::getBarcode)
                .toList();
    }


    private Order getPickupOrder(
            String externalOrderId,
            PickupPoint pickupPoint,
            GeoPoint geoPoint,
            OrderFlowStatus orderFlowStatus,
            int placeCount
    ) {
        OrderGenerateService.OrderGenerateParam.OrderGenerateParamBuilder orderGenerateParamBuilder =
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId(externalOrderId)
                        .deliveryDate(LocalDate.now(clock))
                        .deliveryServiceId(239L)
                        .pickupPoint(pickupPoint)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .flowStatus(orderFlowStatus);

        if (placeCount > 1) {
            List<OrderPlaceDto> places = new ArrayList<>();
            for (int i = 0; i < placeCount; i++) {
                places.add(OrderPlaceDto.builder()
                        .barcode(new OrderPlaceBarcode("145", externalOrderId + "-" + (i + 1)))
                        .build());
            }

            orderGenerateParamBuilder.places(places);
        }

        Order order = orderGenerateService.createOrder(
                orderGenerateParamBuilder
                        .build());


        scManager.createOrders();
        ScOrder scOrder = scOrderRepository.findByYandexIdAndPartnerId(order.getExternalOrderId(),
                        shift.getSortingCenter().getId())
                .orElseThrow();
        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), "SC-" + scOrder.getId(), scOrder.getPartnerId());
        scManager.updateOrderStatuses(order.getExternalOrderId(), scOrder.getPartnerId(), List.of(
                new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                        Instant.now(clock).minusSeconds(2 * 60 * 60 * 24)),
                new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(),
                        Instant.now(clock).minusSeconds(2 * 60 * 60 * 24))
        ));
        return order;
    }
}
