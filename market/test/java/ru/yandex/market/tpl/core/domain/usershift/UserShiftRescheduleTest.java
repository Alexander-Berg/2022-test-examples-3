package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnQueryService;
import ru.yandex.market.tpl.core.domain.clientreturn.events.history.ClientReturnHistoryEvent;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.AutoRescheduleService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryServiceFactory;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.exception.CommandFailedException;
import ru.yandex.market.tpl.core.exception.TplInvalidStateException;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnHistoryEventType.CLIENT_RETURN_RESCHEDULED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERY_FAILED;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;
import static ru.yandex.market.tpl.core.domain.order.OrderManager.DEFAULT_TIME_TO_RESCHEDULE_AFTER;
import static ru.yandex.market.tpl.core.domain.partner.DeliveryService.FAKE_DS_ID;

/**
 * @author kukabara
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class UserShiftRescheduleTest {

    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;

    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final OrderManager orderManager;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final UserShiftQueryService userShiftQueryService;
    private final DeliveryServiceFactory deliveryServiceFactory;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final AutoRescheduleService autoRescheduleService;
    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnRepository clientReturnRepository;
    private final ClientReturnQueryService clientReturnQueryService;
    private final ClientReturnHistoryEventRepository clientReturnHistoryEventRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final TestDataFactory testDataFactory;
    private final OrderDeliveryTaskRepository orderDeliveryTaskRepository;
    private final ShiftRepository shiftRepository;
    private User user;
    private UserShift userShift;
    private Shift shift;

    @MockBean
    private Clock clock;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);
        user = testUserHelper.findOrCreateUser(35236L);

        shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED, false
        );

        List<Order> orders = Stream.generate(orderGenerateService::createOrder).limit(3)
                .collect(Collectors.toList());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskPrepaid("addr1", 12, orders.get(0).getId()))
                .routePoint(helper.taskPrepaid("addr2", 13, orders.get(1).getId()))
                .routePoint(helper.taskPrepaid("addr3", 12, orders.get(2).getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShift = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        deliveryServiceFactory.createScheduledIntervalsForDeliveryServiceAllTimeAvailable();

        configurationServiceAdapter.insertValue(
                ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED, true);
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.ENABLE_CLIENT_RETURN_RESCHEDULE_EVENT_TO_LES, true
        );
    }

    @Test
    void shouldNotRescheduleOrders() {
        //given
        LocalDate date = LocalDate.now(clock);
        ClockUtil.initFixed(clock, LocalDateTime.of(date, DEFAULT_TIME_TO_RESCHEDULE_AFTER.minusHours(1)));

        //when
        userShift
                .getShift()
                .getSortingCenter()
                .getDeliveryServices()
                .forEach(autoRescheduleService::rescheduleOrdersByDeliveryService);
        //then
        assertOrderDate(date);
    }

    @DisplayName("Если курьер не принял заказ, то после срабатывания таски переносим заказ на завтра, " +
            "а задание на доставку отменяем")
    @Test
    void shouldRescheduleOrdersWhenNoDelivery() {
        //given
        LocalDate date = LocalDate.now(clock);
        ClockUtil.initFixed(clock, LocalDateTime.of(date, DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1)));

        //when
        userShift
                .getShift()
                .getSortingCenter()
                .getDeliveryServices()
                .forEach(autoRescheduleService::rescheduleOrdersByDeliveryService);

        //then
        assertThat(userShift.streamOrderDeliveryTasks().map(OrderDeliveryTask::getStatus).toSet())
                .containsOnly(DELIVERY_FAILED);
        assertOrderDate(LocalDate.now(clock).plusDays(1));
    }

    @DisplayName("Не переносим заявку, которая не была назначена на курьера до положенного времени")
    @Test
    void shouldNotRescheduleUnassignedClientReturn() {
        //given
        LocalDate date = LocalDate.now(clock);
        ClientReturn clientReturn = clientReturnGenerator.generateReturnFromClient(
                LocalDateTime.of(date, LocalTime.of(9, 0)),
                LocalDateTime.of(date, LocalTime.of(12, 0)),
                FAKE_DS_ID
        );
        ClockUtil.initFixed(clock, LocalDateTime.of(date, DEFAULT_TIME_TO_RESCHEDULE_AFTER.minusHours(1)));

        autoRescheduleService.rescheduleClientReturns(date, deliveryServiceFactory.getDeliveryService());

        //then
        ClientReturn rescheduled = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(rescheduled.getArriveIntervalFrom()).isEqualTo(
                LocalDateTime.of(date, LocalTime.of(9, 0))
        );
        Optional<ClientReturnHistoryEvent> rescheduleEvent =
                clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(), Pageable.unpaged())
                        .stream()
                        .filter(he -> he.getType() == CLIENT_RETURN_RESCHEDULED)
                        .findFirst();
        assertThat(rescheduleEvent).isEmpty();
    }

    @DisplayName("Переносим заявку, которая не была назначена на курьера")
    @Test
    void shouldRescheduleUnassignedClientReturn() {
        //given
        LocalDate date = LocalDate.now(clock);
        ClientReturn clientReturn = clientReturnGenerator.generateReturnFromClient(
                LocalDateTime.of(date, LocalTime.of(9, 0)),
                LocalDateTime.of(date, LocalTime.of(12, 0)),
                FAKE_DS_ID
        );
        ClockUtil.initFixed(clock, LocalDateTime.of(date, DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1)));

        autoRescheduleService.rescheduleClientReturns(date, deliveryServiceFactory.getDeliveryService());

        //then
        ClientReturn rescheduled = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(rescheduled.getArriveIntervalFrom()).isEqualTo(
                LocalDateTime.of(date.plusDays(1), LocalTime.of(9, 0))
        );
        Optional<ClientReturnHistoryEvent> rescheduleEvent =
                clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(), Pageable.unpaged())
                        .stream()
                        .filter(he -> he.getType() == CLIENT_RETURN_RESCHEDULED)
                        .findFirst();
        assertThat(rescheduleEvent).isPresent();
        assertThat(rescheduleEvent.get().getSource()).isEqualTo(Source.SYSTEM);
    }

    @DisplayName("Не переносим заявку, которая была назначена на курьера")
    @Test
    void shouldNotRescheduleAssignedClientReturn() {
        //given
        LocalDate date = LocalDate.now(clock);
        ClientReturn clientReturn = clientReturnGenerator.generateReturnFromClient(
                LocalDateTime.of(date, LocalTime.of(9, 0)),
                LocalDateTime.of(date, LocalTime.of(12, 0)),
                FAKE_DS_ID
        );
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        ClockUtil.initFixed(clock, LocalDateTime.of(date, DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1)));

        autoRescheduleService.rescheduleClientReturns(date, deliveryServiceFactory.getDeliveryService());

        //then
        ClientReturn rescheduled = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(rescheduled.getArriveIntervalFrom()).isEqualTo(
                LocalDateTime.of(date, LocalTime.of(9, 0))
        );
        Optional<ClientReturnHistoryEvent> rescheduleEvent =
                clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(), Pageable.unpaged())
                        .stream()
                        .filter(he -> he.getType() == CLIENT_RETURN_RESCHEDULED)
                        .findFirst();
        assertThat(rescheduleEvent).isEmpty();
    }

    @DisplayName("Не переносим заявку, которая была назначена на курьера")
    @Test
    void shouldNotRescheduleAssignedClientReturnWithOpenShift() {
        //given
        LocalDate date = LocalDate.now(clock);
        ClientReturn clientReturn = clientReturnGenerator.generateReturnFromClient(
                LocalDateTime.of(date, LocalTime.of(9, 0)),
                LocalDateTime.of(date, LocalTime.of(12, 0)),
                FAKE_DS_ID
        );
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        ClockUtil.initFixed(clock, LocalDateTime.of(date, DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1)));

        autoRescheduleService.rescheduleClientReturnsWithOpenShift(date, deliveryServiceFactory.getDeliveryService());

        //then
        ClientReturn rescheduled = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(rescheduled.getArriveIntervalFrom()).isEqualTo(
                LocalDateTime.of(date, LocalTime.of(9, 0))
        );
        Optional<ClientReturnHistoryEvent> rescheduleEvent =
                clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(), Pageable.unpaged())
                        .stream()
                        .filter(he -> he.getType() == CLIENT_RETURN_RESCHEDULED)
                        .findFirst();
        assertThat(rescheduleEvent).isEmpty();
    }

    @DisplayName("переносим заявку, у которой еще была создана смена, но не было создано задание")
    @Test
    void shouldRescheduleUnassignedClientReturnWithOpenShift() {
        //given
        LocalDate date = LocalDate.now(clock);
        ClientReturn clientReturn = clientReturnGenerator.generateReturnFromClient(
                LocalDateTime.of(date, LocalTime.of(9, 0)),
                LocalDateTime.of(date, LocalTime.of(12, 0)),
                FAKE_DS_ID
        );
        ClockUtil.initFixed(clock, LocalDateTime.of(date, DEFAULT_TIME_TO_RESCHEDULE_AFTER.minusHours(1)));

        Shift s = shiftRepository.findByIdOrThrow(shift.getId());
        assertThat(s.getStatus()).isEqualTo(ShiftStatus.OPEN);
        assertThat(s.getShiftDate()).isEqualTo(date);

        autoRescheduleService.rescheduleClientReturnsWithOpenShift(date, deliveryServiceFactory.getDeliveryService());

        //then
        ClientReturn rescheduled = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(rescheduled.getArriveIntervalFrom()).isEqualTo(
                LocalDateTime.of(date.plusDays(1), LocalTime.of(9, 0))
        );
        Optional<ClientReturnHistoryEvent> rescheduleEvent =
                clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(), Pageable.unpaged())
                        .stream()
                        .filter(he -> he.getType() == CLIENT_RETURN_RESCHEDULED)
                        .findFirst();
        assertThat(rescheduleEvent).isPresent();
        assertThat(rescheduleEvent.get().getSource()).isEqualTo(Source.SYSTEM);
    }

    @DisplayName("Не переносим заявку, которая не была назначена на курьера, при этом смена на день доставки еще не создана")
    @Test
    void shouldNotRescheduleUnAssignedClientReturnWithoutOpenShift() {
        //given
        LocalDate date = LocalDate.now(clock);
        ClientReturn clientReturn = clientReturnGenerator.generateReturnFromClient(
                LocalDateTime.of(date, LocalTime.of(9, 0)),
                LocalDateTime.of(date, LocalTime.of(12, 0)),
                FAKE_DS_ID
        );

        clientReturn.setDeliveryServiceId(98765L);

        ClockUtil.initFixed(clock, LocalDateTime.of(date, DEFAULT_TIME_TO_RESCHEDULE_AFTER.plusHours(1)));

        autoRescheduleService.rescheduleClientReturnsWithOpenShift(date, deliveryServiceFactory.getDeliveryService());

        //then
        ClientReturn rescheduled = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(rescheduled.getArriveIntervalFrom()).isEqualTo(
                LocalDateTime.of(date, LocalTime.of(9, 0))
        );
        Optional<ClientReturnHistoryEvent> rescheduleEvent =
                clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(), Pageable.unpaged())
                        .stream()
                        .filter(he -> he.getType() == CLIENT_RETURN_RESCHEDULED)
                        .findFirst();
        assertThat(rescheduleEvent).isEmpty();
    }

    private void assertOrderDate(LocalDate localDate) {
        Map<Long, Order> ordersMap = orderRepository.findMapForUserShift(userShift);
        assertThat(ordersMap.values().stream().map(o -> o.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID)))
                .containsOnly(localDate);
    }

    @DisplayName("Перенос дат доставки на завтра => отменяем задание")
    @Test
    void shouldRescheduleNextDayWhenUpdateFromDsApi() {
        testUserHelper.checkinAndFinishPickup(userShift);
        OrderDeliveryTask task = getTask();

        Order order = orderRepository.findByIdOrThrow(task.getOrderId());
        Interval interval = new Interval(tomorrowAtHour(18, clock), tomorrowAtHour(20, clock));
        orderManager.rescheduleOrder(order, interval, Source.DELIVERY);

        Page<OrderHistoryEvent> orderHistoryEvents = orderHistoryEventRepository.findByOrderId(task.getOrderId(),
                Pageable.unpaged());

        assertThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);
        assertThat(order.getDelivery().getInterval()).isEqualTo(interval);
        assertThat(orderHistoryEvents.getTotalElements()).isEqualTo(6);
    }

    @DisplayName("Перенос дат доставки в рамках дня => обновляем задание и заказ")
    @Test
    void shouldRescheduleSameDayWhenUpdateFromDsApi() {
        testUserHelper.checkinAndFinishPickup(userShift);
        OrderDeliveryTask task = getTask();

        Order order = orderRepository.findByIdOrThrow(task.getOrderId());
        Interval interval = new Interval(todayAtHour(18, clock), todayAtHour(20, clock));
        orderManager.rescheduleOrder(order, interval, Source.DELIVERY);

        assertThat(task.getStatus().isTerminal()).isFalse();
        assertThat(order.getDelivery().getInterval()).isEqualTo(interval);
    }

    @DisplayName("Перенос дат доставки для завершённого задания => обновляем заказ")
    @Test
    void shouldRescheduleForTerminatedTaskWhenUpdateFromDsApi() {
        testUserHelper.checkinAndFinishPickup(userShift);
        OrderDeliveryTask task = getTask();
        Instant expectedDeliveryTime = task.getExpectedDeliveryTime();
        failTask(task);

        Order order = orderRepository.findByIdOrThrow(task.getOrderId());
        Interval interval = new Interval(todayAtHour(18, clock), todayAtHour(20, clock));
        orderManager.rescheduleOrder(order, interval, Source.DELIVERY);

        assertThat(task.getStatus().isTerminal());
        assertThat(task.getExpectedDeliveryTime()).isEqualTo(expectedDeliveryTime);
        assertThat(order.getDelivery().getInterval()).isEqualTo(interval);
    }

    @Test
    void shouldUpdateOrderAfterReopenRescheduledTask() {
        testUserHelper.checkinAndFinishPickup(userShift);
        OrderDeliveryTask task = getTask();
        OrderDeliveryTaskStatus initialTaskStatus = task.getStatus();

        testUserHelper.rescheduleNextDay(task.getRoutePoint());
        assertThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);
        Order order = orderRepository.findByIdOrThrow(task.getOrderId());
        assertThat(DateTimeUtil.isNextDay(order.getDelivery().getDeliveryIntervalFrom(),
                DateTimeUtil.DEFAULT_ZONE_ID, clock)).isTrue();
        checkActionReopen(task, true);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), task.getRoutePoint().getId(), task.getId(), Source.COURIER
        ));
        assertThat(task.getStatus()).isEqualTo(initialTaskStatus);
        assertThat(task.getFailReason()).isNull();
        assertThat(task.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.UNFINISHED);
        order = orderRepository.findByIdOrThrow(task.getOrderId());
        assertThat(DateTimeUtil.isToday(order.getDelivery().getDeliveryIntervalFrom(), clock)).isTrue();
    }

    @Test
    void shouldReopenByOperator() {
        testUserHelper.checkinAndFinishPickup(userShift);
        OrderDeliveryTask task = getTask();
        OrderDeliveryTaskStatus initialTaskStatus = task.getStatus();
        Order order = orderRepository.findByIdOrThrow(task.getOrderId());

        testUserHelper.rescheduleNextDay(task.getRoutePoint(), Source.OPERATOR,
                OrderDeliveryRescheduleReasonType.DELIVERY_DELAY
        );
        assertThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);

        checkActionReopen(task, false);
        assertThatThrownBy(() -> commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), task.getRoutePoint().getId(), task.getId(), Source.COURIER
        ))).isInstanceOf(CommandFailedException.class)
                .hasRootCauseInstanceOf(TplInvalidStateException.class);

        orderManager.reopenTask(order.getExternalOrderId());
        assertThat(task.getStatus()).isEqualTo(initialTaskStatus);
        assertThat(task.getFailReason()).isNull();
        assertThat(task.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.UNFINISHED);
    }

    private void checkActionReopen(OrderDeliveryTask task, boolean contains) {
        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, task.getRoutePoint().getId());
        assertThat(StreamEx.of(routePointInfo.getTasks())
                .filter(t -> t.getId() == task.getId())
                .select(OrderDeliveryTaskDto.class)
                .findFirst().orElseThrow()
                .getActions().contains(new OrderDeliveryTaskDto.Action(OrderDeliveryTaskDto.ActionType.REOPEN))
        ).isEqualTo(contains);
    }

    private void failTask(OrderDeliveryTask task) {
        commandService.failDeliveryTask(null,
                new UserShiftCommand.FailOrderDeliveryTask(
                        task.getRoutePoint().getUserShift().getId(),
                        task.getRoutePoint().getId(),
                        task.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_REASSIGNED, null,
                                null, Source.DELIVERY)
                ));
    }

    private OrderDeliveryTask getTask() {
        return userShift.streamOrderDeliveryTasks()
                .filter(t -> !t.getStatus().isTerminal())
                .findFirst().orElseThrow();
    }

}
