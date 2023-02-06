package ru.yandex.market.tpl.core.domain.transferact;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
class TransferActRepositoryTest extends TplAbstractTest {

    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final TransferActRepository transferActRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Test
    void testSave() {
        var task = getTask();

        TransferAct transferAct = new TransferAct();
        transferAct.setStatus(TransferActStatus.CREATION);
        transferAct.setDirection(TransferActDirection.PROVIDER);
        transferAct.setTask(task);
        transferAct.setExternalId("test-01");
        transferAct = transferActRepository.save(transferAct);

        var transferActPersisted = transferActRepository.findById(transferAct.getId()).orElseThrow();

        assertThat(transferActPersisted.getId()).isNotEqualTo(null);
        assertTrue(transferActPersisted.getCreatedAt() != null);
        assertTrue(transferActPersisted.getUpdatedAt() != null);
        assertThat(transferActPersisted.getStatus()).isEqualTo(transferAct.getStatus());
        assertThat(transferActPersisted.getDirection()).isEqualTo(transferAct.getDirection());
        assertThat(transferActPersisted.getTask().getId()).isEqualTo(transferAct.getTask().getId());
        assertThat(transferActPersisted.getExternalId()).isEqualTo(transferAct.getExternalId());
    }

    private OrderPickupTask getTask() {
        User user = testUserHelper.findOrCreateUser(35236L);
        Shift shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);

        return transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findById(userShiftId).orElseThrow();
            return userShift.streamPickupTasks().findAny().orElseThrow();
        });
    }

}
