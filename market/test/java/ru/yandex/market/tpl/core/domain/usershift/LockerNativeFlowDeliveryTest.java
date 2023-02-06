package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.locker.FinishMessage;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketDeliveryErrorNotifyDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketDeliverySuccessNotifyDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketExtraditionSuccessNotifyDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.ReturnType;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerSubtaskPlaceStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.SenderWithoutExtId;
import ru.yandex.market.tpl.core.domain.order.SenderWithoutExtIdRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
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
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn.CLIENT_RETURN_BARCODE_PREFIX_PS;

@RequiredArgsConstructor
@Transactional
public class LockerNativeFlowDeliveryTest extends TplAbstractTest {
    public static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";
    public static final String EXTERNAL_ORDER_ID_2 = "EXTERNAL_ORDER_ID_2";
    public static final String EXTERNAL_ORDER_ID_3 = "EXTERNAL_ORDER_ID_3";
    public static final String EXTERNAL_ORDER_ID_4 = "LO-EXTERNAL_ORDER_ID_4";
    private final TestDataFactory testDataFactory;
    private final EntityManager entityManager;
    private final TestUserHelper testUserHelper;
    private final PickupPointRepository pickupPointRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftQueryService queryService;
    private final UserShiftCommandService userShiftCommandService;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final SortingCenterService sortingCenterService;
    private final LockerDeliveryService service;
    private final UserPropertyService userPropertyService;
    private final LockerOrderDataHelper lockerOrderDataHelper;
    private final LmsOrderService lmsOrderService;
    private final SenderWithoutExtIdRepository senderWithoutExtIdRepository;
    private final DbQueueTestUtil dbQueueTestUtil;

