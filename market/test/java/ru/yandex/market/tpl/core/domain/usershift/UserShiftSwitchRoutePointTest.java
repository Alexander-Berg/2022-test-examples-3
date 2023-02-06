package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.task.RoutePointSwitchReasonType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class UserShiftSwitchRoutePointTest extends TplAbstractTest {

    private final RoutePointSwitchedByCourierHistoryRepository eventRepository;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftRepository repository;
    private final TestUserHelper testUserHelper;
    private final Clock clock;

    private User user;
    private long userShiftId;

    @BeforeEach
    void setup() {
        user = testUserHelper.findOrCreateUser(57234L, LocalDate.now(clock));
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("address1", 10, orderGenerateService.createOrder().getId()))
                .routePoint(helper.taskUnpaid("address2", 12, orderGenerateService.createOrder().getId()))
                .routePoint(helper.taskUnpaid("address3", 14, orderGenerateService.createOrder().getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShiftId = commandService.createUserShift(createCommand);
        var userShift = repository.findById(userShiftId).orElseThrow();
        userShift.startShift();
        userShift.forceSwitchToNextRoutePoint(new ArrayList<>());
        userShift.setActive(true);
    }

    @Test
    @Transactional
    void switchRoutePointByCourierTest() {

        var userShift = repository.findByIdOrThrow(userShiftId);
        assertThat(userShift.getCurrentRoutePoint()).isNotNull();
        var currentRoutePoint = userShift.getCurrentRoutePoint();
        var expectedRoutePointIndex = userShift.getRoutePoints().indexOf(currentRoutePoint) + 1;
        var expectedRoutePoint = userShift.getRoutePoints().get(expectedRoutePointIndex);

        var command = new UserShiftCommand.SwitchOpenRoutePoint(userShiftId, expectedRoutePoint.getId(),
                RoutePointSwitchReasonType.OTHER, "test comment");
        commandService.switchOpenRoutePoint(user, command);

        assertThat(userShift.getCurrentRoutePoint()).isNotNull();
        assertThat(userShift.getCurrentRoutePoint().getId()).isEqualTo(expectedRoutePoint.getId());

        var switchEvents = eventRepository.findByRoutePointIdIn(Set.of(expectedRoutePoint.getId()));
        assertThat(switchEvents).hasSize(1);
        var event = switchEvents.get(0);

        assertThat(event.getOldRoutePointId()).isEqualTo(currentRoutePoint.getId());
        assertThat(event.getRoutePointId()).isEqualTo(expectedRoutePoint.getId());
        assertThat(event.getReason()).isEqualTo(RoutePointSwitchReasonType.OTHER);
        assertThat(event.getComment()).isEqualTo("test comment");
    }
}
