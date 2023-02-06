package ru.yandex.market.tpl.core.service.equeue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

@RequiredArgsConstructor
class EqueueStateChangeServiceTest extends TplAbstractTest {

    private final EqueueStateChangeService subject;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final DbQueueTestUtil dbQueueTestUtil;

    @Test
    @Transactional
    void entered() {
        UserShift userShift = createUserShiftAndFinishPickup(9990L, true);

        subject.entered(userShift);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    @Test
    @Transactional
    void enteredWithDisabledEqueue() {
        UserShift userShift = createUserShiftAndFinishPickup(9990L, false);

        subject.entered(userShift);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 0);
    }

    @Test
    @Transactional
    void finished() {
        UserShift userShift = createUserShiftAndFinishPickup(9990L, true);

        subject.finished(userShift);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    @Test
    @Transactional
    void finishedWithDisabledEqueue() {
        UserShift userShift = createUserShiftAndFinishPickup(9990L, false);

        subject.finished(userShift);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    @Test
    @Transactional
    void leftTheService() {
        UserShift userShift = createUserShiftAndFinishPickup(9990L, true);

        subject.leftTheService(userShift);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    @Test
    @Transactional
    void leftTheServiceWithDisabledEqueue() {
        UserShift userShift = createUserShiftAndFinishPickup(9990L, false);

        subject.leftTheService(userShift);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
    }

    private UserShift createUserShiftAndFinishPickup(long uin, boolean equeueEnabled) {
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

}
