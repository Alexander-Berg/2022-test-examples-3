package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketExtraditionSuccessNotifyDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.ReturnType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.core.CoreTest;
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
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn.CLIENT_RETURN_BARCODE_PREFIX_PS;

@Slf4j
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LockerNativeFlowReturnTest {
    public static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";

    private final TestDataFactory testDataFactory;
    private final EntityManager entityManager;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final SortingCenterService sortingCenterService;
    private final ScManager scManager;
    private final ScOrderRepository scOrderRepository;
    private final LockerDeliveryService service;
    private final UserShiftQueryService queryService;

    private User user;
    private Shift shift;
    private Long userShiftId;
    private Order order;
    private RoutePoint routePoint;
    private LockerDeliveryTask lockerDeliveryTask;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order = getPickupOrder(EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 2);

        userShiftReassignManager.assign(userShift, order);

        testUserHelper.checkinAndFinishPickup(userShift);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks()).hasSize(1);

        testUserHelper.arriveAtRoutePoint(routePoint);
        checkAssignment(user, order);
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

    @Test
    public void shouldCreateTwoReturns() {
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        var dto = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.CLIENT_RETURN)
                .barcode(CLIENT_RETURN_BARCODE_PREFIX_PS + "scanned_barcode")
                .returnId("1234567")
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();

        var dto2 = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.CLIENT_RETURN)
                .barcode(CLIENT_RETURN_BARCODE_PREFIX_PS + "scanned_barcode2")
                .returnId("123456")
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();
        service.extraditionSuccess("token", dto, user);
        service.extraditionSuccess("token", dto2, user);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.STARTED);
        assertThat(lockerDeliveryTask.streamClientReturnSubtasks().count()).isEqualTo(2);
        service.finishTask(lockerDeliveryTask.getId(), null, user);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);

    }

    @Test
    public void shouldThrowBarcodeException() {
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        var dto = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.CLIENT_RETURN)
                .barcode("scanned_barcode")
                .returnId("1234567")
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();


        assertThatThrownBy(() -> service.extraditionSuccess("token", dto, user))
                .isInstanceOf(RuntimeException.class);

    }

    @Test
    public void shouldBeIdempotent() {
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        var dto = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.CLIENT_RETURN)
                .barcode(CLIENT_RETURN_BARCODE_PREFIX_PS + "scanned_barcode")
                .returnId("1234567")
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();

        service.extraditionSuccess("token", dto, user);
        service.extraditionSuccess("token", dto, user);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.STARTED);
        assertThat(lockerDeliveryTask.streamClientReturnSubtasks().count()).isEqualTo(1);
        service.finishTask(lockerDeliveryTask.getId(), null, user);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }

    @Test
    public void shouldFailWhenReturnIdExist() {
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        var dto = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.CLIENT_RETURN)
                .barcode(CLIENT_RETURN_BARCODE_PREFIX_PS + "scanned_barcode")
                .returnId("1234567")
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();

        var dto2 = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.CLIENT_RETURN)
                .barcode(CLIENT_RETURN_BARCODE_PREFIX_PS + "scanned_barcode2")
                .returnId("1234567")
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();

        service.extraditionSuccess("token", dto, user);
        assertThatThrownBy(() -> service.extraditionSuccess("token", dto2, user))
                .isInstanceOf(RuntimeException.class);
    }

    private void checkAssignment(User user, Order... orders) {
        List<Order> currentUserOrders = orderRepository.findCurrentUserOrders(user.getId());
        assertThat(currentUserOrders).containsExactlyInAnyOrder(orders);
    }

    private LockerDeliveryTask fetchLockerDeliveryTask() {
        List<Task<?>> tasks = entityManager.find(RoutePoint.class, routePoint.getId()).getTasks();
        return (LockerDeliveryTask) tasks.get(0);
    }
}
