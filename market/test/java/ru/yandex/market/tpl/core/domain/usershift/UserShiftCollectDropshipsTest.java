package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatisticsDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.dropship.CollectDropshipFailReason;
import ru.yandex.market.tpl.core.domain.usershift.dropship.CollectDropshipFailReasonType;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.CARGO_DELIVERED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.CARGO_RECEIVED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.DROPSHIP_TASK_CANCELLED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.DROPSHIP_TASK_CREATED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.DROPSHIP_TASK_REOPENED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.MOVEMENT_CANCELLED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.MOVEMENT_CREATED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.MOVEMENT_REOPENED;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class UserShiftCollectDropshipsTest {

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final MovementGenerator movementGenerator;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftQueryService userShiftQueryService;
    private final MovementHistoryEventRepository movementHistoryEventRepository;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;

    private User user;
    private UserShift userShift;
    private RoutePoint collectDropshipRoutePoint;
    private CollectDropshipTask collectDropshipTask;
    private Movement movement;

    @BeforeEach
    void init() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder().paymentType(OrderPaymentType.CASH).build()
        );
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        movement = movementGenerator.generate(MovementCommand.Create.builder().build());
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .routePoint(helper.taskCollectDropship(LocalDate.now(clock), movement)).build();

        long id = commandService.createUserShift(createCommand);
        userShift = repository.findById(id).orElseThrow();
        List<CollectDropshipTask> collectDropshipTasks = userShift.streamCollectDropshipTasks()
                .collect(Collectors.toList());
        collectDropshipTask = collectDropshipTasks.get(0);
        collectDropshipRoutePoint = collectDropshipTask.getRoutePoint();
        userHelper.checkinAndFinishPickup(userShift);
        userHelper.finishDelivery(userShift.streamDeliveryRoutePoints().findFirst().orElseThrow(), false);
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShift.getId(), collectDropshipRoutePoint.getId(),
                        new LocationDto(
                                collectDropshipRoutePoint.getGeoPoint().getLongitude(),
                                collectDropshipRoutePoint.getGeoPoint().getLatitude(),
                                "", userShift.getId())));
    }

    @Test
    void finishCollectDropshipTask() {
        commandService.collectDropships(user, new UserShiftCommand.CollectDropships(
                userShift.getId(), collectDropshipRoutePoint.getId(), collectDropshipTask.getId()
        ));
        assertThat(collectDropshipTask.getStatus()).isEqualTo(CollectDropshipTaskStatus.FINISHED);
        returnOrders();
        assertThat(movement.getStatus() == MovementStatus.DELIVERED_TO_SC);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
        assertThat(movementHistoryEventRepository.findAll())
                .extracting(MovementHistoryEvent::getType)
                .containsExactly(
                        MOVEMENT_CREATED,
                        DROPSHIP_TASK_CREATED,
                        CARGO_RECEIVED,
                        CARGO_DELIVERED
                );
    }

    @Test
    void finishAndReopenCollectDropshipTask() {
        commandService.collectDropships(user, new UserShiftCommand.CollectDropships(
                userShift.getId(), collectDropshipRoutePoint.getId(), collectDropshipTask.getId()
        ));
        assertThat(collectDropshipTask.getStatus()).isEqualTo(CollectDropshipTaskStatus.FINISHED);
        commandService.reopenDropshipsTask(user, new UserShiftCommand.ReopenDropships(
                userShift.getId(), collectDropshipRoutePoint.getId(), collectDropshipTask.getId(), Source.COURIER
        ));
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShift.getId(), collectDropshipRoutePoint.getId(),
                        new LocationDto(
                                collectDropshipRoutePoint.getGeoPoint().getLongitude(),
                                collectDropshipRoutePoint.getGeoPoint().getLatitude(),
                                "", userShift.getId())));
        commandService.collectDropships(user, new UserShiftCommand.CollectDropships(
                userShift.getId(), collectDropshipRoutePoint.getId(), collectDropshipTask.getId()
        ));
        returnOrders();
        assertThat(movement.getStatus() == MovementStatus.DELIVERED_TO_SC);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
        assertThat(movementHistoryEventRepository.findAll())
                .extracting(MovementHistoryEvent::getType)
                .containsExactly(
                        MOVEMENT_CREATED,
                        DROPSHIP_TASK_CREATED,
                        CARGO_RECEIVED,
                        DROPSHIP_TASK_REOPENED,
                        MOVEMENT_REOPENED,
                        CARGO_RECEIVED,
                        CARGO_DELIVERED
                );
    }

    private void returnOrders() {
        RoutePoint returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        returnRoutePoint.getId(),
                        helper.getLocationDto(userShift.getId())
                ));
        var returnTask = returnRoutePoint.getTasks().get(0);

        if (returnTask.getStatus() != OrderReturnTaskStatus.READY_TO_FINISH
           && returnTask.getStatus() != OrderReturnTaskStatus.AWAIT_CASH_RETURN) {
            var returnOrdersCommand = new UserShiftCommand.FinishScan(
                    userShift.getId(),
                    returnRoutePoint.getId(),
                    returnTask.getId(),
                    ScanRequest.builder().build()
            );

            var returnStartCommand = new UserShiftCommand.StartScan(
                    userShift.getId(),
                    returnRoutePoint.getId(),
                    returnTask.getId()
            );

            commandService.startOrderReturn(user, returnStartCommand);
            commandService.finishReturnOrders(user, returnOrdersCommand);
        }
        if (returnTask.getStatus() == OrderReturnTaskStatus.AWAIT_CASH_RETURN){
            var returnCashCommand = new UserShiftCommand.ReturnCash(
                    userShift.getId(),
                    returnRoutePoint.getId(),
                    returnTask.getId()
            );
            commandService.finishReturnCash(user, returnCashCommand);
        }

        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), returnRoutePoint.streamReturnTasks().findFirst().orElseThrow().getId()));
    }

    @Test
    void cancelCollectDropshipTask() {
        commandService.cancelDropships(user, new UserShiftCommand.CancelDropships(
                userShift.getId(), collectDropshipRoutePoint.getId(), collectDropshipTask.getId(),
                new CollectDropshipFailReason(CollectDropshipFailReasonType.COURIER_NEEDS_HELP,
                        null,
                        Source.COURIER
                )
        ));
        assertThat(collectDropshipTask.getStatus()).isEqualTo(CollectDropshipTaskStatus.CANCELLED);
        assertThat(collectDropshipRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.CREATED);
        returnOrders();
        userHelper.finishUserShift(userShift);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.CANCELLED);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
        List<MovementHistoryEvent> events = movementHistoryEventRepository.findAll();
        assertThat(events)
                .extracting(MovementHistoryEvent::getType)
                .containsExactly(
                        MOVEMENT_CREATED,
                        DROPSHIP_TASK_CREATED,
                        DROPSHIP_TASK_CANCELLED,
                        MOVEMENT_CANCELLED
                );
    }

    @Test
    void restartCollectDropshipTask() {
        commandService.cancelDropships(user, new UserShiftCommand.CancelDropships(
                userShift.getId(), collectDropshipRoutePoint.getId(), collectDropshipTask.getId(),
                new CollectDropshipFailReason(CollectDropshipFailReasonType.COURIER_NEEDS_HELP,
                        null,
                        Source.COURIER
                )
        ));
        commandService.reopenDropshipsTask(user, new UserShiftCommand.ReopenDropships(
                userShift.getId(), collectDropshipRoutePoint.getId(), collectDropshipTask.getId(), Source.COURIER
        ));
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.CREATED);
        assertThat(collectDropshipTask.getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
        assertThat(movementHistoryEventRepository.findAll())
                .extracting(MovementHistoryEvent::getType)
                .containsExactly(
                        MOVEMENT_CREATED,
                        DROPSHIP_TASK_CREATED,
                        DROPSHIP_TASK_CANCELLED,
                        DROPSHIP_TASK_REOPENED
                );
    }

    @Test
    void restartCollectDropshipTaskAfterReturnTaskIsFinished() {
        commandService.cancelDropships(user, new UserShiftCommand.CancelDropships(
                userShift.getId(), collectDropshipRoutePoint.getId(), collectDropshipTask.getId(),
                new CollectDropshipFailReason(CollectDropshipFailReasonType.COURIER_NEEDS_HELP,
                        null,
                        Source.COURIER
                )
        ));
        returnOrders();
        commandService.reopenDropshipsTask(user, new UserShiftCommand.ReopenDropships(
                userShift.getId(), collectDropshipRoutePoint.getId(), collectDropshipTask.getId(), Source.OPERATOR
        ));
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.CREATED);
        assertThat(collectDropshipTask.getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
        assertThat(movementHistoryEventRepository.findAll())
                .extracting(MovementHistoryEvent::getType)
                .containsExactly(
                        MOVEMENT_CREATED,
                        DROPSHIP_TASK_CREATED,
                        DROPSHIP_TASK_CANCELLED,
                        DROPSHIP_TASK_REOPENED
                );
    }

    @Test
    void getUserShiftStatistics() {
        UserShiftStatisticsDto statisticsDto = userShiftQueryService.getUserShiftStatisticsDto(user,
                userShift.getId());
        assertThat(statisticsDto).isNotNull();
        assertThat(collectDropshipTask.getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
        assertThat(statisticsDto.getNumberOfAllTasks()).isEqualTo(2);
        assertThat(statisticsDto.getNumberOfFinishedTasks()).isEqualTo(1);
    }


}
