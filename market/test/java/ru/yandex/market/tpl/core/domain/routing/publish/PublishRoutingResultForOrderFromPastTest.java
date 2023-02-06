package ru.yandex.market.tpl.core.domain.routing.publish;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistory;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistoryRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.routing.RoutingRequestCreator;
import ru.yandex.market.tpl.core.domain.routing.events.ShiftRoutingResultReceivedEvent;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.CreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestTplRoutingFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE;

@RequiredArgsConstructor
public class PublishRoutingResultForOrderFromPastTest extends TplAbstractTest {

    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final OrderFlowStatusHistoryRepository orderFlowStatusHistoryRepository;
    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();

    private final UserScheduleRuleRepository scheduleRuleRepository;
    private final UserShiftRepository userShiftRepository;

    private final RoutingRequestCreator routingRequestCreator;
    private final ShiftManager shiftManager;
    private final ScManager scManager;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final SortingCenterService sortingCenterService;
    private final TestTplRoutingFactory testTplRoutingFactory;

    private final Clock clock;

    private Shift shift;
    private Order order1;
    private Order order2;
    private RoutingRequest routingRequest;

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
    }

    @BeforeEach
    void init() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterService.findSortCenterForDs(239L),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED, false
        );
        ClockUtil.initFixed(clock, LocalDateTime.of(2021, 3, 25, 0, 0, 0));

        long sortingCenterId = 47819L;

        LocalDate shiftDate = LocalDate.now(clock);

        shift = shiftManager.findOrCreate(shiftDate, sortingCenterId);

        userHelper.findOrCreateUserForSc(824126L, LocalDate.now(clock), sortingCenterId);

        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shiftDate)
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.CREATED)
                .build());

        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shiftDate.minusDays(3))
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.CREATED)
                .build());

        Order order3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(shiftDate.plusDays(4))
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.CREATED)
                .build());

        routingRequest = transactionTemplate.execute(tt -> {
            List<UserScheduleRule> users = scheduleRuleRepository.findAllWorkingRulesForDate(
                    shift.getShiftDate(),
                    shift.getSortingCenter().getId());
            assertThat(users).hasSize(1);

            Map<Long, RoutingCourier> couriersById =
                    createShiftRoutingRequestCommandFactory.mapCouriersFromUserSchedules(
                            users,
                            false,
                            Map.of(),
                            Map.of()
                    );


            CreateShiftRoutingRequestCommand command = CreateShiftRoutingRequestCommand.builder()
                    .data(CreateShiftRoutingRequestCommandData.builder()
                            .routeDate(shift.getShiftDate())
                            .sortingCenter(shift.getSortingCenter())
                            .couriers(new HashSet<>(couriersById.values()))
                            .orders(List.of(order1, order2, order3))
                            .movements(List.of())
                            .build()
                    )
                    .createdAt(clock.instant())
                    .mockType(RoutingMockType.REAL)
                    .build();
            return routingRequestCreator.createShiftRoutingRequest(command);
        });


        jdbcTemplate.update("UPDATE orders SET created_at = created_at - (? ||' minutes')::interval WHERE id = ?",
                scManager.getDelayToCreateOrderInMinutes() + 1, order2.getId());
        scManager.createOrders();

        scManager.updateWhenCreatedOrder(order2.getExternalOrderId(), "SC_ORDER_ID", sortingCenterId);
    }

    @Test
    void shouldCreateTasks() {
        RoutingResult routingResult = routingApiDataHelper.mockResult(routingRequest, false);
        testTplRoutingFactory.mockRoutingLogRecord(routingRequest, routingResult);
        shiftManager.processGroupRoutingResult(new ShiftRoutingResultReceivedEvent(shift, routingResult));

        List<UserShift> userShifts = userShiftRepository.findAllByShiftId(shift.getId());

        assertThat(userShifts).hasSize(1);

        transactionTemplate.execute(tt -> {
            UserShift us = userShiftRepository.findByIdOrThrow(userShifts.iterator().next().getId());
            assertThat(us.streamOrderDeliveryTasks().count()).isEqualTo(2);

            Set<Long> orderIds = us.streamOrderDeliveryTasks()
                    .map(OrderDeliveryTask::getOrderId)
                    .toSet();

            assertThat(orderIds).containsExactlyInAnyOrder(order1.getId(), order2.getId());
            return null;
        });
    }

    @Test
    void shouldChangeDeliveryDateAfterAcceptOnSc() {
        shouldCreateTasks();

        scManager.updateOrderStatuses(
                order2.getExternalOrderId(),
                shift.getSortingCenter().getId(),
                List.of(
                        new OrderStatusUpdate(ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(), clock.instant()))
        );

        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER);

        Order updated = orderRepository.findByIdOrThrow(order2.getId());

        assertThat(updated.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_ARRIVED);

        transactionTemplate.execute(tt -> {
            List<UserShift> userShifts = userShiftRepository.findAllByShiftId(shift.getId());
            UserShift userShift = userShifts.iterator().next();

            OrderDeliveryTask orderDeliveryTask =
                    userShift.streamOrderDeliveryTasks()
                            .filter(t -> Objects.equals(t.getOrderId(), order2.getId())).findFirst().orElseThrow();
            assertThat(orderDeliveryTask.getStatus().isFailed()).isFalse();
            return null;
        });

        assertThat(updated.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID)).isEqualTo(shift.getShiftDate());

        assertThat(getStatusCodes(order2)).containsExactlyInAnyOrder(1, 10, 44, 10);
    }

    private List<Integer> getStatusCodes(Order order) {
        List<OrderFlowStatusHistory> statuses =
                orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId());
        return StreamEx.of(statuses)
                .map(OrderFlowStatusHistory::getOrderFlowStatusAfter)
                .map(OrderFlowStatus::getCode)
                .collapse(Integer::equals, (c1, c2) -> c1)
                .collect(Collectors.toList());
    }

}
