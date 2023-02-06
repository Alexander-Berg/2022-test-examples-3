package ru.yandex.market.tpl.core.query.usershift;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class LockerGoTest extends TplAbstractTest {
    private final UserShiftQueryService userShiftQueryService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Test
    void test() {
        User user = testUserHelper.findOrCreateUser(1L);
        testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        LockerDeliveryTask lockerDeliveryTask = transactionTemplate.execute(
                ts -> testDataFactory.addLockerDeliveryTask(userShift.getId(), PartnerSubType.LOCKER_GO));
        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user,
                lockerDeliveryTask.getRoutePoint().getId());
        LockerDeliveryTaskDto task = (LockerDeliveryTaskDto) routePointInfo.getTasks().iterator().next();
        assertThat(task.getType()).isEqualTo(TaskType.LOCKER_DELIVERY);
        assertThat(task.getLocker().getPartnerSubType()).isEqualTo(PartnerSubType.LOCKER_GO);
        assertThat(task.getLocker().getPickupPointType()).isEqualTo(PickupPointType.PVZ);
    }
}
