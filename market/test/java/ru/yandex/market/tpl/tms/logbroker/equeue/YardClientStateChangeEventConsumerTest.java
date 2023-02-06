package ru.yandex.market.tpl.tms.logbroker.equeue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.logbroker.consumer.LogbrokerMessage;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.tms.logbroker.consumer.yard.YardClientStateChangeEventConsumer;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@RequiredArgsConstructor
class YardClientStateChangeEventConsumerTest extends TplTmsAbstractTest {

    private final YardClientStateChangeEventConsumer subject;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final TransactionTemplate transactionTemplate;
    private final DbQueueTestUtil dbQueueTestUtil;

    @Test
    void accept_eventWithInvalidExternalClientId() {
        var message = getMessage("tpl-clientId", "ALLOCATED");

        assertDoesNotThrow(() -> subject.accept(message));
    }

    @Test
    void accept_eventWhenUserShiftDoesNotExist() {
        var message = getMessage("tpl/12765478/2", "WAITING");

        assertDoesNotThrow(() -> subject.accept(message));
    }

    @Test
    void accept_waitingEvent() {
        UserShift userShift = createUserShiftAndEnqueue(35236L, true);
        var userShiftId = userShift.getId();

        var message = getMessage("tpl/" + userShiftId + "/2", "WAITING");

        subject.accept(message);

        var invitedArrivalTimeToLoadingExpected = Instant.now(clock).plus(15, ChronoUnit.MINUTES);

        OrderPickupTask orderPickupTaskResult = transactionTemplate.execute((status) -> {
            UserShift userShiftResult = userShiftRepository.findById(userShiftId).get();
            return userShiftResult.streamPickupTasks().findFirst().get();
        });

        assertThat(orderPickupTaskResult.getStatus()).isEqualTo(OrderPickupTaskStatus.WAITING_ARRIVAL);
        assertThat(orderPickupTaskResult.getInvitedArrivalTimeToLoading())
                .isEqualTo(invitedArrivalTimeToLoadingExpected);
    }

    @Test
    void accept_waitingEventSkippedIfTaskHasUnsupportedStatus() {
        UserShift userShift = createUserShiftAndEnqueue(35236L, true);
        toWaitingArrival(userShift);
        var userShiftId = userShift.getId();

        var message = getMessage("tpl/" + userShiftId + "/2", "WAITING");

        assertDoesNotThrow(() -> subject.accept(message));
    }

    @Test
    void accept_cancelledEvent() {
        UserShift userShift = createUserShiftAndEnqueue(35236L, true);
        toWaitingArrival(userShift);
        var userShiftId = userShift.getId();

        var message = getMessage("tpl/" + userShiftId + "/2", "CANCELLED");

        subject.accept(message);

        OrderPickupTask orderPickupTaskResult = transactionTemplate.execute((status) -> {
            UserShift userShiftResult = userShiftRepository.findById(userShiftId).get();
            return userShiftResult.streamPickupTasks().findFirst().get();
        });

        assertThat(orderPickupTaskResult.getStatus()).isEqualTo(OrderPickupTaskStatus.MISSED_ARRIVAL);
    }

    @Test
    void accept_cancelledEventSkippedIfTaskHasUnsupportedStatus() {
        UserShift userShift = createUserShiftAndEnqueue(35236L, true);
        var userShiftId = userShift.getId();

        var message = getMessage("tpl/" + userShiftId + "/2", "CANCELLED");

        assertDoesNotThrow(() -> subject.accept(message));
    }

    @Test
    void accept_cancelledEventSkippedIfTaskHasTargetStatus() {
        UserShift userShift = createUserShiftAndEnqueue(35236L, true);
        toWaitingArrival(userShift);
        toMissedArrival(userShift);
        var userShiftId = userShift.getId();

        var message = getMessage("tpl/" + userShiftId + "/2", "CANCELLED");

        assertDoesNotThrow(() -> subject.accept(message));
    }

    @Test
    void accept_waitingEventWithInvalidOrderPickupTaskStatus() {
        UserShift userShift = createUserShiftAndEnqueue(35237L, false);
        var userShiftId = userShift.getId();

        var message = getMessage("tpl/" + userShiftId + "/2", "WAITING");

        assertDoesNotThrow(() -> subject.accept(message));

        OrderPickupTask orderPickupTaskResult = transactionTemplate.execute((status) -> {
            UserShift userShiftResult = userShiftRepository.findById(userShiftId).get();
            return userShiftResult.streamPickupTasks().findFirst().get();
        });

        assertThat(orderPickupTaskResult.getStatus()).isEqualTo(OrderPickupTaskStatus.NOT_STARTED);
    }

