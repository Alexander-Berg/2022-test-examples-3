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

/**
 * @author imelnikov
 * @since 14.03.2022
 */
public class DirectFeatureCommandTest extends FunctionalTest {

    @Autowired
    private DirectFeatureCommand directFeatureCommand;

    @Test
    @DbUnitDataSet(
            before = "DirectFeatureCommand.before.csv",
            after = "DirectFeatureCommand.after.csv"
    )
    void executeCommand() {
        executeCommand("10", "2004");
    }


    private void executeCommand(String... args) {
        directFeatureCommand.executeCommand(
                new CommandInvocation("direct-feature-state", args, Collections.emptyMap()),
                createTerminal());
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }
}
