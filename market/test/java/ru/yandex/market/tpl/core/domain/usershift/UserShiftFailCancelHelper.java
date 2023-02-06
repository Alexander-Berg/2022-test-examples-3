package ru.yandex.market.tpl.core.domain.usershift;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class UserShiftFailCancelHelper {
    private final UserShiftCommandService commandService;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;


    public RoutePoint currentRoutePoint(UserShift userShift) {
        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        assumeThat(currentRoutePoint).isNotNull();
        return Objects.requireNonNull(currentRoutePoint);
    }

    public void arriveAtCurrentRoutePoint(User user, UserShift userShift) {
        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), currentRoutePoint(userShift).getId(),
                helper.getLocationDto(userShift.getId())
        ));
    }


    public void failTask(User user, UserShift userShift, RoutePoint rp) {
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.NO_CONTACT, "Недозвон")
        ));

        userHelper.finishCallTasksAtRoutePoint(rp);

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        assertThat(task.getFailReason().getType()).isEqualTo(OrderDeliveryTaskFailReasonType.NO_CONTACT);
        assertThat(task.getFailReason().getComment()).isEqualTo("Недозвон");
    }
}
