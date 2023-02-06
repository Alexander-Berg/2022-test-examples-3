package ru.yandex.market.commands.feed;

import java.io.PrintWriter;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Функциональный тест для {@link ManageGenerationCommand}
 */
class ManageGenerationCommandTest extends FunctionalTest {

    @Autowired
    private ManageGenerationCommand manageGenerationCommand;
    @Autowired
    private Terminal terminal;
    @Autowired
    private PrintWriter printWriter;

    @BeforeEach
    void init() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(
            before = "ManageGenerationCommandTest/testMarkGenerationToImport.before.csv",
            after = "ManageGenerationCommandTest/testMarkGenerationToImport.after.csv"
    )
    void testMarkGenerationToImport() {
        var commandInvocation = new CommandInvocation(
              "mark-generation-to-import",
                new String[] {"1", "2"},
                Map.of()
        );
        manageGenerationCommand.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DbUnitDataSet(
            before = "ManageGenerationCommandTest/testMarkGenerationToSkip.before.csv",
            after = "ManageGenerationCommandTest/testMarkGenerationToSkip.after.csv"
    )
    void testMarkGenerationToSkip() {
        var commandInvocation = new CommandInvocation(
                "mark-generation-to-skip",
                new String[] {"3"},
                Map.of()
        );
        manageGenerationCommand.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DbUnitDataSet(
            before = "ManageGenerationCommandTest/testMarkGenerationAsImported.before.csv",
            after = "ManageGenerationCommandTest/testMarkGenerationAsImported.after.csv"
    )
    void testMarkGenerationAsImported() {
        var commandInvocation = new CommandInvocation(
                "mark-generation-as-imported",
                new String[] {"2"},
                Map.of()
        );
        manageGenerationCommand.executeCommand(commandInvocation, terminal);
    }
}
