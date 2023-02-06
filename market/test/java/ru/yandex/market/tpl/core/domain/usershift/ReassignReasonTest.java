package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.dropship.CollectDropshipFailReason;
import ru.yandex.market.tpl.core.domain.usershift.dropship.CollectDropshipFailReasonType;
import ru.yandex.market.tpl.core.domain.usershift.reassignment.ReassignItemType;
import ru.yandex.market.tpl.core.domain.usershift.reassignment.ReassignmentManager;
import ru.yandex.market.tpl.core.domain.usershift.reassignment.ReassignmentRepository;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.ORDER_NOT_ACCEPTED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.WRONG_COORDINATES;

@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReassignReasonTest extends TplAbstractTest {
    private final ReassignmentRepository reassignmentRepository;
    private final ReassignmentManager reassignmentManager;
    private final OrderGenerateService orderGenerateService;
    private final MovementGenerator movementGenerator;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final ClientReturnGenerator clientReturnGenerator;


    private List<Order> orders;
    private List<Movement> movements;
    private List<ClientReturn> clientReturns;
    private UserShift userShift;
    private User user;

    private final Long courierId = 123456L;

    @BeforeEach
    void init() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.REASSIGN_REASONS_ENABLED, true);
        orders = Stream.generate(() -> orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .build()
        )).limit(3).collect(Collectors.toList());

        movements = Stream.generate(() -> movementGenerator.generate(
                MovementCommand.Create.builder()
                        .build()
        )).limit(3).collect(Collectors.toList());

        clientReturns = Stream.generate(clientReturnGenerator::generateReturnFromClient)
                .limit(3)
                .collect(Collectors.toList());

        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        user = testUserHelper.findOrCreateUser(3522236L);
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(Instant.now()))
                .routePoint(helper.taskPrepaid("addr1", 12, orders.get(0).getId()))
                .routePoint(helper.taskPrepaid("addr2", 13, orders.get(1).getId()))
                .routePoint(helper.taskPrepaid("addr3", 12, orders.get(2).getId()))
                .routePoint(helper.taskCollectDropship(LocalDate.now(), movements.get(0)))
                .routePoint(helper.taskCollectDropship(LocalDate.now(), movements.get(1)))
                .routePoint(helper.clientReturn("Addr4", 15, clientReturns.get(0).getId()))
                .routePoint(helper.clientReturn("Addr5", 16, clientReturns.get(1).getId()))
                .routePoint(helper.clientReturn("Addr6", 16, clientReturns.get(2).getId()))
                .routePoint(helper.taskDropOffReturn(movements.get(2).getId(), pickupPoint.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShift = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        testUserHelper.checkinAndFinishPickup(userShift);
    }

    @Test
    void shouldCreateOrdersAndMovementsReasons() {
        reassignmentManager.addReason(orders, movements, List.of(), courierId, "ABSENTEEISM");
        var reassignments = reassignmentRepository.findAll();
        assertThat(reassignments.size()).isEqualTo(6);
    }

    @Test
    void shouldCreateOrdersAndMovementsAndClientRetunrnReasons() {
        reassignmentManager.addReason(orders, movements, clientReturns, courierId, "ABSENTEEISM");
        var reassignments = reassignmentRepository.findAll();
        assertThat(reassignments.size()).isEqualTo(9);
    }

    @Test
    void shouldReturnMessage() {
        var msgVal = "Невыход";
        var msg = reassignmentManager.addReason(orders, movements, clientReturns, courierId, "ABSENTEEISM");
        assertThat(msg).isEqualTo(msgVal);
    }

    @Test
    void shouldCreateOrdersReasons() {
        reassignmentManager.addReason(orders, List.of(), List.of(), courierId, "ABSENTEEISM");
        var reassignments = reassignmentRepository.findAll();
        assertThat(reassignments.size()).isEqualTo(3);
        assertThat(reassignments.get(0).getItemType()).isEqualTo(ReassignItemType.ORDER);
    }

    @Test
    void shouldCreateMovementsReasons() {
        reassignmentManager.addReason(List.of(), movements, List.of(), courierId, "ABSENTEEISM");
        var reassignments = reassignmentRepository.findAll();
        assertThat(reassignments.size()).isEqualTo(3);
        assertThat(reassignments.get(0).getItemType()).isEqualTo(ReassignItemType.MOVEMENT);
    }

    @Test
    void shouldCreateClientReturnReasons() {
        reassignmentManager.addReason(List.of(), List.of(), clientReturns, courierId, "ABSENTEEISM");
        var reassignments = reassignmentRepository.findAll();
        assertThat(reassignments.size()).isEqualTo(3);
        assertThat(reassignments.get(0).getItemType()).isEqualTo(ReassignItemType.CLIENT_RETURN);
    }

    @Test
    void ShouldDoNothingWhenReasonKeyEmpty() {
        reassignmentManager.addReason(orders, movements, clientReturns, courierId, null);
        var reassignments = reassignmentRepository.findAll();
        assertThat(reassignments.size()).isEqualTo(0);
    }

    @Test
    void shouldReassignOrderWhenFailTask() {

        userShift.streamOrderDeliveryTasks()
                .remove(OrderDeliveryTask::isClientReturn)
                .toList()
                .forEach(t -> commandService.failDeliveryTask(
                        user,
                        new UserShiftCommand.FailOrderDeliveryTask(
                                userShift.getId(),
                                t.getRoutePoint().getId(),
                                t.getId(),
                                new OrderDeliveryFailReason(
                                        ORDER_NOT_ACCEPTED,
                                        "comment",
                                        null,
                                        Source.COURIER)
                        )));
        reassignmentManager.addReason(orders, List.of(), List.of(), courierId, "ABSENTEEISM");
        var reassignments = reassignmentRepository.findAll();
        assertThat(reassignments.size()).isEqualTo(3);
        assertThat(reassignments.get(0).getItemType()).isEqualTo(ReassignItemType.ORDER);
    }

    @Test
    void shoulfReassignClientReturnWhenFailTask() {
        userShift.streamOrderDeliveryTasks()
                .filter(OrderDeliveryTask::isClientReturn)
                .toList()
                .forEach(t -> commandService.failDeliveryTask(
                        user,
                        new UserShiftCommand.FailOrderDeliveryTask(
                                userShift.getId(),
                                t.getRoutePoint().getId(),
                                t.getId(),
                                new OrderDeliveryFailReason(
                                        WRONG_COORDINATES,
                                        "comment",
                                        null,
                                        Source.COURIER)
                        )));
        reassignmentManager.addReason(List.of(), List.of(), clientReturns, courierId, "ABSENTEEISM");
        var reassignments = reassignmentRepository.findAll();
        assertThat(reassignments.size()).isEqualTo(3);
        assertThat(reassignments.get(0).getItemType()).isEqualTo(ReassignItemType.CLIENT_RETURN);

    }

    @Test
    void shouldReassignMovementsWhenFailTask() {

        userShift.streamCollectDropshipTasks()
                .toList()
                .forEach(t -> commandService.cancelDropships(
                        user,
                        new UserShiftCommand.CancelDropships(
                                userShift.getId(),
                                t.getRoutePoint().getId(),
                                t.getId(),
                                new CollectDropshipFailReason(CollectDropshipFailReasonType.COURIER_NEEDS_HELP,
                                        null,
                                        Source.COURIER
                                )
                        )));
        reassignmentManager.addReason(List.of(), movements, List.of(), courierId, "ABSENTEEISM");
        var reassignments = reassignmentRepository.findAll();
        assertThat(reassignments.size()).isEqualTo(3);
        assertThat(reassignments.get(0).getItemType()).isEqualTo(ReassignItemType.MOVEMENT);
    }

    @Test
    void shouldReassignUnassignedItem() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .build());

        var movement = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .build());

        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        reassignmentManager.addReason(List.of(order), List.of(movement), List.of(clientReturn), courierId,
                "ABSENTEEISM");
        var reassignments = reassignmentRepository.findAll();
        assertThat(reassignments.size()).isEqualTo(3);
    }

}
