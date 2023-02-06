package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;

import ru.yandex.market.tpl.api.model.order.OrderChequeDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.exception.CommandFailedException;
import ru.yandex.market.tpl.core.exception.TplInvalidStateException;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.DELIVERY_DELAY;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERY_FAILED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.NOT_DELIVERED;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;
import static ru.yandex.market.tpl.core.test.AssertionUtils.assertPresent;

/**
 * @author ungomma
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@CoreTest
class UserShiftHappyPathTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final OrderGenerateService orderGenerateService;
    private final MovementGenerator movementGenerator;
    private final OrderRepository orderRepository;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;

    private final Clock clock;

    private UserShift userShift;
    private User user;

    @BeforeAll
    void createShift() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(OrderPaymentType.CASH)
                .build());
        var order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(OrderPaymentType.PREPAID)
                .build());
        var order3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(OrderPaymentType.PREPAID)
                .build());

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .routePoint(helper.taskPrepaid("addr3", 14, order2.getId()))
                .routePoint(helper.taskCollectDropship(
                        LocalDate.now(clock), // 20:00
                        movementGenerator.generate(MovementCommand.Create.builder().build()))
                )
                .routePoint(helper.taskPrepaid("addrPaid", 13, order3.getId()))
                .build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
    }

    @BeforeEach
    void refreshReference() {
        userShift = repository.findById(userShift.getId()).orElseThrow();
    }

    @Test
    @Order(5)
    void cantRescheduleBeforeShiftStart() {
        assumeThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        assumeThat(userShift.getRoutePoints()).extracting(RoutePoint::getStatus).containsOnly(RoutePointStatus.NOT_STARTED);

        RoutePoint rp = Objects.requireNonNull(userShift.getRoutePoints().get(1));
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assertThatThrownBy(() -> commandService.rescheduleDeliveryTask(user,
                new UserShiftCommand.RescheduleOrderDeliveryTask(userShift.getId(), rp.getId(), task.getId(),
                        DeliveryReschedule.fromCourier(user, todayAtHour(18, clock), todayAtHour(20, clock),
                                DELIVERY_DELAY),
                        todayAtHour(11, clock), userShift.getZoneId())))
                .isInstanceOf(CommandFailedException.class)
                .hasCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    @Commit
    @Order(15)
    void startShift() {
        commandService.switchActiveUserShift(user, userShift.getId());
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        assertThat(userShift.getStartedAt()).isNotNull();
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(
                userShift.streamPickupRoutePoints().findFirst().orElseThrow()
        );

        assertThat(userShift.getRoutePoints().get(0).getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        assertThat(userShift.getRoutePoints()).extracting(RoutePoint::getStatus)
                .containsOnlyOnce(RoutePointStatus.IN_TRANSIT)
                .containsOnly(RoutePointStatus.IN_TRANSIT, RoutePointStatus.NOT_STARTED);
    }

    @Test
    @Commit
    @Order(17)
    void canRescheduleDeliveryTask() {

        RoutePoint rp = Objects.requireNonNull(userShift.getRoutePoints().get(2));
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.NOT_STARTED);
        assumeThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        commandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                DeliveryReschedule.fromCourier(user, todayAtHour(16, clock),
                        todayAtHour(18, clock), OrderDeliveryRescheduleReasonType.CLIENT_REQUEST),
                todayAtHour(11, clock),
                userShift.getZoneId()
        ));

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        // создается новая точка
        Optional<RoutePoint> newRpO = userShift.getRoutePoints().stream()
                .filter(point -> point.getStatus() != RoutePointStatus.FINISHED)
                .filter(point -> point.streamDeliveryTasks().anyMatch(t -> t.getId().equals(task.getId())))
                .findFirst();
        assertThat(newRpO).isPresent();
        RoutePoint newRp = newRpO.get();

        assertThat(newRp.getExpectedDateTime()).isAfter(todayAtHour(16, clock).plus(45, ChronoUnit.MINUTES));
        assertThat(newRp.getExpectedDateTime()).isBefore(todayAtHour(18, clock));
        assertThat(newRp.getExpectedDateTime()).isEqualTo(task.getExpectedDeliveryTime());

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
    }

    @Test
    @Commit
    @Order(20)
    void arriveAtRoutePoint() {

        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());

        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);

        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), rp.getId(),
                helper.getLocationDto(userShift.getId())
        ));

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
    }

    @Test
    @Commit
    @Order(21)
    void startOrderPickup() {

        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());

        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        var pickupTask = ((OrderPickupTask) rp.getTasks().get(0));
        commandService.startOrderPickup(user, new UserShiftCommand.StartScan(
                userShift.getId(), rp.getId(), pickupTask.getId()
        ));

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.IN_PROGRESS);
    }

    @Test
    @Commit
    @Order(22)
    void pickupOrders() {

        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());

        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        List<Long> shiftOrderIds = userShift.streamDeliveryTasks()
                .map(DeliveryTask::getOrderIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        var pickupTask = ((OrderPickupTask) rp.getTasks().get(0));
        commandService.pickupOrders(user, new UserShiftCommand.FinishScan(
                userShift.getId(), rp.getId(), pickupTask.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(shiftOrderIds)
                        .finishedAt(clock.instant())
                        .build()
        ));

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        commandService.finishLoading(
                userShift.getUser(),
                new UserShiftCommand.FinishLoading(
                        userShift.getId(),
                        rp.getId(), pickupTask.getId()));
        userHelper.finishCallTasksAtRoutePoint(rp);
        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(pickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.FINISHED);

        commandService.switchOpenRoutePoint(user, new UserShiftCommand.SwitchOpenRoutePoint(
                userShift.getId(), userShift.streamDeliveryRoutePoints().findFirst().orElseThrow().getId()));
    }

    @Test
    @Commit
    @Order(23)
    void arriveAtFirstDeliveryRoutePoint() {
        arriveAtRoutePoint(); // lol
    }

    @Test
    @Order(30)
    void chequeWithoutPayShouldFail() {

        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assumeThat(task.getStatus()).isEqualTo(NOT_DELIVERED);

        assertThatThrownBy(() ->
                commandService.printCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                        userShift.getId(), rp.getId(), task.getId(), helper.getChequeDto(OrderPaymentType.CASH),
                        Instant.now(clock), false, null, Optional.empty()
                )))
                .isInstanceOf(CommandFailedException.class)
                .hasCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    @Commit
    @Order(35)
    void pay() {

        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assumeThat(task.getStatus()).isEqualTo(NOT_DELIVERED);

        commandService.payOrder(user,
                new UserShiftCommand.PayOrder(userShift.getId(), rp.getId(), task.getId(), OrderPaymentType.CASH,
                        null));

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

    }

    @Test
    @Commit
    @Order(36)
    void canPayTwice() {

        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assumeThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        commandService.payOrder(user, new UserShiftCommand.PayOrder(
                userShift.getId(), rp.getId(), task.getId(), OrderPaymentType.CARD, null
        ));

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
    }

    @Test
    @Commit
    @Order(40)
    void printCheque() {

        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assumeThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        commandService.printCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                userShift.getId(), rp.getId(), task.getId(), getChequeDto(task), Instant.now(clock), false, null,
                Optional.empty()
        ));
        userHelper.finishCallTasksAtRoutePoint(rp);

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED);
        assertThat(rp.getStatus())
                .describedAs("RoutePoint is finished if all tasks are done")
                .isEqualTo(RoutePointStatus.FINISHED);
    }

    @Test
    void printChequeDemoMode() {
        LocalDate date = LocalDate.now();
        user = userHelper.findOrCreateUser(1, date);
        Shift shift = userHelper.findOrCreateOpenShiftForSc(date, SortingCenter.DEMO_SC_ID);

        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        userShift = userShiftRepository.findById(userShiftId).orElseThrow();
        userHelper.finishPickupAtStartOfTheDay(userShift);

        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));

        var rp = userShift.getCurrentRoutePoint();
        var task = rp.streamOrderDeliveryTasks()
                .findAny()
                .orElseThrow();

        assumeThat(task.getStatus()).isEqualTo(NOT_DELIVERED);

        commandService.payOrder(user,
                new UserShiftCommand.PayOrder(userShift.getId(), rp.getId(), task.getId(), OrderPaymentType.CARD,
                        null));

        commandService.printCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                userShift.getId(), rp.getId(), task.getId(), getChequeDto(task), Instant.now(clock), false, null,
                Optional.empty()
        ));
        userHelper.finishCallTasksAtRoutePoint(rp);

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED);
        assertThat(rp.getStatus())
                .describedAs("RoutePoint is finished if all tasks are done")
                .isEqualTo(RoutePointStatus.FINISHED);

        var order = orderRepository.findByIdOrThrow(task.getOrderId());
        assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.DELIVERED);
    }

    private OrderChequeDto getChequeDto(OrderDeliveryTask task) {
        return helper.getChequeDto(task.isPrepaidOrder() ? OrderPaymentType.PREPAID : OrderPaymentType.CARD);
    }

    @Test
    @Order(41)
    void routePointWasSwitched() {
        assertThatRoutePointWasSwitched(3);
    }

    void assertThatRoutePointWasSwitched(int currentIndex) {

        RoutePoint firstNotFinished = assertPresent(userShift.getRoutePoints().stream()
                .filter(rp -> !rp.getStatus().isTerminal())
                .findFirst());

        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(firstNotFinished);
        assertThat(firstNotFinished.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        assertThat(userShift.getRoutePoints().get(currentIndex)).isEqualTo(firstNotFinished);
    }

    @Test
    @Order(50)
    void chequeInTransitMustFail() {
        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        assumeThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        assertThatThrownBy(() ->
                commandService.printCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                        userShift.getId(), rp.getId(), task.getId(), helper.getChequeDto(OrderPaymentType.CASH),
                        Instant.now(clock), false, null, Optional.empty()
                )))
                .isInstanceOf(CommandFailedException.class)
                .hasCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    @Commit
    @Order(56)
    void arriveAtNextRoutePoint() {
        arriveAtRoutePoint(); // lol
    }

    @Test
    @Order(57)
    @Commit
    void failTaskInProgress() {
        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.NO_CONTACT, "Недозвон")
        ));

        assertThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);

        userHelper.finishCallTasksAtRoutePoint(rp);
    }

    @Test
    @Order(58)
    void routePointWasSwitchedAgain() {
        assertThatRoutePointWasSwitched(4);
    }

    @Test
    @Order(60)
    void cantRescheduleCancelledTask() {
        RoutePoint rp = userShift.streamDeliveryRoutePoints()
                .filter(p -> p.getTasks().size() > 0 && p.getTasks().iterator().next().getStatus() == DELIVERY_FAILED)
                .findFirst().orElseThrow();

        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assumeThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);

        assertThatThrownBy(() -> commandService.rescheduleDeliveryTask(user,
                new UserShiftCommand.RescheduleOrderDeliveryTask(userShift.getId(), rp.getId(), task.getId(),
                        DeliveryReschedule.fromCourier(user, todayAtHour(18, clock), todayAtHour(20, clock),
                                DELIVERY_DELAY),
                        todayAtHour(11, clock), userShift.getZoneId())))
                .isInstanceOf(CommandFailedException.class)
                .hasCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    @Commit
    @Order(63)
    void canReopenFailedTask() {
        RoutePoint rp = userShift.streamDeliveryRoutePoints()
                .filter(p -> p.getTasks().size() > 0 && p.getTasks().iterator().next().getStatus() == DELIVERY_FAILED)
                .findFirst().orElseThrow();

        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assumeThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(), Source.COURIER
        ));

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(task.getFailReason()).isNull();
        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.UNFINISHED);
    }

    @Test
    @Commit
    @Order(65)
    void arriveAtNextDeliveryRoutePoint() {
        arriveAtRoutePoint();
    }

    @Test
    @Commit
    @Order(70)
    void finishNextDeliveryTask() {
        printCheque();
    }

    @Test
    @Commit
    @Order(73)
    void rescheduleNextDeliveryTask() {

        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.UNFINISHED);
        assertThat(rp.getExpectedDateTime()).isEqualTo(todayAtHour(14, clock));
        assumeThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        commandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                DeliveryReschedule.fromCourier(
                        user, todayAtHour(18, clock), todayAtHour(20, clock),
                        OrderDeliveryRescheduleReasonType.DELIVERY_DELAY),
                todayAtHour(11, clock),
                userShift.getZoneId()
        ));

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        // создается новая точка
        Optional<RoutePoint> newRpO = userShift.getRoutePoints().stream()
                .filter(point -> point.getStatus() != RoutePointStatus.FINISHED)
                .filter(point -> point.streamDeliveryTasks().anyMatch(t -> t.getId().equals(task.getId())))
                .findFirst();
        assertThat(newRpO).isPresent();
        RoutePoint newRp = newRpO.get();

        assertThat(newRp.getExpectedDateTime()).isAfter(todayAtHour(19, clock).plus(45, ChronoUnit.MINUTES));
        assertThat(newRp.getExpectedDateTime()).isBefore(todayAtHour(20, clock));
        assertThat(newRp.getExpectedDateTime()).isEqualTo(task.getExpectedDeliveryTime());

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
    }

    @Test
    @Commit
    @Order(75)
    void failNextCurrentTask() {
        RoutePoint rp = userShift.getCurrentRoutePoint();

        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        assumeThat(task.getStatus()).isEqualTo(NOT_DELIVERED);

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, null)
        ));
        assertThat(task.getStatus()).isEqualTo(DELIVERY_FAILED);
        assertThat(rp.getStatus())
                .describedAs("RoutePoint is finished if all tasks are done")
                .isEqualTo(RoutePointStatus.FINISHED);

        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
        assertThat(userShift.streamReturnRoutePoints().findFirst().get().streamReturnTasks().count()).isEqualTo(1);
    }

    @Test
    @Commit
    @Order(76)
    void arriveAtNextCollectDropshipsRoutePoint() {
        arriveAtRoutePoint();
    }

    @Test
    @Commit
    @Order(77)
    void collectDropships() {
        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        CollectDropshipTask task = (CollectDropshipTask) rp.getTasks().get(0);

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(rp.getExpectedDateTime()).isEqualTo(todayAtHour(20, clock));
        assumeThat(task.getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);

        commandService.collectDropships(user,
                new UserShiftCommand.CollectDropships(userShift.getId(), rp.getId(), task.getId()));

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assumeThat(task.getStatus()).isEqualTo(CollectDropshipTaskStatus.FINISHED);
    }

    @Test
    @Commit
    @Order(78)
    void arriveToSortingCenter() {
        arriveAtRoutePoint();
    }

    @Test
    @Commit
    @Order(78)
    void startOrderReturn() {

        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());

        assumeThat(rp.getType()).isEqualTo(RoutePointType.ORDER_RETURN);
        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        var returnTask = ((OrderReturnTask) rp.getTasks().get(0));
        commandService.startOrderReturn(user, new UserShiftCommand.StartScan(
                userShift.getId(), rp.getId(), returnTask.getId()
        ));

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(returnTask.getStatus()).isEqualTo(OrderReturnTaskStatus.IN_PROGRESS);
    }

    @Test
    @Commit
    @Order(79)
    void returnOrders() {
        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());

        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        List<Long> shiftOrderIds = userShift.streamDeliveryRoutePoints()
                .flatMap(RoutePoint::streamFailedDeliveryTasks)
                .map(DeliveryTask::getOrderIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        var returnTask = ((OrderReturnTask) rp.getTasks().get(0));

        commandService.finishReturnOrders(user, new UserShiftCommand.FinishScan(
                userShift.getId(), rp.getId(), returnTask.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(shiftOrderIds)
                        .finishedAt(clock.instant())
                        .build()
        ));

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(returnTask.getStatus()).isEqualTo(OrderReturnTaskStatus.READY_TO_FINISH);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(
                userShift.getId(), rp.getId(), returnTask.getId()
        ));
    }

    @Test
    @Order(80)
    void shiftWasClosed() {
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
        assertThat(userShift.getCurrentRoutePoint()).isNull();
        assertThat(userShift.getClosedAt()).isNotNull();

        assertThat(userShift.getRoutePoints()).extracting(RoutePoint::getStatus).containsOnly(RoutePointStatus.FINISHED);
    }

}