    @Test
    void accept_addsMessageToDlqIfProcessorThrowsException() {
        var message = getMessage("tpl/abc/2", "WAITING");

        assertDoesNotThrow(() -> subject.accept(message));

        dbQueueTestUtil.executeAllQueueItems(QueueType.DLQ_YARD_CLIENT_STATE_CHANGE_EVENT);
        List<Task> tasks = dbQueueTestUtil.getTasks(QueueType.DLQ_YARD_CLIENT_STATE_CHANGE_EVENT);
        assertThat(tasks.size()).isEqualTo(1);
        assertThat(tasks.get(0).getAttemptsCount()).isEqualTo(1);
    }

    @Test
    void accept_addsMessageToDlqIfMessageHasInvalidFormat() {
        var message = new LogbrokerMessage("", "test");

        assertDoesNotThrow(() -> subject.accept(message));

        dbQueueTestUtil.executeAllQueueItems(QueueType.DLQ_YARD_CLIENT_STATE_CHANGE_EVENT);
        List<Task> tasks = dbQueueTestUtil.getTasks(QueueType.DLQ_YARD_CLIENT_STATE_CHANGE_EVENT);
        assertThat(tasks.size()).isEqualTo(1);
        assertThat(tasks.get(0).getAttemptsCount()).isEqualTo(1);
    }

    @Test
    void accept_waitingEventWithInvalidShiftStatus() {
        User user = userHelper.findOrCreateUser(35237L);
        var userShiftId = createUserShift(user);

        var message = getMessage("tpl/" + userShiftId + "/2", "WAITING");

        assertDoesNotThrow(() -> subject.accept(message));

        OrderPickupTask orderPickupTaskResult = transactionTemplate.execute((status) -> {
            UserShift userShiftResult = userShiftRepository.findById(userShiftId).get();
            return userShiftResult.streamPickupTasks().findFirst().get();
        });

        assertThat(orderPickupTaskResult.getStatus()).isEqualTo(OrderPickupTaskStatus.NOT_STARTED);
    }

    private UserShift createUserShiftAndEnqueue(long uin, boolean doEnqueue) {
        User user = userHelper.findOrCreateUser(uin);
        long userShiftId = createUserShift(user);

        commandService.switchActiveUserShift(user, userShiftId);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        UserShift userShift = userShiftRepository.findById(userShiftId).get();

        commandService.arriveAtRoutePoint(
                user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShiftId, userShift.getCurrentRoutePoint().getId(),
                        new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShiftId)
                )
        );

        if (doEnqueue) {
            commandService.enqueue(
                    user,
                    new UserShiftCommand.Enqueue(
                            userShiftId,
                            userShift.getCurrentRoutePoint().getId(),
                            userShift.getCurrentRoutePoint().streamPickupTasks().findFirst().get().getId()
                    )
            );
        }

        return userShift;
    }

    private void toWaitingArrival(UserShift userShift) {
        commandService.inviteToLoading(
                userShift.getUser(),
                new UserShiftCommand.InviteToLoading(
                        userShift.getId(),
                        userShift.getCurrentRoutePoint().getId(),
                        userShift.getCurrentRoutePoint().streamPickupTasks().findFirst().get().getId(),
                        Instant.now(clock).plus(5, ChronoUnit.MINUTES)
                )
        );
    }

    private void toMissedArrival(UserShift userShift) {
        commandService.missArrival(
                userShift.getUser(),
                new UserShiftCommand.MissArrival(
                        userShift.getId(),
                        userShift.getCurrentRoutePoint().getId(),
                        userShift.getCurrentRoutePoint().streamPickupTasks().findFirst().get().getId(),
                        false,
                        Source.SYSTEM
                )
        );
    }

    private long createUserShift(User user) {
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();
        long userShiftId = commandService.createUserShift(createCommand);
        return userShiftId;
    }

    @NotNull
    private LogbrokerMessage getMessage(String clientId, String stateToName) {
        return new LogbrokerMessage(
                "",
                "{" +
                        "\"clientId\":294," +
                        "\"stateFromId\":1," +
                        "\"stateFromName\":\"REGISTERED\"," +
                        "\"stateToId\":2," +
                        "\"stateToName\":\"" + stateToName + "\"," +
                        "\"externalClientId\":\"" + clientId + "\"," +
                        "\"serviceId\":1," +
                        "\"sentTime\":\"2021-08-25T10:51:08.425085\"" +
                    "}"
                );
    }

}
