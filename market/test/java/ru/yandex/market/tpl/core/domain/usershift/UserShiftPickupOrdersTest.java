package ru.yandex.market.tpl.core.domain.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.base.property.PropertyDefinition;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.exception.CommandFailedException;
import ru.yandex.market.tpl.core.exception.TplInvalidStateException;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointType.ORDER_PICKUP;
import static ru.yandex.market.tpl.api.model.user.UserMode.SOFT_MODE;

/**
 * @author valter
 */
@RequiredArgsConstructor
class UserShiftPickupOrdersTest extends TplAbstractTest {

    private final Clock clock;

    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper helper;
    private final TransactionTemplate transactionTemplate;

    @SpyBean
    private OrderCommandService orderCommandService;
    private final UserShiftQueryService userShiftQueryService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final UserPropertyService userPropertyService;
    private final OrderRepository orderRepository;
    private final OrderManager orderManager;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    private SortingCenter sortingCenter;
    private User user;
    private Long userShiftId;
    private Order order;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(35236L);

        initTasks();

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter, SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED, true
        );
    }

    void initTasks() {
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        sortingCenter = shift.getSortingCenter();

        order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShiftId = commandService.createUserShift(createCommand);
        commandService.switchActiveUserShift(user, userShiftId);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));
    }

    @Test
    void orderStatusChangedAfterSuccessfulPickup() {
        userHelper.finishPickupAtStartOfTheDay(userShiftId, true);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
    }

    @Test
    @DisplayName("Таска на забор заказов для курьеров DBS не создается")
    void noReturnTaskIfPropertyDisabled() {
        user = userHelper.findOrCreateUser(123L);
        addPropertyToUser(user, UserProperties.PICKUP_TASK_CREATING_ENABLED, false);

        initTasks();
        transactionTemplate.execute(tt -> {
            UserShift userShift = repository.findByIdOrThrow(userShiftId);
            assertTrue(userShift.streamRoutePoints().noneMatch(point -> ORDER_PICKUP == point.getType()));

            return null;
        });
    }

    @Test
    void orderStatusChangedAfterSuccessfulPickupWithScanDuration() {
        Duration duration = Duration.ofSeconds(10000L);
        UserShift us = repository.findByIdOrThrow(userShiftId);
        userHelper.finishPickupAtStartOfTheDay(us, true, true, true, duration);
        transactionTemplate.execute(tt -> {
            UserShift userShift = repository.findByIdOrThrow(userShiftId);
            List<OrderPickupTask> tasks =
                    userShift.getRoutePoints().stream().filter(e -> ORDER_PICKUP.equals(e.getType()))
                    .flatMap(routePoint -> routePoint.getTasks().stream()).map(task -> ((OrderPickupTask) task))
                    .filter(orderPickupTask -> orderPickupTask.getScanStartedAt()
                            .equals(orderPickupTask.getFinishedAt().minus(duration))).collect(Collectors.toList());
            assertThat(tasks.size()).isEqualTo(1);
            return null;
        });
    }

    @Test
    void orderStatusNotChangedAfterSkippedPickup() {
        userHelper.finishPickupAtStartOfTheDay(userShiftId, false);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus()).isNotEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
    }

    @Test
    void orderStatusChangedAfterSkippedPickup() {
        addPropertyToUser(user, UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, true);
        userHelper.finishPickupAtStartOfTheDay(userShiftId, true, false);

        var info = transactionTemplate.execute(
                tt -> new PickupTaskInfo(repository.findByIdOrThrow(userShiftId))
        );

        commandService.waitForTransferActSignature(user, new UserShiftCommand.WaitForTransferActSignature(
                userShiftId,
                info.getRoutePoint().getId(),
                info.getTask().getId(),
                info.getTransferActId(),
                "1"
        ));
        commandService.signTransferAct(user, new UserShiftCommand.SignTransferAct(
                userShiftId,
                info.getRoutePoint().getId(),
                info.getTask().getId(),
                info.getTransferActId()
        ));

        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
    }

    @Test
    void orderStatusChangedAfterReopenByOperator() {
        userHelper.finishPickupAtStartOfTheDay(userShiftId, false);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);

        orderManager.reopenTask(order.getExternalOrderId());
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);

        transactionTemplate.execute(tt -> {
            UserShift userShift = repository.findByIdOrThrow(userShiftId);

            OrderDeliveryTask task = userShift.streamOrderDeliveryTasks()
                    .filter(t -> Objects.equals(t.getOrderId(), order.getId())).findFirst().orElseThrow();
            assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
            assertThat(task.getFailReason()).isNull();
            assertThat(task.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.UNFINISHED);

            return null;
        });
    }

    @Test
    void orderStatusNotChangedForCancelledOrder() {
        orderCommandService.forceUpdateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), OrderFlowStatus.SORTING_CENTER_CREATED));
        orderCommandService.cancelOrReturn(new OrderCommand.CancelOrReturn(
                order.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, null, null, Source.SYSTEM),
                null,
                false
        ));
        userHelper.finishPickupAtStartOfTheDay(userShiftId, true);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus()).isEqualTo(OrderFlowStatus.CANCELLED);
    }

    @Test
    void orderStatusNotChangedForReturnOrder() {
        orderCommandService.cancelOrReturn(new OrderCommand.CancelOrReturn(
                order.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, null, null, Source.SYSTEM),
                null,
                false
        ));
        orderCommandService.updateFlowStatusFromSc(
                new OrderCommand.UpdateFlowStatus(order.getId(), OrderFlowStatus.RETURNED_ORDER_IS_DELIVERED_TO_SENDER)
        );
        userHelper.finishPickupAtStartOfTheDay(userShiftId, true);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus()).isEqualTo(OrderFlowStatus.RETURNED_ORDER_IS_DELIVERED_TO_SENDER);
    }

    @Test
    void startPickup() {
        transactionTemplate.execute(tt -> {
            UserShift userShift = repository.findByIdOrThrow(userShiftId);
            assertThat(userShift.getRoutePoints()).isNotNull().hasSize(3);
            return null;
        });
        PickupTaskInfo info = arriveAtPickupReturnInfo();

        assertThat(info.getTask().getStatus()).isEqualTo(OrderPickupTaskStatus.NOT_STARTED);
        commandService.startOrderPickup(user, info.getPickupStartCommand());
        OrderPickupTask orderPickupTask = getOrderPickupTask();
        assertThat(orderPickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.IN_PROGRESS);
    }

    private PickupTaskInfo arriveAtPickupReturnInfo() {
        return transactionTemplate.execute(tt -> {
            var info = new PickupTaskInfo(repository.findByIdOrThrow(userShiftId));
            commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                    userShiftId, info.getRoutePoint().getId(),
                    new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShiftId)
            ));
            return info;
        });
    }

    @Test
    void pickupOrdersBeforeStart() {
        PickupTaskInfo info = arriveAtPickupReturnInfo();

        var pickupOrders = new UserShiftCommand.FinishScan(
                userShiftId,
                info.getRoutePoint().getId(),
                info.getTask().getId(),
                orderPickupRequest()
        );

        assertThatThrownBy(() -> commandService.pickupOrders(user, pickupOrders))
                .isInstanceOf(CommandFailedException.class).hasCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    void arriveToDeliveryBeforePickup() {
        var deliveryRoutePoint = getRoutePointByIndex(userShiftId, 1);

        assertThatThrownBy(() -> commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShiftId, deliveryRoutePoint.getId(),
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShiftId)
        ))).isInstanceOf(CommandFailedException.class).hasCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    void switchRoutePointBeforeFinishPickup() {
        PickupTaskInfo info = arriveAtPickupReturnInfo();
        commandService.startOrderPickup(user, info.getPickupStartCommand());

        var deliveryRoutePoint = getRoutePointByIndex(userShiftId, 1);

        thenThrownBy(() -> commandService.switchOpenRoutePoint(user, new UserShiftCommand.SwitchOpenRoutePoint(
                userShiftId, deliveryRoutePoint.getId(), true
        ))).isInstanceOf(TplIllegalArgumentException.class);
    }

    @Test
    void shouldNotSwitchOpenRoutePointWhenPickupIsNotFinished() {
        addPropertyToUser(user, UserProperties.USER_MODE, SOFT_MODE.name());
        PickupTaskInfo info = arriveAtPickupReturnInfo();
        commandService.startOrderPickup(user, info.getPickupStartCommand());
        var deliveryRoutePoint = getRoutePointByIndex(userShiftId, 1);
        thenThrownBy(() -> commandService.switchOpenRoutePoint(user, new UserShiftCommand.SwitchOpenRoutePoint(
                userShiftId, deliveryRoutePoint.getId(), true
        ))).isInstanceOf(TplIllegalArgumentException.class);

        var routePoint = getRoutePointByIndex(userShiftId, 0);
        var task = routePoint.streamPickupTasks().findFirst().orElseThrow();

        var pickupOrdersCommand = new UserShiftCommand.FinishScan(
                userShiftId,
                routePoint.getId(),
                task.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(getAllOrderIdFromShift())
                        .skippedOrders(List.of())
                        .finishedAt(routePoint.getExpectedDateTime())
                        .build()
        );
        commandService.pickupOrders(user, pickupOrdersCommand);
        commandService.finishLoading(
                user,
                new UserShiftCommand.FinishLoading(
                        userShiftId,
                        routePoint.getId(),
                        task.getId()));
        transactionTemplate.execute(tt -> {
            var rp = getRoutePointByIndex(userShiftId, 0);

            userHelper.finishCallTasksAtRoutePoint(rp);
            return null;
        });
        commandService.switchOpenRoutePoint(user, new UserShiftCommand.SwitchOpenRoutePoint(
                userShiftId, getReturnRoutePoint().getId()));

        assertThat(getCurrentRoutePoint().getType()).isEqualTo(RoutePointType.ORDER_RETURN);
    }

    private RoutePoint getReturnRoutePoint() {
        return transactionTemplate.execute(tt ->
                repository.findByIdOrThrow(userShiftId).streamReturnRoutePoints().findFirst().get()
        );
    }

    private List<Long> getAllOrderIdFromShift() {
        return transactionTemplate.execute(tt -> repository.findByIdOrThrow(userShiftId)
                .streamOrderDeliveryTasks()
                .map(OrderDeliveryTask::getOrderId)
                .collect(Collectors.toList()));
    }

    private RoutePoint getCurrentRoutePoint() {
        return transactionTemplate.execute(tt -> repository.findByIdOrThrow(userShiftId).getCurrentRoutePoint());
    }

    @Test
    void partiallyFinished() {
        var expectedPickupOrders = List.of(1L);
        var expectedSkippedOrders = List.of(2L);
        var expectedComment = "не виноватый я";
        pickupOrders(expectedPickupOrders, expectedSkippedOrders, expectedComment,
                OrderPickupTaskStatus.PARTIALLY_FINISHED);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 2);
    }

    @Test
    void finished() {
        var expectedPickupOrders = List.of(1L);
        List<Long> expectedSkippedOrders = List.of();
        pickupOrders(expectedPickupOrders, expectedSkippedOrders, null,
                OrderPickupTaskStatus.FINISHED);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 2);
    }

    @Test
    void finishedWithDisabledEqueue() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter, SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED, false
        );

        var expectedPickupOrders = List.of(1L);
        List<Long> expectedSkippedOrders = List.of();
        pickupOrders(expectedPickupOrders, expectedSkippedOrders, null,
                OrderPickupTaskStatus.FINISHED);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 2);
    }

    @Test
    void loadNotPreparedOrdersForPickup() {
        orderCommandService.forceUpdateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), OrderFlowStatus.SORTING_CENTER_CREATED));
        OrderPickupTask task = getOrderPickupTask();
        OrderScanTaskDto<OrderPickupTaskStatus> scanTaskInfo = (OrderScanTaskDto<OrderPickupTaskStatus>)
                userShiftQueryService.getTaskInfo(user, task.getRoutePoint().getId(), task.getId());
        assertThat(scanTaskInfo.getType()).isEqualTo(TaskType.ORDER_PICKUP);
        assertThat(scanTaskInfo.getOrders().stream()
                .filter(o -> o.getExternalOrderId().equals(order.getExternalOrderId())).findFirst().orElseThrow()
        ).extracting(OrderScanTaskDto.OrderForScanDto::getDisplayMode)
                .isEqualTo(OrderScanTaskDto.ScanOrderDisplayMode.OK);
    }

    @Test
    void twoPhasePickupOrders() {
        PickupTaskInfo info = arriveAtPickupReturnInfo();

        List<Long> expectedPickupOrders = List.of(order.getId());
        List<Long> expectedSkippedOrders = List.of();
        var pickupOrdersCommand = new UserShiftCommand.FinishScan(
                userShiftId,
                info.getRoutePoint().getId(),
                info.getTask().getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(expectedPickupOrders)
                        .skippedOrders(expectedSkippedOrders)
                        .comment("")
                        .finishedAt(Instant.now(clock))
                        .build()
        );

        commandService.startOrderPickup(user, info.getPickupStartCommand());
        commandService.pickupOrders(user, pickupOrdersCommand);

        OrderPickupTask orderPickupTask = getOrderPickupTask();
        assertThat(orderPickupTask.getPickupOrderIds()).isEqualTo(expectedPickupOrders);
        assertThat(orderPickupTask.getSkippedOrderIds()).isEqualTo(expectedSkippedOrders);
        assertThat(orderPickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.BOX_LOADING);

        commandService.finishLoading(
                user,
                new UserShiftCommand.FinishLoading(
                        userShiftId,
                        info.getRoutePoint().getId(),
                        info.getTask().getId()));
        orderPickupTask = getOrderPickupTask();
        assertThat(orderPickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.FINISHED);
    }

    @Test
    void pickupOrdersVerifyOrderAcceptedMark() {
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.SEND_EVENT_LOG_TO_SQS))
                .thenReturn(true);

        PickupTaskInfo info = arriveAtPickupReturnInfo();

        List<Long> expectedPickupOrders = List.of(order.getId());
        List<Long> expectedSkippedOrders = List.of();
        var pickupOrdersCommand = new UserShiftCommand.FinishScan(
                userShiftId,
                info.getRoutePoint().getId(),
                info.getTask().getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(expectedPickupOrders)
                        .skippedOrders(expectedSkippedOrders)
                        .comment("")
                        .finishedAt(Instant.now(clock))
                        .build()
        );

        commandService.startOrderPickup(user, info.getPickupStartCommand());
        commandService.pickupOrders(user, pickupOrdersCommand);

        commandService.finishLoading(
                user,
                new UserShiftCommand.FinishLoading(
                        userShiftId,
                        info.getRoutePoint().getId(),
                        info.getTask().getId()));

        var argument = ArgumentCaptor.forClass(OrderCommand.MarkAcceptedByCourier.class);
        verify(orderCommandService).markAcceptedByCourier(argument.capture());
        assertThat(argument.getValue().getUser()).isNotNull();
        assertThat(argument.getValue().getSortingCenter()).isNotNull();
    }

    @Test
    void idempotentPickupOrders() {
        PickupTaskInfo info = arriveAtPickupReturnInfo();

        List<Long> expectedPickupOrders = List.of(order.getId());
        List<Long> expectedSkippedOrders = List.of();

        commandService.startOrderPickup(user, info.getPickupStartCommand());

        var pickupOrdersCommand = new UserShiftCommand.FinishScan(
                userShiftId,
                info.getRoutePoint().getId(),
                info.getTask().getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(expectedPickupOrders)
                        .skippedOrders(expectedSkippedOrders)
                        .comment("")
                        .finishedAt(Instant.now(clock))
                        .build()
        );
        commandService.pickupOrders(user, pickupOrdersCommand);

        var pickupOrdersRetryCommand = new UserShiftCommand.FinishScan(
                userShiftId,
                info.getRoutePoint().getId(),
                info.getTask().getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(expectedPickupOrders)
                        .skippedOrders(expectedSkippedOrders)
                        .comment("")
                        .finishedAt(Instant.now(clock))
                        .build()
        );
        commandService.pickupOrders(user, pickupOrdersRetryCommand);

        OrderPickupTask orderPickupTask = getOrderPickupTask();

        assertThat(orderPickupTask.getPickupOrderIds()).isEqualTo(expectedPickupOrders);
        assertThat(orderPickupTask.getSkippedOrderIds()).isEqualTo(expectedSkippedOrders);
        assertThat(orderPickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.BOX_LOADING);
    }

    @Test
    void idempotentFinishLoading() {
        PickupTaskInfo info = arriveAtPickupReturnInfo();

        List<Long> expectedPickupOrders = List.of(order.getId());
        List<Long> expectedSkippedOrders = List.of();
        var pickupOrdersCommand = new UserShiftCommand.FinishScan(
                userShiftId,
                info.getRoutePoint().getId(),
                info.getTask().getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(expectedPickupOrders)
                        .skippedOrders(expectedSkippedOrders)
                        .comment("")
                        .finishedAt(Instant.now(clock))
                        .build()
        );

        commandService.startOrderPickup(user, info.getPickupStartCommand());
        commandService.pickupOrders(user, pickupOrdersCommand);

        commandService.finishLoading(
                user,
                new UserShiftCommand.FinishLoading(
                        userShiftId,
                        info.getRoutePoint().getId(),
                        info.getTask().getId()));
        OrderPickupTask orderPickupTask = getOrderPickupTask();
        assertThat(orderPickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.FINISHED);

        commandService.finishLoading(
                user,
                new UserShiftCommand.FinishLoading(
                        userShiftId,
                        info.getRoutePoint().getId(),
                        info.getTask().getId()));

        orderPickupTask = getOrderPickupTask();
        assertThat(orderPickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.FINISHED);
    }

    @Test
    void startOrderPickupOrEnqueue_electronicQueueEnabled() {
        arriveAtPickupReturnInfo();
        transactionTemplate.execute(tt -> {
            UserShift userShift = repository.findByIdOrThrow(userShiftId);
            var routePoint = userShift.getRoutePoints().get(0);
            var task = (OrderPickupTask) routePoint.getTasks().get(0);
            commandService.enqueue(user, new UserShiftCommand.Enqueue(userShiftId,
                    routePoint.getId(),
                    task.getId()));
            var pickupTask = userShift.streamPickupTasks().findFirst().orElseThrow();
            getRoutePointByIndex(userShiftId, 0).getTasks().stream().findFirst().orElseThrow();
            assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.IN_QUEUE);
            assertThat(pickupTask.getArrivalAttempt()).isEqualTo(1);
            assertThat(userShift.generateClientIdForYard()).isEqualTo("tpl/" + userShiftId + "/1");
            return null;
        });
        dbQueueTestUtil.assertQueueHasSize(QueueType.REGISTER_COURIER_IN_YARD, 1);
    }

    @Test
    void equeueInviteToLoading() {
        arriveAtPickupReturnInfo();
        OrderPickupTask orderPickupTask = getOrderPickupTask();
        Instant invitedArrivalTimeToLoading = Instant.now(clock).plusSeconds(300);

        Long routePointId = orderPickupTask.getRoutePoint().getId();
        Long orderPickupTaskId = orderPickupTask.getId();

        commandService.enqueue(
                user,
                new UserShiftCommand.Enqueue(
                        userShiftId,
                        routePointId,
                        orderPickupTaskId
                )
        );

        commandService.inviteToLoading(
                user,
                new UserShiftCommand.InviteToLoading(
                        userShiftId,
                        routePointId,
                        orderPickupTaskId,
                        invitedArrivalTimeToLoading
                )
        );

        var resultOrderPickupTask = getOrderPickupTask();
        assertThat(resultOrderPickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.WAITING_ARRIVAL);
        assertThat(resultOrderPickupTask.getInvitedArrivalTimeToLoading()).isEqualTo(invitedArrivalTimeToLoading);
    }

    private OrderPickupTask getOrderPickupTask() {
        return transactionTemplate.execute(tt ->
                repository.findByIdOrThrow(userShiftId).streamPickupTasks().findFirst().orElseThrow()
        );
    }

    void pickupOrders(List<Long> expectedPickupOrders, List<Long> expectedSkippedOrders,
                      @Nullable String expectedComment, OrderPickupTaskStatus expectedStatus) {
        PickupTaskInfo info = arriveAtPickupReturnInfo();

        var pickupOrdersCommand = new UserShiftCommand.FinishScan(
                userShiftId,
                info.getRoutePoint().getId(),
                info.getTask().getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(expectedPickupOrders)
                        .skippedOrders(expectedSkippedOrders)
                        .comment(expectedComment)
                        .finishedAt(Instant.now(clock))
                        .build()
        );

        commandService.startOrderPickup(user, info.getPickupStartCommand());
        commandService.pickupOrders(user, pickupOrdersCommand);
        var finishLoadingCommand = new UserShiftCommand.FinishLoading(
                userShiftId,
                info.getRoutePoint().getId(),
                info.getTask().getId()
        );
        commandService.finishLoading(user, finishLoadingCommand);

        OrderPickupTask orderPickupTask = getOrderPickupTask();

        assertThat(orderPickupTask.getPickupOrderIds()).isEqualTo(expectedPickupOrders);
        assertThat(orderPickupTask.getSkippedOrderIds()).isEqualTo(expectedSkippedOrders);
        assertThat(orderPickupTask.getComment()).isEqualTo(expectedComment);
        assertThat(orderPickupTask.getStatus()).isEqualTo(expectedStatus);
    }

    private ScanRequest orderPickupRequest() {
        return ScanRequest.builder()
                .successfullyScannedOrders(List.of(1L))
                .skippedOrders(List.of(2L))
                .comment("не виноватый я")
                .finishedAt(Instant.now(clock))
                .build();
    }

    private <T> void addPropertyToUser(User user, PropertyDefinition<T> property, T value) {
        transactionTemplate.execute(tt -> {
            userPropertyService.addPropertyToUser(user, property, value);

            return null;
        });
    }

    private RoutePoint getRoutePointByIndex(Long userShiftId, int index) {
        return transactionTemplate.execute(
                tt -> repository.findByIdOrThrow(userShiftId).getRoutePoints().get(index)
        );
    }

    @Value
    private static class PickupTaskInfo {

        RoutePoint routePoint;
        OrderPickupTask task;
        UserShiftCommand.StartScan pickupStartCommand;
        Long transferActId;

        PickupTaskInfo(UserShift userShift) {
            routePoint = userShift.getRoutePoints().get(0);
            task = (OrderPickupTask) routePoint.getTasks().get(0);
            pickupStartCommand = new UserShiftCommand.StartScan(
                    userShift.getId(),
                    routePoint.getId(),
                    task.getId()
            );
            if (CollectionUtils.isNotEmpty(task.getTransferActs())) {
                transferActId = task.getTransferActs().stream().findFirst().get().getId();
            } else {
                transferActId = null;
            }
        }

    }

}
