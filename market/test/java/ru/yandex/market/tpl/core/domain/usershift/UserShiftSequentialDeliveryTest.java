package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ungomma
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
class UserShiftSequentialDeliveryTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final UserPropertyService userPropertyService;
    private final Clock clock;

    private UserShift userShift;
    private User user;

    @BeforeEach
    void createShiftAndPassOneRoutePoint() {

        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskPrepaid("addr3", 14, orderGenerateService.createOrder().getId()))
                .build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
    }

    @Test
    void openTaskForSequentialDelivery() {
        userPropertyService.addPropertyToUser(user, UserProperties.SEQUENTIAL_DELIVERY_ENABLED, true);

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
    }

    @Test
    void submitTaskForNonSequentialDelivery() {
        userPropertyService.addPropertyToUser(user, UserProperties.SEQUENTIAL_DELIVERY_ENABLED, false);

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_OPEN);
    }

}
