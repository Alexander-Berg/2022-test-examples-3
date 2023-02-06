package ru.yandex.market.outlet.tms;

import java.io.PrintWriter;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.outlet.tms.ManageOutletsCommand.COMMAND_NAME;

/**
 * Тесты для синхронизации с Nesu через TMS-команду manage-outlets.
 * @author Vladislav Bauer
 */
class ManageOutletsSyncNesuCommandTest extends AbstractNesuSyncTest {

    @Autowired
    private ManageOutletsCommand command;

    @Autowired
    private Terminal terminal;

    @Autowired
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(
            before = "SyncOutletsWithNesuTest.resetAll.before.csv",
            after = "SyncOutletsWithNesuTest.resetAll.after.csv"
    )
    void testResetAll() {
        runCommand("sync-nesu", "reset-all");
        verifyNoInteractions(nesuClient);
    }

    @Override
    protected final void runProcess() {
        runCommand("sync-nesu", "1");
    }

    private void runCommand(final String... args) {
        command.executeCommand(new CommandInvocation(COMMAND_NAME, args, Map.of()), terminal);
    }

}
