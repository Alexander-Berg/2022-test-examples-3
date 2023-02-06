package ru.yandex.market.rg.asyncreport;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.DisabledAsyncReportService;
import ru.yandex.market.core.asyncreport.ReportsDao;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.ReportsServiceSettings;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.rg.config.FunctionalTest;

class ReportsCommandTest extends FunctionalTest {

    @Autowired
    private ReportsDao<ReportsType> reportsDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DisabledAsyncReportService disabledAsyncReportService;

    @Autowired
    private EnvironmentService environmentService;

    private ReportsCommand reportsCommand;

    @BeforeEach
    void setUp() {
        reportsCommand = new ReportsCommand(new ReportsService(
                new ReportsServiceSettings.Builder<ReportsType>().setReportsQueueLimit(10).build(),
                reportsDao,
                transactionTemplate,
                () -> "10",
                Clock.fixed(
                        DateTimes.toInstantAtDefaultTz(2019, 10, 24, 10, 0, 0),
                        ZoneId.systemDefault()
                ),
                disabledAsyncReportService,
                environmentService
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "DeleteReportsCommandTest.before.csv", after = "DeleteReportsCommandTest.specific.after.csv"
    )
    void testDeleteSpecificReports() {
        executeCommand("delete", "1", "11");
    }

    private void executeCommand(final String... commandArguments) {
        final CommandInvocation commandInvocation = commandInvocation(commandArguments);
        final Terminal terminal = createTerminal();

        reportsCommand.executeCommand(commandInvocation, terminal);
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }

    private CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("reports", args, Collections.emptyMap());
    }
}
