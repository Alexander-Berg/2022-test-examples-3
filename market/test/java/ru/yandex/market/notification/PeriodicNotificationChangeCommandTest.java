package ru.yandex.market.notification;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.shop.FunctionalTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.mockito.Mockito.*;

public class PeriodicNotificationChangeCommandTest extends FunctionalTest {

    @Autowired
    private PeriodicNotifierChangeCommand command;

    @Autowired
    private PeriodicNotificationDao periodicNotificationDao;

    private void runCommand(final String... args) {
        final CommandInvocation invocation = new CommandInvocation(command.getNames()[0], args, Map.of());
        final Terminal terminal = mock(Terminal.class, withSettings().defaultAnswer(RETURNS_MOCKS));
        command.executeCommand(invocation, terminal);
    }

    @Test
    void testChange() {
        runCommand("UCHiddenOffersNotification",
                String.valueOf(Instant.now().plus(5, ChronoUnit.HOURS).toEpochMilli()));
        Assertions.assertThat(periodicNotificationDao.getTemplatesWithNextNotificationTimeAfter(Instant.now()))
                .containsExactly("UCHiddenOffersNotification");
    }

}
