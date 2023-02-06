package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class UserShiftRescheduleOrderAfterClientRescheduleTest extends TplAbstractTest {
    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final TransactionTemplate transactionTemplate;
    private final OrderRepository orderRepository;

    private final OrderManager orderManager;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;
    private User user;
    private Order order;
    private Shift shift;
    private UserShift userShift;
    private OrderDeliveryTask orderDeliveryTask;

    @BeforeEach
    void before() {
        user = userHelper.findOrCreateUser(824150L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .build());

        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        Instant arrivalTime = Instant.now(clock);

        transactionTemplate.execute(status -> {
            userShift = repository.findById(
                    commandService.createUserShift(
                            UserShiftCommand.Create.builder()
                                    .userId(user.getId())
                                    .shiftId(shift.getId())
                                    .routePoint(helper.taskPrepaid("addr1", order.getId(), arrivalTime, false))
                                    .build()
                    )).orElseThrow();
            orderDeliveryTask = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();
            commandService.switchActiveUserShift(user, userShift.getId());
            commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
            commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
            userHelper.finishPickupAtStartOfTheDayAndSelectNext(userShift);

            commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                    userShift.getId(), userShift.getCurrentRoutePoint().getId(),
                    helper.getLocationDto(userShift.getId())
            ));
            return null;
        });
    }

    /**
     * Ситуация.
     * Курьер не дозвонился клиенту и отменил заказ
     * Клиент меняет дату заказа через 'где курьер' на завтра
     * Проходит маршрутизация, во время которой заказ назначается на завтрашнюю смену
     * Курьер возвращает заказ на СЦ, в результате чего происходит его переназначение на завтра
     * Ожидаемая логика - заказ перенесен на завтра
     */
    @Test
    void shouldCancelNextDayTaskIfOrderCancelledByDelivery() {
        Instant startInstantTo = order.getDelivery().getDeliveryIntervalTo();
        Instant startInstantFrom = order.getDelivery().getDeliveryIntervalFrom();

        // Курьер не дозвонился клиенту и отменяет заказ
        OrderDeliveryFailReason failReason = new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.NO_CONTACT,
                null, null,
                Source.DELIVERY);
        UserShiftCommand.FailOrderDeliveryTask failOrderDeliveryTask =
                new UserShiftCommand.FailOrderDeliveryTask(userShift.getId(),
                        orderDeliveryTask.getRoutePoint().getId(), orderDeliveryTask.getId(), failReason);
        commandService.failDeliveryTask(user, failOrderDeliveryTask);

        // Клиент принимает решение перенести заказ на завтра
        transactionTemplate.execute(status -> {
            order = orderRepository.findByIdOrThrow(order.getId());
            Interval deliveryInterval = order.getDelivery().getInterval();
            orderManager.rescheduleOrder(
                    order,
                    new Interval(deliveryInterval.getStart().plus(1, ChronoUnit.DAYS),
                            deliveryInterval.getEnd().plus(1, ChronoUnit.DAYS)),
                    OrderDeliveryRescheduleReasonType.CLIENT_BY_TRACKING,
                    Source.CLIENT);

            //Происходит маршрутизация, которая назначает заказ на завтрашнюю смену
            Instant arrivalTime = Instant.now(clock);
            Shift shiftNextDay = userHelper.findOrCreateOpenShift(LocalDate.now(clock).plusDays(1));
            UserShift userShiftNextDay = repository.findById(
                    commandService.createUserShift(
                            UserShiftCommand.Create.builder()
                                    .userId(user.getId())
                                    .shiftId(shiftNextDay.getId())
                                    .routePoint(helper.taskPrepaid("addr1", order.getId(), arrivalTime.plus(1,
                                            ChronoUnit.DAYS), false))
                                    .build()
                    )).orElseThrow();

            // Курьер возвращает заказа на СЦ и завершает смену
            returnOrders();
            commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

            //Проверяем, что таска на завтра не отменена и дата заказа действительно изменилась только на 1 день
            userShiftNextDay = repository.findByIdOrThrow(userShiftNextDay.getId());
            OrderDeliveryTask orderDeliveryTask = userShiftNextDay.streamOrderDeliveryTasks().findFirst().orElseThrow();
            assertThat(orderDeliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
            assertThat(order.getDelivery().getDeliveryIntervalTo()).isEqualTo(startInstantTo.plus(1, ChronoUnit.DAYS));
            assertThat(order.getDelivery().getDeliveryIntervalFrom()).isEqualTo(startInstantFrom.plus(1,
                    ChronoUnit.DAYS));

            return null;
        });
    }


    private void returnOrders() {
        RoutePoint returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        returnRoutePoint.getId(),
                        helper.getLocationDto(userShift.getId())
                ));
        var returnTask = returnRoutePoint.getTasks().get(0);

        if (returnTask.getStatus() != OrderReturnTaskStatus.READY_TO_FINISH) {
            var returnOrdersCommand = new UserShiftCommand.FinishScan(
                    userShift.getId(),
                    returnRoutePoint.getId(),
                    returnTask.getId(),
                    ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order.getId()))
                            .finishedAt(Instant.now(clock))
                            .build()
            );

            var returnStartCommand = new UserShiftCommand.StartScan(
                    userShift.getId(),
                    returnRoutePoint.getId(),
                    returnTask.getId()
            );

            commandService.startOrderReturn(user, returnStartCommand);
            commandService.finishReturnOrders(user, returnOrdersCommand);
        }
        var returnFinishTaskCommand = new UserShiftCommand.FinishReturnTask(
                userShift.getId(),
                returnRoutePoint.getId(),
                returnTask.getId()
        );
        commandService.finishReturnTask(user, returnFinishTaskCommand);

    }

}
