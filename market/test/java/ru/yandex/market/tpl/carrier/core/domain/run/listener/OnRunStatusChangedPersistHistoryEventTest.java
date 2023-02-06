package ru.yandex.market.tpl.carrier.core.domain.run.listener;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.history.RunHistoryEvent;
import ru.yandex.market.tpl.carrier.core.domain.run.history.RunHistoryRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@CoreTestV2
public class OnRunStatusChangedPersistHistoryEventTest {

    private final UserShiftCommandService userShiftCommandService;
    private final TestUserHelper testUserHelper;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final RunHistoryRepository runHistoryRepository;
    private final RunCommandService runCommandService;

    @SneakyThrows
    @Test
    void shouldSaveRunStatusChangedEvent() {
        User user = testUserHelper.findOrCreateUser(123L);
        Transport transport = testUserHelper.findOrCreateTransport();

        Run run = runGenerator.generate();
        RunHistoryEvent event = runHistoryRepository.findLastByRunId(run.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.CREATED, event.getStatusAfter());

        runCommandService.confirm(new RunCommand.Confirm(run.getId()));
        event = runHistoryRepository.findLastByRunId(run.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.CONFIRMED, event.getStatusAfter());

        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        event = runHistoryRepository.findLastByRunId(run.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.ASSIGNED, event.getStatusAfter());

        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        event = runHistoryRepository.findLastByRunId(run.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.STARTED, event.getStatusAfter());

        userShiftCommandService.closeUserShift(new UserShiftCommand.ForceClose(userShift.getId()));
        event = runHistoryRepository.findLastByRunId(run.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.COMPLETED, event.getStatusAfter());

        List<RunHistoryEvent> events = runHistoryRepository.findByRunId(run.getId());
        Assertions.assertEquals(5, events.size());
    }
}
