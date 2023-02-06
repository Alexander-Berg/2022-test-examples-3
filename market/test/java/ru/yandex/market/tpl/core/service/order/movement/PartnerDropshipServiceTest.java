package ru.yandex.market.tpl.core.service.order.movement;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderDeliveryDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTaskFactory;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.core.domain.movement.Movement.TAG_DROPOFF_CARGO_RETURN;


@RequiredArgsConstructor
class PartnerDropshipServiceTest extends TplAbstractTest {

    public static final long NOT_EXISTS_MOVEMENT_ID = -777L;
    private final PartnerDropshipService partnerDropshipService;

    private final TestUserHelper userHelper;
    private final MovementGenerator movementGenerator;
    private final UserShiftCommandDataHelper helper;

    private final UserShiftCommandService commandService;
    private final Clock clock;

    private final UserShiftCommandService userShiftCommandService;
    private final PickupPointRepository pickupPointRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserShiftRepository userShiftRepository;
    private final CollectDropshipTaskFactory collectDropshipTaskFactory;
    private final TestDataFactory testDataFactory;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TransactionTemplate transactionTemplate;

    private User user;
    private Movement movement;
    private PickupPoint pickupPoint;
    private Movement movementDirect;
    private Long userShiftId;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getDropshipsInfoByExternalId(boolean isDropOffMovement) {
        init(isDropOffMovement);

        PartnerOrderDeliveryDto dropshipsDto =
                partnerDropshipService.getDropshipsInfoByExternalId(movement.getExternalId());
        assertThat(dropshipsDto).isNotNull();
        assertThat(dropshipsDto.getOrder().getOrderType())
                .isEqualTo(PartnerOrderType.DROPSHIP);
        assertThat(dropshipsDto.getDelivery().getLastDelivery().getDropshipStatus())
                .isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
    }

    @Test
    void shouldCorrectCancel_collectDropshipTask() {
        //given
        setUpDropoffShift();

        var addCollectDropshipTaskCommand = collectDropshipTaskFactory.createAddCollectDrophipTask(
                userShiftRepository.findByIdOrThrow(userShiftId),
                movementDirect,
                Instant.now(clock)
        );
        userShiftCommandService.addCollectDropshipTask(user, addCollectDropshipTaskCommand);

        //when
        String dropshipComment = "cancel Dropship Comment";
        partnerDropshipService.cancelTask(new PartnerkaCommand.CancelMovementExecutionTask(
                movementDirect.getId(), dropshipComment));

        //then
        List<CollectDropshipTask> dropshipTasks =
                transactionTemplate.execute(st -> userShiftRepository.findByIdOrThrow(userShiftId)
                        .streamCollectDropshipTasks()
                        .collect(Collectors.toList()));

        assertThat(dropshipTasks).hasSize(1);
        assertThat(dropshipTasks.get(0).getStatus()).isEqualTo(CollectDropshipTaskStatus.CANCELLED);
        assertThat(dropshipTasks.get(0).getFailReason()).isNotNull();
        assertThat(dropshipTasks.get(0).getFailReason().getComment()).isEqualTo(dropshipComment);
    }

