package ru.yandex.market.tpl.core.domain.order.listener;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketDeliverySuccessNotifyDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTaskFactory;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.TplTestCargoFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.REVERT_DELIVERED_ORDERS_WHEN_CANCEL_LOCKER_TASK;

@RequiredArgsConstructor
public class OnLockerDeliverySubtaskFailedEventTest extends TplAbstractTest {

    private final TransactionTemplate transactionTemplate;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final MovementGenerator movementGenerator;
    private final UserShiftRepository userShiftRepository;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;

    private final UserShiftCommandService userShiftCommandService;
    private final TplTestCargoFactory tplTestCargoFactory;
    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final LockerDeliveryService service;
    private final ConfigurationService configurationService;

    private PickupPoint pickupPoint;
    private Movement movement;
    private Order pvzOrder;
    private Long userShiftId;
    private User user;

    @BeforeEach
    void setUp() {
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );
        clearAfterTest(pickupPoint);

        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        movement = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .orderWarehouseTo(
                                orderWarehouseGenerator.generateWarehouse(
                                        wh -> wh.setYandexId(pickupPoint.getLogisticPointId().toString())
                                )
                        )
                        .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                        .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .build();

        userShiftId = userShiftCommandService.createUserShift(createCommand);

        pvzOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());

        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .orderReference(OrderReference.fromSources(pvzOrder, false))
                                .address(CollectDropshipTaskFactory.fromWarehouseAddress(movement.getWarehouseTo().getAddress()))
                                .name(movement.getWarehouseTo().getAddress().getAddress())
                                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .pickupPointId(pickupPoint.getId())
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void revertDeliveredOrder(boolean revertEnabled) {
        //given
        configurationService.mergeValue(REVERT_DELIVERED_ORDERS_WHEN_CANCEL_LOCKER_TASK.getName(), revertEnabled);

        tplTestCargoFactory.initPickupCargoFlow(
                TplTestCargoFactory.CargoPickupContext.of(
                        List.of(pvzOrder.getId()),
                        Set.of(), Set.of()),
                TplTestCargoFactory.ShiftContext.of(user, userShiftId)
        );

        Optional<RoutePoint> rpO =
                transactionTemplate.execute(st -> userShiftRepository.findByIdOrThrow(userShiftId)
                        .streamDeliveryRoutePoints().filter(rp -> rp.getType() == RoutePointType.LOCKER_DELIVERY).findFirst());
        RoutePoint routePoint = rpO.get();
        userHelper.arriveAtRoutePoint(routePoint);

        LockerDeliveryTask lockerDeliveryTask =
                transactionTemplate.execute(st -> userShiftRepository.findByIdOrThrow(userShiftId)
                        .streamLockerDeliveryTasks().findFirst().orElseThrow());

        MarketDeliverySuccessNotifyDto successNotifyDto = MarketDeliverySuccessNotifyDto.builder()
                .barcode("barcode")
                .taskId(lockerDeliveryTask.getId())
                .idShipmentItem(1)
                .idStage(1)
                .idPostamatCell(12)
                .externalOrderId(pvzOrder.getExternalOrderId())
                .build();
        service.deliverySuccess("token", successNotifyDto, user);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(), null, ScanRequest.builder()
                        .successfullyScannedOrders(List.of(pvzOrder.getId()))
                        .successfullyScannedDropoffCargos(Set.of())
                        .build()));

        //Проверка что заказ доставлен
        Order deliveredOrder = orderRepository.findByIdOrThrow(pvzOrder.getId());
        assertThat(deliveredOrder.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.DELIVERED);
        assertThat(deliveredOrder.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);

        //when
        UserShiftCommand.FailOrderDeliveryTask command = getFailOrderDeliveryTaskCommand();
        transactionTemplate.execute(tt -> {
            userShiftCommandService.failDeliveryTask(user, command);
            return 0;
        });

        //then
        assertThatLockerSubtasksFailed();

        Order revertedOrder = orderRepository.findByIdOrThrow(pvzOrder.getId());
        if (revertEnabled) {
            assertThat(revertedOrder.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
            assertThat(revertedOrder.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        } else {
            assertThat(revertedOrder.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.DELIVERED);
            assertThat(revertedOrder.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        }

    }

    private void assertThatLockerSubtasksFailed() {
        transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();

            assertThat(lockerDeliveryTask.getSubtasks())
                    .extracting(LockerSubtask::getStatus)
                    .containsExactly(LockerDeliverySubtaskStatus.FAILED);

            assertThat(lockerDeliveryTask.getSubtasks())
                    .extracting(LockerSubtask::getFailReason)
                    .extracting(OrderDeliveryFailReason::getType)
                    .containsExactly(OrderDeliveryTaskFailReasonType.PVZ_CLOSED);

            return null;
        });
    }

    private UserShiftCommand.FailOrderDeliveryTask getFailOrderDeliveryTaskCommand() {
        return transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            return new UserShiftCommand.FailOrderDeliveryTask(
                    userShiftId,
                    routePoint.getId(),
                    lockerDeliveryTask.getId(),
                    new OrderDeliveryFailReason(
                            OrderDeliveryTaskFailReasonType.PVZ_CLOSED,
                            "Никого нет дома",
                            List.of(),
                            Source.COURIER
                    )
            );
        });
    }
}
