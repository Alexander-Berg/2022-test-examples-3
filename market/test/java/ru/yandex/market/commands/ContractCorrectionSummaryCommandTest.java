package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.SingleFileCsvProducer;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.core.summary.ContractCorrectionSummaryDao;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractCorrectionSummaryCommandTest extends FunctionalTest {
    private CommandInvocation commandInvocation;
    private Terminal terminal;
    private ContractCorrectionSummaryCommand command;

    @Autowired
    private ContractCorrectionSummaryDao summaryDao;

    @Autowired
    Clock clock;

    @BeforeEach
    void setUp() {
        commandInvocation = mock(CommandInvocation.class);
        when(commandInvocation.getArgument(anyInt())).thenReturn("500");

        terminal = mock(Terminal.class);
        when(terminal.getWriter()).thenReturn(mock(PrintWriter.class));

        when(clock.instant()).thenReturn(SingleFileCsvProducer.Functions.truncedsysdate(0).toInstant());

        command = new ContractCorrectionSummaryCommand(summaryDao);
    }

    @Test
    @DbUnitDataSet(
            before = "SupplierCorrectionSummaryCommandTest.before.csv",
            after = "SupplierCorrectionSummaryCommandTest.after.csv"
    )
    void doJob() {
        command.executeCommand(commandInvocation, terminal);
    }
}
