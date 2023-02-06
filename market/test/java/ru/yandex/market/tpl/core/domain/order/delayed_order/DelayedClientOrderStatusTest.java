package ru.yandex.market.tpl.core.domain.order.delayed_order;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistory;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistoryRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftFailCancelHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static ru.yandex.market.tpl.api.model.routepoint.RoutePointType.DELIVERY;


@RequiredArgsConstructor
class DelayedClientOrderStatusTest extends TplAbstractTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftFailCancelHelper userShiftFailCancelHelper;
    private final DelayedOrderStatusService delayedOrderStatusService;
    private final OrderFlowStatusHistoryRepository historyRepository;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;
    private final TransactionTemplate tt;

    private UserShift userShift;
    private User user;

    Order prepaidOrder1;

    @BeforeEach
    void createShiftAndPassOneRoutePoint() {
        tt.execute(a -> {
            user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19" +
                    ":00"));

            var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

            prepaidOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .paymentType(OrderPaymentType.PREPAID)
                    .build());


            var createCommand = UserShiftCommand.Create.builder()
                    .userId(user.getId())
                    .shiftId(shift.getId())
                    .routePoint(helper.taskPrepaid("addr3", 14, prepaidOrder1.getId()))
                    .build();

            userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
            commandService.switchActiveUserShift(user, userShift.getId());
            commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
            commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
            userHelper.finishPickupAtStartOfTheDayAndSelectNext(userShift);
            return userShift;
        });
    }

    @Test
    void shouldChangeOrderHistoryStatus() {
        var routePoint = userShift.streamDeliveryRoutePoints()
                .filter(rp -> rp.getType() == DELIVERY)
                .findFirst().get();

        var deliveryTask = routePoint
                .streamDeliveryTasks()
                .findFirst().get();

        var failReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                "Source.COURIER"
        );

        var payload = new DelayedOrderStatusPayload(
                "requestId",
                userShift.getId(),
                deliveryTask.getId(),
                failReason,
                Instant.now(clock)
        );

        commandService.failDeliveryTask(
                user,
                new UserShiftCommand.FailOrderDeliveryTask(
                        userShift.getId(),
                        deliveryTask.getRoutePoint().getId(),
                        deliveryTask.getId(),
                        failReason
                ));
        List<OrderFlowStatusHistory> history = historyRepository
                .findByExternalOrderIdHistory(prepaidOrder1.getExternalOrderId());
        long countReschedule = history.stream()
                .filter(h -> h.getOrderFlowStatusAfter() == OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_DELIVERY)
                .count();
        delayedOrderStatusService.processPayload(payload);
        history = historyRepository
                .findByExternalOrderIdHistory(prepaidOrder1.getExternalOrderId());
        countReschedule = history.stream()
                .filter(h -> h.getOrderFlowStatusAfter() == OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_DELIVERY)
                .count();

        AssertionsForClassTypes.assertThat(countReschedule).isEqualTo(1L);
        commandService.closeShift(new UserShiftCommand.Close(userShift.getId()));
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        history = historyRepository
                .findByExternalOrderIdHistory(prepaidOrder1.getExternalOrderId());

        countReschedule = history.stream()
                .filter(h -> h.getOrderFlowStatusAfter() == OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_DELIVERY)
                .count();

        AssertionsForClassTypes.assertThat(countReschedule).isEqualTo(1L);
    }
}
