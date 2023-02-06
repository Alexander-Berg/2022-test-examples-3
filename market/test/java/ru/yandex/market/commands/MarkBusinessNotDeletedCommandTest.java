package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Тесты для {@link  MarkBusinessNotDeletedCommand}.
 */
@DbUnitDataSet(before = "MarkBusinessNotDeletedCommand/testCommand.before.csv")
public class MarkBusinessNotDeletedCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;
    @Autowired
    private MarkBusinessNotDeletedCommand cmd;
    @Autowired
    private PrintWriter printWriter;

    @BeforeEach
    private void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(after = "MarkBusinessNotDeletedCommand/markNotDeleted.after.csv")
    void markNotDeleted() {
        CommandInvocation commandInvocation = new CommandInvocation("mark-business-not-deleted",
                new String[]{"1234"},
                Collections.emptyMap());
        cmd.executeCommand(commandInvocation, terminal);
    }


}
