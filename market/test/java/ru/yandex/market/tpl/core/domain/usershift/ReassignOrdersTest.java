package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.user.partner.PartnerReportCourierToReassignOptionDto;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.util.CacheTestUtil;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.CLIENT_REFUSED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERY_FAILED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class ReassignOrdersTest {

    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;

    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final OrderManager orderManager;
    private final UserShiftReassignManager userShiftReassignManager;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    @Qualifier("fiveMinutesCacheManager")
    private final CacheManager fiveMinutesCacheManager;

    private UserShift userShiftA;
    private UserShift userShiftB;
    private User userA;
    private User userB;
    private Shift shift;

    @MockBean
    private Clock clock;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);

        userA = testUserHelper.findOrCreateUser(35236L);
        userB = testUserHelper.findOrCreateUser(95123L);

        shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        List<Order> orders = Stream.generate(() -> orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .build()
        )).limit(3).collect(Collectors.toList());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(userA.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskPrepaid("addr1", 12, orders.get(0).getId()))
                .routePoint(helper.taskPrepaid("addr2", 13, orders.get(1).getId()))
                .routePoint(helper.taskPrepaid("addr3", 12, orders.get(2).getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShiftA = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        testUserHelper.checkinAndFinishPickup(userShiftA);

        var createUserShiftBCommand = UserShiftCommand.Create.builder()
                .userId(userB.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .active(true)
                .build();

        userShiftB =
                userShiftRepository.findById(commandService.createUserShift(createUserShiftBCommand)).orElseThrow();
        testUserHelper.checkinAndFinishPickup(userShiftB);

        configurationServiceAdapter.insertValue(
                ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED, true);
    }

    @Test
    void shouldUnassign() {
        OrderDeliveryTask task = userShiftA.streamOrderDeliveryTasks()
                .filter(t -> !t.getStatus().isTerminal())
                .findFirst().orElseThrow();

        Order order = orderRepository.findByIdOrThrow(task.getOrderId());
        orderManager.unassignOrders(List.of(order), null, null);

        Optional<OrderDeliveryTask> cancelledTaskO = userShiftA.streamOrderDeliveryTasks()
                .filter(dt -> Objects.equals(dt.getOrderId(), order.getId()))
                .findFirst();
        assertThat(cancelledTaskO).isPresent();
        OrderDeliveryTask orderDeliveryTask = cancelledTaskO.get();
        assertThat(orderDeliveryTask.getFailReason().getType()).isEqualTo(OrderDeliveryTaskFailReasonType.COURIER_REASSIGNED);
        assertThat(orderDeliveryTask.getStatus()).isEqualTo(DELIVERY_FAILED);
        assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        // После отмены с причиной COURIER_REASSIGN заказ не должен появляться в возврате
        assertThat(orderDeliveryTask.canReturnOrders()).isFalse();
    }

    @Test
    void shouldNotShowForReasssignFailedOrdersCancelledByCourier() {
        OrderDeliveryTask task = userShiftA.streamOrderDeliveryTasks()
                .filter(t -> !t.getStatus().isTerminal())
                .findFirst().orElseThrow();

        Order order = orderRepository.findByIdOrThrow(task.getOrderId());
        OrderDeliveryFailReason orderDeliveryFailReason = new OrderDeliveryFailReason(CLIENT_REFUSED, "");
        orderDeliveryFailReason.setSource(Source.CLIENT);
        commandService.failDeliveryTask(userShiftA.getUser(), new UserShiftCommand.FailOrderDeliveryTask(
                userShiftA.getId(),
                userShiftA.getCurrentRoutePoint().getId(),
                userShiftA.getCurrentRoutePoint().getTasks().get(0).getId(),
                orderDeliveryFailReason
        ));
        Optional<OrderDeliveryTask> cancelledTaskO = userShiftA.streamOrderDeliveryTasks()
                .filter(dt -> Objects.equals(dt.getOrderId(), order.getId()))
                .findFirst();
        assertThat(cancelledTaskO).isPresent();
        OrderDeliveryTask orderDeliveryTask = cancelledTaskO.get();
        assertThat(orderDeliveryTask.getFailReason().getType()).isEqualTo(CLIENT_REFUSED);
        assertThat(orderDeliveryTask.getStatus()).isEqualTo(DELIVERY_FAILED);
        assertThat(userShiftReassignManager.canReassign(order.getOrderFlowStatus(),
                order.getDelivery().getDeliveryDate(clock.getZone()), clock,
                Optional.ofNullable(orderDeliveryTask.getFailReason())
                        .map(OrderDeliveryFailReason::getSource).orElse(null), false)).isFalse();
    }

    @Test
    void shouldAssignOrders() {
        int countOfOrders = 2;
        List<Order> orders = Stream.generate(orderGenerateService::createOrder).limit(countOfOrders)
                .collect(Collectors.toList());
        List<Long> orderIdsToAssign = orders.stream().map(o -> o.getId()).collect(Collectors.toList());

        userShiftReassignManager.assignOrders(
                userShiftB.getId(),
                orderIdsToAssign);

        assertThat(userShiftB.streamDeliveryTasks().count()).isEqualTo(countOfOrders);
    }

    @Test
    void shouldAssignUnassignedOrder() {
        // unassign order from userShiftA
        OrderDeliveryTask task = userShiftA.streamOrderDeliveryTasks()
                .filter(t -> !t.getStatus().isTerminal())
                .findFirst().orElseThrow();
        Order order = orderRepository.findByIdOrThrow(task.getOrderId());
        orderManager.unassignOrders(List.of(order), null, null);

        // assign order to userShiftB
        userShiftReassignManager.assignOrders(
                userShiftB.getId(),
                List.of(order.getId()));

        assertThat(userShiftB.streamDeliveryTasks().count()).isEqualTo(1);
    }

    @Test
    void shouldReassignOrder() {
        Set<Long> orderIds = userShiftA.streamOrderDeliveryTasks()
                .map(OrderDeliveryTask::getOrderId)
                .collect(Collectors.toSet());

        userShiftReassignManager.reassignOrders(orderIds, Set.of(), Set.of(), userShiftB.getUser().getId());
        assertThat(userShiftB.streamDeliveryTasks().count()).isEqualTo(orderIds.size());
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCInDifferentTimeZone.sql")
    void shouldReassignOrderInDifferentTimeZone() {
        var shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), 100501);

        var userC = testUserHelper.findOrCreateUser(354534L);
        var userD = testUserHelper.findOrCreateUser(3234235L);

        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam
                .builder()
                .deliveryDate(LocalDate.now(clock))
                .deliveryInterval(LocalTimeInterval.valueOf("19:00-23:59"))
                .zoneId(ZoneId.ofOffset("UTC", ZoneOffset.ofHours(4)))
                .deliveryServiceId(100500L)
                .build()
        );

        var createCommand = UserShiftCommand.Create.builder()
                .userId(userC.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskPrepaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        var userShiftC = userShiftRepository.findById(commandService.createUserShift(createCommand))
                .orElseThrow();
        testUserHelper.checkinAndFinishPickup(userShiftC);

        var createUserShiftDCommand = UserShiftCommand.Create.builder()
                .userId(userD.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .active(true)
                .build();

        var userShiftD =
                userShiftRepository.findById(commandService.createUserShift(createUserShiftDCommand)).orElseThrow();
        testUserHelper.checkinAndFinishPickup(userShiftD);

        Set<Long> orderIds = userShiftC.streamOrderDeliveryTasks()
                .map(OrderDeliveryTask::getOrderId)
                .collect(Collectors.toSet());

        userShiftReassignManager.reassignOrders(orderIds, Set.of(), Set.of(), userShiftD.getUser().getId());
        assertThat(userShiftD.streamDeliveryTasks()
                .remove(DeliveryTask::isInTerminalStatus)
                .count()
        ).isEqualTo(orderIds.size());
        assertThat(userShiftC.streamDeliveryTasks()
                .remove(DeliveryTask::isInTerminalStatus)
                .count()
        ).isEqualTo(0);
    }

    @Test
    void shouldReassignOrderNewUserShift() {
        Set<Long> orderIds = userShiftA.streamOrderDeliveryTasks()
                .map(OrderDeliveryTask::getOrderId)
                .collect(Collectors.toSet());

        User userC = testUserHelper.findOrCreateUser(95126L);

        userShiftReassignManager.reassignOrders(orderIds, Set.of(), Set.of(), userC.getId());

        UserShift userShiftC = userShiftRepository.findCurrentShift(userC).orElseThrow();
        assertThat(userShiftC.streamOrderDeliveryTasks().map(OrderDeliveryTask::getOrderId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(orderIds.toArray(Long[]::new));
        assertThat(userShiftC.isActive()).isTrue();
    }

    @Test
    void shouldAddReassignFlagToUserShift() {
        Set<Long> orderIds = userShiftA.streamOrderDeliveryTasks()
                .map(OrderDeliveryTask::getOrderId)
                .collect(Collectors.toSet());

        User userC = testUserHelper.findOrCreateUser(95126L);

        userShiftReassignManager.reassignOrdersV2(orderIds, Set.of(), Set.of(), userC.getId(), "ABSENTEEISM");

        UserShift userShiftC = userShiftRepository.findCurrentShift(userC).orElseThrow();
        assertThat(userShiftC.streamOrderDeliveryTasks().map(OrderDeliveryTask::getOrderId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(orderIds.toArray(Long[]::new));
        assertThat(userShiftC.isActive()).isTrue();
        assertThat(userShiftC.getHaveReassignments()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldFindCourierToReassign(boolean withFlagEnabled) {
        if (withFlagEnabled) {
            configurationServiceAdapter.insertValue(ConfigurationProperties.GET_COURIER_TO_REASSIGN_FILTER_ENABLED,
                    true);
        }
        User userC = testUserHelper.findOrCreateUser(95124L);
        commandService.createUserShift(UserShiftCommand.Create.builder()
                .userId(userC.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build());
        LocalDate today = LocalDate.now(clock);
        User userD = testUserHelper.findOrCreateUser(95125L, today);

        UserShift userShiftD =
                userShiftRepository.findByIdOrThrow(commandService.createUserShift(UserShiftCommand.Create.builder()
                        .userId(userD.getId())
                        .shiftId(shift.getId())
                        .routePoint(helper.taskOrderPickup(clock.instant()))
                        .mergeStrategy(SimpleStrategies.NO_MERGE)
                        .active(true)
                        .build()));

        testUserHelper.checkinAndFinishPickup(userShiftD);

        List<PartnerReportCourierToReassignOptionDto> courierToReassign =
                userShiftReassignManager.findCourierToReassign(today, today, Set.of(1L), 0L).get(today);
        if (withFlagEnabled) {
            assertThat(courierToReassign).extracting(PartnerReportCourierToReassignOptionDto::getUserId)
                    .containsExactlyInAnyOrder(userA.getId(), userB.getId(), userD.getId());
        } else {
            assertThat(courierToReassign).extracting(PartnerReportCourierToReassignOptionDto::getUserId)
                    .containsExactlyInAnyOrder(userA.getId(), userB.getId(), userC.getId(), userD.getId());
        }
        CacheTestUtil.clear(fiveMinutesCacheManager);

    }

    @Test
    void notReassignOrderForCourierInAnotherSC() {
        LocalDate today = LocalDate.now(clock);
        long sortingCenterId = 498;

        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(today, sortingCenterId);
        var courier = testUserHelper.findOrCreateUserForSc(sortingCenterId + 123L, today, sortingCenterId);
        commandService.createUserShift(UserShiftCommand.Create.builder()
                .shiftId(shift.getId())
                .userId(courier.getId())
                .active(true)
                .build());

        // заказ для СЦ по-умолчанию
        Order order = orderGenerateService.createOrder();

        assertThatThrownBy(
                () -> userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(),
                        courier.getId())
        )
                .isInstanceOf(TplInvalidParameterException.class)
                .hasMessageContaining("Существует активная смена курьера в другом СЦ");
    }

    @Test
    void notReassignOrderForCourierInAnotherShiftDate() {
        LocalDate today = LocalDate.now(clock);

        User userC = testUserHelper.findOrCreateUser(95124L);
        commandService.createUserShift(UserShiftCommand.Create.builder()
                .userId(userC.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .active(true)
                .build());

        // заказ для СЦ по-умолчанию
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(today.plus(1, ChronoUnit.DAYS))
                .build());

        assertThatThrownBy(
                () -> userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), userC.getId())
        )
                .isInstanceOf(TplInvalidParameterException.class)
                .hasMessageContaining("Существует активная смена курьера на другую дату");
    }

    @Test
    void reassignAfterPickupOrder() {
        OrderDeliveryTask task = userShiftA.streamOrderDeliveryTasks()
                .filter(t -> !t.getStatus().isTerminal())
                .findFirst().orElseThrow();

        Order order = orderRepository.findByIdOrThrow(task.getOrderId());
        Interval initialInterval = order.getDelivery().getInterval();
        orderManager.rescheduleOrder(
                order,
                new Interval(
                        initialInterval.getStart().plus(1, DAYS),
                        initialInterval.getEnd().plus(1, DAYS)),
                Source.OPERATOR);

        assertThat(task.getStatus().isTerminal()).isTrue();
        // заказ должен быть закреплен за первым курьером
        List<Order> currentUserAOrders = orderRepository.findCurrentUserOrders(userA.getId());
        assertThat(currentUserAOrders).contains(order);

        // возвращаем дату доставки назад, как в кейсе из
        // https://st.yandex-team.ru/MARKETTPL-1876#5fc02b7ab575146042a46e52
        orderManager.rescheduleOrder(
                order,
                initialInterval,
                Source.OPERATOR);

        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), userB.getId());

        // заказ НЕ должен быть закреплен за первым курьером
        currentUserAOrders = orderRepository.findCurrentUserOrders(userA.getId());
        assertThat(currentUserAOrders).doesNotContain(order);

        List<Order> currentUserBOrders = orderRepository.findCurrentUserOrders(userB.getId());
        assertThat(currentUserBOrders).contains(order);
    }

    @Test
    void shouldNotReassignToClosedUserShift() {
        commandService.closeShift(new UserShiftCommand.Close(userShiftB.getId()));
        var userAOrderIds = orderRepository.findCurrentUserOrders(userA.getId()).stream()
                .map(Order::getId)
                .collect(Collectors.toSet());
        assertThatThrownBy(() -> userShiftReassignManager.reassignOrdersV2(userAOrderIds, Set.of(), Set.of(),
                userB.getId(),
                null))
                .isInstanceOf(TplInvalidParameterException.class)
                .hasMessage("Текущая смена курьера закрыта");
    }

}
