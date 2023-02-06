package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementCommandService;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.dropship.CollectDropshipFailReasonType;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestTplDropoffFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.COURIER_REASSIGNED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.COURIER_WITH_ENABLED_LOT_FLOW_IDS;

/**
 * @author valter
 */
@RequiredArgsConstructor
class UserShiftReassignManagerTest extends TplAbstractTest {

    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long SORTING_CENTER_ID = 47819L;

    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserPropertyService userPropertyService;
    private final MovementGenerator movementGenerator;
    private final MovementRepository movementRepository;
    private final MovementCommandService movementCommandService;
    private final TestDataFactory testDataFactory;
    private final SortingCenterService sortingCenterService;
    private final TransactionTemplate transactionTemplate;
    private final OrderRepository orderRepository;
    private final RoutePointRepository routePointRepository;
    private final PickupPointRepository pickupPointRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final Clock clock;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final DropoffCargoCommandService dropoffCargoCommandService;
    private final TestTplDropoffFactory testTplDropoffFactory;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    Shift shift;
    User user1;
    User user2;
    Order order;
    Order orderSortingCenterCreated;
    PickupPoint pickupPoint;


    @BeforeEach
    void init() {
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );

        clearAfterTest(pickupPoint);

        user1 = testUserHelper.findOrCreateUser(1L);
        user2 = testUserHelper.findOrCreateUser(2L);

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());
        orderSortingCenterCreated = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .flowStatus(OrderFlowStatus.SORTING_CENTER_CREATED)
                .build());
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), SORTING_CENTER_ID);

        sortingCenterPropertyService.deletePropertyFromSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED);
    }

    @AfterEach
    void clear() {
        sortingCenterPropertyService.deletePropertyFromSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED);
        when(configurationProviderAdapter.getValueAsLongs(COURIER_WITH_ENABLED_LOT_FLOW_IDS))
                .thenReturn(Set.of());
    }

    @Test
    void reassignAfterPickup() {
        testUserHelper.createOpenedShift(user1, order, LocalDate.now(clock), SORTING_CENTER_ID);

        var order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().build());
        UserShift userShift2 = testUserHelper.createOpenedShift(user2, order2, LocalDate.now(clock), SORTING_CENTER_ID);
        testUserHelper.finishPickupAtStartOfTheDay(userShift2, true, false);

        assertThat(order.getOrderFlowStatus()).isNotEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user2.getId());
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
    }

    @Test
    void reassignAfterPickupOrderSortingCenterCreated() {
        testUserHelper.createOpenedShift(user1, orderSortingCenterCreated, LocalDate.now(clock), SORTING_CENTER_ID);

        var order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.SORTING_CENTER_CREATED).build());
        UserShift userShift2 = testUserHelper.createOpenedShift(user2, order2, LocalDate.now(clock), SORTING_CENTER_ID);
        testUserHelper.finishPickupAtStartOfTheDay(userShift2, true, false);

        assertThat(order.getOrderFlowStatus()).isNotEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        assertThatThrownBy(() -> userShiftReassignManager.reassignOrders(Set.of(orderSortingCenterCreated.getId()),
                Set.of(), Set.of(), user2.getId())
        ).isInstanceOf(TplInvalidActionException.class);
        assertThat(orderSortingCenterCreated.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_CREATED);
    }

    @Test
    void shouldReassignOrderAfterUserShiftFinished() {
        UserShift userShift = transactionTemplate.execute(status -> {
            var newUserShift = testUserHelper.createOpenedShift(user1, order, LocalDate.now(clock), SORTING_CENTER_ID);

            testUserHelper.finishPickupAtStartOfTheDay(newUserShift);
            testUserHelper.finishDelivery(newUserShift.getCurrentRoutePoint(), true);
            RoutePoint returnRoutPoint = newUserShift.getCurrentRoutePoint();
            testUserHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(newUserShift, true);
            userShiftCommandService.finishReturnTask(user1, new UserShiftCommand.FinishReturnTask(newUserShift.getId(),
                    returnRoutPoint.getId(), returnRoutPoint.streamReturnTasks().findFirst().orElseThrow().getId()));
            userShiftCommandService.finishUserShift(new UserShiftCommand.Finish(newUserShift.getId()));
            return newUserShift;
        });
        assert userShift != null;
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);

        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID).build());
        userShiftReassignManager.reassignOrders(Set.of(order2.getId()), Set.of(), Set.of(), user1.getId());
        List<UserShift> userShifts = userShiftRepository.findAllByShiftIdAndUserId(shift.getId(), user1.getId());
        assertThat(userShifts).hasSize(2);
        assertHasActiveTaskOrderDelivery(userShifts.stream().filter(UserShift::isActive).findFirst().get().getId(),
                order2);
    }

    @Test
    void reassignOrderNoShift() {
        UserShift userShift1 = testUserHelper.createOpenedShift(user1, order, LocalDate.now(clock), SORTING_CENTER_ID);
        assertHasActiveTaskOrderDelivery(userShift1.getId(), order);
        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user2.getId());
        assertThat(userShift1.streamReturnRoutePoints().toList()).hasSize(1); // always need to return reassigned order
        assertDoesNotHaveActiveTaskOrderDelivery(userShift1.getId(), order);
        UserShift userShift2 = userShiftRepository.findByShiftIdAndUserId(shift.getId(), user2.getId()).orElseThrow();
        assertThat(userShift2.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        assertHasActiveTaskOrderDelivery(userShift2.getId(), order);
    }

    private void assertHasActiveTaskOrderDelivery(Long userShiftId, Order order) {
        transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            assertThat(
                    userShift.streamOrderDeliveryTasks()
                            .anyMatch(t -> !t.getStatus().isTerminal() && Objects.equals(t.getOrderId(), order.getId()))
            ).isTrue();
            return null;
        });
    }

    private void assertDoesNotHaveActiveTaskOrderDelivery(Long userShiftId, Order order) {
        transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            assertThat(
                    userShift.streamOrderDeliveryTasks()
                            .noneMatch(t -> !t.getStatus().isTerminal() && Objects.equals(t.getOrderId(),
                                    order.getId()))
            ).isTrue();
            return null;
        });
    }

    private void assertHaveSingleTaskOrderDelivery(Long userShiftId, Order order) {
        transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            assertThat(
                    userShift.streamOrderDeliveryTasks()
                            .filter(t -> Objects.equals(t.getOrderId(), order.getId()))
                            .count()
            ).isEqualTo(1L);
            return null;
        });
    }

    @Test
    void reassignTwice() {
        UserShift userShift1 = testUserHelper.createOpenedShift(user1, order, LocalDate.now(clock), SORTING_CENTER_ID);
        UserShift userShift2 = testUserHelper.createEmptyShift(user2,
                testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), SORTING_CENTER_ID));
        assertHasActiveTaskOrderDelivery(userShift1.getId(), order);
        assertDoesNotHaveActiveTaskOrderDelivery(userShift2.getId(), order);

        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user2.getId());
        assertDoesNotHaveActiveTaskOrderDelivery(userShift1.getId(), order);
        assertHasActiveTaskOrderDelivery(userShift2.getId(), order);
        assertHaveSingleTaskOrderDelivery(userShift1.getId(), order);
        assertHaveSingleTaskOrderDelivery(userShift2.getId(), order);
        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER).stream()
                .filter(e -> e.equals(order.getExternalOrderId()))
                .count()).isEqualTo(2);

        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user1.getId());
        assertHasActiveTaskOrderDelivery(userShift1.getId(), order);
        assertDoesNotHaveActiveTaskOrderDelivery(userShift2.getId(), order);
        assertHaveSingleTaskOrderDelivery(userShift1.getId(), order);
        assertHaveSingleTaskOrderDelivery(userShift2.getId(), order);
        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER).stream()
                .filter(e -> e.equals(order.getExternalOrderId()))
                .count()).isEqualTo(3);
    }

    @Test
    void assignOrderTaskExistsAndOpen() {
        UserShift userShift1 = testUserHelper.createOpenedShift(user1, order, LocalDate.now(clock), SORTING_CENTER_ID);

        assertThatThrownBy(
                () -> transactionTemplate.execute(
                        tt -> {
                            userShiftReassignManager.assignOrders(userShift1.getId(), List.of(order.getId()));
                            return null;
                        }))
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void reassignAfterUserFinishPickupWithCallTasks() {
        transactionTemplate.execute(status -> {
            userPropertyService.addPropertyToUser(user1, UserProperties.CALL_TO_RECIPIENT_ENABLED, true);
            userPropertyService.addPropertyToUser(user2, UserProperties.CALL_TO_RECIPIENT_ENABLED, true);
            return null;
        });

        UserShift userShift1 = testUserHelper.createOpenedShift(user1, order, LocalDate.now(clock), SORTING_CENTER_ID);

        var order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().build());
        testUserHelper.createOpenedShift(user2, order2, LocalDate.now(clock), SORTING_CENTER_ID);
        testUserHelper.finishPickupAtStartOfTheDay(userShift1, true, true, false);

        RoutePoint pickupRoutePoint = userShift1.getCurrentRoutePoint();
        assertThat(pickupRoutePoint.getType()).isEqualTo(RoutePointType.ORDER_PICKUP);
        assertThat(pickupRoutePoint.getStatus().isTerminal()).isFalse();

        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user2.getId());

        pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePoint.getId());
        assertThat(pickupRoutePoint.getStatus().isTerminal()).isTrue();
    }

    @Test
    void reassignDuringReturn() {
        UserShift userShift = testUserHelper.createOpenedShift(user1, order, LocalDate.now(clock), SORTING_CENTER_ID);
        var orderToAssign = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());

        var returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        userShiftCommandService.switchOpenRoutePoint(userShift.getUser(), new UserShiftCommand.SwitchOpenRoutePoint(
                userShift.getId(),
                returnRoutePoint.getId(),
                true
        ));

        returnRoutePoint = routePointRepository.findByIdOrThrow(returnRoutePoint.getId());
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);

        userShiftReassignManager.reassignOrders(Set.of(orderToAssign.getId()), Set.of(), Set.of(),
                userShift.getUser().getId());
        var newRoutePoint = transactionTemplate.execute(
                status -> userShiftRepository.findByIdOrThrow(userShift.getId()).getCurrentRoutePoint()
        );
        returnRoutePoint = routePointRepository.findByIdOrThrow(returnRoutePoint.getId());

        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.NOT_STARTED);
        assertThat(newRoutePoint.getType()).isEqualTo(RoutePointType.DELIVERY);
        assertThat(newRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);

    }

    @Test
    void reassignDeliveryBySellerOrderFromForeverWorkingCourierToRealOne() {
        User user = testUserHelper.findOrCreateUser(100500L);
        Order order = transactionTemplate.execute(status -> {
            var generatedOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .deliveryServiceId(DELIVERY_SERVICE_ID)
                    .build());
            userPropertyService.addPropertyToUser(user, UserProperties.RETURN_TASK_CREATING_ENABLED, false);
            userPropertyService.addPropertyToUser(user, UserProperties.PICKUP_TASK_CREATING_ENABLED, false);
            return generatedOrder;
        });
        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user.getId());
        transactionTemplate.execute(status -> {
            UserShift userShift = userShiftRepository.findByUserIdAndActiveIsTrue(user.getId()).get();
            assertThat(userShift.streamPickupRoutePoints().count()).isEqualTo(0);
            assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(0);
            return null;
        });
    }

    @Test
    void reassignMovementsMultipleTimesTest() {
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        var user1 = testUserHelper.findOrCreateUser(111777L);
        var user2 = testUserHelper.findOrCreateUser(111888L);
        var user3 = testUserHelper.findOrCreateUser(111999L);
        var userShift1 = testUserHelper.createEmptyShift(user1, shift);
        var userShift2 = testUserHelper.createEmptyShift(user2, shift);
        var userShift3 = testUserHelper.createEmptyShift(user3, shift);
        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        var movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(deliveryServiceId)
                .deliveryIntervalFrom(shift.getShiftDate().atTime(LocalTime.of(14, 30)).toInstant(ZoneOffset.of("+03" +
                        ":00")))
                .deliveryIntervalFrom(shift.getShiftDate().atTime(LocalTime.of(14, 35)).toInstant(ZoneOffset.of("+03" +
                        ":00")))
                .build()
        );
        var task1 = testDataFactory.addDropshipTask(userShift1.getId(), movement);
        assertThat(task1.getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);

        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user2.getId());
        assertMovementReassigned(userShift1.getId(), userShift2.getId(), movement.getId());

        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user3.getId());
        assertMovementReassigned(userShift2.getId(), userShift3.getId(), movement.getId());
    }

    @Test
    void reassignCanceledMovement() {
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        var user1 = testUserHelper.findOrCreateUser(111777L);
        var userShift1 = testUserHelper.createEmptyShift(user1, shift);
        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        var movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(deliveryServiceId)
                .deliveryIntervalFrom(shift.getShiftDate().atTime(LocalTime.of(14, 30)).toInstant(ZoneOffset.of("+03" +
                        ":00")))
                .deliveryIntervalFrom(shift.getShiftDate().atTime(LocalTime.of(14, 35)).toInstant(ZoneOffset.of("+03" +
                        ":00")))
                .build()
        );

        movementCommandService.cancel(
                new MovementCommand.Cancel(
                        movement.getId(),
                        Source.OPERATOR,
                        ""
                )
        );

        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user1.getId());

        transactionTemplate.execute(status -> {
            Movement movementAfter = movementRepository.findByIdOrThrow(movement.getId());
            var userShiftFrom = userShiftRepository.findByIdOrThrow(userShift1.getId());
            assertThat(movementAfter.getStatus()).isEqualTo(MovementStatus.CREATED);
            var task = userShiftFrom.streamCollectDropshipTasks()
                    .findFirst(t -> t.getMovementId().equals(movement.getId()))
                    .orElseThrow();
            assertThat(task.getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
            return null;
        });
    }

    @Test
    void reassignReturnDropOffMovement() {
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        var user1 = testUserHelper.findOrCreateUser(111777L);
        var user2 = testUserHelper.findOrCreateUser(111888L);
        var user3 = testUserHelper.findOrCreateUser(111999L);
        var userShift1 = testUserHelper.createEmptyShift(user1, shift);
        var userShift2 = testUserHelper.createEmptyShift(user2, shift);
        var userShift3 = testUserHelper.createEmptyShift(user3, shift);
        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        var movement = testTplDropoffFactory.generateReturnMovement(shift, deliveryServiceId, pickupPoint);

        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShift1.getId(),
                        NewDeliveryRoutePointData.builder()
                                .cargoReference(
                                        CargoReference.builder()
                                                .movementId(movement.getId())
                                                .build()
                                )
                                .address(CollectDropshipTaskFactory.fromWarehouseAddress(movement.getWarehouseTo().getAddress()))
                                .name(movement.getWarehouseTo().getAddress().getAddress())
                                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .pickupPointId(pickupPoint.getId())
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user2.getId());
        assertDropOffMovementReassigned(userShift1.getId(), userShift2.getId(), movement.getId(), true);

        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user3.getId());
        assertDropOffMovementReassigned(userShift2.getId(), userShift3.getId(), movement.getId(), true);

        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user1.getId());
        assertDropOffMovementReassigned(userShift3.getId(), userShift1.getId(), movement.getId(), true);
    }

    @Test
    void reassignDirectDropOffMovement() {
        //given
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        var user1 = testUserHelper.findOrCreateUser(222777L);
        var user2 = testUserHelper.findOrCreateUser(222888L);
        var user3 = testUserHelper.findOrCreateUser(222999L);
        var userShift1 = testUserHelper.createEmptyShift(user1, shift);
        var userShift2 = testUserHelper.createEmptyShift(user2, shift);
        var userShift3 = testUserHelper.createEmptyShift(user3, shift);
        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        var movement = testTplDropoffFactory.generateDirectMovement(shift, deliveryServiceId, pickupPoint);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED,
                true
        );
        when(configurationProviderAdapter.getValueAsLongs(COURIER_WITH_ENABLED_LOT_FLOW_IDS))
                .thenReturn(Set.of(user1.getId(), user2.getId(), user3.getId()));

        testTplDropoffFactory.addDropoffTask(userShift1, movement, null, pickupPoint);

        //then
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user2.getId());
        assertDropOffMovementReassigned(userShift1.getId(), userShift2.getId(), movement.getId(), false);

        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user3.getId());
        assertDropOffMovementReassigned(userShift2.getId(), userShift3.getId(), movement.getId(), false);

        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user1.getId());
        assertDropOffMovementReassigned(userShift3.getId(), userShift1.getId(), movement.getId(), false);
    }

    @Test
    void reassignDirectDropOffMovement_fromNewToOldVersion() {
        //given
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        var user1 = testUserHelper.findOrCreateUser(222777L);
        var user2 = testUserHelper.findOrCreateUser(222888L);

        var userShift1 = testUserHelper.createEmptyShift(user1, shift);
        var userShift2 = testUserHelper.createEmptyShift(user2, shift);

        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        var movement = testTplDropoffFactory.generateDirectMovement(shift, deliveryServiceId, pickupPoint);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED,
                false
        );

        testTplDropoffFactory.addDropoffTask(userShift1, movement, null, pickupPoint);

        //when
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user2.getId());

        //then
        transactionTemplate.execute(status -> {
            Movement movementAfter = movementRepository.findByIdOrThrow(movement.getId());
            var userShiftFrom = userShiftRepository.findByIdOrThrow(userShift1.getId());

            assertThat(movementAfter.getStatus()).isEqualTo(MovementStatus.CREATED);
            var taskFrom = userShiftFrom.streamLockerDeliveryTasks()
                    .flatMap(LockerDeliveryTask::streamDropoffCargoSubtasks)
                    .findFirst(t -> t.getLockerSubtaskDropOff().getMovementId().equals(movement.getId()))
                    .map(LockerSubtask::getTask)
                    .orElseThrow();

            assertThat(taskFrom.getStatus()).isEqualTo(LockerDeliveryTaskStatus.CANCELLED);
            assertThat(taskFrom.getFailReason()).isNotNull();
            assertThat(taskFrom.getFailReason().getType()).isEqualTo(COURIER_REASSIGNED);

            var userShiftTo = userShiftRepository.findByIdOrThrow(userShift2.getId());
            var task = userShiftTo.streamCollectDropshipTasks()
                    .findFirst(t -> t.getMovementId().equals(movement.getId()))
                    .orElseThrow();
            assertThat(task.getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
            return null;
        });
    }

    @Test
    void reassignDirectDropOffMovement_whenScEnabled_andCourierEnabled() {
        //given
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        var user1 = testUserHelper.findOrCreateUser(222777L);
        var user2 = testUserHelper.findOrCreateUser(222888L);

        var userShift1 = testUserHelper.createEmptyShift(user1, shift);
        var userShift2 = testUserHelper.createEmptyShift(user2, shift);

        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        var movement = testTplDropoffFactory.generateDirectMovement(shift, deliveryServiceId, pickupPoint);

        testDataFactory.addDropshipTask(userShift1.getId(), movement);


        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED,
                true
        );
        when(configurationProviderAdapter.getValueAsLongs(COURIER_WITH_ENABLED_LOT_FLOW_IDS))
                .thenReturn(Set.of(user2.getId()));

        //when
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user2.getId());

        //then
        transactionTemplate.execute(status -> {
            Movement movementAfter = movementRepository.findByIdOrThrow(movement.getId());
            var userShiftFrom = userShiftRepository.findByIdOrThrow(userShift1.getId());
            var userShiftTo = userShiftRepository.findByIdOrThrow(userShift2.getId());
            assertThat(movementAfter.getStatus()).isEqualTo(MovementStatus.CREATED);

            var taskTo = userShiftTo.streamLockerDeliveryTasks()
                    .flatMap(LockerDeliveryTask::streamDropoffCargoSubtasks)
                    .findFirst(t -> t.getLockerSubtaskDropOff().getMovementId().equals(movement.getId()))
                    .map(LockerSubtask::getTask)
                    .orElseThrow();

            assertThat(taskTo.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);


            var task = userShiftFrom.streamCollectDropshipTasks()
                    .findFirst(t -> t.getMovementId().equals(movement.getId()))
                    .orElseThrow();
            assertThat(task.getStatus()).isEqualTo(CollectDropshipTaskStatus.CANCELLED);
            assertThat(task.getFailReason()).isNotNull();
            assertThat(task.getFailReason().getType()).isEqualTo(CollectDropshipFailReasonType.COURIER_REASSIGNED);
            return null;
        });
    }

    @Test
    void reassignDirectDropOffMovement_fromOldToOldVersion_andCourierDisabled() {
        //given
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        var user1 = testUserHelper.findOrCreateUser(222777L);
        var user2 = testUserHelper.findOrCreateUser(222888L);

        var userShift1 = testUserHelper.createEmptyShift(user1, shift);
        var userShift2 = testUserHelper.createEmptyShift(user2, shift);

        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        var movement = testTplDropoffFactory.generateDirectMovement(shift, deliveryServiceId, pickupPoint);

        testDataFactory.addDropshipTask(userShift1.getId(), movement);


        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED,
                true
        );
        long another = 1234567L;
        when(configurationProviderAdapter.getValueAsLongs(COURIER_WITH_ENABLED_LOT_FLOW_IDS))
                .thenReturn(Set.of(another));

        //when
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user2.getId());

        //then
        transactionTemplate.execute(status -> {
            Movement movementAfter = movementRepository.findByIdOrThrow(movement.getId());
            var userShiftFrom = userShiftRepository.findByIdOrThrow(userShift1.getId());

            assertThat(movementAfter.getStatus()).isEqualTo(MovementStatus.CREATED);
            var taskFrom = userShiftFrom.streamCollectDropshipTasks()
                    .findFirst(t -> t.getMovementId().equals(movement.getId()))
                    .orElseThrow();

            assertThat(taskFrom.getStatus()).isEqualTo(CollectDropshipTaskStatus.CANCELLED);
            assertThat(taskFrom.getFailReason()).isNotNull();
            assertThat(taskFrom.getFailReason().getType()).isEqualTo(CollectDropshipFailReasonType.COURIER_REASSIGNED);

            var userShiftTo = userShiftRepository.findByIdOrThrow(userShift2.getId());
            var task = userShiftTo.streamCollectDropshipTasks()
                    .findFirst(t -> t.getMovementId().equals(movement.getId()))
                    .orElseThrow();
            assertThat(task.getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
            return null;
        });
    }

    @Test
    void reassignDirectDropOffMovement_failWhenAlreadyPickup() {
        //given
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        var user1 = testUserHelper.findOrCreateUser(222777L);
        var user2 = testUserHelper.findOrCreateUser(222888L);
        var userShift1 = testUserHelper.createEmptyShift(user1, shift);
        var userShift2 = testUserHelper.createEmptyShift(user2, shift);
        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        var movement = testTplDropoffFactory.generateDirectMovement(shift, deliveryServiceId, pickupPoint);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED,
                true
        );
        when(configurationProviderAdapter.getValueAsLongs(COURIER_WITH_ENABLED_LOT_FLOW_IDS))
                .thenReturn(Set.of(user2.getId()));

        DropoffCargo cargo = dropoffCargoCommandService.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode("DROPOFF_CARGO_BARCODE")
                        .logisticPointIdFrom("fakeIdFrom")
                        .logisticPointIdTo("logisticPointTo")
                        .build());

        testTplDropoffFactory.addDropoffTask(userShift1, movement, null, pickupPoint);
        testTplDropoffFactory.addDropoffTask(userShift2, movement, cargo.getId(), pickupPoint);

        //then
        assertThrows(TplInvalidActionException.class,
                () -> userShiftReassignManager.reassignOrders(Set.of(), Set.of(),
                        Set.of(movement.getId()), user2.getId()));
    }

    @Test
    void reassignDirectDropOffMovement_mergeRoutePoint() {
        //given
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        var user1 = testUserHelper.findOrCreateUser(222777L);
        var user2 = testUserHelper.findOrCreateUser(222999L);
        var userShift1 = testUserHelper.createEmptyShift(user1, shift);
        var userShift2 = testUserHelper.createEmptyShift(user2, shift);
        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        var movement = testTplDropoffFactory.generateDirectMovement(shift, deliveryServiceId, pickupPoint);
        var movementReturn = testTplDropoffFactory.generateReturnMovement(shift, deliveryServiceId, pickupPoint);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED,
                true
        );
        when(configurationProviderAdapter.getValueAsLongs(COURIER_WITH_ENABLED_LOT_FLOW_IDS))
                .thenReturn(Set.of(user1.getId()));

        testTplDropoffFactory.addDropoffTask(userShift1, movementReturn, null, pickupPoint);
        testTplDropoffFactory.addDropoffTask(userShift2, movement, null, pickupPoint);

        //then
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), Set.of(movement.getId()), user1.getId());
        List<LockerSubtask> subtasks =
                transactionTemplate.execute(status -> userShiftRepository.findByIdOrThrow(userShift1.getId())
                        .streamLockerDeliveryTasks()
                        .flatMap(LockerDeliveryTask::streamSubtask)
                        .collect(Collectors.toList()));

        Set<LockerDeliveryTask> tasks = subtasks.stream().map(LockerSubtask::getTask).collect(Collectors.toSet());
        assertThat(tasks).hasSize(2);
        assertThat(tasks.stream().filter(ldt -> ldt.streamDropoffCargoSubtasks().count() == 1).findFirst()
                .orElseThrow().getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
    }

    private void assertMovementReassigned(long userShiftFromId, long userShiftToId, long movementId) {
        transactionTemplate.execute(status -> {
            var userShiftFrom = userShiftRepository.findByIdOrThrow(userShiftFromId);
            var userShiftTo = userShiftRepository.findByIdOrThrow(userShiftToId);
            var taskFrom = userShiftFrom.streamCollectDropshipTasks()
                    .findFirst(t -> t.getMovementId().equals(movementId))
                    .orElseThrow();
            var taskTo = userShiftTo.streamCollectDropshipTasks()
                    .findFirst(t -> t.getMovementId().equals(movementId))
                    .orElseThrow();
            assertThat(taskFrom.getStatus()).isEqualTo(CollectDropshipTaskStatus.CANCELLED);
            assertThat(taskFrom.getFailReason()).isNotNull();
            assertThat(taskFrom.getFailReason().getType()).isEqualTo(CollectDropshipFailReasonType.COURIER_REASSIGNED);
            assertThat(taskTo.getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
            return null;
        });
    }

    private void assertDropOffMovementReassigned(long userShiftFromId, long userShiftToId, long movementId,
                                                 boolean isReturn) {
        transactionTemplate.execute(status -> {
            var userShiftFrom = userShiftRepository.findByIdOrThrow(userShiftFromId);
            var userShiftTo = userShiftRepository.findByIdOrThrow(userShiftToId);
            var taskFrom = userShiftFrom.streamLockerDeliveryTasks()
                    .flatMap(ldt -> StreamEx.of(isReturn ?
                                    ldt.streamDropOffReturnSubtasks() :
                                    ldt.streamDropoffCargoSubtasks()
                            )
                    )
                    .findFirst(t -> t.getLockerSubtaskDropOff().getMovementId().equals(movementId))
                    .orElseThrow();
            var taskTo = userShiftTo.streamLockerDeliveryTasks()
                    .flatMap(ldt -> StreamEx.of(isReturn ?
                            ldt.streamDropOffReturnSubtasks() :
                            ldt.streamDropoffCargoSubtasks()
                    ))
                    .findFirst(t -> t.getLockerSubtaskDropOff().getMovementId().equals(movementId))
                    .orElseThrow();
            assertThat(taskFrom.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
            assertThat(taskFrom.getFailReason()).isNotNull();
            assertThat(taskFrom.getFailReason().getType()).isEqualTo(COURIER_REASSIGNED);
            assertThat(taskTo.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);
            return null;
        });
    }

}
