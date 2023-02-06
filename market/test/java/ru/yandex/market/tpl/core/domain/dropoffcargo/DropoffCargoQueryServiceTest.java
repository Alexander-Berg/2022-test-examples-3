package ru.yandex.market.tpl.core.domain.dropoffcargo;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseSchedule;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestTplDropoffFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor
class DropoffCargoQueryServiceTest extends TplAbstractTest {

    public static final String DROPOFF_CARGO_BARCODE = "barcode";
    private final DropoffCargoCommandService dropoffCargoCommandService;
    private final DropoffCargoQueryService dropoffCargoQueryService;
    private final DropoffCargoRepository dropoffCargoRepository;
    private final MovementGenerator movementGenerator;
    private final AddressGenerator addressGenerator;
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final TestTplDropoffFactory testTplDropoffFactory;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;
    private final UserShiftRepository userShiftRepository;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftCommandService commandService;

    private UserShift userShift;
    private Movement directMovement;

    @Test
    void getAllByMovementIdForPickup_whenExists() {
        //given
        String expectedLogisticPointTo = "logisticPointTo";
        addDropoffCargo(expectedLogisticPointTo);
        Movement movement = addMovement(expectedLogisticPointTo);

        //when
        List<DropoffCargo> allByMovementIdForPickup =
                dropoffCargoQueryService.getAllByMovementIdForPickup(movement.getId());

        //then
        assertThat(allByMovementIdForPickup).hasSize(1);

        assertEquals(expectedLogisticPointTo, allByMovementIdForPickup.iterator().next().getLogisticPointIdTo());
    }

    @Test
    void getAllByMovementIdForPickup_whenEmpty() {
        //given
        String expectedLogisticPointTo = "logisticPointTo";
        Movement movement = addMovement(expectedLogisticPointTo);

        //when
        List<DropoffCargo> allByMovementIdForPickup =
                dropoffCargoQueryService.getAllByMovementIdForPickup(movement.getId());

        //then
        assertThat(allByMovementIdForPickup).hasSize(0);
    }

    @Test
    void getAllByMovementIdForPickup_whenEmptyForMovement() {
        //given
        String expectedLogisticPointTo = "logisticPointTo";
        addDropoffCargo("anotherPointId");
        Movement movement = addMovement(expectedLogisticPointTo);

        //when
        List<DropoffCargo> allByMovementIdForPickup =
                dropoffCargoQueryService.getAllByMovementIdForPickup(movement.getId());

        //then
        assertThat(allByMovementIdForPickup).hasSize(0);
    }

    @Test
    void getAllByMovementIdForPickup_whenAnotherStatus() {
        //given
        String expectedLogisticPointTo = "logisticPointTo";
        addDropoffCargo(expectedLogisticPointTo);

        DropoffCargo dropoff =
                dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(DROPOFF_CARGO_BARCODE).orElseThrow();
        dropoff.setStatus(DropoffCargoFlowStatus.CANCELLED);
        dropoffCargoRepository.save(dropoff);

        Movement movement = addMovement(expectedLogisticPointTo);

        //when
        List<DropoffCargo> allByMovementIdForPickup =
                dropoffCargoQueryService.getAllByMovementIdForPickup(movement.getId());

        //then
        assertThat(allByMovementIdForPickup).hasSize(0);
    }

    @Test
    void getAllByMovementIdForPickup_ByReference() {
        //given
        preperaUserShiftWithDropshipTasks();
        var cargo1 = testTplDropoffFactory.generateCargo("multy-1", "multy", directMovement);
        var cargo2 = testTplDropoffFactory.generateCargo("multy-2", "multy", directMovement);

        //when
        var allCargosByReference = dropoffCargoQueryService.getDropoffForPickupByReference(
                userShift.getId(), Set.of(cargo1.getReferenceId()));

        //then
        assertThat(allCargosByReference).containsExactlyInAnyOrder(cargo1, cargo2);
    }

    @Test
    void getAllByMovementIdForPickup_ByReferenceAndDeleted() {
        //given
        preperaUserShiftWithDropshipTasks();
        var cargo1 = testTplDropoffFactory.generateCargo("multy-1", "multy", directMovement);
        var cargo2 = testTplDropoffFactory.generateCargo("multy-2", "multy", directMovement);
        dropoffCargoCommandService.markDeleted(DropoffCargoCommand.UpdateOrSkipDeleted.of(
                cargo1.getId(), 1L, true
        ));
        //when
        var allCargosByReference = dropoffCargoQueryService.getDropoffForPickupByReference(
                userShift.getId(), Set.of(cargo1.getReferenceId()));

        //then
        assertThat(allCargosByReference).containsExactlyInAnyOrder(cargo2);
    }


