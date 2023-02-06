package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

public class ChangeFeatureCommandTest  extends FunctionalTest {

    @Autowired
    private ChangeFeatureCommand changeFeatureCommand;

    @Test
    @DbUnitDataSet(
            before = "ChangeFeatureCommandTest/ChangeFeatureCommandSupplier.before.csv",
            after = "ChangeFeatureCommandTest/ChangeFeatureCommandSupplier.after.csv"
    )
    void executeCommandSupplier() {
        executeCommand("10", "DROPSHIP", "DONT_WANT");
    }

    @Test
    @DbUnitDataSet(
            before = "ChangeFeatureCommandTest/ChangeFeatureCommandShop.before.csv",
            after = "ChangeFeatureCommandTest/ChangeFeatureCommandShop.after.csv"
    )
    void executeCommandShop() {
        executeCommand("11", "CASHBACK", "NEW");
    }

    private void executeCommand(final String... commandArguments) {
        final CommandInvocation commandInvocation = commandInvocation(commandArguments);
        final Terminal terminal = createTerminal();

        changeFeatureCommand.executeCommand(commandInvocation, terminal);
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        return terminal;
    }

    private CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("change-feature", args, Collections.emptyMap());
    }

}
