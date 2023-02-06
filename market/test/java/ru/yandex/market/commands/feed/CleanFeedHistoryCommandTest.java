package ru.yandex.market.commands.feed;

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
 * Функциольный тест для {@link CleanFeedHistoryCommand}
 */
class CleanFeedHistoryCommandTest extends FunctionalTest {

    @Autowired
    private CleanFeedHistoryCommand cleanFeedHistoryCommand;
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
            before = "CleanFeedHistoryCommandTest/test.before.csv",
            after = "CleanFeedHistoryCommandTest/test.after.csv"
    )
    void testExecuteCommand() {
        var commandInvocation = new CommandInvocation(
                "clean-feed-history",
                new String[] {"906724", "862847"},
                Collections.emptyMap()
        );

        cleanFeedHistoryCommand.executeCommand(commandInvocation, terminal);
    }
}
