package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.usershift.LockerNativeFlowDeliveryTest.EXTERNAL_ORDER_ID_1;

@RequiredArgsConstructor
public class ReassignOrdersServiceTest extends TplAbstractTest {

    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TransactionTemplate transactionTemplate;
    private final LockerOrderDataHelper lockerOrderDataHelper;
    private final SortingCenterService sortingCenterService;
    private final MovementGenerator movementGenerator;
    private final UserShiftRepository repository;
    private final TestUserHelper userHelper;


    private UserShift userShiftA;
    private UserShift userShiftB;
    private UserShift userShiftC;
    private User userA;
    private User userB;
    private User userC;
    private Shift shift;
    private Long userShiftId;
    private Order orderLocker;
    private PickupPoint pickupPoint;
    private Movement movement;


    private final Clock clock;

    @BeforeEach
    void init() {
        transactionTemplate.execute(t -> {
                    ClockUtil.initFixed(clock);
                    userA = testUserHelper.findOrCreateUser(2L);
                    userB = testUserHelper.findOrCreateUser(1L);
                    userC = testUserHelper.findOrCreateUser(3L);
                    shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                            sortingCenterService.findSortCenterForDs(239).getId());
                    userShiftId = testDataFactory.createEmptyShift(shift.getId(), userA);
                    userShiftA = userShiftRepository.findByIdOrThrow(userShiftId);
                    pickupPoint = pickupPointRepository.save(
                            testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
                    GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

                    orderLocker = lockerOrderDataHelper.getPickupOrder(
                            shift, EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint,
                            OrderFlowStatus.SORTING_CENTER_PREPARED, 2);
                    userShiftReassignManager.assign(userShiftA, orderLocker);

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

                    movement = movementGenerator.generate(MovementCommand.Create.builder()
                            .deliveryServiceId(239L)
                            .build());
                    var createCommand = UserShiftCommand.Create.builder()
                            .userId(userC.getId())
                            .shiftId(shift.getId())
                            .routePoint(helper.taskCollectDropship(LocalDate.now(clock), movement)).build();

                    long id = commandService.createUserShift(createCommand);
                    userShiftC = repository.findById(id).orElseThrow();
                    userHelper.checkinAndFinishPickup(userShiftA);
                    return null;
                }
        );
    }

    @Test
    public void reassignOrdersWithMovements() {
        Set<Long> movementIds = transactionTemplate.execute(t -> {
            userShiftC = userShiftRepository.findById(userShiftC.getId()).orElseThrow();
            return userShiftC.streamCollectDropshipTasks()
                    .map(CollectDropshipTask::getMovementId)
                    .collect(Collectors.toSet());
        });
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(), movementIds, userShiftB.getUser().getId());

        Long count = transactionTemplate.execute(t -> {
                    userShiftB = userShiftRepository.findById(userShiftB.getId()).orElseThrow();
                    return userShiftB.streamCollectDropshipTasks().count();
                }
        );
        assertThat(count).isEqualTo(movementIds.size());
    }


    @Test
    public void reassignOrders() {
        Set<Long> orderIds = transactionTemplate.execute(t -> {
            userShiftA = userShiftRepository.findById(userShiftId).orElseThrow();
            return userShiftA.streamLockerDeliveryTasks()
                    .flatMap(LockerDeliveryTask::streamSubtask)
                    .map(LockerSubtask::getOrderId)
                    .collect(Collectors.toSet());
        });
        userShiftReassignManager.reassignOrders(orderIds, Set.of(), Set.of(), userShiftB.getUser().getId());

        Long count = transactionTemplate.execute(t -> {
                    userShiftB = userShiftRepository.findById(userShiftB.getId()).orElseThrow();
                    return userShiftB.streamDeliveryTasks().count();
                }
        );
        assertThat(count).isEqualTo(orderIds.size());
    }


}
