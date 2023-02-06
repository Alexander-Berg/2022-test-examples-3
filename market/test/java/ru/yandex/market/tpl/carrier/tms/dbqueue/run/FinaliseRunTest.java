package ru.yandex.market.tpl.carrier.tms.dbqueue.run;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.price_control.PriceStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@TmsIntTest
public class FinaliseRunTest {

    private final DbQueueTestUtil dbQueueTestUtil;

    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;

    private final UserShiftCommandService userShiftCommandService;
    private final RunCommandService runCommandService;
    private final RunRepository runRepository;

    private UserShift userShift;
    private User user;

    @BeforeEach
    void setUp() {
        Run run = runGenerator.generate();
        user = testUserHelper.findOrCreateUser(1L);
        Transport transport = testUserHelper.findOrCreateTransport();

        userShift = runHelper.assignUserAndTransport(run, user, transport);
    }

    @Test
    void shouldCreateRunFinalisation() {
        userShiftCommandService.closeUserShift(new UserShiftCommand.ForceClose(userShift.getId()));

        dbQueueTestUtil.assertQueueHasSize(QueueType.RUN_FINALISATION, 1);
    }

    @Test
    void shouldUpdateEstimateTime() {
        runCommandService.finaliseRun(new RunCommand.Finalise(userShift.getRunId()));
        Assertions.assertEquals(
                PriceStatus.READY_FOR_PENALTY,
                runRepository.findById(userShift.getRunId()).get().getPriceStatus()
        );
    }
}
