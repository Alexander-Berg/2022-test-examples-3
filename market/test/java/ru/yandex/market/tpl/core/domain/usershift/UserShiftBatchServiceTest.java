package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteBatchDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaidBatchDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class UserShiftBatchServiceTest extends TplAbstractTest {

    private final TestUserHelper testUserHelper;

    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftBatchService userShiftBatchService;
    private final UserShiftQueryService userShiftQueryService;
    private final RoutePointRepository routePointRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    private User user;
    private UserShift userShift;
    private CallToRecipientTask callTask;

    @BeforeEach
    public void init() {
        transactionTemplate.execute(
                ts -> {
                    user = testUserHelper.findOrCreateUser(1L);
                    var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

                    GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

                    Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                            .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                            .paymentStatus(OrderPaymentStatus.UNPAID)
                            .paymentType(OrderPaymentType.CARD)
                            .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                    .geoPoint(geoPoint)
                                    .build())
                            .build());
                    Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                            .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                            .paymentStatus(OrderPaymentStatus.UNPAID)
                            .paymentType(OrderPaymentType.CARD)
                            .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                    .geoPoint(geoPoint)
                                    .build())
                            .build());

                    Instant deliveryTime = order1.getDelivery().getDeliveryIntervalFrom();
                    RoutePointAddress my_address = new RoutePointAddress("my_address", geoPoint);

                    NewDeliveryRoutePointData delivery1 = NewDeliveryRoutePointData.builder()
                            .address(my_address)
                            .expectedArrivalTime(deliveryTime)
                            .expectedDeliveryTime(deliveryTime)
                            .name("my_name")
                            .withOrderReferenceFromOrder(order1, false, false)
                            .build();

                    NewDeliveryRoutePointData delivery2 = NewDeliveryRoutePointData.builder()
                            .address(my_address)
                            .expectedArrivalTime(deliveryTime)
                            .expectedDeliveryTime(deliveryTime)
                            .name("my_name")
                            .withOrderReferenceFromOrder(order2, false, false)
                            .build();

                    var createCommand = UserShiftCommand.Create.builder()
                            .userId(user.getId())
                            .active(true)
                            .shiftId(shift.getId())
                            .routePoint(delivery1)
                            .routePoint(delivery2)
                            .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                            .build();

                    long userShiftId = commandService.createUserShift(createCommand);
                    userShift = userShiftRepository.findByIdOrThrow(userShiftId);
                    callTask = userShift.streamCallTasks().findFirst().orElseThrow();
                    return null;
                }
        );
    }

    @Test
    public void testBatchPayOrders() {
        var orderPaidDto = callTask.getOrderDeliveryTasks()
                .stream()
                .map(task -> createOrderPaidBatchDto(task.getId(), OrderPaymentType.CARD))
                .collect(Collectors.toList());
        var taskIds = orderPaidDto.stream()
                .map(OrderPaidBatchDto::getTaskId)
                .collect(Collectors.toList());
        long routePointId = userShiftQueryService.getRoutePointId(user, taskIds);
        var routePoint = routePointRepository.findById(routePointId).orElseThrow();

        testUserHelper.checkinAndFinishPickup(userShift);
        testUserHelper.arriveAtRoutePoint(routePoint);

        var uuid = UUID.randomUUID();
        userShiftBatchService.pay(user, orderPaidDto, uuid);
        userShiftBatchService.pay(user, orderPaidDto, uuid);

        var deliveryTaskInfo = userShiftQueryService.getDeliveryTasksInfo(user, routePointId, taskIds);
        deliveryTaskInfo.getTasks().forEach(
                task -> assertThat(task.getOrder().getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID)
        );
    }


    @Test
    public void testBatchRegisterCheques() {
        //проверяем одинаковые uuid'ы в разных методах
        var uuid = UUID.randomUUID();
        var orderPaidDto = callTask.getOrderDeliveryTasks()
                .stream()
                .map(task -> createOrderPaidBatchDto(task.getId(), OrderPaymentType.CARD))
                .collect(Collectors.toList());
        var taskIds = orderPaidDto.stream()
                .map(OrderPaidBatchDto::getTaskId)
                .collect(Collectors.toList());
        long routePointId = userShiftQueryService.getRoutePointId(user, taskIds);
        var routePoint = routePointRepository.findById(routePointId).orElseThrow();
        var routePointsToTasks = userShiftQueryService.getRoutePointsIdsToTaskIds(user, taskIds);

        testUserHelper.checkinAndFinishPickup(userShift);
        testUserHelper.arriveAtRoutePoint(routePoint);

        userShiftBatchService.pay(user, orderPaidDto, uuid);

        var deliveryTaskInfo = userShiftQueryService.getDeliveryTasksInfo(user, routePointId, taskIds);
        var deliveryTaskInfoV2 = userShiftQueryService.getDeliveryTasksInfo(user, routePointsToTasks);
        deliveryTaskInfo.getTasks().forEach(
                task -> assertThat(task.getOrder().getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID)
        );
        assertThat(deliveryTaskInfo).isEqualTo(deliveryTaskInfoV2);

        var registerChequeDto = callTask.getOrderDeliveryTasks()
                .stream()
                .map(task -> createOrderChequeRemoteBatchDto(task.getId(), OrderPaymentType.CARD, OrderChequeType.SELL))
                .collect(Collectors.toList());

        userShiftBatchService.registerCheques(user, registerChequeDto, uuid);
        userShiftBatchService.registerCheques(user, registerChequeDto, uuid);

        deliveryTaskInfo = userShiftQueryService.getDeliveryTasksInfo(user, routePointId, taskIds);
        deliveryTaskInfo.getTasks().forEach(
                task -> assertThat(task.getOrder().getCheques()).isNotEmpty()
        );
    }

    @Test
    public void testBatchRegisterChequesWithTasksOnDifferentRoutePoints() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(OrderPaymentType.PREPAID)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .build());

        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(OrderPaymentType.PREPAID)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .build());
        testUserHelper.addDeliveryTaskToShift(user, userShift, order);
        testUserHelper.addDeliveryTaskToShift(user, userShift, order2);

        var taskIds = userShift.streamCallTasks().map(CallToRecipientTask::getOrderDeliveryTasks)
                .flatMap(Collection::stream)
                .filter(OrderDeliveryTask::isPrepaidOrder)
                .map(Task::getId)
                .collect(Collectors.toList());

        var routePointsToTasks = userShiftQueryService.getRoutePointsIdsToTaskIds(user, taskIds);

        testUserHelper.checkinAndFinishPickup(userShift);

        var registerChequeDto = userShift.streamCallTasks().map(CallToRecipientTask::getOrderDeliveryTasks)
                .flatMap(Collection::stream)
                .filter(OrderDeliveryTask::isPrepaidOrder)
                .map(task -> createOrderChequeRemoteBatchDto(task.getId(), OrderPaymentType.PREPAID,
                        OrderChequeType.SELL))
                .collect(Collectors.toList());
        userShiftBatchService.registerCheques(user, registerChequeDto, null);


        userShiftQueryService.getDeliveryTasksInfo(user, routePointsToTasks).getTasks().forEach(
                task -> assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED)
        );
    }

    @Test
    void testReopenBatchOrders() {
        var taskIds = callTask.getOrderDeliveryTasks().stream().map(Task::getId).collect(Collectors.toList());

        var rp = transactionTemplate.execute(ts -> {
            long routePointId = userShiftQueryService.getRoutePointId(user, taskIds);
            var routePoint = routePointRepository.findById(routePointId).orElseThrow();

            testUserHelper.checkinAndFinishPickup(userShift);
            testUserHelper.finishDelivery(routePoint, true);
            return routePointId;
        });

        userShiftBatchService.reopenOrderDeliveryTasks(user, rp, taskIds);

        transactionTemplate.execute(ts -> {
            var us = userShiftRepository.findById(userShift.getId()).orElseThrow();
            assertThat(us.getCurrentRoutePoint().getId()).isEqualTo(rp);
            assertThat(us.getCurrentRoutePoint().getTasks().stream().map(Task::getId).collect(Collectors.toList()))
                    .containsAll(taskIds);
            us.getCurrentRoutePoint().streamOrderDeliveryTasks().forEach(
                    t -> assertThat(t.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED)
            );
            return null;
        });
    }

    private OrderPaidBatchDto createOrderPaidBatchDto(Long taskId, OrderPaymentType orderPaymentType) {
        var orderPaidBatchDto = new OrderPaidBatchDto();
        orderPaidBatchDto.setTaskId(taskId);
        orderPaidBatchDto.setPaymentType(orderPaymentType);
        return orderPaidBatchDto;
    }

    private OrderChequeRemoteBatchDto createOrderChequeRemoteBatchDto(Long taskId,
                                                                      OrderPaymentType orderPaymentType,
                                                                      OrderChequeType orderChequeType) {
        var orderCheque = new OrderChequeRemoteBatchDto();
        orderCheque.setTaskId(taskId);
        orderCheque.setPaymentType(orderPaymentType);
        orderCheque.setChequeType(orderChequeType);
        return orderCheque;
    }
}