    @Test
    void shouldCorrectReopen_collectDropshipTask() {
        //given
        setUpDropoffShift();

        var addCollectDropshipTaskCommand = collectDropshipTaskFactory.createAddCollectDrophipTask(
                userShiftRepository.findByIdOrThrow(userShiftId),
                movementDirect,
                Instant.now(clock)
        );
        userShiftCommandService.addCollectDropshipTask(user, addCollectDropshipTaskCommand);

        String dropshipComment = "cancel Dropship Comment";
        partnerDropshipService.cancelTask(new PartnerkaCommand.CancelMovementExecutionTask(
                movementDirect.getId(), dropshipComment));
        //when
        partnerDropshipService.reopenTask(new PartnerkaCommand.ReopenMovementExecutionTask(
                movementDirect.getId()));

        //then
        List<CollectDropshipTask> dropshipTasks =
                transactionTemplate.execute(st -> userShiftRepository.findByIdOrThrow(userShiftId)
                        .streamCollectDropshipTasks()
                        .collect(Collectors.toList()));

        assertThat(dropshipTasks).hasSize(1);
        assertThat(dropshipTasks.get(0).getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
        assertThat(dropshipTasks.get(0).getFailReason()).isNull();
    }

    @Test
    void shouldCorrectCancel_lockerDeliveryTask() {
        //given
        setUpDropoffShift();

        addDropoffTask(userShiftId, movementDirect, null);

        //when
        String dropshipComment = "cancel Dropship Comment";
        partnerDropshipService.cancelTask(new PartnerkaCommand.CancelMovementExecutionTask(
                movementDirect.getId(), dropshipComment));

        //then

        transactionTemplate.execute(st -> {
            List<LockerDeliveryTask> lockerDeliveryTasks = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamLockerDeliveryTasks()
                    .collect(Collectors.toList());

            assertThat(lockerDeliveryTasks).hasSize(1);
            assertThat(lockerDeliveryTasks.get(0).getStatus()).isEqualTo(LockerDeliveryTaskStatus.CANCELLED);
            assertThat(lockerDeliveryTasks.get(0).getFailReason()).isNotNull();
            assertThat(lockerDeliveryTasks.get(0).getFailReason().getComment()).isEqualTo(dropshipComment);
            assertThat(lockerDeliveryTasks.get(0).getSubtasks()).hasSize(1);
            assertThat(lockerDeliveryTasks.get(0).getSubtasks().get(0).getStatus())
                    .isEqualTo(LockerDeliverySubtaskStatus.FAILED);

            return null;
        });
    }


    @Test
    void shouldCorrectReopen_lockerDeliveryTask() {
        //given
        setUpDropoffShift();

        addDropoffTask(userShiftId, movementDirect, null);

        String dropshipComment = "cancel Dropship Comment";
        partnerDropshipService.cancelTask(new PartnerkaCommand.CancelMovementExecutionTask(
                movementDirect.getId(), dropshipComment));
        //when
        partnerDropshipService.reopenTask(new PartnerkaCommand.ReopenMovementExecutionTask(
                movementDirect.getId())
        );
        //then


        transactionTemplate.execute(st -> {
            List<LockerDeliveryTask> lockerDeliveryTasks = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamLockerDeliveryTasks()
                    .collect(Collectors.toList());

            assertThat(lockerDeliveryTasks).hasSize(1);
            assertThat(lockerDeliveryTasks.get(0).getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
            assertThat(lockerDeliveryTasks.get(0).getFailReason()).isNull();
            assertThat(lockerDeliveryTasks.get(0).getSubtasks()).hasSize(1);
            assertThat(lockerDeliveryTasks.get(0).getSubtasks().get(0).getStatus())
                    .isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);

            return null;
        });
    }

    @Test
    void shouldThrowsException_whenMovementNotExists() {
        //given
        setUpDropoffShift();

        var addCollectDropshipTaskCommand = collectDropshipTaskFactory.createAddCollectDrophipTask(
                userShiftRepository.findByIdOrThrow(userShiftId),
                movementDirect,
                Instant.now(clock)
        );
        userShiftCommandService.addCollectDropshipTask(user, addCollectDropshipTaskCommand);

        String dropshipComment = "cancel Dropship Comment";

        //then
        assertThrows(TplEntityNotFoundException.class,
                () -> partnerDropshipService.cancelTask(new PartnerkaCommand.CancelMovementExecutionTask(
                        PartnerDropshipServiceTest.NOT_EXISTS_MOVEMENT_ID, dropshipComment)));
    }

    @Test
    void shouldThrowsException_whenReopenMovementNotExists() {
        //given
        setUpDropoffShift();

        var addCollectDropshipTaskCommand = collectDropshipTaskFactory.createAddCollectDrophipTask(
                userShiftRepository.findByIdOrThrow(userShiftId),
                movementDirect,
                Instant.now(clock)
        );
        userShiftCommandService.addCollectDropshipTask(user, addCollectDropshipTaskCommand);

        //then
        assertThrows(TplEntityNotFoundException.class,
                () -> partnerDropshipService.reopenTask(new PartnerkaCommand.ReopenMovementExecutionTask(
                        PartnerDropshipServiceTest.NOT_EXISTS_MOVEMENT_ID)));
    }

    private DeliveryTask addDropoffTask(Long usId, Movement movement, Long cargoId) {
        return userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        usId,
                        NewDeliveryRoutePointData.builder()
                                .cargoReference(
                                        CargoReference.builder()
                                                .movementId(movement.getId())
                                                .dropOffCargoId(cargoId)
                                                .isReturn(movement.isDropOffReturn())
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
    }

    void setUpDropoffShift() {
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );

        clearAfterTest(pickupPoint);

        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        movementDirect = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .orderWarehouse(
                                orderWarehouseGenerator.generateWarehouse(
                                        wh -> wh.setYandexId(pickupPoint.getLogisticPointId().toString())
                                )
                        )
                        .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .build();

        userShiftId = userShiftCommandService.createUserShift(createCommand);
    }

    void init(boolean isDropOffMovement) {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(-1L)
                .tags(isDropOffMovement ? List.of(TAG_DROPOFF_CARGO_RETURN) : List.of())
                .build());
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(isDropOffMovement ? helper.taskDropOffReturn(movement.getId()) :
                        helper.taskCollectDropship(LocalDate.now(clock), movement)).build();
        commandService.createUserShift(createCommand);
    }
}
