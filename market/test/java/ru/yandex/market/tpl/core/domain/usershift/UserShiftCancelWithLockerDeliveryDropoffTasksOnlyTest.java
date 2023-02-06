package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestTplDropoffFactory;

@RequiredArgsConstructor
public class UserShiftCancelWithLockerDeliveryDropoffTasksOnlyTest extends TplAbstractTest {

    private final TestTplDropoffFactory testTplDropoffFactory;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final UserShiftCommandService commandService;
    private final UserShiftManager userShiftManager;
    private final TransactionTemplate transactionTemplate;

    private UserShift userShift;
    private PickupPoint pickupPoint;
    private Movement directMovement;

    @BeforeEach
    void setUp() {
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );
        clearAfterTest(pickupPoint);
        var user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), 12345L);
        userShift = testUserHelper.createEmptyShift(user, shift);

        directMovement = testTplDropoffFactory.generateDirectMovement(shift, 1L, pickupPoint);

    }

    @Test
    void shouldNotCancelUserShift_whenLockerDeliveryDropoutTasksOnly_FinishedSuccessfully() {
        //given
        testTplDropoffFactory.addDropoffTask(userShift, directMovement, null, pickupPoint);
        commandService.startShift(userShift.getUser(), new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishAllDeliveryTasks(userShift.getId());


        //then
        Assertions.assertThat(userShiftManager.findShiftsToClose(userShift.getShift().getShiftDate(),
                Instant.now().plusSeconds(1))).isEmpty();
    }

    @Test
    void shouldCancelUserShift_whenLockerDeliveryDropoffTasksOnly_Cancel() {
        //given
        transactionTemplate.execute(st -> {
            var task = testTplDropoffFactory.addDropoffTask(userShift, directMovement, null, pickupPoint);
            commandService.startShift(userShift.getUser(), new UserShiftCommand.Start(userShift.getId()));
            testUserHelper.finishDelivery(task.getRoutePoint(), true);
            return 1;
        });

        //then
        Assertions.assertThat(userShiftManager.findShiftsToClose(userShift.getShift().getShiftDate(),
                Instant.now().plusSeconds(1))).hasSize(1);
    }
}
