package ru.yandex.market.supplier.command;

import java.io.PrintWriter;
import java.util.Collections;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.Mockito.when;

/**
 * Тесты для {@link OverdraftControlWhitelistReportCommand}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class OverdraftControlWhitelistReportCommandTest extends FunctionalTest {

    private static final CommandInvocation CMD_CALL_WITH_CLIENTS = new CommandInvocation(
            "nvm",
            new String[]{"456", "789"},
            Collections.emptyMap()
    );

    @Mock
    private Terminal terminal;

    @Autowired
    private OverdraftControlWhitelistReportCommand command;

    @BeforeEach
    private void beforeEach() {
        when(terminal.getWriter())
                .thenReturn(Mockito.mock(PrintWriter.class));
    }

    @DbUnitDataSet(after = "OverdraftControlWhitelistReportCommandTest.after.csv")
    @Test
    void test_executeCommand() {
        command.executeCommand(CMD_CALL_WITH_CLIENTS, terminal);
    }

}