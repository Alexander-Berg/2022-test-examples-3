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
import ru.yandex.market.tpl.api.model.locker.LockerExtraditionErrorResponseDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketExtraditionErrorNotifyDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketExtraditionSuccessNotifyDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.ReturnType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnFail;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnFailRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.SenderWithoutExtId;
import ru.yandex.market.tpl.core.domain.order.SenderWithoutExtIdRepository;
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
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.LOCKER_FULL;

@Slf4j
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LockerNativeFlowExtraditionTest {
    public static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";
    public static final String EXTERNAL_ORDER_ID_2 = "LO-EXTERNAL_ORDER_ID_2";
    public static final String EXTERNAL_ORDER_ID_3 = "EXTERNAL_ORDER_ID_3";

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
    private final ClientReturnFailRepository clientReturnFailRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final SenderWithoutExtIdRepository senderWithoutExtIdRepository;

    private User user;
    private Shift shift;
    private Long userShiftId;
    private Order order;
    private Order order2;
    private Order order3;
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
        order2 = getPickupOrder(EXTERNAL_ORDER_ID_2, pickupPoint, geoPoint, OrderFlowStatus.DELIVERED_TO_PICKUP_POINT,
                1);
        order3 = getPickupOrder(EXTERNAL_ORDER_ID_3, pickupPoint, geoPoint, OrderFlowStatus.DELIVERED_TO_PICKUP_POINT,
                2);

        var s = new SenderWithoutExtId(order2.getSender().getYandexId());
        senderWithoutExtIdRepository.save(s);

        userShiftReassignManager.assign(userShift, order);

        testUserHelper.checkinAndFinishPickup(userShift);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(1);
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

        if (placeCount > 0) {
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
    public void shouldChangeOrderStatusToReadyForReturnForLoOrder() {
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        var dto = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.EXTRADITION)
                .barcode(order2.getPlaces().iterator().next().getBarcode().getBarcode())
                .externalOrderId(order2.getPlaces().iterator().next().getBarcode().getBarcode())
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();

        service.extraditionSuccess("token", dto, user);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.STARTED);
        var rp = queryService.getRoutePointInfo(user, routePoint.getId());
        var task = (LockerDeliveryTaskDto) rp.getTasks().get(0);
        service.finishTask(task.getId(), null, user);
        assertThat(order2.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        assertThat(order3.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }

    @Test
    public void shouldChangeOrderStatusToReadyForReturnForLoOrderWithoutExternalOrderId() {
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        String barcode = order2.getPlaces().iterator().next().getBarcode().getBarcode();
        var dto = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.EXTRADITION)
                .barcode(barcode)
                .externalOrderId(barcode)
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();

        service.extraditionSuccess("token", dto, user);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.STARTED);
        var rp = queryService.getRoutePointInfo(user, routePoint.getId());
        var task = (LockerDeliveryTaskDto) rp.getTasks().get(0);
        service.finishTask(task.getId(), null, user);
        assertThat(order2.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        assertThat(order3.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }

    @Test
    public void shouldChangeOrderStatusToReadyForReturn() {
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.SEND_COURIER_RECEIVED_RETURN_PICKUP_EVENT_FOR_CANCELLATION_RETURN_ENABLED,
                true
        );
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        var dto = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.EXTRADITION)
                .barcode(EXTERNAL_ORDER_ID_2)
                .externalOrderId(EXTERNAL_ORDER_ID_2)
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();

        service.extraditionSuccess("token", dto, user);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.STARTED);
        var rp = queryService.getRoutePointInfo(user, routePoint.getId());
        var task = (LockerDeliveryTaskDto) rp.getTasks().get(0);
        service.finishTask(task.getId(), null, user);
        service.finishTask(task.getId(), null, user);
        assertThat(order2.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        assertThat(order3.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);

        assertThat(dbQueueTestUtil.getQueue(QueueType.CREATE_CLIENT_RETURN)).containsExactly(EXTERNAL_ORDER_ID_2 + "-1");
    }

    @Test
    public void extraditionClientReturnErrorTest() {
        var errorNotifyDto = new MarketExtraditionErrorNotifyDto(2, "barcode", 1L,
                "returnId",
                ReturnType.CLIENT_RETURN, "2", LOCKER_FULL, null, null
        );
        var response = service.extraditionError(
                "token",
                errorNotifyDto,
                user);
        List<ClientReturnFail> all = clientReturnFailRepository.findAll();
        assertThat(all).hasSize(1);
        ClientReturnFail clientReturnFail = all.get(0);
        assertThat(clientReturnFail.getBarcode()).isNotBlank();
        assertThat(clientReturnFail.getReturnId()).isNotBlank();
        assertThat(clientReturnFail.getTaskId()).isNotNull();
        assertThat(clientReturnFail.getReturnId()).isNotNull();
        assertThat(response).isEqualTo(new LockerExtraditionErrorResponseDto(
                        errorNotifyDto.getIdStage(),
                        errorNotifyDto.getIdShipmentItem(),
                        errorNotifyDto.getBarcode(),
                        errorNotifyDto.getExternalOrderId(),
                        errorNotifyDto.getReturnId()
                )
        );
    }

    @Test
    public void extraditionOrderClientReturnErrorTest() {
        var errorNotifyDto = new MarketExtraditionErrorNotifyDto(
                2, "barcode", 1L, "returnId", ReturnType.CLIENT_RETURN,
                "2", OrderDeliveryTaskFailReasonType.CELL_FAILED_TO_OPEN, "Не работает",
                List.of("url1", "url2")
        );
        service.extraditionError("token", errorNotifyDto, user);
        var returnFails = clientReturnFailRepository.findAll();
        assertThat(returnFails).hasSize(1);
        var clientReturnFail = returnFails.get(0);
        assertThat(clientReturnFail.getBarcode()).isEqualTo("barcode");
        assertThat(clientReturnFail.getTaskId()).isEqualTo(1);
        assertThat(clientReturnFail.getReturnId()).isEqualTo("returnId");
        assertThat(clientReturnFail.getPhotoUrls()).contains("url1", "url2");
        assertThat(clientReturnFail.getComment()).isEqualTo("Не работает");
        assertThat(clientReturnFail.getFailReasonType()).isEqualTo(OrderDeliveryTaskFailReasonType.CELL_FAILED_TO_OPEN);
    }

    @Test
    public void extraditionOrderClientReturnErrorTestOldFlow() {
        var errorNotifyDto = new MarketExtraditionErrorNotifyDto(
                null, "barcode", 1L, "returnId", ReturnType.CLIENT_RETURN,
                "2", OrderDeliveryTaskFailReasonType.CELL_FAILED_TO_OPEN, "Не работает",
                List.of("url1", "url2")
        );
        service.extraditionError("token", errorNotifyDto, user);
        var returnFails = clientReturnFailRepository.findAll();
        assertThat(returnFails).hasSize(1);
        var clientReturnFail = returnFails.get(0);
        assertThat(clientReturnFail.getBarcode()).isEqualTo("barcode");
        assertThat(clientReturnFail.getTaskId()).isEqualTo(1);
        assertThat(clientReturnFail.getReasonId())
                .isEqualTo(
                        LockerDeliveryService.FAIL_REASON_TYPE_TO_BOX_BOT_ERROR_CODES.get(OrderDeliveryTaskFailReasonType.CELL_FAILED_TO_OPEN)
                );
        assertThat(clientReturnFail.getReturnId()).isEqualTo("returnId");
        assertThat(clientReturnFail.getPhotoUrls()).contains("url1", "url2");
        assertThat(clientReturnFail.getComment()).isEqualTo("Не работает");
        assertThat(clientReturnFail.getFailReasonType()).isEqualTo(OrderDeliveryTaskFailReasonType.CELL_FAILED_TO_OPEN);
    }

    @Test
    public void shouldChangeOrderStatusToReadyForReturnWhenPlaceExtracted() {
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.SEND_COURIER_RECEIVED_RETURN_PICKUP_EVENT_FOR_CANCELLATION_RETURN_ENABLED,
                true
        );
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        var dto = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.EXTRADITION)
                .barcode(EXTERNAL_ORDER_ID_3 + "-1")
                .externalOrderId(EXTERNAL_ORDER_ID_3)
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();

        service.extraditionSuccess("token", dto, user);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.STARTED);
        var rp = queryService.getRoutePointInfo(user, routePoint.getId());
        var task = (LockerDeliveryTaskDto) rp.getTasks().get(0);
        service.finishTask(task.getId(), null, user);
        assertThat(order2.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        assertThat(order3.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }

    @Test
    public void shouldBeIdempotent() {
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.SEND_COURIER_RECEIVED_RETURN_PICKUP_EVENT_FOR_CANCELLATION_RETURN_ENABLED,
                true
        );
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        var dto = MarketExtraditionSuccessNotifyDto.builder()
                .returnType(ReturnType.EXTRADITION)
                .barcode(EXTERNAL_ORDER_ID_3 + "-1")
                .externalOrderId(EXTERNAL_ORDER_ID_3)
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(2)
                .type(0)
                .build();

        service.extraditionSuccess("token", dto, user);
        service.extraditionSuccess("token", dto, user);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.STARTED);
        var rp = queryService.getRoutePointInfo(user, routePoint.getId());
        var task = (LockerDeliveryTaskDto) rp.getTasks().get(0);
        service.finishTask(task.getId(), null, user);
        assertThat(order2.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        assertThat(order3.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
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
