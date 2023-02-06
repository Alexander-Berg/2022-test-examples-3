package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.model.locker.LockerDeliveryErrorResponseDto;
import ru.yandex.market.tpl.api.model.locker.OrderAction;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketDeliveryErrorNotifyDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketDeliverySuccessNotifyDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketExtraditionSuccessNotifyDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.ReturnType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
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
import ru.yandex.market.tpl.core.domain.user.UserPropertyEntity;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.lms.order.LmsOrderService;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.service.user.UserPropsType;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.locker.LockerDeliveryFailReasonType.BIG_PLACE_FOR_FREE_CELLS;
import static ru.yandex.market.tpl.api.model.locker.LockerDeliveryFailReasonType.BIG_PLACE_FOR_LOCKER;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn.CLIENT_RETURN_BARCODE_PREFIX_PS;

@RequiredArgsConstructor
public class LockerNativeFlowFullTest extends TplAbstractTest {
    public static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";
    public static final String EXTERNAL_ORDER_ID_2 = "EXTERNAL_ORDER_ID_2";
    public static final String EXTERNAL_ORDER_ID_3 = "EXTERNAL_ORDER_ID_3";

    public static final String EXTERNAL_EXT_ORDER_ID_1 = "EXTERNAL_EXT_ORDER_ID_1";
    public static final String EXTERNAL_EXT_ORDER_ID_2 = "EXTERNAL_EXT_ORDER_ID_2";
    public static final String EXTERNAL_EXT_ORDER_ID_3 = "EXTERNAL_EXT_ORDER_ID_3";

    private final TestDataFactory testDataFactory;
    private final EntityManager entityManager;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftQueryService queryService;
    private final UserShiftCommandService commandService;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final SortingCenterService sortingCenterService;
    private final ScManager scManager;
    private final ScOrderRepository scOrderRepository;
    private final LockerDeliveryService service;
    private final UserPropertyService userPropertyService;
    private final LmsOrderService lmsOrderService;
    private final TransactionTemplate transactionTemplate;

    private UserShift userShift;
    private User user;
    private Shift shift;
    private Long userShiftId;
    private Order deliveryOrder1;
    private Order deliveryOrder2;
    private Order deliveryOrder3;


