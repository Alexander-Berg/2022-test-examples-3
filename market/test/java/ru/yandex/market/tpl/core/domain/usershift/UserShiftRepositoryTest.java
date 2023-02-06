package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.test.AssertionUtils.assertPresent;

/**
 * @author ungomma
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
@Slf4j
class UserShiftRepositoryTest {

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final EntityManager entityManager;

    @MockBean
    private Clock clock;

    private User user;
    private Shift shift;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock, LocalDateTime.of(1999, 1, 1, 0, 0, 0));
        user = userHelper.findOrCreateUser(45L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
    }

    @Test
    void shouldCreateShift() {
        createUserShift(shift.getId(), 2);

        var savedShift = assertPresent(repository.findByShiftIdAndUserId(shift.getId(), user.getId()));
        assertThat(savedShift.getId()).isNotNull();
        assertThat(savedShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
    }

    @Test
    void testFindUsersWithPickupResult() {
        createUserShift(shift.getId(), 2);
        UserShift savedShift = assertPresent(repository.findCurrentShift(user));

        List<UserShiftRepository.UserWithPickupResult> usersWithPickupResult = repository
                .findUsersWithPickupResult(shift.getShiftDate());

        assertThat(usersWithPickupResult.stream()
                .map(UserShiftRepository.UserWithPickupResult::getId)
                .collect(Collectors.toList())
        ).containsOnly(user.getId());
        assertThat(usersWithPickupResult.stream()
                .map(UserShiftRepository.UserWithPickupResult::getShiftStart)
                .collect(Collectors.toList())
        ).containsOnly(savedShift.getScheduleData().getTimeInterval().getStart());
        assertThat(usersWithPickupResult.stream()
                .map(UserShiftRepository.UserWithPickupResult::getShiftEnd)
                .collect(Collectors.toList())
        ).containsOnly(savedShift.getScheduleData().getTimeInterval().getEnd());
    }

    @Test
    void shouldGetCurrentShift() {
        assertThat(repository.findCurrentShift(user)).isNotPresent();
        assertThat(repository.findCurrentShiftStatus(user)).isNotPresent();

        createUserShift(shift.getId(), 1);

        Shift anotherShift = userHelper.findOrCreateOpenShift(shift.getShiftDate().plusDays(1));

        createUserShift(anotherShift.getId(), 1);

        var currentShift = assertPresent(repository.findCurrentShift(user));
        assertThat(currentShift.getUser()).isEqualTo(user);
        assertThat(currentShift.getId()).isNotNull();

        var currentShiftStatus = assertPresent(repository.findCurrentShiftStatus(user));
        assertThat(currentShiftStatus.getUser()).isEqualTo(user);
        assertThat(currentShiftStatus.getId()).isEqualTo(currentShift.getId());

        var savedShiftById = assertPresent(repository.findByShiftIdAndUserId(shift.getId(), user.getId()));
        var anotherSavedShiftById = assertPresent(repository.findByShiftIdAndUserId(anotherShift.getId(),
                user.getId()));
        assertThat(savedShiftById.identityEquals(currentShift)).isFalse();
        assertThat(anotherSavedShiftById.identityEquals(currentShift))
                .describedAs("Last created shift becomes current shift").isTrue();
    }

    @Test
    void shouldFetchAllTasks() {
        List<RoutePoint> routePoints = createUserShift(shift.getId(), 2);
        long userShiftId = routePoints.get(0).getUserShift().getId();
        entityManager.clear();

        UserShift us = repository.findByIdOrThrow(userShiftId);

        var tasks = us.streamRoutePoints().flatMap(RoutePoint::streamTasks).toList();
        assertThat(tasks).hasSize(4);
    }

    @Test
    void shouldRetrieveTasks() {
        List<RoutePoint> deliveryRoutePoints = createUserShift(shift.getId(), 2);
        RoutePoint firstDeliveryTaskData = deliveryRoutePoints.get(0);
        RoutePoint secondDeliveryTaskData = deliveryRoutePoints.get(1);
        entityManager.clear();

        UserShift savedShift = assertPresent(repository.findCurrentShift(user));

        assertThat(savedShift.getUser()).isEqualTo(user);

        assertThat(savedShift.getRoutePoints()).hasSize(4);
        assertThat(savedShift.getCurrentRoutePoint()).isNull();

        assertThat(savedShift.getRoutePoints())
                .describedAs("RoutePoints should have ids")
                .extracting(RoutePoint::getId)
                .doesNotContainNull();

        assertThat(savedShift.getRoutePoints())
                .extracting(RoutePoint::getUserShift)
                .doesNotContainNull();

        assertThat(savedShift.getRoutePoints())
                .describedAs("RoutePoints should be created in NOT_STARTED status")
                .extracting(RoutePoint::getStatus)
                .containsOnly(RoutePointStatus.NOT_STARTED);

        assertThat(savedShift.getRoutePoints())
                .describedAs("At least one created tasks should be ORDER_DELIVERY")
                .flatExtracting(RoutePoint::getTasks)
                .hasAtLeastOneElementOfType(OrderDeliveryTask.class);

        assertThat(savedShift.getRoutePoints())
                .describedAs("At least one created tasks should be ORDER_PICKUP")
                .flatExtracting(RoutePoint::getTasks)
                .hasAtLeastOneElementOfType(OrderPickupTask.class);

        // rp0
        {
            RoutePoint firstRp = savedShift.getRoutePoints().get(0);
            assertThat(firstRp.getType()).isEqualTo(RoutePointType.ORDER_PICKUP);

            List<OrderPickupTask> tasks = firstRp.streamPickupTasks().toList();
            assertThat(tasks).hasSize(1);
            assertThat(tasks.get(0).getRoutePoint()).isEqualTo(firstRp);
        }

        // rp1
        {
            RoutePoint secondRp = savedShift.getRoutePoints().get(1);
            assertThat(secondRp.getName()).isEqualTo(firstDeliveryTaskData.getName());

            assertThat(secondRp.getRoutePointAddress()).isEqualTo(firstDeliveryTaskData.getRoutePointAddress());
            assertThat(secondRp.getRoutePointAddress()).isEqualToComparingFieldByField(firstDeliveryTaskData.getRoutePointAddress());

            List<DeliveryTask> tasks = secondRp.streamDeliveryTasks().toList();
            assertThat(tasks).hasSameSizeAs(secondRp.tasks);
            List<Long> orderIds = StreamEx.of(firstDeliveryTaskData.getTasks())
                    .select(OrderDeliveryTask.class)
                    .map(OrderDeliveryTask::getOrderId).collect(Collectors.toList());
            assertThat(tasks).flatExtracting(DeliveryTask::getOrderIds)
                    .containsExactlyElementsOf(orderIds);
            assertThat(tasks.get(0).getRoutePoint()).isEqualTo(secondRp);
        }

        // rp2
        {
            RoutePoint thirdRp = savedShift.getRoutePoints().get(2);
            assertThat(thirdRp.getName()).isEqualTo(secondDeliveryTaskData.getName());

            List<DeliveryTask> tasks = thirdRp.streamDeliveryTasks().toList();
            assertThat(tasks).hasSameSizeAs(thirdRp.tasks);
            List<Long> orderIds = StreamEx.of(secondDeliveryTaskData.getTasks())
                    .select(OrderDeliveryTask.class)
                    .map(OrderDeliveryTask::getOrderId).collect(Collectors.toList());
            assertThat(tasks).flatExtracting(DeliveryTask::getOrderIds)
                    .containsExactlyElementsOf(orderIds);
            assertThat(tasks.get(0).getRoutePoint()).isEqualTo(thirdRp);
        }

    }

    private List<RoutePoint> createUserShift(long shiftId, int routePointsSize) {
        long userShiftId = commandService.createUserShift(
                UserShiftCommand.Create.builder()
                        .shiftId(shiftId)
                        .userId(user.getId())
                        .build()
        );

        commandService.switchActiveUserShift(user, userShiftId);

        return IntStreamEx.range(0, routePointsSize)
                .mapToObj(i -> commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                                userShiftId,
                                helper.taskPrepaid("addr_" + i, 5 + i, orderGenerateService.createOrder().getId()),
                                SimpleStrategies.NO_MERGE)
                        ).getRoutePoint()
                ).toList();
    }

}
