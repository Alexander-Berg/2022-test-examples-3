package ru.yandex.market.tpl.core.domain.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.user.UserMode;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
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
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.IN_TRANSIT;
import static ru.yandex.market.tpl.api.model.user.UserMode.SOFT_MODE;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LockerDeliveryTaskSoftModeTest {

    public static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";
    public static final String EXTERNAL_ORDER_ID_2 = "EXTERNAL_ORDER_ID_2";
    public static final String RETURN_EXTERNAL_ORDER_ID = "RETURN_EXTERNAL_ORDER_ID_2";

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final UserPropertyService userPropertyService;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final SortingCenterService sortingCenterService;
    private final ScManager scManager;
    private final ScOrderRepository scOrderRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;


    private User user;
    private Shift shift;
    private Long userShiftId;

    private Order order;
    private Order order2;

    private RoutePoint routePoint;
    private RoutePoint routePoint2;
    private LockerDeliveryTask lockerDeliveryTask;
    private LockerDeliveryTask lockerDeliveryTask2;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        PickupPoint pickupPoint2 = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 2L, 1L));
        pickupPoint2.setPhoneNumber("+79999999993");
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        GeoPoint geoPoint2 = GeoPoint.ofLatLon(new BigDecimal(56.572341), new BigDecimal(55.502341));

        order = getPickupOrder(EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        order2 = getPickupOrder(EXTERNAL_ORDER_ID_2, pickupPoint2, geoPoint2, OrderFlowStatus.SORTING_CENTER_PREPARED
                , 1);

        userShiftReassignManager.assign(userShift, order);
        userShiftReassignManager.assign(userShift, order2);

        testUserHelper.checkinAndFinishPickup(userShift);
        List<RoutePoint> routePoints = userShift.streamDeliveryRoutePoints().collect(Collectors.toList());
        routePoint = routePoints.get(0);
        routePoint2 = routePoints.get(1);
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks()
                .findFirst().orElseThrow();
        lockerDeliveryTask2 = (LockerDeliveryTask) routePoint2.streamDeliveryTasks()
                .findFirst().orElseThrow();
        configurationServiceAdapter.insertValue(ConfigurationProperties.IS_UPDATE_CANCEL_SC_130_160, true);
    }

    @Test
    void shouldFinishLoadingSoftMode() {
        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, SOFT_MODE.name());
        RoutePoint notCurrentRoutePoint = routePoint.getStatus() == IN_TRANSIT ? routePoint2 : routePoint;
        LockerDeliveryTask notCurrentDeliveryTask = routePoint.getStatus() == IN_TRANSIT ? lockerDeliveryTask2
                : lockerDeliveryTask;
        Order notCurrentOrder = routePoint.getStatus() == IN_TRANSIT ? order2 : order;
        boolean isSoftMode = SOFT_MODE.equals(UserMode.valueOf(
                userPropertyService.findPropertyForUser(UserProperties.USER_MODE, user)));
        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(
                        routePoint.getUserShift().getId(),
                        notCurrentRoutePoint.getId(),
                        notCurrentDeliveryTask.getId(),
                        null,
                        isSoftMode,
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of(notCurrentOrder.getId()))
                                .build()
                )
        );

        //then
        assertThat(notCurrentDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.ORDERS_LOADED);
        assertThat(orderRepository.findByIdOrThrow(notCurrentOrder.getId()).getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
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