    private RoutePoint routePoint;
    private LockerDeliveryTask lockerDeliveryTask;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);

        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        deliveryOrder1 = getPickupOrder(EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint,
                OrderFlowStatus.SORTING_CENTER_PREPARED, 2);
        deliveryOrder2 = getPickupOrder(EXTERNAL_ORDER_ID_2, pickupPoint, geoPoint,
                OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        deliveryOrder3 = getPickupOrder(EXTERNAL_ORDER_ID_3, pickupPoint, geoPoint,
                OrderFlowStatus.SORTING_CENTER_PREPARED, 1);

        getPickupOrder(EXTERNAL_EXT_ORDER_ID_1, pickupPoint, geoPoint,
                OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        getPickupOrder(EXTERNAL_EXT_ORDER_ID_2, pickupPoint, geoPoint,
                OrderFlowStatus.SORTING_CENTER_PREPARED, 2);
        getPickupOrder(EXTERNAL_EXT_ORDER_ID_3, pickupPoint, geoPoint,
                OrderFlowStatus.SORTING_CENTER_PREPARED, 1);

        transactionTemplate.execute(
                ts -> {
                    userShift = userShiftRepository.findByIdOrThrow(userShiftId);
                    userShiftReassignManager.assign(userShift, deliveryOrder1);
                    userShiftReassignManager.assign(userShift, deliveryOrder2);
                    userShiftReassignManager.assign(userShift, deliveryOrder3);
                    return null;
                }
        );


        testUserHelper.checkinAndFinishPickup(userShift);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(3);

        testUserHelper.arriveAtRoutePoint(routePoint);
        checkAssignment(user, deliveryOrder1, deliveryOrder2, deliveryOrder3);
        UserPropertyEntity up = new UserPropertyEntity();
        up.setUser(user);
        up.setName(UserPropsType.NATIVE_LOCKER_FLOW_ENABLED.getName());
        up.setType(TplPropertyType.BOOLEAN);
        up.setValue(Boolean.toString(true));
        userPropertyService.save(up);

        clearAfterTest(pickupPoint);
    }

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
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


        List<OrderPlaceDto> places = new ArrayList<>();
        for (int i = 0; i < placeCount; i++) {
            places.add(OrderPlaceDto.builder()
                    .barcode(new OrderPlaceBarcode("145", externalOrderId + "-" + (i + 1)))
                    .build());
        }

        orderGenerateParamBuilder.places(places);


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
    public void shouldBeTwoReturnsAndThreeExtraditions() {
        clientReturns();
        extraditionsAllOrders();
        deliveryAllOrders();
        finishTask();
        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(5);
    }

    @Test
    public void shouldBeZeroReturns() {
        service.finishTaskBySupport(lockerDeliveryTask.getId(), user);
        var returnTaskDto = finishReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(0);
    }

    @Test
    public void shouldBeTwoReturnsThreeExtraditionsAndOneNotDeliveredOrder() {
        clientReturns();
        extraditionsAllOrders();
        deliveryTwoOrders();
        finishTask();
        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(6);
    }

    @Test
    public void shouldBeTwoReturnsThreeExtraditionsAfterManualDeliveryAfterFinish() {
        clientReturns();
        extraditionsAllOrders();
        deliveryTwoOrders();
        finishTask();
        lmsOrderService.makeStatusTransition(deliveryOrder3.getId(), OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(5);
    }

    @Test
    public void shouldBeTwoReturnsThreeExtraditionsAfterManualDeliveryBeforeFinish() {
        clientReturns();
        extraditionsAllOrders();
        deliveryTwoOrders();
        lmsOrderService.makeStatusTransition(deliveryOrder3.getId(), OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        finishTask();
        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(5);
    }

    @Test
    public void shouldBeTwoReturnsAndThreeExtraditionsOneDeliverAfterReopen() {
        var task = fetchLockerDeliveryTask();
        clientReturns();
        extraditionsAllOrders();
        deliveryTwoOrders();
        finishTask();
        service.reopen(task.getId(), user);
        testUserHelper.arriveAtRoutePoint(routePoint);
        finishTask();
        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(6);
    }

    @Test
    public void shouldBeTwoReturnsAndThreeExtraditionsAfterReopenAndDeliver() {
        var task = fetchLockerDeliveryTask();
        clientReturns();
        extraditionsAllOrders();
        deliveryTwoOrders();
        finishTask();
        service.reopen(task.getId(), user);
        testUserHelper.arriveAtRoutePoint(routePoint);
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_3, null);
        finishTask();
        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(5);
    }

    @Test
    public void shouldReturnFiveExtrAndOneFailDelivery() {
        var task = fetchLockerDeliveryTask();
        clientReturns();
        extraditionsAllOrders();
        deliveryTwoOrdersAndOneFail(BIG_PLACE_FOR_FREE_CELLS.getId());
        finishTask();
        service.reopen(task.getId(), user);
        testUserHelper.arriveAtRoutePoint(routePoint);
        finishTask();
        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(6);
    }

    @Test
    public void shouldReturnFiveExtrAfterFailAndReopenDelivery() {
        var task = fetchLockerDeliveryTask();
        clientReturns();
        extraditionsAllOrders();
        deliveryTwoOrdersAndOneFail(BIG_PLACE_FOR_LOCKER.getId());
        finishTask();
        service.reopen(task.getId(), user);
        testUserHelper.arriveAtRoutePoint(routePoint);
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_3, null);
        finishTask();
        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(5);
    }

    @Test
    public void shouldReturnFiveExtrAndOneFailDeliveryBigPlace() {
        var task = fetchLockerDeliveryTask();
        clientReturns();
        extraditionsAllOrders();
        deliveryTwoOrdersAndOneFail(BIG_PLACE_FOR_LOCKER.getId());
        finishTask();
        service.reopen(task.getId(), user);
        testUserHelper.arriveAtRoutePoint(routePoint);
        finishTask();
        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(6);
    }

    @Test
    public void shouldReturnFiveExtrAfterFailAndReopenDeliveryBigPlace() {
        var task = fetchLockerDeliveryTask();
        clientReturns();
        extraditionsAllOrders();
        deliveryTwoOrdersAndOneFail(BIG_PLACE_FOR_LOCKER.getId());
        finishTask();
        service.reopen(task.getId(), user);
        testUserHelper.arriveAtRoutePoint(routePoint);
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_3, null);
        finishTask();
        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(5);
    }

    @Test
    @Transactional
    public void shouldReturnTwoPlacesWhenFailOnePlace() {
        var task = fetchLockerDeliveryTask();
        deliveryTwoOrdersAndFailMultiplaceOrder(BIG_PLACE_FOR_LOCKER.getId());
        finishTask();

        service.reopen(task.getId(), user);
        testUserHelper.arriveAtRoutePoint(routePoint);
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, EXTERNAL_ORDER_ID_1 + "-1");
        finishTask();

        var returnTaskDto = startReturnTask();
        assertThat(returnTaskDto.getOrders().get(0).getPlaces().size()).isEqualTo(2);
        assertThat(deliveryOrder1.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
    }

    @Test
    @Transactional
    public void shouldNotReturnEnyPlaceAfterFailAndThenDeliverAllPlaces() {
        var task = fetchLockerDeliveryTask();
        deliveryTwoOrdersAndFailMultiplaceOrder(BIG_PLACE_FOR_LOCKER.getId());
        finishTask();

        service.reopen(task.getId(), user);
        testUserHelper.arriveAtRoutePoint(routePoint);
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, EXTERNAL_ORDER_ID_1 + "-1");
        finishTask();

        service.reopen(task.getId(), user);

        testUserHelper.arriveAtRoutePoint(routePoint);
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, EXTERNAL_ORDER_ID_1 + "-1");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, EXTERNAL_ORDER_ID_1 + "-2");
        finishTask();

        var returnTaskDto = finishReturnTask();
        assertThat(returnTaskDto.getOrders().size()).isEqualTo(0);
        assertThat(deliveryOrder1.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
    }

    public OrderReturnTaskDto startReturnTask() {
        var returnRp = userShift.getRoutePoints().stream()
                .filter(rp -> rp.getType() == RoutePointType.ORDER_RETURN)
                .findFirst()
                .get();
        var taskId = returnRp.getOrderReturnTask().getId();
        testUserHelper.arriveAtRoutePoint(returnRp);
        commandService.startOrderReturn(user, new UserShiftCommand.StartScan(userShiftId, returnRp.getId(), taskId));
        return (OrderReturnTaskDto) queryService.getTaskInfo(user, returnRp.getId(), taskId);
    }

    public OrderReturnTaskDto finishReturnTask() {
        var returnRp = userShift.getRoutePoints().stream()
                .filter(rp -> rp.getType() == RoutePointType.ORDER_RETURN)
                .findFirst()
                .get();
        var taskId = returnRp.getOrderReturnTask().getId();
        testUserHelper.arriveAtRoutePoint(returnRp);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShiftId, returnRp.getId(),
                taskId));
        return (OrderReturnTaskDto) queryService.getTaskInfo(user, returnRp.getId(), taskId);
    }

    public void deliveryAllOrders() {
        LockerSubtaskPlace o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        LockerSubtaskPlace o1Place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place2.getBarcode());

        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_2, null);

        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_3, null);
    }

    public void deliveryTwoOrders() {
        LockerSubtaskPlace o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        LockerSubtaskPlace o1Place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place2.getBarcode());

        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_2, null);
    }

    public void deliveryTwoOrdersAndOneFail(int failReason) {
        var reasonType = LockerDeliveryService.FAIL_REASON_TYPE_TO_BOX_BOT_ERROR_CODES
                .entrySet().stream()
                .filter(entry -> entry.getValue().equals(failReason))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        LockerSubtaskPlace o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        LockerSubtaskPlace o1Place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place2.getBarcode());

        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_2, null);

        failLoadingLockerPlace(EXTERNAL_ORDER_ID_3, null, failReason, reasonType);
    }

    public void deliveryTwoOrdersAndFailMultiplaceOrder(int failReason) {
        var reasonType = LockerDeliveryService.FAIL_REASON_TYPE_TO_BOX_BOT_ERROR_CODES
                .entrySet().stream()
                .filter(entry -> entry.getValue().equals(failReason))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);


        LockerSubtaskPlace o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        LockerSubtaskPlace o1Place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());

        failLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place2.getBarcode(), failReason, reasonType);

        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_2, null);
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_3, null);
    }


    @ParameterizedTest
    @EnumSource(value = OrderDeliveryTaskFailReasonType.class, names = {
            "DIMENSIONS_EXCEEDS",
            "ORDER_IS_DAMAGED"
    })
    public void actionShouldBeCancelledOnDeliveryTwoOrdersAndFail(OrderDeliveryTaskFailReasonType failReasonType) {
        var o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        var o1Place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());

        var response = failLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place2.getBarcode(), 0, failReasonType);

        assertThat(response.getOrderActionList()).isNotEmpty();
        var orderActionDto = response.getOrderActionList().get(0);
        assertThat(orderActionDto.getOrderDto().getExternalOrderId()).isEqualTo(EXTERNAL_ORDER_ID_1);
        assertThat(orderActionDto.getOrderActions()).isNotEmpty();
        assertThat(orderActionDto.getOrderActions().get(0)).isEqualTo(OrderAction.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = OrderDeliveryTaskFailReasonType.class, names = {
            "LOCKER_FULL", "OTHER"
    })
    public void actionShouldBeRescheduledOnDeliveryTwoOrdersAndFail(OrderDeliveryTaskFailReasonType failReasonType) {
        var o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        var o1Place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());

        var response = failLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place2.getBarcode(), 0, failReasonType);

        assertThat(response.getOrderActionList()).isNotEmpty();
        var orderActionDto = response.getOrderActionList().get(0);
        assertThat(orderActionDto.getOrderDto().getExternalOrderId()).isEqualTo(EXTERNAL_ORDER_ID_1);
        assertThat(orderActionDto.getOrderActions()).isNotEmpty();
        assertThat(orderActionDto.getOrderActions().get(0)).isEqualTo(OrderAction.RESCHEDULED);
    }

    @ParameterizedTest
    @EnumSource(value = OrderDeliveryTaskFailReasonType.class, names = {
            "LOCKER_FULL", "OTHER", "DIMENSIONS_EXCEEDS", "ORDER_IS_DAMAGED"
    }, mode = EnumSource.Mode.EXCLUDE)
    public void actionShouldBeEmptyOnDeliveryTwoOrdersAndFail(OrderDeliveryTaskFailReasonType failReasonType) {
        var o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        var o1Place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());

        var response = failLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place2.getBarcode(), 0, failReasonType);

        assertThat(response.getOrderActionList()).isEmpty();
    }

    private void clientReturns() {
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
    }

    private void extraditionsAllOrders() {
        var dto1 = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.EXTRADITION)
                .barcode("scanned_barcode1")
                .externalOrderId(EXTERNAL_EXT_ORDER_ID_1)
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(1)
                .type(0)
                .build();

        var dto2 = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.EXTRADITION)
                .barcode("scanned_barcode2")
                .externalOrderId(EXTERNAL_EXT_ORDER_ID_2)
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(1)
                .type(0)
                .build();

        var dto3 = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.EXTRADITION)
                .barcode("scanned_barcode3")
                .externalOrderId(EXTERNAL_EXT_ORDER_ID_3)
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(1)
                .type(0)
                .build();

        service.extraditionSuccess("token", dto1, user);
        service.extraditionSuccess("token", dto2, user);
        service.extraditionSuccess("token", dto3, user);
    }


    private void finishLoadingLockerPlace(String extId, String barcode) {
        MarketDeliverySuccessNotifyDto successNotifyDto = MarketDeliverySuccessNotifyDto.builder()
                .barcode(barcode)
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(1)
                .idPostamatCell(12)
                .externalOrderId(extId)
                .build();
        service.deliverySuccess("token", successNotifyDto, user);
    }

    private LockerDeliveryErrorResponseDto failLoadingLockerPlace(
            String extId, String barcode, int failReason, OrderDeliveryTaskFailReasonType reasonType
    ) {
        MarketDeliveryErrorNotifyDto successNotifyDto = MarketDeliveryErrorNotifyDto.builder()
                .externalOrderId(extId)
                .taskId(lockerDeliveryTask.getId())
                .reasonType(reasonType)
                .reason(failReason)
                .barcode(barcode)
                .build();
        return service.deliveryError("token", successNotifyDto, user);
    }

    private LockerSubtaskPlace getPlace(Set<LockerSubtaskPlace> places, String barcode) {
        return places.stream().filter(p -> p.getBarcode().equals(barcode)).findFirst().get();
    }

    private void finishTask() {
        service.finishTask(lockerDeliveryTask.getId(), null, user);
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

