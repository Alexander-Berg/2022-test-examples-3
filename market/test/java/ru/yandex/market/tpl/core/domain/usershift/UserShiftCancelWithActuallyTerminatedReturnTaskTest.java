package ru.yandex.market.tpl.core.domain.usershift;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.OrderPaymentType.CASH;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_SHIFT_CLOSED_CRON_UPDATED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class UserShiftCancelWithActuallyTerminatedReturnTaskTest {

    private final OrderGenerateService orderGenerateService;
    private final OrderCommandService orderCommandService;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftManager userShiftManager;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    private User user;
    private User user2;
    private Order order;
    private List<Order> orders = new ArrayList<>();
    private Shift shift;
    private UserShift userShift;
    private UserShift userShiftMultiOrders;
    @MockBean
    private Clock clock;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    void createShifts() {
        when(configurationProviderAdapter.isBooleanEnabled(IS_SHIFT_CLOSED_CRON_UPDATED)).thenReturn(true);
        ClockUtil.initFixed(clock, LocalDateTime.now().minusHours(4).minusMinutes(40));
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(CASH)
                .build());
        orders = Stream.generate(() -> orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(CASH)
                        .build()
        )).limit(3)
                .collect(Collectors.toList());
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));
        user2 = userHelper.findOrCreateUser(824126L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShift = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        var createCommand2 = UserShiftCommand.Create.builder()
                .userId(user2.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskUnpaid("addr2", 12, orders.get(0).getId()))
                .routePoint(helper.taskUnpaid("addr3", 12, orders.get(1).getId()))
                .routePoint(helper.taskUnpaid("addr4", 12, orders.get(2).getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShiftMultiOrders =
                userShiftRepository.findById(commandService.createUserShift(createCommand2)).orElseThrow();
    }

    @Test
    void shouldCloseUserShiftAfterFinishScan() {
        commandService.checkin(userShift.getUser(), new UserShiftCommand.CheckIn(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(getAllShiftsToClose()).isEmpty();

        userHelper.finishDelivery(userShift.getCurrentRoutePoint(), true);
        ClockUtil.initFixed(clock, LocalDateTime.now(clock).plusHours(4));

        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, false);
        assertThat(getOrderReturnTask(userShift).getStatus()).isEqualTo(OrderReturnTaskStatus.READY_TO_FINISH);

        List<UserShift> userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(1);
        userShiftManager.finishTasksAndCloseShift(userShifts.get(0).getId(), "TEST", Source.SYSTEM);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
    }

    @Test
    void shouldNoCloseUserShiftAfterTwoHoursAfterFinishingDeliveryWithOrdersForReturn() {
        commandService.checkin(userShift.getUser(), new UserShiftCommand.CheckIn(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(getAllShiftsToClose()).isEmpty();

        userHelper.finishDelivery(userShift.getCurrentRoutePoint(), true);
        assertThat(getOrderReturnTask(userShift).getStatus()).isEqualTo(OrderReturnTaskStatus.NOT_STARTED);
        assertThat(getOrderReturnTask(userShift).getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        entityManager.flush();
        entityManager.detach(userShift);

        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(70, ChronoUnit.MINUTES)),
                getOrderReturnTask(userShift).getId());
        List<UserShift> userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(0);

        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(130, ChronoUnit.MINUTES)),
                getOrderReturnTask(userShift).getId());
        userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(0);

    }

    @Test
    void shouldCloseUserShiftTwoHoursAfterFinishingDeliveryWithoutOrdersForReturnAndFailTaskNotAccepted() {
        UserShift us = userShiftMultiOrders;
        List<OrderDeliveryTask> tasks = us.streamOrderDeliveryTasks().toList();

        userHelper.checkin(us);
        userHelper.finishPickupAtStartOfTheDay(us,
                List.of(tasks.get(0).getOrderId(), tasks.get(2).getOrderId()),
                List.of(tasks.get(1).getOrderId()));
        assertThat(getAllShiftsToClose()).isEmpty();

        assertThat(tasks.get(1).getFailReason().getType()).isEqualTo(OrderDeliveryTaskFailReasonType.ORDER_NOT_ACCEPTED);
        List<UserShift> userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(0);

        userShifts = getAllShiftsToClose();
        //Не должно закрыть, так как не завершены 2 доставки
        assertThat(userShifts).hasSize(0);
        userHelper.finishDelivery(userShiftRepository.findById(us.getId()).get().getCurrentRoutePoint(), false);

        userShifts = getAllShiftsToClose();
        //Не должно закрыть, так как не завершена 1 доставка
        assertThat(userShifts).hasSize(0);
        userHelper.finishDelivery(userShiftRepository.findById(us.getId()).get().getCurrentRoutePoint(), false);
        var orderReturnTask =
                userShiftRepository.findById(us.getId()).get().getCurrentRoutePoint().getOrderReturnTask();
        entityManager.persist(us);
        entityManager.detach(us);
        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(70, ChronoUnit.MINUTES)),
                orderReturnTask.getId());

        userShifts = getAllShiftsToClose();
        //Не должно закрыть, так как не прошло 2-ух часов с обновления заказа на возврат
        assertThat(userShifts).hasSize(0);
        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(130, ChronoUnit.MINUTES)),
                orderReturnTask.getId());

        us = entityManager.find(UserShift.class, us.getId());
        // прошло 2 часа, 2 доставки доставлена, треться отменена с причиной ORDER_NOT_ACCEPTED -- закрываем смены
        userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(1);
        us = userShiftRepository.findById(us.getId()).orElseThrow();

        userShiftManager.finishTasksAndCloseShift(us.getId(), "TEST", Source.SYSTEM);
        assertThat(us.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
    }

    @Test
    void shouldCloseUserShiftTwoHoursAfterFinishingDeliveryWithoutOrdersForReturnAndFailTaskLost() {
        UserShift us = userShiftMultiOrders;
        List<OrderDeliveryTask> tasks = us.streamOrderDeliveryTasks().toList();

        userHelper.checkin(us);
        userHelper.finishPickupAtStartOfTheDay(us,
                List.of(tasks.get(0).getOrderId(), tasks.get(2).getOrderId()),
                List.of(tasks.get(1).getOrderId()));
        assertThat(getAllShiftsToClose()).isEmpty();

        List<UserShift> userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(0);

        userShifts = getAllShiftsToClose();
        //Не должно закрыть, так как не завершены 2 доставки
        assertThat(userShifts).hasSize(0);
        userHelper.finishDelivery(userShiftRepository.findById(us.getId()).get().getCurrentRoutePoint(), false);

        userShifts = getAllShiftsToClose();
        //Не должно закрыть, так как не завершена 1 доставка
        assertThat(userShifts).hasSize(0);
        commandService.failDeliveryTask(null,
                new UserShiftCommand.FailOrderDeliveryTask(
                        us.getId(),
                        userShiftRepository.findById(us.getId()).get().getCurrentRoutePoint().getId(),
                        userShiftRepository.findById(us.getId()).get().getCurrentRoutePoint().getTasks().get(0).getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.ORDER_WAS_LOST, null, null, Source.COURIER)
                ));
        var orderReturnTask =
                userShiftRepository.findById(us.getId()).get().getCurrentRoutePoint().getOrderReturnTask();
        entityManager.persist(us);
        entityManager.detach(us);
        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(70, ChronoUnit.MINUTES)),
                orderReturnTask.getId());

        userShifts = getAllShiftsToClose();
        //Не должно закрыть, так как не прошло 2-ух часов с обновления заказа на возврат
        assertThat(userShifts).hasSize(0);
        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(130, ChronoUnit.MINUTES)),
                orderReturnTask.getId());

        us = entityManager.find(UserShift.class, us.getId());
        // прошло 2 часа, 1 доставка доставлена, вторая отменена ORDER_WAS_LOST,
        // третья отменена с причиной ORDER_NOT_ACCEPTED -- закрываем смены
        userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(1);
        us = userShiftRepository.findById(us.getId()).orElseThrow();

        userShiftManager.finishTasksAndCloseShift(us.getId(), "TEST", Source.SYSTEM);
        assertThat(us.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
    }

    @Test
    void shouldNoCloseUserShiftTwoHoursAfterFinishingDeliveryWithOrdersForReturnAndDelivered() {
        UserShift us = userShiftMultiOrders;
        List<OrderDeliveryTask> tasks = us.streamOrderDeliveryTasks().toList();

        userHelper.checkin(us);
        userHelper.finishPickupAtStartOfTheDay(us,
                List.of(tasks.get(0).getOrderId(), tasks.get(2).getOrderId()),
                List.of(tasks.get(1).getOrderId()));
        assertThat(getAllShiftsToClose()).isEmpty();

        assertThat(tasks.get(1).getFailReason().getType()).isEqualTo(OrderDeliveryTaskFailReasonType.ORDER_NOT_ACCEPTED);
        List<UserShift> userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(0);

        userShifts = getAllShiftsToClose();
        //Не должно закрыть, так как не завершены доставки
        assertThat(userShifts).hasSize(0);
        userShifts = getAllShiftsToClose();
        //Не должно закрыть, так как не завершены доставки
        assertThat(userShifts).hasSize(0);
        userHelper.finishDelivery(userShiftRepository.findById(us.getId()).get().getCurrentRoutePoint(), false);
        userShifts = getAllShiftsToClose();
        //Не должно закрыть, так как не завершены доставки
        assertThat(userShifts).hasSize(0);
        RoutePoint rp = userShiftRepository.findById(us.getId()).get().getCurrentRoutePoint();
        commandService.failDeliveryTask(null,
                new UserShiftCommand.FailOrderDeliveryTask(
                        us.getId(),
                        rp.getId(),
                        rp.getTasks().get(0).getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED, null, null, Source.COURIER)
                ));


        var orderReturnTask =
                userShiftRepository.findById(us.getId()).get().getCurrentRoutePoint().getOrderReturnTask();
        entityManager.persist(us);
        entityManager.detach(us);
        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(70, ChronoUnit.MINUTES)),
                orderReturnTask.getRoutePoint().getId());

        userShifts = getAllShiftsToClose();
        //Не должно закрыть, так как не прошло 2-ух часов с обновления заказа на возврат
        assertThat(userShifts).hasSize(0);
        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(130, ChronoUnit.MINUTES)),
                orderReturnTask.getRoutePoint().getId());

        userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(0);
    }

    @Test
    void shouldCloseUserShiftTwoHoursAfterFinishingDeliveryWithoutOrdersForReturnWithFailedTask() {
        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(getAllShiftsToClose()).isEmpty();

        userHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        assertThat(getOrderReturnTask(userShift).getStatus()).isEqualTo(OrderReturnTaskStatus.NOT_STARTED);
        assertThat(getOrderReturnTask(userShift).getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        entityManager.flush();
        entityManager.detach(userShift);

        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(70, ChronoUnit.MINUTES)),
                getOrderReturnTask(userShift).getId());
        List<UserShift> userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(0);

        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(130, ChronoUnit.MINUTES)),
                getOrderReturnTask(userShift).getId());
        userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(1);

        userShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        userShiftManager.finishTasksAndCloseShift(userShifts.get(0).getId(), "TEST", Source.SYSTEM);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
    }

    @Test
    void shouldCloseUserShiftAfterWithoutOrderToReturn() {
        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(getAllShiftsToClose()).isEmpty();
        RoutePoint rp = userShift.getCurrentRoutePoint();
        userHelper.finishDelivery(rp, false);
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, false);
        assertThat(getOrderReturnTask(userShift).getStatus()).isEqualTo(OrderReturnTaskStatus.AWAIT_CASH_RETURN);

        List<UserShift> userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(1);
        userShiftManager.finishTasksAndCloseShift(userShifts.get(0).getId(), "TEST", Source.SYSTEM);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
    }

    private List<UserShift> getAllShiftsToClose() {
        return userShiftManager.findShiftsToClose(shift.getShiftDate(),
                Instant.now(clock).plusSeconds(1));
    }

    private OrderReturnTask getOrderReturnTask(UserShift us) {
        return us.streamReturnRoutePoints().findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
    }

}
