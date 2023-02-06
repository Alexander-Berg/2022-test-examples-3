package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_SHIFT_CLOSED_CRON_UPDATED;

@RequiredArgsConstructor
public class UserShiftCancelWithoutTerminatedTasksTest extends TplAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftManager userShiftManager;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    private User user;
    private User user2;
    private Order order;
    private Shift shift;
    private UserShift userShift;


    /**
     * Создаем смену с заказом на забор и на доставку.
     */
    @BeforeEach
    void createShifts() {
        when(configurationProviderAdapter.isBooleanEnabled(IS_SHIFT_CLOSED_CRON_UPDATED)).thenReturn(true);
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .build());
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));
        user2 = userHelper.findOrCreateUser(824126L);
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        jdbcTemplate.update("UPDATE user_shift SET updated_at = now() - INTERVAL '10 minutes' WHERE id = ?",
                userShift.getId());
    }

    @Test
    void shouldCancelUserShiftAfterReassignIfOtherNotTerminalDontExists() {
        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user2.getId());
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        List<UserShift> userShifts = getAllShiftsToClose();
        assertThat(userShifts.size()).isEqualTo(1);
        userShiftManager.finishTasksAndCloseShift(userShifts.get(0).getId(),  "TEST", Source.SYSTEM);
        assertThat(repository.findById(userShift.getId()).orElseThrow().getStatus())
                .isEqualTo(UserShiftStatus.SHIFT_CLOSED);
    }


    @Test
    void shouldCancelUserShiftAfterStartingPickup() {
        transactionTemplate.execute(ts -> {
            var us = repository.findById(this.userShift.getId()).orElseThrow();

            commandService.checkin(us.getUser(), new UserShiftCommand.CheckIn(us.getId()));
            commandService.arriveAtRoutePoint(us.getUser(),
                    new UserShiftCommand.ArriveAtRoutePoint(
                            us.getId(),
                            us.streamPickupRoutePoints().findFirst().orElseThrow().getId(),
                            helper.getLocationDto(us.getId())
                    ));
            commandService.startOrderPickup(us.getUser(),
                    new UserShiftCommand.StartScan(
                            us.getId(),
                            us.getCurrentRoutePoint().getId(),
                            us.getCurrentRoutePoint().streamPickupTasks().findFirst().orElseThrow().getId()
                    ));


            assertThat(getAllShiftsToClose()).isEmpty();

            userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user2.getId());
            List<UserShift> userShifts = getAllShiftsToClose();
            assertThat(userShifts).hasSize(1);
            userShiftManager.finishTasksAndCloseShift(userShifts.get(0).getId(), "TEST", Source.SYSTEM);
            assertThat(us.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
            assertThat(getOrderPickupTask(us).getStatus()).isEqualTo(OrderPickupTaskStatus.FINISHED);

            return null;
        });

    }

    private List<UserShift> getAllShiftsToClose() {
        return userShiftManager.findShiftsToClose(shift.getShiftDate(),
                Instant.now().plusSeconds(1));
    }

    private OrderPickupTask getOrderPickupTask(UserShift userShift) {
        return userShift.streamPickupTasks().findFirst()
                .orElseThrow(() -> new RuntimeException("OrderPickupTask not found"));
    }

}
