package ru.yandex.market.tpl.core.domain.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.exception.CommandFailedException;
import ru.yandex.market.tpl.core.exception.TplInvalidStateException;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class UserShiftReturnOrdersTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;

    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftQueryService userShiftQueryService;
    private final UserShiftRepository repository;
    private final EntityManager entityManager;
    private final UserShiftManager userShiftManager;
    private final UserPropertyService userPropertyService;

    private User user;
    private UserShift userShift;
    private RoutePoint firstDeliveryRoutePoint;
    private OrderDeliveryTask firstDeliveryTask;
    private Map<Long, Order> ordersById;

    @MockBean
    private Clock clock;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);
        user = userHelper.findOrCreateUser(35236L);

        initTasks();
    }

    void initTasks() {
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        List<Order> orders = Stream.generate(orderGenerateService::createOrder).limit(3)
            .collect(Collectors.toList());
        ordersById = orders.stream().collect(Collectors.toMap(Order::getId, o -> o));

        var createCommand = UserShiftCommand.Create.builder()
            .userId(user.getId())
            .shiftId(shift.getId())
            .routePoint(helper.taskOrderPickup(clock.instant()))
            .routePoint(helper.taskPrepaid("addr1", 12, orders.get(0).getId()))
            .routePoint(helper.taskPrepaid("addr2", 13, orders.get(1).getId()))
            .routePoint(helper.taskPrepaid("addr3", 12, orders.get(2).getId()))
            .mergeStrategy(SimpleStrategies.NO_MERGE)
            .build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        firstDeliveryRoutePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();

        userHelper.checkin(userShift);
        firstDeliveryTask = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();
    }

    @Test
    void noReturnRoutePointIfNoCancelledOrders() {
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
    }

    @Test
    void returnCreatedAfterPickupIfAccepted() {
        cancelTaskBeforePickup();

        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(userShift.streamOrderDeliveryTasks().filter(OrderDeliveryTask::canReturnOrders).count())
            .isEqualTo(1);
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
        userShift.streamRoutePoints().findFirst().orElseThrow().getStatus().equals(RoutePointStatus.FINISHED);
    }

    @Test
    @DisplayName("Таска на возврат заказов для курьеров DBS не создается")
    void noReturnTaskIfPropertyDisabled() {
        user = userHelper.findOrCreateUser(123L);
        userPropertyService.addPropertyToUser(user, UserProperties.RETURN_TASK_CREATING_ENABLED, false);
        initTasks();

        assertTrue(userShift.streamRoutePoints().noneMatch(point -> RoutePointType.ORDER_RETURN == point.getType()));

        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(userShift.streamOrderDeliveryTasks().filter(OrderDeliveryTask::canReturnOrders).count())
            .isEqualTo(0);
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(0);
    }

    @Test
    void noReturnCreatedAfterPickupIfSkipped() {
        cancelTaskBeforePickup();

        List<Long> skippedOrderIds = List.of(firstDeliveryTask.getOrderId());
        List<Long> acceptedOrderIds = userShift.streamOrderDeliveryTasks()
            .mapToLong(OrderDeliveryTask::getOrderId)
            .filter(o -> o != firstDeliveryTask.getOrderId())
            .boxed()
            .collect(Collectors.toList());
        userHelper.finishPickupAtStartOfTheDay(userShift, acceptedOrderIds, skippedOrderIds);
        assertThat(userShift.streamOrderDeliveryTasks().filter(OrderDeliveryTask::canReturnOrders).count())
            .isEqualTo(0);
        assertThat(userShift.streamReturnRoutePoints().count())
            .describedAs("Создаём задание на возврат при создании смены")
            .isEqualTo(1);

        userShift.streamDeliveryRoutePoints()
            .filter(rp -> !rp.getId().equals(firstDeliveryRoutePoint.getId()))
            .forEach(rp -> userHelper.finishDelivery(rp, true));
        assertThat(userShift.streamReturnRoutePoints().count())
            .describedAs("После отмены принятого заказа уже существующие задание на возврат не удаляем")
            .isEqualTo(1);

        var returnRoutePoint = userShiftQueryService.getRoutePointInfo(
            user,
            userShift.streamReturnRoutePoints().findFirst().orElseThrow().getId()
        );
        OrderScanTaskDto<?> scanTaskDto = StreamEx.of(returnRoutePoint.getTasks())
            .select(OrderScanTaskDto.class)
            .findFirst().orElseThrow();
        assertThat(scanTaskDto.getOrders())
            .extracting(OrderScanTaskDto.OrderForScanDto::getExternalOrderId)
            .describedAs("В задании на возврат показываем только то, что принимали")
            .containsExactlyInAnyOrderElementsOf(ordersById.values().stream()
                .filter(o -> !Objects.equals(o.getId(), firstDeliveryTask.getOrderId()))
                .map(Order::getExternalOrderId)
                .collect(Collectors.toList())
            );
    }

    private void cancelTaskBeforePickup() {
        assertThat(userShift.streamOrderDeliveryTasks().filter(OrderDeliveryTask::canReturnOrders).count())
            .isEqualTo(0);
        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
            userShift.getId(), firstDeliveryRoutePoint.getId(),
            firstDeliveryRoutePoint.streamDeliveryTasks().findFirst().orElseThrow().getId(),
            new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.CLIENT_REFUSED,
                "my client refused!", null, Source.CLIENT
            )
        ));
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
    }

    @Test
    void returnCreatedIfTaskRescheduledToNextDay() {
        commandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
            userShift.getId(), firstDeliveryRoutePoint.getId(), firstDeliveryTask.getId(),
            new DeliveryReschedule(
                LocalTimeInterval.valueOf("18:00-22:00").toInterval(
                    LocalDate.now(clock).plusDays(1),
                    DateTimeUtil.DEFAULT_ZONE_ID
                ),
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST,
                "клиент попросил",
                Source.CLIENT,
                null
            ),
            clock.instant(),
            userShift.getZoneId()
        ));
        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
    }

    @Test
    void returnNotCreatedIfTaskRescheduledToCurrentDay() {
        commandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
            userShift.getId(), firstDeliveryRoutePoint.getId(), firstDeliveryTask.getId(),
            new DeliveryReschedule(
                LocalTimeInterval.valueOf("18:00-22:00").toInterval(
                    LocalDate.now(clock),
                    DateTimeUtil.DEFAULT_ZONE_ID
                ),
                OrderDeliveryRescheduleReasonType.DELIVERY_DELAY,
                "задержались немного",
                Source.DELIVERY,
                null
            ),
            clock.instant(),
            userShift.getZoneId()
        ));
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
    }

    @Test
    void startReturn() {
        ReturnTaskInfo info = arriveAtReturnInfo();

        assertThat(info.getTask().getStatus()).isEqualTo(OrderReturnTaskStatus.NOT_STARTED);
        commandService.startOrderReturn(user, info.getReturnStartCommand());
        assertThat(info.getTask().getStatus()).isEqualTo(OrderReturnTaskStatus.IN_PROGRESS);
    }

    @Test
    void noReturnIfNoPickup() {
        userHelper.finishPickupAtStartOfTheDay(userShift, false);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
    }

    @Test
    void arriveToReturnBeforeDelivery() {
        // finish only one delivery route point
        userHelper.finishPickupAtStartOfTheDay(userShift);
        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), true);

        var returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();

        assertThatThrownBy(() -> commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
            userShift.getId(), returnRoutePoint.getId(),
            new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId())
        ))).isInstanceOf(CommandFailedException.class).hasCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    void partiallyFinished() {
        var expectedReturnOrders = List.of(1L);
        var expectedSkippedOrders = List.of(2L);
        var expectedComment = "не виноватый я";
        returnOrders(expectedReturnOrders, expectedSkippedOrders, expectedComment,
            OrderReturnTaskStatus.PARTIALLY_FINISHED
        );
    }

    @Test
    void finished() {
        var expectedReturnOrders = List.of(1L);
        List<Long> expectedSkippedOrders = List.of();
        returnOrders(expectedReturnOrders, expectedSkippedOrders, null, OrderReturnTaskStatus.FINISHED);

        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        List<Order> orders = orderRepository.findAllById(userShift.streamDeliveryRoutePoints()
            .flatMap(rp -> rp.streamTasks(OrderDeliveryTask.class))
            .filter(t -> t.getStatus() == OrderDeliveryTaskStatus.DELIVERED)
            .map(DeliveryTask::getOrderIds)
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
        assertThat(orders).extracting(Order::getOrderFlowStatus)
            .containsExactly(OrderFlowStatus.DELIVERED_TO_RECIPIENT);
    }

    void returnOrders(
        List<Long> expectedReturnOrders, List<Long> expectedSkippedOrders,
        @Nullable String expectedComment, OrderReturnTaskStatus expectedStatus
    ) {
        ReturnTaskInfo info = arriveAtReturnInfo();

        var returnOrdersCommand = new UserShiftCommand.FinishScan(
            userShift.getId(),
            info.getRoutePoint().getId(),
            info.getTask().getId(),
            ScanRequest.builder()
                .successfullyScannedOrders(expectedReturnOrders)
                .skippedOrders(expectedSkippedOrders)
                .comment(expectedComment)
                .finishedAt(Instant.now(clock))
                .build()
        );
        var returnRoutPoint = userShift.getCurrentRoutePoint();

        commandService.startOrderReturn(user, info.getReturnStartCommand());
        commandService.finishReturnOrders(user, returnOrdersCommand);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(
            userShift.getId(),
            returnRoutPoint.getId(),
            returnRoutPoint.getTasks().stream().findFirst().orElseThrow().getId()
        ));
        assertThat(info.getTask().getReturnOrderIds()).isEqualTo(expectedReturnOrders);
        assertThat(info.getTask().getSkippedOrderIds()).isEqualTo(expectedSkippedOrders);
        assertThat(info.getTask().getComment()).isEqualTo(expectedComment);
        assertThat(info.getTask().getStatus()).isEqualTo(expectedStatus);

        entityManager.flush();
    }

    @Test
    public void shouldReturnAllNotDeliveredOrders() {
        userHelper.finishPickupAtStartOfTheDay(userShift, true);
        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), true);
        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), false);
        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), true);
        userShiftManager.returnOrders(userShift.getId());

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
    }

    @Test
    public void shouldntReturnAllWhenNotAllDelivered() {
        userHelper.finishPickupAtStartOfTheDay(userShift, true);
        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), true);

        TplInvalidActionException tplInvalidActionException = assertThrows(
            TplInvalidActionException.class,
            () -> userShiftManager.returnOrders(userShift.getId())
        );

        assertThat(tplInvalidActionException.getMessage()).isEqualTo("Not all delivery tasks completed!");
    }

    @Test
    public void shouldntRemoveReturnAfterReopen() {
        userHelper.finishPickupAtStartOfTheDay(userShift, true);
        RoutePoint rp = userShift.getCurrentRoutePoint();

        userHelper.finishDelivery(Objects.requireNonNull(rp), true);
        assertThat(userShift.streamReturnRoutePoints().count())
            .isEqualTo(1);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
            userShift.getId(), rp.getId(), rp.streamDeliveryTasks().findFirst().orElseThrow().getId(),
            Source.COURIER
        ));
        assertThat(userShift.streamReturnRoutePoints().count())
            .isEqualTo(1);
    }

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    private ReturnTaskInfo arriveAtReturnInfo() {
        userHelper.finishPickupAtStartOfTheDay(userShift, true);
        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), true);
        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), false);
        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), true);

        var info = new ReturnTaskInfo(userShift);
        commandService.arriveAtRoutePoint(
            user,
            new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(),
                info.getRoutePoint().getId(),
                helper.getLocationDto(userShift.getId())
            )
        );
        return info;
    }

    @Value
    private static class ReturnTaskInfo {

        private RoutePoint routePoint;
        private OrderReturnTask task;
        private UserShiftCommand.StartScan returnStartCommand;

        ReturnTaskInfo(UserShift userShift) {
            routePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
            task = (OrderReturnTask) routePoint.getTasks().get(0);
            returnStartCommand = new UserShiftCommand.StartScan(
                userShift.getId(),
                routePoint.getId(),
                task.getId()
            );
        }

    }

}
