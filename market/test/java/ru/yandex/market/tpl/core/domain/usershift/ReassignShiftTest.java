package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.tpl.Company;
import ru.yandex.market.logistics.les.tpl.CourierForSc;
import ru.yandex.market.logistics.les.tpl.TplCourierReassignEvent;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.reassignment.ReassignCourierSendEventToSqsPayload;
import ru.yandex.market.tpl.core.domain.usershift.reassignment.ReassignCourierSendEventToSqsService;
import ru.yandex.market.tpl.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.tpl.core.domain.movement.Movement.TAG_DROPOFF_CARGO_RETURN;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReassignShiftTest extends TplAbstractTest {
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final MovementGenerator movementGenerator;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftReassignManager reassignmentManager;
    private final ClientReturnGenerator clientReturnGenerator;
    private final TransactionTemplate tt;
    private final Clock clock;
    private final JmsTemplate jmsTemplate;
    private final SqsQueueProperties sqsQueueProperties;
    private final ReassignCourierSendEventToSqsService reassignCourierSendEventToSqsService;

    private User user1;
    private User user2;
    private Shift shift;
    private UserShift userShift1;
    private ClientReturn clientReturn;

    private List<Order> orders;
    private List<Movement> movements;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock, LocalDateTime.now());
        tt.execute(action -> {
            shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
            var scId = shift.getSortingCenter().getId();
            var dsId = shift.getSortingCenter().getDeliveryServices()
                    .get(0).getId();
            user1 = testUserHelper.findOrCreateUserForSc(1462012L, LocalDate.now(), scId);
            user2 = testUserHelper.findOrCreateUserForSc(5098145L, LocalDate.now(), scId);

            orders = Stream.generate(() -> orderGenerateService.createOrder(
                    OrderGenerateService.OrderGenerateParam.builder()
                            .deliveryServiceId(dsId)
                            .build()
            )).limit(3).collect(Collectors.toList());

            clientReturn = clientReturnGenerator.generateReturnFromClient(dsId);

            movements = Stream.generate(() -> movementGenerator.generate(
                    MovementCommand.Create.builder()
                            .deliveryServiceId(dsId)
                            .build()
            )).limit(3).collect(Collectors.toList());
            movements.get(2).setTags(List.of(TAG_DROPOFF_CARGO_RETURN));
            PickupPoint pickupPoint = pickupPointRepository.save(
                    testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));

            movements.get(2).getWarehouseTo().setYandexId(Long.toString(pickupPoint.getLogisticPointId()));
            var createCommand = UserShiftCommand.Create.builder()
                    .userId(user1.getId())
                    .shiftId(shift.getId())
                    .routePoint(helper.taskOrderPickup(Instant.now()))
                    .routePoint(helper.taskPrepaid("addr1", 12, orders.get(0).getId()))
                    .routePoint(helper.taskPrepaid("addr2", 13, orders.get(1).getId()))
                    .routePoint(helper.taskPrepaid("addr3", 12, orders.get(2).getId()))
                    .routePoint(helper.clientReturn("addr4", 15, clientReturn.getId()))
                    .routePoint(helper.taskCollectDropship(LocalDate.now(), movements.get(0)))
                    .routePoint(helper.taskCollectDropship(LocalDate.now(), movements.get(1)))
                    .routePoint(helper.taskDropOffReturn(movements.get(2).getId(), pickupPoint.getId()))
                    .mergeStrategy(SimpleStrategies.NO_MERGE)
                    .build();
            userShift1 = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();
            return null;
        });
    }

    @Test
    void reassignShiftTest() {
        var courierTo = user2.getId();
        var userShiftId = userShift1.getId();
        reassignmentManager.reassignUserShiftOrders(userShiftId, courierTo, null);
        tt.execute((action) -> {
            var userShift2 = userShiftRepository.findCurrentShift(user2).get();
            var rOrders = userShift2.getRoutePoints().stream()
                    .flatMap(RoutePoint::streamDeliveryTasks)
                    .filter(t -> t.getType() != TaskType.CLIENT_RETURN)
                    .map(DeliveryTask::getOrderIds)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            assertTrue(rOrders.contains(orders.get(0).getId()));
            assertTrue(rOrders.contains(orders.get(1).getId()));
            assertTrue(rOrders.contains(orders.get(2).getId()));

            var rMovements = userShift2.getRoutePoints().stream()
                    .flatMap(RoutePoint::streamCollectDropshipTasks)
                    .map(CollectDropshipTask::getMovementId)
                    .collect(Collectors.toSet());
            assertTrue(rMovements.contains(movements.get(0).getId()));
            assertTrue(rMovements.contains(movements.get(1).getId()));

            var rClientReturns = userShift2.streamOrderDeliveryTasks()
                    .filter(OrderDeliveryTask::isClientReturn)
                    .map(OrderDeliveryTask::getClientReturnId)
                    .collect(Collectors.toList());
            assertTrue(rClientReturns.contains(clientReturn.getId()));

            var returnDropOffs = StreamEx.of(userShift2.getRoutePoints())
                    .flatMap(RoutePoint::streamLockerDeliveryTasks)
                    .map(LockerDeliveryTask::getSubtasks)
                    .flatMap(List::stream)
                    .filter(st -> st.getLockerSubtaskDropOff() != null)
                    .map(LockerSubtask::getLockerSubtaskDropOff)
                    .map(LockerSubtaskDropOff::getMovementId)
                    .toSet();

            assertTrue(returnDropOffs.contains(movements.get(2).getId()));
            return null;
        });
    }

    @Test
    void shouldSendEvent() {
        Mockito.clearInvocations(jmsTemplate);
        var courierTo = user2.getId();
        var userShiftId = userShift1.getId();
        reassignmentManager.reassignUserShiftOrders(userShiftId, courierTo, null);
        reassignCourierSendEventToSqsService.processPayload(
                new ReassignCourierSendEventToSqsPayload(
                        "requestId",
                        userShiftId,
                        courierTo
                )
        );

        var userShiftFrom = userShiftRepository.findByIdOrThrow(userShiftId);
        var courierFrom = userShiftFrom.getUser();

        var courierFromForSc = new CourierForSc(courierFrom.getUid(), null, null);
        var courierToForSc = new CourierForSc(
                user2.getUid(),
                user2.getName(),
                new Company(user2.getCompany().getName())
        );
        var event = new TplCourierReassignEvent(
                courierFromForSc,
                courierToForSc,
                shift.getShiftDate(),
                shift.getSortingCenter().getId(),
                shift.getSortingCenter().getToken()
        );
        var eventId = Objects.hash(userShiftId, user1.getUid(), user2.getUid());
        var lesEvent = new Event(
                sqsQueueProperties.getSource(),
                Long.toString(eventId),
                Instant.now(clock).toEpochMilli(),
                TplCourierReassignEvent.EVENT_NAME,
                event,
                "Событие TPL_COURIER_REASSIGN в курьерсской платформе"
        );


        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(anyString(), eq(lesEvent));

    }

}
