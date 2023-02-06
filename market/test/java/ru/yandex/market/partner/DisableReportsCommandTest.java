package ru.yandex.market.partner;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.DisabledAsyncReportService;
import ru.yandex.market.terminal.TestTerminal;

public class DisableReportsCommandTest extends FunctionalTest {

    @Autowired
    private DisabledAsyncReportService disabledAsyncReportService;

    @Autowired
    private DisableReportsCommand command;

    @Test
    @DbUnitDataSet(
            before = "DisableReportsCommandTest.empty.csv",
            after = "DisableReportsCommandTest.singlereport.after.csv"
    )
    public void shouldDisableSingeReport() {
        command.executeCommand(new CommandInvocation("disable-reports", new String[]{"add", "DSBS_ORDERS", "all"}, Map.of()), new TestTerminal());
    }

    @Test
    @DbUnitDataSet(
            before = "DisableReportsCommandTest.empty.csv",
            after = "DisableReportsCommandTest.singlepartner.after.csv"
    )
    public void shouldDisableSingeReportForPartners() {
        command.executeCommand(new CommandInvocation("disable-reports", new String[]{"add", "DSBS_ORDERS", "12345678"}, Map.of()), new TestTerminal());
    }

    @Test
    @DbUnitDataSet(
            before = "DisableReportsCommandTest.enableall.before.csv",
            after = "DisableReportsCommandTest.enableall.after.csv"
    )
    public void shouldEnableSingleReportForAllPartners() {
        command.executeCommand(new CommandInvocation("disable-reports", new String[]{"remove", "FULFILLMENT_ORDERS", "all"}, Map.of()), new TestTerminal());
    }

    @Test
    @DbUnitDataSet(
            before = "DisableReportsCommandTest.enablesingle.before.csv",
            after = "DisableReportsCommandTest.enablesingle.after.csv"
    )
    public void shouldEnableSingeReportForPartner() {
        command.executeCommand(new CommandInvocation("disable-reports", new String[]{"remove", "DSBS_ORDERS", "12345678"}, Map.of()), new TestTerminal());
    }

}
