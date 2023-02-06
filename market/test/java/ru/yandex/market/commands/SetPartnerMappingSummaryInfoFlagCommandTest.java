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

public class SetPartnerMappingSummaryInfoFlagCommandTest extends FunctionalTest {
    @Autowired
    private SetPartnerMappingSummaryInfoFlagCommand setPartnerMappingSummaryInfoFlagCommand;

    @Test
    @DbUnitDataSet(
            before = "SetPartnerMappingSummaryInfoFlagCommandTest.testUpdateFlags.before.csv",
            after = "SetPartnerMappingSummaryInfoFlagCommandTest.testUpdateFlags.after.csv"
    )
    @DisplayName("Проверяет обновление флагов")
    void testUpdateFlags() {
        executeCommand("201", "1", "1", "1");
    }

    private void executeCommand(String... commandArguments) {
        final CommandInvocation commandInvocation = commandInvocation(commandArguments);
        final Terminal terminal = createTerminal();

        setPartnerMappingSummaryInfoFlagCommand.executeCommand(commandInvocation, terminal);
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }

    private static CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("set-partner-summary-info-flags", args, Collections.emptyMap());
    }
}