    @Test
    void getAllByMovementIdForPickup_ByReferenceAndDeleted_whenNull() {
        //given
        preperaUserShiftWithDropshipTasks();
        DropoffCargo entity = new DropoffCargo();
        entity.setIsDeleted(null);
        entity.setReferenceId("multy");
        entity.setBarcode("multy-1");
        entity.setLogisticPointIdTo(directMovement.getWarehouseTo().getYandexId());
        entity.setLogisticPointIdFrom(directMovement.getWarehouse().getYandexId());
        entity.setStatus(DropoffCargoFlowStatus.CREATED);
        var cargo1 = dropoffCargoRepository.save(entity);

        entity = new DropoffCargo();
        entity.setIsDeleted(null);
        entity.setReferenceId("multy");
        entity.setBarcode("multy-2");
        entity.setLogisticPointIdTo(directMovement.getWarehouseTo().getYandexId());
        entity.setLogisticPointIdFrom(directMovement.getWarehouse().getYandexId());
        entity.setStatus(DropoffCargoFlowStatus.CREATED);
        var cargo2 = dropoffCargoRepository.save(entity);

        dropoffCargoCommandService.markDeleted(DropoffCargoCommand.UpdateOrSkipDeleted.of(
                cargo1.getId(), 1L, true
        ));
        //when
        var allCargosByReference = dropoffCargoQueryService.getDropoffForPickupByReference(
                userShift.getId(), Set.of(cargo1.getReferenceId()));

        //then
        assertThat(allCargosByReference).containsExactlyInAnyOrder(cargo2);
    }

    @Test
    void getAllByMovementIdForPickup_byBarcodes() {
        //given
        preperaUserShiftWithDropshipTasks();
        var cargo1 = testTplDropoffFactory.generateCargo("multy-1", "multy", directMovement);
        var cargo2 = testTplDropoffFactory.generateCargo("multy-2", "multy", directMovement);

        //when
        var allCargosByReference = dropoffCargoQueryService.getDropoffForPickup(
                userShift.getId(), Set.of(cargo1.getBarcode()));

        //then
        assertThat(allCargosByReference).containsExactlyInAnyOrder(cargo1);
    }


    @Test
    void getAllByMovementIdForPickup_byBarcodes_empty() {
        //given
        preperaUserShiftWithDropshipTasks();
        var cargo1 = testTplDropoffFactory.generateCargo("multy-1", "multy", directMovement);
        var cargo2 = testTplDropoffFactory.generateCargo("multy-2", "multy", directMovement);

        //when
        var allCargosByReference = dropoffCargoQueryService.getDropoffForPickup(
                userShift.getId(), Set.of(cargo1.getReferenceId()));

        //then
        assertThat(allCargosByReference).isEmpty();
    }

    private void preperaUserShiftWithDropshipTasks() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );
        clearAfterTest(pickupPoint);
        var user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), 12345L);
        userShift = testUserHelper.createEmptyShift(user, shift);

        directMovement = testTplDropoffFactory.generateDirectMovement(shift, 1L, pickupPoint);
        testTplDropoffFactory.addDropoffTask(userShift, directMovement, null, pickupPoint);

        transactionTemplate.execute(st -> {
            commandService.startShift(userShift.getUser(), new UserShiftCommand.Start(userShift.getId()));
            testUserHelper.arriveAtRoutePoint(
                    userShiftRepository.findByIdOrThrow(userShift.getId())
                            .streamDeliveryRoutePoints().findFirst().orElseThrow());
            return 1;
        });
    }

    private DropoffCargo addDropoffCargo(String logisticPointTo) {
        return dropoffCargoCommandService.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(DROPOFF_CARGO_BARCODE)
                        .logisticPointIdFrom("fakeIdFrom")
                        .logisticPointIdTo(logisticPointTo)
                        .build());
    }

    private Movement addMovement(String logisticPointTo) {
        return movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouseTo(orderWarehouseRepository.saveAndFlush(new OrderWarehouse(
                        logisticPointTo,
                        "corp",
                        addressGenerator.generateWarehouseAddress(
                                AddressGenerator.AddressGenerateParam.builder()
                                        .build()
                        ),
                        Arrays.stream(DayOfWeek.values())
                                .collect(Collectors.toMap(
                                        d -> d,
                                        d -> new OrderWarehouseSchedule(
                                                d,
                                                OffsetTime.of(LocalTime.of(9, 27), DateTimeUtil.DEFAULT_ZONE_ID),
                                                OffsetTime.of(LocalTime.of(10, 27), DateTimeUtil.DEFAULT_ZONE_ID)
                                        )
                                ))
                        ,
                        List.of(), null, null)))
                .build());
    }
}
