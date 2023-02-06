package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тесты для {@link YaCrmSpaceSyncCommand}.
 */
@DbUnitDataSet(before = "YaCrmSpaceSyncCommandTest/data.before.csv")
class YaCrmSpaceSyncCommandTest extends FunctionalTest {

    @Autowired
    private YaCrmSpaceSyncCommand command;

    @Test
    @DbUnitDataSet(after = "YaCrmSpaceSyncCommandTest/testAll.after.csv")
    @DisplayName("Проверить удаление всей информации о синхронизации с CRM Space")
    void testAll() {
        executeCommand("all");
    }

    @Test
    @DbUnitDataSet(after = "YaCrmSpaceSyncCommandTest/testClientIds.after.csv")
    @DisplayName("Проверить точечное (по клиентам) удаление информации о синхронизации с CRM Space")
    void testClientIds() {
        executeCommand("1, 3");
    }


    private void executeCommand(final String argument) {
        final CommandInvocation commandInvocation = commandInvocation(argument);
        final Terminal terminal = createTerminal();
        command.executeCommand(commandInvocation, terminal);
    }

    private CommandInvocation commandInvocation(final String argument) {
        return new CommandInvocation("sync-with-crm-space", new String[]{argument}, Collections.emptyMap());
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        return terminal;
    }

}
