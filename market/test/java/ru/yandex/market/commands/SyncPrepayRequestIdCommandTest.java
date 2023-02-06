package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "SyncPrepayRequestIdCommandTest.csv")
public class SyncPrepayRequestIdCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;
    @Autowired
    private SyncPrepayRequestIdCommand tested;

    @BeforeEach
    private void beforeEach() {
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    @DbUnitDataSet(after = "SyncPrepayRequestIdCommandTest.after.csv")
    void testExecuteCommand() {
        CommandInvocation commandInvocation = new CommandInvocation("sync-prepay-request-ids",
                new String[]{},
                Collections.emptyMap());
        tested.executeCommand(commandInvocation, terminal);
    }
}
