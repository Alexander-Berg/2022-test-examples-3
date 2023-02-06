package ru.yandex.market.tpl.core.query.usershift;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.task.TaskStateDto;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class TaskQueryServiceTest extends TplAbstractTest {

    private final TaskQueryService taskQueryService;
    private final TestUserHelper testUserHelper;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    private User user;
    private UserShift userShift;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .active(true)
                .build();

        userShift = userShiftRepository.findByIdOrThrow(userShiftCommandService.createUserShift(createCommand));
    }

    @Test
    void getTaskStatus() {
        var task = transactionTemplate.execute(
                status -> userShiftRepository.getOne(userShift.getId()).streamPickupTasks().findFirst()
                        .orElseThrow(() -> new TplEntityNotFoundException("OrderPickupTask", "any"))
        );
        TaskStateDto taskState = taskQueryService.getTaskState(task.getId());

        assertThat(taskState).isNotNull();
        assertThat(taskState.getId()).isEqualTo(task.getId());
        assertThat(taskState.getStatus()).isEqualTo(task.getStatus().name());
    }

}