    private User user;
    private Shift shift;
    private Long userShiftId;
    private Order order1;
    private Order order2;
    private Order order3;
    private Order order4;
    private UserShift userShift;
    private RoutePoint routePoint;
    private LockerDeliveryTask lockerDeliveryTask;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order1 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 2);
        order2 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_ORDER_ID_2, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        order3 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_ORDER_ID_3, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        order4 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_ORDER_ID_4, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        senderWithoutExtIdRepository.save(
                new SenderWithoutExtId(order4.getSender().getYandexId())
        );

        userShiftReassignManager.assign(userShift, order1);
        userShiftReassignManager.assign(userShift, order2);
        userShiftReassignManager.assign(userShift, order3);
        userShiftReassignManager.assign(userShift, order4);

        testUserHelper.checkinAndFinishPickup(userShift);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(4);

        testUserHelper.arriveAtRoutePoint(routePoint);
        checkAssignment(user, order1, order2, order3, order4);
        UserPropertyEntity up = new UserPropertyEntity();
        up.setUser(user);
        up.setName(UserPropsType.NATIVE_LOCKER_FLOW_ENABLED.getName());
        up.setType(TplPropertyType.BOOLEAN);
        up.setValue(Boolean.toString(true));
        userPropertyService.save(up);
    }

    @Test
    public void shouldChangePlaceStatus() {
        var task = fetchLockerDeliveryTask();
        LockerSubtaskPlace place = getPlace(
                lockerDeliveryTask.getSubtasks().get(1).getPlaces(),
                EXTERNAL_ORDER_ID_2 + "-1"
        );
        assertThat(place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);

        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_2, place.getBarcode());
        finishTask();

        assertThat(place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
        assertThat(task.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }

    @Test
    public void shouldChangePlaceStatusOnlyByBarcode() {
        var task = fetchLockerDeliveryTask();
        String barcode = EXTERNAL_ORDER_ID_4 + "-1";
        LockerSubtaskPlace place = getPlace(
                lockerDeliveryTask.getSubtasks().get(3).getPlaces(),
                barcode
        );
        assertThat(place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);

        finishLoadingLockerPlace(place.getBarcode(), place.getBarcode());
        finishTask();

        assertThat(place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
        assertThat(task.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }

    @Test
    public void shouldBeIdempotent() {
        var task = fetchLockerDeliveryTask();
        LockerSubtaskPlace place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        assertThat(place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);

        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, place.getBarcode());
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, place.getBarcode());
        finishTask();

        assertThat(place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
        assertThat(task.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }

    @Test
    public void shouldFinishTwoLockerSubtask() {
        var task = fetchLockerDeliveryTask();
        LockerSubtaskPlace o2Place = getPlace(lockerDeliveryTask.getSubtasks().get(1).getPlaces(),
                EXTERNAL_ORDER_ID_2 + "-1");
        assertThat(o2Place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(task.getSubtasks().get(1).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_2, o2Place.getBarcode());
        assertThat(task.getSubtasks().get(1).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);

        LockerSubtaskPlace o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        LockerSubtaskPlace o1Place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        assertThat(o1Place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(o1Place2.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place2.getBarcode());

        LockerDeliveryTaskDto taskDto = (LockerDeliveryTaskDto) service.getTask(lockerDeliveryTask.getId(), user);
        assertThat(taskDto.getOrders().size()).isEqualTo(4);
        assertThat(taskDto.getCompletedOrders().size()).isEqualTo(2);
        var rp = service.addLockerExtraditionIfNeed(queryService.getRoutePointInfo(user, routePoint.getId()), user);
        rp.getTasks();
        finishTask();

        assertThat(o1Place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.FINISHED);
        assertThat(o1Place2.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.FINISHED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(task.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);

    }

    @Test
    public void shouldFinishThreeLockerSubtaskAfterManualOrderFinish() {
        var task = fetchLockerDeliveryTask();
        LockerSubtaskPlace o2Place = getPlace(lockerDeliveryTask.getSubtasks().get(1).getPlaces(),
                EXTERNAL_ORDER_ID_2 + "-1");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_2, o2Place.getBarcode());
        LockerSubtaskPlace o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        LockerSubtaskPlace o1Place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place2.getBarcode());

        LockerDeliveryTaskDto taskDto = (LockerDeliveryTaskDto) service.getTask(lockerDeliveryTask.getId(), user);
        var rp = service.addLockerExtraditionIfNeed(queryService.getRoutePointInfo(user, routePoint.getId()), user);
        rp.getTasks();
        finishTask();

        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(task.getSubtasks().get(1).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(task.getSubtasks().get(2).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);

        userShiftCommandService.closeShift(new UserShiftCommand.Close(userShift.getId()));
        userShiftCommandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
        long orderId = order3.getId();
        lmsOrderService.deliverOrderToPickupPoint(orderId);

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(task.getSubtasks().get(1).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(task.getSubtasks().get(2).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);

    }

    @Test
    public void shouldReopenOneSubtask() {
        var task = fetchLockerDeliveryTask();
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_2, null);

        LockerSubtaskPlace o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        LockerSubtaskPlace o1Place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place2.getBarcode());

        finishTask();
        service.reopen(task.getId(), user);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(task.getSubtasks().get(1).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(task.getSubtasks().get(2).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);
        testUserHelper.arriveAtRoutePoint(routePoint);
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_3, null);
        finishTask();
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(task.getSubtasks().get(1).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(task.getSubtasks().get(2).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(order1.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        assertThat(order2.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        assertThat(order3.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
    }

    @Test
    public void shouldCancelAndRescheduleOrderNewType() {
        createTwoReturns();
        var task = fetchLockerDeliveryTask();
        rescheduleLoadingLockerPlace(EXTERNAL_ORDER_ID_2, OrderDeliveryTaskFailReasonType.LOCKER_FULL);
        assertThat(task.getSubtasks().get(1).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
        assertThat(task.getSubtasks().get(1).getFailReason().getType())
                .isEqualTo(OrderDeliveryTaskFailReasonType.LOCKER_FULL);
        assertThat(order2.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);

        LockerSubtaskPlace o1Place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, o1Place.getBarcode());
        cancelLoadingLockerPlace(EXTERNAL_ORDER_ID_1, OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
        assertThat(task.getSubtasks().get(0).getFailReason().getType())
                .isEqualTo(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED);
        assertThat(order1.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);
        assertThat(order1.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        finishTask();
        assertThat(o1Place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.FINISHED);

        assertThat(task.getSubtasks().get(2).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
        assertThat(task.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }

    @Test
    public void shouldNotCancelAndSendSqsEvent() {
        userPropertyService.addPropertyToUser(user, UserProperties.LOM_CONTROLLED_LOCKER_CANCEL_ENABLED, true);
        var task = fetchLockerDeliveryTask();
        cancelLoadingLockerPlace(EXTERNAL_ORDER_ID_1, OrderDeliveryTaskFailReasonType.DIMENSIONS_EXCEEDS);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
        assertThat(task.getSubtasks().get(0).getFailReason().getType())
                .isEqualTo(OrderDeliveryTaskFailReasonType.DIMENSIONS_EXCEEDS);
        assertThat(order1.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
        assertThat(order1.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_LOCKER_DELIVERY_FAILED_SEND_TO_SQS, 1);
        finishTask();
        assertThat(task.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }

    public void createTwoReturns() {
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

    @Test
    public void shouldFinishLockerSubtask() {
        var task = fetchLockerDeliveryTask();
        LockerSubtaskPlace place = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-1");
        LockerSubtaskPlace place2 = getPlace(lockerDeliveryTask.getSubtasks().get(0).getPlaces(),
                EXTERNAL_ORDER_ID_1 + "-2");
        assertThat(place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(place2.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.NOT_STARTED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);

        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, place.getBarcode());
        finishLoadingLockerPlace(EXTERNAL_ORDER_ID_1, place2.getBarcode());
        finishTask();

        assertThat(place.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.FINISHED);
        assertThat(place2.getStatus()).isEqualTo(LockerSubtaskPlaceStatus.FINISHED);
        assertThat(task.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(task.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }


    private LockerSubtaskPlace getPlace(Set<LockerSubtaskPlace> places, String barcode) {
        return places.stream().filter(p -> p.getBarcode().equals(barcode)).findFirst().get();
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

    private void cancelLoadingLockerPlace(String externalOrderId) {
        MarketDeliveryErrorNotifyDto successNotifyDto = MarketDeliveryErrorNotifyDto.builder()
                .externalOrderId(externalOrderId)
                .taskId(lockerDeliveryTask.getId())
                .reason(3)
                .barcode(externalOrderId)
                .build();
        service.deliveryError("token", successNotifyDto, user);
    }

    private void cancelLoadingLockerPlace(String externalOrderId, OrderDeliveryTaskFailReasonType reasonType) {
        MarketDeliveryErrorNotifyDto successNotifyDto = MarketDeliveryErrorNotifyDto.builder()
                .externalOrderId(externalOrderId)
                .taskId(lockerDeliveryTask.getId())
                .reasonType(reasonType)
                .barcode(externalOrderId)
                .build();
        service.deliveryError("token", successNotifyDto, user);
    }

    private void rescheduleLoadingLockerPlace(String externalOrderId) {
        MarketDeliveryErrorNotifyDto successNotifyDto = MarketDeliveryErrorNotifyDto.builder()
                .externalOrderId(externalOrderId)
                .taskId(lockerDeliveryTask.getId())
                .reason(2)
                .barcode(externalOrderId)
                .build();
        service.deliveryError("token", successNotifyDto, user);
    }

    private void rescheduleLoadingLockerPlace(String externalOrderId, OrderDeliveryTaskFailReasonType reasonType) {
        MarketDeliveryErrorNotifyDto successNotifyDto = MarketDeliveryErrorNotifyDto.builder()
                .externalOrderId(externalOrderId)
                .taskId(lockerDeliveryTask.getId())
                .reasonType(reasonType)
                .barcode(externalOrderId)
                .build();
        service.deliveryError("token", successNotifyDto, user);
    }

    private void finishTask() {
        service.finishTask(lockerDeliveryTask.getId(), new FinishMessage("finish message"), user);
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
