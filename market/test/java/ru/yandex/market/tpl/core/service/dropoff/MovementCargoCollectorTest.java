package ru.yandex.market.tpl.core.service.dropoff;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoQueryService;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CARGO_DROPOFF_DIRECT_FLOW_ENABLED;

@RequiredArgsConstructor
class MovementCargoCollectorTest extends TplAbstractTest {

    public static final String LOGISTICPOINT_ID_FOR_PICKUP_DROPOFF = "1234567";
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final TransactionTemplate transactionTemplate;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandService commandService;
    private final MovementGenerator movementGenerator;
    private final UserShiftCommandDataHelper helper;
    private final DropoffCargoCommandService dropoffCargoCommandService;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;
    private final MovementCargoCollector movementCargoCollector;
    private final UserShiftRepository userShiftRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final DropoffCargoQueryService dropoffCargoQueryService;
    private User user;
    private Movement movementDropoff;

    @BeforeEach
    void setUp() {
        when(configurationProviderAdapter.isBooleanEnabled(CARGO_DROPOFF_DIRECT_FLOW_ENABLED)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        reset(configurationProviderAdapter);
    }

    @Test
    void collectDropoffCargoByPvz() {
        //given
        Long userShiftId = buildUserShiftWithDropoffTask();
        String barcode = "barcode";
        buildAndSaveCargo(barcode);

        //when
        var dropoffCargos = movementCargoCollector.collectDropoffCargoForCurrentUserShiftPoint(userShiftId);

        //then
        assertThat(dropoffCargos).hasSize(1);
        assertThat(dropoffCargos.get(0).getBarcode()).isEqualTo(barcode);
        assertThat(dropoffCargos.get(0).getLogisticPointIdFrom()).isEqualTo(LOGISTICPOINT_ID_FOR_PICKUP_DROPOFF);
    }

    @Test
    void collectDropoffCargoByPvz_forRoutePoint() {
        //given
        Long userShiftId = buildUserShiftWithDropoffTask();
        UserShift userShift = userShiftRepository.findById(userShiftId).orElseThrow();
        String barcode = "barcode";
        buildAndSaveCargo(barcode);

        //when
        var dropoffCargos = dropoffCargoQueryService.getDropoffForPickupByRoutePointId(userShift.getCurrentRoutePoint()
                .getId());

        //then
        assertThat(dropoffCargos).hasSize(1);
        assertThat(dropoffCargos.get(0).getBarcode()).isEqualTo(barcode);
        assertThat(dropoffCargos.get(0).getLogisticPointIdFrom()).isEqualTo(LOGISTICPOINT_ID_FOR_PICKUP_DROPOFF);
    }

    @Test
    void collectDropoffCargoByPvz_forRoutePointWhenDeleted() {
        //given
        Long userShiftId = buildUserShiftWithDropoffTask();
        UserShift userShift = userShiftRepository.findById(userShiftId).orElseThrow();
        String barcode = "barcode";
        var cargo = buildAndSaveCargo(barcode);
        dropoffCargoCommandService.markDeleted(DropoffCargoCommand.UpdateOrSkipDeleted.of(
                cargo.getId(), 1L, true
        ));
        //when
        var dropoffCargos = dropoffCargoQueryService.getDropoffForPickupByRoutePointId(userShift.getCurrentRoutePoint()
                .getId());

        //then
        assertThat(dropoffCargos).isEmpty();
    }

    @Test
    void collectDropoffCargo() {
        //given
        Long userShiftId = buildUserShiftWithDropoffTask();
        String barcode1 = "barcode";
        String barcode2 = "barcode2";
        Set<DropoffCargo> cargos = Stream.of(barcode1, barcode2)
                .map(this::buildAndSaveCargo)
                .collect(Collectors.toSet());

        arriveAndFinishUnloading(userShiftId, cargos);

        //when
        Map<Long, List<DropoffCargo>> cargoMap = transactionTemplate.execute(st ->
                movementCargoCollector.collectDirectCargosMap(userShiftRepository.findByIdOrThrow(userShiftId)));


        //then
        assertThat(cargoMap).hasSize(1);
        assertThat(cargoMap.containsKey(movementDropoff.getId())).isTrue();
        assertThat(cargoMap.get(movementDropoff.getId())).containsExactlyElementsOf(cargos);

    }

    @Test
    void streamAcceptedDirectCargosIds() {
        //given
        Long userShiftId = buildUserShiftWithDropoffTask();
        String barcode1 = "barcode";
        String barcode2 = "barcode2";
        Set<DropoffCargo> cargos = Stream.of(barcode1, barcode2)
                .map(this::buildAndSaveCargo)
                .collect(Collectors.toSet());

        arriveAndFinishUnloading(userShiftId, cargos);

        //then
        assertThat(transactionTemplate.<Set<Long>>execute(st ->
                movementCargoCollector.streamAcceptedDirectCargosIds(userShiftRepository.findByIdOrThrow(userShiftId))
                        .collect(Collectors.toSet()))).containsExactlyElementsOf(
                cargos.stream().map(DropoffCargo::getId).collect(Collectors.toSet()));
    }

    private DropoffCargo buildAndSaveCargo(String barcode) {
        return dropoffCargoCommandService.createOrGet(DropoffCargoCommand.Create.builder()
                .barcode(barcode)
                .logisticPointIdFrom(LOGISTICPOINT_ID_FOR_PICKUP_DROPOFF)
                .logisticPointIdTo("to")
                .build());
    }

    private void arriveAndFinishUnloading(Long userShiftId, Set<DropoffCargo> cargos) {
        RoutePoint lockerRoutePoint = transactionTemplate.execute(st ->
                userShiftRepository.findByIdOrThrow(userShiftId).getCurrentRoutePoint());

        assertThat(lockerRoutePoint.getType()).isEqualTo(RoutePointType.LOCKER_DELIVERY);
        LockerDeliveryTask lockerDeliveryTask = lockerRoutePoint.streamLockerDeliveryTasks().findFirst().orElseThrow();

        testUserHelper.arriveAtRoutePoint(lockerRoutePoint);

        commandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(
                        userShiftId,
                        lockerRoutePoint.getId(),
                        lockerDeliveryTask.getId(),
                        null,
                        false,
                        ScanRequest.builder().build()
                )
        );

        Iterator<DropoffCargo> iterator = cargos.iterator();
        var barcode1 = iterator.next().getBarcode();
        var barcode2 = iterator.next().getBarcode();

        commandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        userShiftId,
                        lockerRoutePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(
                                new UnloadedOrder(barcode1, null, List.of(barcode1)),
                                new UnloadedOrder(barcode2, null, List.of(barcode2))
                        )
                )
        );
    }

    @Nullable
    private Long buildUserShiftWithDropoffTask() {
        user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        movementDropoff = movementGenerator.generate(buildCreateMovmentCommand());

        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                        Long.parseLong(LOGISTICPOINT_ID_FOR_PICKUP_DROPOFF),
                        1L)
        );
        var createCommand = buildCreateUserShiftCommand(shift, user, movementDropoff, pickupPoint.getId());
        Long userShiftId = transactionTemplate.execute(status -> commandService.createUserShift(createCommand));
        testUserHelper.openShift(user, userShiftId);
        return userShiftId;
    }

    private MovementCommand.Create buildCreateMovmentCommand() {
        return MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseRepository.saveAndFlush(new OrderWarehouse(
                        LOGISTICPOINT_ID_FOR_PICKUP_DROPOFF,
                        "corp",
                        new OrderWarehouseAddress("abc", "cde", "efg", "ads", "asd", "12", "2", "1",
                                1, BigDecimal.ONE, BigDecimal.ONE),
                        Map.of(),
                        List.of(),
                        "Спросить старшего",
                        "Иван Дропшипов")))
                .build();
    }

    private UserShiftCommand.Create buildCreateUserShiftCommand(Shift shift, User user,
                                                                Movement movementDropoffReturn,
                                                                Long pickupPointId) {

        return UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoint(helper.taskDropOff(movementDropoffReturn.getId(), pickupPointId, false))
                .build();
    }
}
