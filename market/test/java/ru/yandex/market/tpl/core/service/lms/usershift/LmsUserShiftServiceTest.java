package ru.yandex.market.tpl.core.service.lms.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.task.OrderPickupTaskWorkflowService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class LmsUserShiftServiceTest extends TplAbstractTest {

    private final LmsUserShiftService subject;
    private final OrderPickupTaskWorkflowService orderPickupTaskWorkflowService;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final TransactionTemplate transactionTemplate;

    @Test
    void closeUserShiftWithEqueueEnabled() {
        UserShift userShift = transactionTemplate.execute(
                status -> {
                    var shift = createUserShiftAndArriveAtPickup(0L, true);
                    orderPickupTaskWorkflowService.enqueue(
                            shift.getId(),
                            shift.getUser(),
                            shift.getCurrentRoutePoint().getId(),
                            getOrderPickupTask(shift).getId()
                    );
                    return shift;
                }
        );
        Long userShiftId = userShift.getId();

        subject.closeUserShift(userShiftId);

        transactionTemplate.execute(status -> {
            UserShift result = userShiftRepository.findByIdOrThrow(userShiftId);

            assertThat(result.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
            assertThat(getOrderPickupTask(result).getStatus()).isEqualTo(OrderPickupTaskStatus.PARTIALLY_FINISHED);
            return null;
        });
    }

    @Test
    void closeUserShiftWithEqueueDisabled() {
        UserShift userShift = transactionTemplate.execute(
                status -> createUserShiftAndArriveAtPickup(0L, false)
        );
        Long userShiftId = userShift.getId();

        subject.closeUserShift(userShiftId);

        transactionTemplate.execute(status -> {
            UserShift result = userShiftRepository.findByIdOrThrow(userShiftId);

            assertThat(result.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
            assertThat(getOrderPickupTask(result).getStatus()).isEqualTo(OrderPickupTaskStatus.PARTIALLY_FINISHED);
            return null;
        });
    }

    private UserShift createUserShiftAndArriveAtPickup(long uin, boolean equeueEnabled) {
        User user = userHelper.findOrCreateUser(uin);
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();
        long userShiftId = commandService.createUserShift(createCommand);

        UserShift userShift = userShiftRepository.findById(userShiftId).get();

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getShift().getSortingCenter(),
                SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED,
                equeueEnabled
        );

        commandService.switchActiveUserShift(user, userShiftId);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        RoutePoint routePoint = userShift.getCurrentRoutePoint();
        Long routePointId = routePoint.getId();

        commandService.arriveAtRoutePoint(
                user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShiftId, routePointId,
                        new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShiftId)
                )
        );

        return userShift;
    }

    private OrderPickupTask getOrderPickupTask(UserShift userShift) {
        return userShift.streamPickupTasks().findFirst()
                .orElseThrow(() -> new RuntimeException("OrderPickupTask not found"));
    }

}
